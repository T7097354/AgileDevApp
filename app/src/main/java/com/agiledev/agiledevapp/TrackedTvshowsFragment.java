package com.agiledev.agiledevapp;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by s6104158 on 07/02/19.
 */

public class TrackedTvshowsFragment extends Fragment {

    View myView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_tvshow, container, false);

        getActivity().setTitle(R.string.tvshows_name);

        return myView;
    }
}