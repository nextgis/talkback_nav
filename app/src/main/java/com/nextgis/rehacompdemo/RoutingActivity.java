package com.nextgis.rehacompdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

public class RoutingActivity extends AppCompatActivity {
    private ListView mSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        int routeNum = -1;
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            routeNum = extras.getInt(Constants.BUNDLE_ROUTE_ID);

            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(extras.getString(Constants.BUNDLE_ROUTE_NAME));
        }

        if (routeNum == -1)
            return;

        final String[] steps = getRouteSteps(routeNum);
        mSteps = (ListView) findViewById(R.id.lv_steps);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_step, steps);
        mSteps.setAdapter(adapter);

        mSteps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
        });
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }

    private String[] getRouteSteps(int routeNum) {
        TreeMap<Integer, String> steps = new TreeMap<>();

        try {
            JSONObject jsonObj = new JSONObject(loadGeoJSON(routeNum));
            JSONArray jsonStepsArray = jsonObj.getJSONArray("features");
            JSONObject jsonStep;

            for (int i = 0; i < jsonStepsArray.length(); i++) {
                jsonStep = jsonStepsArray.getJSONObject(i).getJSONObject("properties");
                steps.put(jsonStep.getInt("id"), jsonStep.getString("instr"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return steps.values().toArray(new String[steps.size()]);
    }

    public String loadGeoJSON(int routeNum) {
        String json;

        try {
            InputStream is = getAssets().open("route_" + routeNum + ".geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return json;
    }
}
