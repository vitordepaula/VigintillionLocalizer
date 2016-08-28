package br.inatel.hackathon.vigintillionlocalizer;


import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MapFragment.class.getSimpleName();

    public MapFragment() {
        // Required empty public constructor
    }

    private Handler mUiHandler;
    private ExecutorService mExecutor;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000);
    private MapView mMapView;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = false;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mUiHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.submit(mConnectToDatabaseTask);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        mMapView = (MapView) rootView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        rootView.findViewById(R.id.searchTagFab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = "DevicesToAddFragment";
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new BeaconsToTrackFragment(), name)
                        .addToBackStack(name)
                        .commit();
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "Leaving map");
        mExecutor.shutdown();
        super.onDestroyView();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "Updating Map");
        GoogleMap mMap = googleMap;
        if (mCurrentLocation != null) {
            LatLng me = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            mMap.clear();
            // our position
            mMap.addMarker(new MarkerOptions().position(me).flat(true).anchor(0.5f,0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.radiobutton_on_background)));
            mMap.addCircle(new CircleOptions().center(me).radius(100.0f));
            // scanners
            synchronized (mScanners) {
                for (ScannerEntry scanner : mScanners.values()) {
                    mMap.addMarker(new MarkerOptions().position(scanner.getLatLng()).flat(true).anchor(0.5f,0.5f)
                            .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.radiobutton_off_background)));
                }
            }
            //mMap.addMarker(new MarkerOptions().position(me).title("Marker on me"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 17.5f));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        ((MainActivity)getActivity()).getScanner().setMongoCollection(null);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mMapView != null)
            mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
            ActivityCompat.requestPermissions(getActivity(), permissions, 3030);
            return;
        }
        Log.d(TAG, "We have location permissions. Let's go.");
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
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
    public void onConnectionSuspended(int i) {
        // do nothing
    }

    private class ScannerEntry {
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
    final private HashMap<String,ScannerEntry> mScanners = new HashMap<>();

    private MongoCollection<Document> mSensorsCollection = null;

    private Runnable mConnectToDatabaseTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Connecting to Database");
            MongoClientURI uri = new MongoClientURI("mongodb://vps.de.paula.nom.br/vigintillion");
            MongoClient mongoClient = new MongoClient(uri);
            MongoDatabase db = mongoClient.getDatabase(uri.getDatabase());
            mSensorsCollection = db.getCollection("ble-sensors");
            ((MainActivity)getActivity()).getScanner().setMongoCollection(mSensorsCollection);
        }
    };

    private Runnable mUpdateMapTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Requesting map update");
            mMapView.getMapAsync(MapFragment.this);
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
                            mScanners.put(json.getString("name"), new ScannerEntry(
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

    private boolean mScannerLocationUpdated = false;

    @Override
    public void onLocationChanged(Location location) {
        // only updates the scanner location if we have moved at least 15 meters
        if (mCurrentLocation == null || !mScannerLocationUpdated || mCurrentLocation.distanceTo(location) > 15.0) {
            LatLng latlng = new LatLng(location.getLatitude(),location.getLongitude());
            mScannerLocationUpdated = ((MainActivity)getActivity()).getScanner().setLocation(latlng);
        }
        // update current location
        mCurrentLocation = location;
        // Obtain nearby scanners
        mExecutor.submit(mFetchSensorsFromDatabaseTask);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mCurrentLocation = null;
        mRequestingLocationUpdates = false;
    }
}
