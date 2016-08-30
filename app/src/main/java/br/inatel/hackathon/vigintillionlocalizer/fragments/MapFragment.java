package br.inatel.hackathon.vigintillionlocalizer.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Collection;
import java.util.Map;

import br.inatel.hackathon.vigintillionlocalizer.R;
import br.inatel.hackathon.vigintillionlocalizer.activity.MainActivity;
import br.inatel.hackathon.vigintillionlocalizer.model.TrackedBeacon;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = MapFragment.class.getSimpleName();

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    public MapFragment() {
        // Required empty public constructor
    }

    private MapView mMapView;
    private boolean mInitialMapUpdate = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

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
        if (((MainActivity)getActivity()).getLocation() != null) {
            mInitialMapUpdate = true;
            mMapView.getMapAsync(this);
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
        Location currentLocation = ((MainActivity)getActivity()).getLocation();
        if (currentLocation != null) {
            LatLng me = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            googleMap.clear();
            // our position
            googleMap.addMarker(new MarkerOptions().position(me).flat(true).anchor(0.5f,0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.radiobutton_on_background)));
            googleMap.addCircle(new CircleOptions().center(me).radius(200.0f));
            // scanners
            Collection<MainActivity.ScannerEntry> scannerCollection = ((MainActivity)getActivity()).getScannerCollection();
            for (MainActivity.ScannerEntry scanner : scannerCollection) {
                googleMap.addMarker(new MarkerOptions().position(scanner.getLatLng()).flat(true).anchor(0.5f,0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.radiobutton_off_background)));
            }
            // found beacons
            Map<TrackedBeacon,LatLng> beacons = ((MainActivity)getActivity()).getBeaconLocationCalculationResults();
            for (Map.Entry<TrackedBeacon,LatLng> beacon: beacons.entrySet())
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(colorIdToHue(beacon.getKey().color_id)))
                        .position(beacon.getValue()));
            if (MainActivity.TEST_MODE) {
                final float d = 0.001f;
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(colorIdToHue(0)))
                        .position(new LatLng(me.latitude+d, me.longitude+d)));
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(colorIdToHue(1)))
                        .position(new LatLng(me.latitude-d, me.longitude+d)));
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(colorIdToHue(2)))
                        .position(new LatLng(me.latitude+d, me.longitude-d)));
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(colorIdToHue(3)))
                        .position(new LatLng(me.latitude-d, me.longitude-d)));
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(colorIdToHue(4)))
                        .position(new LatLng(me.latitude+d, me.longitude)));
            }
            if (mInitialMapUpdate) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 16.8f));
                mInitialMapUpdate = false;
            }
        }
    }

    private static float colorIdToHue(int color_id) {
        switch (color_id) {
            case 1: return BitmapDescriptorFactory.HUE_GREEN;
            case 2: return BitmapDescriptorFactory.HUE_AZURE;
            case 3: return BitmapDescriptorFactory.HUE_YELLOW;
            case 4: return BitmapDescriptorFactory.HUE_MAGENTA;
            default: return BitmapDescriptorFactory.HUE_RED;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
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

    public void updateMap() {
        Log.d(TAG, "Requesting map update");
        mMapView.getMapAsync(MapFragment.this);
    }
}
