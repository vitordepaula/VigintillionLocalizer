package br.inatel.hackathon.vigintillionlocalizer.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.inatel.hackathon.vigintillionlocalizer.R;
import br.inatel.hackathon.vigintillionlocalizer.database.DB;
import br.inatel.hackathon.vigintillionlocalizer.fragments.BluetoothScannerFragment;
import br.inatel.hackathon.vigintillionlocalizer.fragments.LaunchFragment;
import br.inatel.hackathon.vigintillionlocalizer.fragments.MapFragment;
import br.inatel.hackathon.vigintillionlocalizer.model.TrackedBeacon;

import static com.mongodb.client.model.Filters.geoWithinCenter;

public class MainActivity extends FragmentActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Set this to "true" to be able to add example tags and show them on map without using real RF tags
    public static final boolean TEST_MODE = false;

    private static final String TAG = MainActivity.class.getSimpleName();
    private final List<TrackedBeacon> mBeaconToTrackList = new LinkedList<>();
    final private HashMap<String, ScannerEntry> mScanners = new HashMap<>();
    private final HashMap<TrackedBeacon, LatLng> mBeaconLocationResults = new HashMap<>();
    private BluetoothScannerFragment mBluetoothScanner;
    private LocationRequest mLocationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000);
    private GoogleApiClient mGoogleApiClient;
    private boolean mLaunchScreenShowing = true;
    private boolean mRequestingLocationUpdates = false;
    private Location mCurrentLocation;
    private MapFragment mMapFragment;
    private MongoCollection<Document> mSensorsCollection = null;
    private Handler mUiHandler;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundHandlerThread;
    private DB mDb;
    private Runnable mBackgroundInitializationTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Connecting to Database");
            MongoClientURI uri = new MongoClientURI("mongodb://vps.de.paula.nom.br/vigintillion");
            MongoClient mongoClient = new MongoClient(uri);
            MongoDatabase db = mongoClient.getDatabase(uri.getDatabase());
            mSensorsCollection = db.getCollection("sensors.ble");
            mBluetoothScanner.initialize();
        }
    };
    private boolean mScannerLocationUpdated = false;
    private Runnable mUpdateMapTask = new Runnable() {
        @Override
        public void run() {
            if (mMapFragment.isVisible())
                mMapFragment.updateMap();
        }
    };
    private Runnable mFetchSensorsFromDatabaseTask = new Runnable() {
        @Override
        public void run() {
            if (mSensorsCollection == null) {
                Log.w(TAG, "Could not query DB: not connected.");
                return;
            }
            Log.d(TAG, "Querying data from DB");
            synchronized (mScanners) {
                mScanners.clear();
                Bson filter = geoWithinCenter("loc", mCurrentLocation.getLongitude(), mCurrentLocation.getLatitude(), 300.0); // 300m radius
                mSensorsCollection.find(filter).forEach(new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        try {
                            JSONObject json = new JSONObject(document.toJson());
                            JSONArray coord = json.getJSONObject("loc").getJSONArray("coordinates");
                            mScanners.put(json.getString("id"), new ScannerEntry(
                                    new LatLng(coord.getDouble(1), coord.getDouble(0)),
                                    json.getString("ip"), json.getInt("port")));
                        } catch (JSONException je) {
                            Log.w(TAG, "Error reading JSON format from MongoDB");
                            // skip
                        }
                    }
                });
                Log.d(TAG, "We got " + mScanners.size() + " scanners");
            }
            mUiHandler.post(mUpdateMapTask);
        }
    };
    private boolean mBeaconLoopTaskRunning = true;
    private ExecutorService mBeaconRequestScheduler;
    private Runnable mBeaconQueryingMainLoopTask = new Runnable() {
        @Override
        public void run() {
            List<TrackedBeacon> mBeacons = new LinkedList<>();
            while (mBeaconLoopTaskRunning) {
                synchronized (mBeaconToTrackList) {
                    mBeacons.clear();
                    mBeacons.addAll(mBeaconToTrackList);
                }
                for (TrackedBeacon beacon : mBeacons) {
                    mBeaconRequestScheduler.submit(new BeaconRequestTask(beacon));
                    try {
                        Thread.sleep(100, 0);
                    } catch (InterruptedException i) {
                        mBeaconLoopTaskRunning = false;
                    }
                    if (!mBeaconLoopTaskRunning)
                        break;
                }
                if (mBeaconLoopTaskRunning) try {
                    Thread.sleep(5000, 0);
                } catch (InterruptedException i) {
                    mBeaconLoopTaskRunning = false;
                }
            }
            Log.d(TAG, "Beacon Querying Main Loop Task Terminated");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUiHandler = new Handler(Looper.getMainLooper());
        (mBackgroundHandlerThread = new HandlerThread("VigintillionBackgroundThread")).start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
        mBluetoothScanner = new BluetoothScannerFragment();
        mBackgroundHandler.post(mBackgroundInitializationTask);
        // Get the list of beacons to search for from the database
        mDb = new DB(this);
        refreshBeaconList();
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = MapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new LaunchFragment())
                .add(mBluetoothScanner, "BTScanner")
                .commit();
    }

    @Override
    protected void onDestroy() {
        mBackgroundHandler.getLooper().quit();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        mBeaconLoopTaskRunning = true;
        mBeaconRequestScheduler = Executors.newCachedThreadPool();
        mBeaconRequestScheduler.submit(mBeaconQueryingMainLoopTask);
    }

    @Override
    protected void onStop() {
        mBeaconLoopTaskRunning = false;
        mBeaconRequestScheduler.shutdown();
        mBluetoothScanner.terminate();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    public BluetoothScannerFragment getScanner() { return mBluetoothScanner; }

    protected void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mRequestingLocationUpdates = true;
        } catch (SecurityException se) {
            Log.w(TAG, "Security Exception: we have no permission to get location");
            mRequestingLocationUpdates = false;
        }
    }

    protected void stopLocationUpdates() {
        if (mRequestingLocationUpdates) {
            Log.d(TAG, "Stopping location updates");
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mRequestingLocationUpdates = false;
        }
    }

    private void closeLaunchScreen() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                .replace(R.id.fragmentContainer, mMapFragment)
                .commit();
        mLaunchScreenShowing = false;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, 3030);
            return;
        }
        Log.d(TAG, "We have location permissions. Let's go.");
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // do nothing
        // TODO: check this
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            startLocationUpdates();
            Log.d(TAG, "Successfully got location permission. Starting updates.");
        } catch (SecurityException se) {
            mCurrentLocation = null;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mCurrentLocation = null;
        mRequestingLocationUpdates = false;
        new AlertDialog.Builder(this)
                .setMessage(R.string.error_connecting)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        MainActivity.this.finish();
                    }
                }).create().show();
    }

    public Location getLocation() {
        return mCurrentLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        float acc = location.hasAccuracy() ? location.getAccuracy() : 999.0f;
        Log.d(TAG, "LOCATION: (" + location.getLatitude() + "," + location.getLongitude() + "), acc=" + acc);
        if (acc > 50.0) return; // discard bad values
        if (mLaunchScreenShowing)
            closeLaunchScreen();
        // only updates the scanner location if we have moved at least 15 meters
        if (mCurrentLocation == null || !mScannerLocationUpdated || mCurrentLocation.distanceTo(location) > 15.0) {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            mScannerLocationUpdated = mBluetoothScanner.updateLocation(latlng);
        }
        // update current location
        mCurrentLocation = location;
        // Obtain nearby scanners
        mBackgroundHandler.post(mFetchSensorsFromDatabaseTask);
    }

    public void postToBackgroundHandler(Runnable task) {
        mBackgroundHandler.post(task);
    }

    /*
     * Code below talk to each detected sensor periodically and ask about the tags they want
     */

    public Collection<ScannerEntry> getScannerCollection() {
        Collection<ScannerEntry> results = new LinkedList<>();
        synchronized (mScanners) {
            results.addAll(mScanners.values());
        }
        return results;
    }

    public MongoCollection<Document> getMongoSensorCollection() {
        return mSensorsCollection;
    }

    public void refreshBeaconList() {
        synchronized (mBeaconToTrackList) {
            mBeaconToTrackList.clear();
            mBeaconToTrackList.addAll(mDb.tracked_get());
        }
        synchronized (mBeaconLocationResults) {
            mBeaconLocationResults.clear();
        }
    }

    public Map<TrackedBeacon,LatLng> getBeaconLocationCalculationResults() {
        Map<TrackedBeacon,LatLng> results = new HashMap<>();
        synchronized (mBeaconLocationResults) {
            results.putAll(mBeaconLocationResults);
        }
        return results;
    }

    public class ScannerEntry {
        private LatLng mLatLng;
        private String mAddress;
        private int mPort;

        public ScannerEntry(LatLng latlng, String address, int port) {
            mLatLng = latlng;
            mAddress = address;
            mPort = port;
        }

        public LatLng getLatLng() {
            return mLatLng;
        }

        public String getAddress() {
            return mAddress;
        }

        public int getPort() {
            return mPort;
        }
    }

    // This class ask about a beacon to all nearby scanners and triangulate position if possible
    private class BeaconRequestTask implements Runnable {
        private TrackedBeacon mBeacon;
        public BeaconRequestTask(TrackedBeacon beacon) {
            mBeacon = beacon;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1500];
            float mean_lat = 0.0f;
            float mean_lon = 0.0f;
            int acc_weight = 0;
            List<ScannerEntry> mScannersToAsk = new LinkedList<>();
            synchronized (mScanners) {
                mScannersToAsk.clear();
                mScannersToAsk.addAll(mScanners.values());
            }
            for (ScannerEntry scanner : mScannersToAsk) {
                try {
                    Log.d(TAG, "Looking for Dev " + mBeacon.beacon_id);
                    URL url = new URL("http://" + scanner.getAddress() + ":" + scanner.getPort() + "/beacon?id=" + mBeacon.beacon_id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    // TODO: set accepted content type as json
                    conn.setReadTimeout(2000 /* milliseconds */);
                    conn.setConnectTimeout(5000 /* milliseconds */);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    // start the query
                    conn.connect();
                    int response = conn.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        // Convert the InputStream into a string
                        InputStream is = conn.getInputStream();
                        int len = is.read(buf);
                        String json_string = new String(buf, 0, len, "UTF-8");
                        Log.d(TAG, "Dev " + mBeacon.beacon_id + ": from " + scanner.getAddress() + ":" + scanner.getPort() + "->" + json_string);
                        // Then to JSON
                        try {
                            JSONObject obj = new JSONObject(json_string);
                            long timestamp = obj.getLong("timestamp");
                            long delay = System.currentTimeMillis() - timestamp;
                            JSONArray loc = obj.getJSONObject("loc").getJSONArray("coordinates"); // GeoJSON
                            float lat = (float) loc.getDouble(1);
                            float lon = (float) loc.getDouble(0);
                            int signal;
                            try {
                                signal = obj.getInt("signal");
                            } catch (JSONException x) {
                                signal = -80;
                            }
                            int compensated_signal = (int) (signal - Math.log(delay));
                            // Calculate running mean
                            int weight = Math.max(0, compensated_signal + 130);
                            //Log.d(TAG, "Dev " + mBeacon + " processed " + timestamp + "," + lat + "," + lon + "," + compensated_signal + "->" + weight);
                            int total_weight = acc_weight + weight;
                            mean_lat = (mean_lat * acc_weight + lat * weight) / total_weight;
                            mean_lon = (mean_lon * acc_weight + lon * weight) / total_weight;
                            acc_weight = total_weight;
                        } catch (JSONException e) {
                            // Not calculating running mean for error responses
                        }
                        conn.disconnect();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Dev " + mBeacon.beacon_id + ": Error talking to " + scanner.getAddress() + " at port " + scanner.getPort());
                }
            }
            Log.d(TAG, "Dev " + mBeacon.beacon_id + " acc " + acc_weight + ", pos_m " + mean_lat + "," + mean_lon);
            synchronized (mBeaconLocationResults) {
                if (acc_weight == 0)
                    mBeaconLocationResults.remove(mBeacon);
                else
                    mBeaconLocationResults.put(mBeacon, new LatLng(mean_lat, mean_lon));
            }
        }
    }
}
