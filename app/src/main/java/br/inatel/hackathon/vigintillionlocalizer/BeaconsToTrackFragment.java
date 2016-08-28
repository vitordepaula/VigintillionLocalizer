package br.inatel.hackathon.vigintillionlocalizer;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import java.util.List;

import br.inatel.hackathon.vigintillionlocalizer.database.DB;

/**
 * A simple {@link Fragment} subclass.
 */
public class BeaconsToTrackFragment extends Fragment implements BeaconsAdapter.IMultiSelectionCallbacks {

    public BeaconsToTrackFragment() {
        // Required empty public constructor
    }

    private Handler mUiHandler;
    private BeaconsAdapter mAdapter;
    private FloatingActionButton mFab;
    private DB mDb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mUiHandler = new Handler(Looper.getMainLooper());
        mDb = new DB(getContext());
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_beacons_to_track, container, false);
        RecyclerView mRecyclerView = (RecyclerView)rootView.findViewById(R.id.beacon_list);
        mRecyclerView.setHasFixedSize(true);
        // FAB
        mFab = (FloatingActionButton)rootView.findViewById(R.id.addOrRemoveBeaconFab);
        // Use a Linear Layout Manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter
        mAdapter = new BeaconsAdapter(getContext(),this,null);
        mRecyclerView.setAdapter(mAdapter);
        // Initialize Fab as no item selected
        onNoMoreSelectedItems();
        // Read data from DB
        mAdapter.getDataSet().addAll(mDb.tracked_get());
        mAdapter.notifyDataSetChanged();
        return rootView;
    }

    private View.OnClickListener mDeleteSelectedItems = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            List<String> selected = mAdapter.getSelected();
            mDb.tracked_delete(selected);
            mAdapter.deleteSelected();
            ((MainActivity)getActivity()).refreshBeaconList();
        }
    };

    public static final int ADD_NEW_BEACON = 6666;
    public static final int RESULT_OK = 0;
    public static final int RESULT_CANCEL = -1;
    public static final String SELECTED_ITEM = "SelectedItem";

    void showAddNewDialog() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        AddBeaconDialogFragment fragment = AddBeaconDialogFragment.newInstance(mAdapter.getDataSet());
        fragment.setTargetFragment(this,ADD_NEW_BEACON);
        fragment.show(ft,"dialog");
    }

    private View.OnClickListener mAddNewItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showAddNewDialog();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_NEW_BEACON && resultCode == RESULT_OK) {
            String new_beacon = data.getStringExtra(SELECTED_ITEM);
            mAdapter.getDataSet().add(new_beacon);
            mAdapter.notifyDataSetChanged();
            mDb.tracked_add(new_beacon);
            ((MainActivity)getActivity()).refreshBeaconList();
        }
    }

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
