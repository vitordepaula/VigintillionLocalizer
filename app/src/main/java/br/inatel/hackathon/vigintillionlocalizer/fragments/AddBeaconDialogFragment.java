package br.inatel.hackathon.vigintillionlocalizer.fragments;


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

import br.inatel.hackathon.vigintillionlocalizer.adapters.BeaconsAdapter;
import br.inatel.hackathon.vigintillionlocalizer.R;
import br.inatel.hackathon.vigintillionlocalizer.activity.MainActivity;
import br.inatel.hackathon.vigintillionlocalizer.database.DB;

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

        final View mSelectedColor = rootview.findViewById(R.id.selected_color);
        View.OnClickListener colorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedColor.setBackground(v.getBackground());
            }
        };
        rootview.findViewById(R.id.select_color_blue).setOnClickListener(colorClickListener);
        rootview.findViewById(R.id.select_color_green).setOnClickListener(colorClickListener);
        rootview.findViewById(R.id.select_color_purple).setOnClickListener(colorClickListener);
        rootview.findViewById(R.id.select_color_red).setOnClickListener(colorClickListener);
        rootview.findViewById(R.id.select_color_yellow).setOnClickListener(colorClickListener);
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
        resultData.putExtra(BeaconsToTrackFragment.SELECTED_ITEM, data);
        getTargetFragment().onActivityResult(BeaconsToTrackFragment.ADD_NEW_BEACON, BeaconsToTrackFragment.RESULT_OK, resultData);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getTargetFragment().onActivityResult(BeaconsToTrackFragment.ADD_NEW_BEACON, BeaconsToTrackFragment.RESULT_CANCEL, null);
    }

    @Override
    public void onDeviceFound(String name, int signal) {
        // Add only if not already added or not already present in the list
        if (!mExceptionList.contains(name)) {
            for (DB.TrackedBeacon beacon: mAdapter.getDataSet())
                if (beacon.beacon_id == name)
                    return;
            DB.TrackedBeacon newObj = new DB.TrackedBeacon(name,0);
            mAdapter.getDataSet().add(newObj);
            mAdapter.notifyDataSetChanged();
        }
    }
}
