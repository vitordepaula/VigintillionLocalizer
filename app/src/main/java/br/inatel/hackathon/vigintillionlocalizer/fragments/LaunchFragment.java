package br.inatel.hackathon.vigintillionlocalizer.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import br.inatel.hackathon.vigintillionlocalizer.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class LaunchFragment extends Fragment {


    public LaunchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View rootView = inflater.inflate(R.layout.fragment_launch, container, false);
        rootView.findViewById(R.id.launch_image).startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));
        return rootView;
    }

}
