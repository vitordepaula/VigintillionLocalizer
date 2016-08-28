package br.inatel.hackathon.vigintillionlocalizer;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class MainActivity extends FragmentActivity {

    private BluetoothScannerFragment mBluetoothScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mBluetoothScanner = new BluetoothScannerFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new MapFragment())
                .add(mBluetoothScanner, "BTScanner")
                .commit();
    }

    public BluetoothScannerFragment getScanner() { return mBluetoothScanner; }
}
