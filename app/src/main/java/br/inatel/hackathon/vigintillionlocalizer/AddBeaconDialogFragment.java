package br.inatel.hackathon.vigintillionlocalizer;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;
import java.util.List;

import static br.inatel.hackathon.vigintillionlocalizer.BeaconsToTrackFragment.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddBeaconDialogFragment extends DialogFragment implements BeaconsAdapter.IItemClickCallback, BluetoothScannerFragment.IBluetoothScannerCallbacks {

    /**
     * Create a new instance of AddBeaconDialogFragment
     */
    static AddBeaconDialogFragment newInstance(List<String> exceptionList) {
        AddBeaconDialogFragment instance = new AddBeaconDialogFragment();
        instance.mExceptionList.addAll(exceptionList);
        return instance;
    }

    private List<String> mExceptionList = new LinkedList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

    private BeaconsAdapter mAdapter;
    private BluetoothScannerFragment scanner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.fragment_add_beacon_dialog, container, false);
        RecyclerView mRecyclerView = (RecyclerView)rootview.findViewById(R.id.beacon_list);
        mRecyclerView.setHasFixedSize(true);
        // Use a Linear Layout Manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter
        mAdapter = new BeaconsAdapter(getContext(),null,this);
        mRecyclerView.setAdapter(mAdapter);

        scanner = ((MainActivity)getActivity()).getScanner();
        scanner.setCallbacks(this);

        return rootview;
    }

    @Override
    public void onDestroyView() {
        scanner.setCallbacks(null);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(String data) {
        getFragmentManager().popBackStack();
        Intent resultData = new Intent();
        resultData.putExtra(SELECTED_ITEM, data);
        getTargetFragment().onActivityResult(ADD_NEW_BEACON, RESULT_OK, resultData);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getTargetFragment().onActivityResult(ADD_NEW_BEACON, RESULT_CANCEL, null);
    }

    @Override
    public void onDeviceFound(String name, int signal) {
        // Add only if not already added or not already present in the list
        if (!mExceptionList.contains(name) && !mAdapter.getDataSet().contains(name)) {
            mAdapter.getDataSet().add(name);
            mAdapter.notifyDataSetChanged();
        }
    }
}
