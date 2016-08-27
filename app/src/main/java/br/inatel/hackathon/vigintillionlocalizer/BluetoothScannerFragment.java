package br.inatel.hackathon.vigintillionlocalizer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 * To receive events from this fragment, an instance of {@link IBluetoothScannerCallbacks}
 * must be passed to setCallbacks function.
 */
public class BluetoothScannerFragment extends Fragment {

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

    /**
     * Sets callbacks to receive events from the bluetooth scanner
     * @param callbacks an object that implements IBluetoothScannnerCallbacks inteface
     */
    public void setCallbacks(IBluetoothScannerCallbacks callbacks) {
        mCallbacks = new WeakReference<>(callbacks);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCallbacks = null;
        // TODO
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCallbacks = null;
    }

}
