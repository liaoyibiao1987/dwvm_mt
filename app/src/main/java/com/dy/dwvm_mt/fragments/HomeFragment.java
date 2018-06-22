package com.dy.dwvm_mt.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dy.dwvm_mt.R;
import com.dy.dwvm_mt.adapters.TabsAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeFragment extends Fragment {


    /*@BindView(R.id.viewpager)
    ViewPager viewPager;

    @BindView(R.id.tablayout)
    TabLayout tabLayout;*/

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragments_home, container, false);
        ButterKnife.bind(this, rootView);

        /*TabsAdapter tabsAdapter = new TabsAdapter(getChildFragmentManager());
        tabsAdapter.addFragment(new DialTabFragment(1), "Favorite 1");
        tabsAdapter.addFragment(new DialTabFragment(2), "Favorite 2");
        viewPager.setAdapter(tabsAdapter);
        tabLayout.setupWithViewPager(viewPager);*/

        return rootView;
    }
}
