package br.inatel.hackathon.vigintillionlocalizer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import br.inatel.hackathon.vigintillionlocalizer.database.DB;
import br.inatel.hackathon.vigintillionlocalizer.model.Beacon;

/**
 * A simple {@link Fragment} subclass.
 * To receive events from this fragment, an instance of {@link IBluetoothScannerCallbacks}
 * must be passed to setCallbacks function.
 */
public class BluetoothScannerFragment extends Fragment {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "MainActivity";

    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btLeScanner;

    private ScanCallback scanCallback;

    private ArrayList<Beacon> beaconList;
    private RecyclerView recyclerView;

    private DB database;

    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().
                    setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();

    private static final ScanFilter SCAN_FILTER = new ScanFilter.Builder()
            .build();

    private static final List<ScanFilter> SCAN_FILTERS = buildScanFilters();

    private static List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(SCAN_FILTER);
        return scanFilters;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface IBluetoothScannerCallbacks {
        void onDeviceFound(String name, int signal);
    }


    private WeakReference<IBluetoothScannerCallbacks> mCallbacks;

    public BluetoothScannerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCallbacks = null;

        // SCAN CALLBACK AND SCANNER
        setScanCallback();

        // DATABASE AND ARRAYLIST
        database = new DB(getContext());
        beaconList = new ArrayList<>();

        // BLUETOOTH
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothState();

    }

    private void checkBluetoothState(){
        if (btAdapter == null)
            Log.i("BleError", "Seu dispositivo não suporta Bluetooth!");
        if (!btAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
        else{
            btLeScanner = btAdapter.getBluetoothLeScanner();
            btLeScanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
        }
    }


    private class postDataAsync extends AsyncTask<String, Integer, Double> {
        String urlString;
        HttpURLConnection urlConnection;

        @Override
        protected Double doInBackground(String... params) {
            //urlString = "http://www.mysite.se/index.asp?data=" + params[0];
            urlString = "https://www.google.com.br/#q=" + params[0];
            Log.i(TAG, params[0].toString());
            postData();
            return null;
        }

        public void postData(){
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream is = new BufferedInputStream(urlConnection.getInputStream());
                Log.i(TAG, urlConnection.getResponseMessage());
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
            }
        }
    }

    private void setScanCallback() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i("MainActivity", "Callback: Success");
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
                    return;
                }
                else{
                    Log.i("Beacon Name: ", result.getDevice().getName());
                    Beacon beacon = addBeacon(result);
                    onDeviceFound(beacon.getMac(), beacon.getRssi());

                    String dataString = "";
                    new postDataAsync().execute(result.getDevice().getName());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "onScanFailed errorCode " + errorCode);
            }
        };
    }

    private Beacon addBeacon(ScanResult result){
        Beacon beacon = new Beacon();
        String deviceName = result.getDevice().getName();
        String mac = result.getDevice().getAddress();

        beacon.setRssi(result.getRssi());
        beacon.setMac(mac);
        beacon.setName(deviceName);
        beacon.setDate(getDateAndTime());

        boolean found = false;
        for(int i =0; i<beaconList.size(); i++){
            if(beaconList.get(i).getMac().equals(mac)){
                beaconList.remove(i);
                beaconList.add(beacon);
                database.detected_update(beacon);
                found = true;
                break;
            }
        }

        if(!found){
            beaconList.add(beacon);
            database.detected_insert(beacon);
        }

        return beacon;
    }

    private String getDateAndTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mCallbacks = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            checkBluetoothState();
        }

    }

    /**
     * Sets callbacks to receive events from the bluetooth scanner
     * @param callbacks an object that implements IBluetoothScannnerCallbacks inteface
     */
    public void setCallbacks(IBluetoothScannerCallbacks callbacks) {
        mCallbacks = new WeakReference<>(callbacks);
    }

    /**
     * Call this to send a callback for a device found
     */
    private void onDeviceFound(String name, int signal) {
        if (mCallbacks != null) {
            IBluetoothScannerCallbacks cb = mCallbacks.get();
            if (cb != null)
                cb.onDeviceFound(name,signal);
        }
    }

}
