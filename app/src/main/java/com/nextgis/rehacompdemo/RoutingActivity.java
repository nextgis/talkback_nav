package com.nextgis.rehacompdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class RoutingActivity extends AppCompatActivity {
    private ViewPager mPager;
    private StepPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        mPager = (ViewPager) findViewById(R.id.vp_steps);
        mPagerAdapter = new StepPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
    }

    private class StepPagerAdapter extends FragmentStatePagerAdapter {
        public StepPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new StepFragment().setData(position);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
