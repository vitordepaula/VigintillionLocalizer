package br.inatel.hackathon.vigintillionlocalizer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import org.bson.Document;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.inatel.hackathon.vigintillionlocalizer.database.DB;
import br.inatel.hackathon.vigintillionlocalizer.model.Beacon;

import static com.mongodb.client.model.Filters.*;

/**
 * A simple {@link Fragment} subclass.
 * To receive events from this fragment, an instance of {@link IBluetoothScannerCallbacks}
 * must be passed to setCallbacks function.
 */
public class BluetoothScannerFragment extends Fragment {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = BluetoothScannerFragment.class.getSimpleName();

    static final int HTTP_SERVER_PORT = 5476;

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothLeScanner btLeScanner;
    private WebServer mWebServer;
    private ScanCallback scanCallback;
    private ArrayList<Beacon> mBeaconList;
    private DB mDb;
    private LatLng mLastLocation = null;
    private MongoCollection<Document> mMongoCollection = null;
    private ExecutorService mExecutor;

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
        mExecutor = Executors.newSingleThreadExecutor();
        mCallbacks = null;

        // SCAN CALLBACK AND SCANNER
        setScanCallback();

        // DATABASE AND ARRAYLIST
        mDb = new DB(getContext());
        mBeaconList = new ArrayList<>();

        // BLUETOOTH
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // WEB SERVER
        mWebServer = new WebServer(mDb);
    }

    public String getLocalIpAddress()
    {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface i = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = i.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "IP Address error: " + ex.toString());
        }
        return null;
    }

    public void stopScanner(){
        btLeScanner.stopScan(scanCallback);
        mMongoCollection.deleteOne(eq("id", mBtAdapter.getAddress()));
        mWebServer.stop();
    }

    public boolean setLocation(LatLng location) {
        Log.d(TAG, "got location");
        if (mLastLocation == null) {
            if (mBtAdapter == null) {
                Log.w(TAG, "adapter not ready yet");
                return false;
            }
            checkBluetoothState();
        }
        if (location == null) {
            Log.d(TAG, "location cleared. Stopping");
            stopScanner();
            mLastLocation = null;
        } else {
            Log.d(TAG, "updating remote database");
            mLastLocation = location;
            updateMongoWebService();
        }
        return true;
    }

    public void setMongoCollection(MongoCollection<Document> mongoCollection){
        if(mongoCollection == null){
            stopScanner();
        }
        mMongoCollection = mongoCollection;
    }

    private Runnable mUpdateMongoWebServiceTask = new Runnable() {
        @Override
        public void run() {
            if (mMongoCollection != null && mLastLocation != null) {
                Log.d(TAG, "Mongo: updating server with our information");
                Document document = new Document()
                        .append("name", mBtAdapter.getAddress())
                        .append("ip", getLocalIpAddress())
                        .append("port", HTTP_SERVER_PORT)
                        .append("loc", new Document()
                                .append("types", "Point"))
                        .append("coordinates", Arrays.asList(mLastLocation.latitude, mLastLocation.longitude));
                mMongoCollection.updateOne(eq("id", mBtAdapter.getAddress()), document, new UpdateOptions().upsert(true));
            }
        }
    };

    public void updateMongoWebService() {
        mExecutor.submit(mUpdateMongoWebServiceTask);
    }

    private void checkBluetoothState(){
        if (mBtAdapter == null)
            Log.i(TAG, "BLE Error: O dispositivo não suporta Bluetooth!");
        if (!mBtAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
        else{
            btLeScanner = mBtAdapter.getBluetoothLeScanner();
            btLeScanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
            try {
                mWebServer.start();
            } catch(IOException ioe) {
                Log.e(TAG, "Error starting WEB server");
            }
        }
    }

    private void setScanCallback() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i(TAG, "Callback: Success");
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
                }
                else{
                    Log.i(TAG, "Beacon Name: " + result.getDevice().getName());
                    Beacon beacon = addBeacon(result);
                    onDeviceFound(beacon.getId(), beacon.getRssi());
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
        // don't care: result.getDevice().getName();
        String id = result.getDevice().getAddress(); // id = MAC address

        beacon.setRssi(result.getRssi());
        beacon.setId(id);
        beacon.setTimestamp(System.currentTimeMillis() / 1000);
        beacon.setLocation(mLastLocation);

        boolean found = false;
        for(int i = 0; i< mBeaconList.size(); i++){
            if(mBeaconList.get(i).getId().equals(id)){
                mBeaconList.remove(i);
                mBeaconList.add(beacon);
                mDb.detected_update(beacon);
                found = true;
                break;
            }
        }

        if(!found){
            mBeaconList.add(beacon);
            mDb.detected_insert(beacon);
        }

        return beacon;
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown();
        mCallbacks = null;
        super.onDestroy();
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
