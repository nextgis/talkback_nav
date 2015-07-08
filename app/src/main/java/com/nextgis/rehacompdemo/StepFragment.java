package com.nextgis.rehacompdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StepFragment extends Fragment {
    private String mInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_step, container, false);
        TextView info = (TextView) view.findViewById(R.id.tv_info);
        info.setText(mInfo);
        return view;
    }

    public Fragment setData(int data) {
        mInfo = "" + data;
        return this;
    }
}
