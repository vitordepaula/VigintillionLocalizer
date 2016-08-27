package br.inatel.hackathon.vigintillionlocalizer;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

/**
 * A simple {@link Fragment} subclass.
 */
public class BeaconsToTrackFragment extends Fragment implements BeaconsAdapter.ISelectionCallbacks {

    public BeaconsToTrackFragment() {
        // Required empty public constructor
    }

    private Handler mUiHandler;
    private BeaconsAdapter mAdapter;
    private FloatingActionButton mFab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mUiHandler = new Handler(Looper.getMainLooper());
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_beacons_to_track, container, false);
        RecyclerView mRecyclerView = (RecyclerView)rootview.findViewById(R.id.beacon_list);
        mRecyclerView.setHasFixedSize(true);
        // FAB
        mFab = (FloatingActionButton)rootview.findViewById(R.id.addOrRemoveBeaconFab);
        // Use a Linear Layout Manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter
        mAdapter = new BeaconsAdapter(getContext(),this);
        mRecyclerView.setAdapter(mAdapter);
        // Initialize Fab as no item selected
        onNoMoreSelectedItems();
        return rootview;
    }

    private View.OnClickListener mDeleteSelectedItems = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mAdapter.deleteSelected();
        }
    };

    private View.OnClickListener mAddNewItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO
            BluetoothScannerFragment scanner = ((MainActivity)getActivity()).getScanner();
        }
    };

    private void animateFabTo(final int res_id) {
        final int ROT_PERIOD = 300;
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                final Animation an = new ScaleAnimation(1,0,1,1,50,0);
                an.setDuration(ROT_PERIOD);
                an.setFillAfter(true);
                mFab.clearAnimation();
                mFab.startAnimation(an);
            }
        });
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Animation an = new ScaleAnimation(0,1,1,1,50,0);
                an.setDuration(ROT_PERIOD);
                an.setFillAfter(true);
                mFab.setImageResource(res_id);
                mFab.clearAnimation();
                mFab.startAnimation(an);
            }
        },ROT_PERIOD);
    }

    @Override
    public void onThereAreSelectedItems() {
        // Flip to delete function
        animateFabTo(android.R.drawable.ic_menu_delete);
        mFab.setOnClickListener(mDeleteSelectedItems);

    }

    @Override
    public void onNoMoreSelectedItems() {
        // Flip to add function
        animateFabTo(android.R.drawable.ic_input_add);
        mFab.setOnClickListener(mAddNewItem);
    }
}
