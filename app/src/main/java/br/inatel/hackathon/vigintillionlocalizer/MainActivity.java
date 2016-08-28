package br.inatel.hackathon.vigintillionlocalizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import static com.mongodb.client.model.Filters.geoWithinCenter;

public class MainActivity extends FragmentActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothScannerFragment mBluetoothScanner;
    private LocationRequest mLocationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000);
    private GoogleApiClient mGoogleApiClient;
    private boolean mRequestingLocationUpdates = false;
    private Location mCurrentLocation;
    private MapFragment mMapFragment;
    private MongoCollection<Document> mSensorsCollection = null;
    private Handler mUiHandler;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundHandlerThread;
    final private HashMap<String,ScannerEntry> mScanners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUiHandler = new Handler(Looper.getMainLooper());
        (mBackgroundHandlerThread = new HandlerThread("VigintillionBackgroundThread")).start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
        mBackgroundHandler.post(mConnectToDatabaseTask);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mBluetoothScanner = new BluetoothScannerFragment();
        mMapFragment = MapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, mMapFragment)
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
    }

    @Override
    protected void onStop() {
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

    private Runnable mConnectToDatabaseTask = new Runnable() {
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

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
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
    }

    public Location getLocation() {
        return mCurrentLocation;
    }

    private boolean mScannerLocationUpdated = false;

    @Override
    public void onLocationChanged(Location location) {
        // only updates the scanner location if we have moved at least 15 meters
        if (mCurrentLocation == null || !mScannerLocationUpdated || mCurrentLocation.distanceTo(location) > 15.0) {
            LatLng latlng = new LatLng(location.getLatitude(),location.getLongitude());
            mScannerLocationUpdated = mBluetoothScanner.updateLocation(latlng);
        }
        // update current location
        mCurrentLocation = location;
        // Obtain nearby scanners
        mBackgroundHandler.post(mFetchSensorsFromDatabaseTask);
    }

    void postToBackgroundHandler(Runnable task) {
        mBackgroundHandler.post(task);
    }

    class ScannerEntry {
        private LatLng mLatLng;
        private String mAddress;
        private int mPort;
        public ScannerEntry(LatLng latlng, String address, int port) {
            mLatLng = latlng; mAddress = address; mPort = port;
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

    Collection<ScannerEntry> getScannerCollection() {
        Collection<ScannerEntry> results = new LinkedList<>();
        synchronized (mScanners) {
            results.addAll(mScanners.values());
        }
        return results;
    }

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
                Bson filter = geoWithinCenter("loc",mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),1.0);
                mSensorsCollection.find(filter).forEach(new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        try {
                            JSONObject json = new JSONObject(document.toJson());
                            JSONArray coord = json.getJSONObject("loc").getJSONArray("coordinates");
                            mScanners.put(json.getString("id"), new ScannerEntry(
                                    new LatLng(coord.getDouble(0),coord.getDouble(1)),
                                    json.getString("ip"),json.getInt("port")));
                        } catch (JSONException je) {
                            Log.w(TAG, "Error reading JSON format from MongoDB");
                            // skip
                        }
                    }
                });
            }
            mUiHandler.post(mUpdateMapTask);
        }
    };

    MongoCollection<Document> getMongoSensorCollection() {
        return mSensorsCollection;
    }
}
