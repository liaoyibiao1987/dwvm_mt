package com.dy.dwvm_mt.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dy.dwvm_mt.R;

@SuppressLint("ValidFragment")
public class DialTabFragment extends Fragment {
    private int mPosition;

    public DialTabFragment(int position) {
        mPosition = position;
    }

    public DialTabFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dail_tab, container, false);

        TextView textView =  rootView.findViewById(R.id.fav_number);
        textView.setText("Favorite " + mPosition);

        return rootView;
    }
}
