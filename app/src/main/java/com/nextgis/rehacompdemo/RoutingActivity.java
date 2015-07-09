package com.nextgis.rehacompdemo;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.mapui.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

public class RoutingActivity extends AppCompatActivity {
    private ListView mSteps;
    private String mRoute, mPoints;
    private MapDrawable mMap;
    private MapView mMapView;

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

        mRoute = "route_" + routeNum;
        mPoints = "points_" + routeNum;

        final String[] steps = getRouteSteps();
        mSteps = (ListView) findViewById(R.id.lv_steps);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_step, steps);
        mSteps.setAdapter(adapter);

        mSteps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
        });

        mMap = (MapDrawable) ((GISApplication) getApplication()).getMap();
        new MapLoader().execute();
    }

    private void initializeMap() {
        setHardwareAccelerationOff();

        GeoEnvelope geoEnvelope = new GeoEnvelope(4188874.25,4190170.25,7514486.5,7515398.5);

        GeoPoint center = new GeoPoint();
        center.setCoordinates(37.635950, 55.781001);
        center.setCRS(GeoConstants.CRS_WGS84);
        center.project(GeoConstants.CRS_WEB_MERCATOR);

        mMap.setLimits(geoEnvelope, com.nextgis.maplib.util.Constants.MAP_LIMITS_XY);
        mMap.setZoomAndCenter(16, center);
    }

    private void setHardwareAccelerationOff() {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ILayer layer = mMap.getLayerByName(mRoute);

        while (layer != null) {
            layer.delete();
            layer = mMap.getLayerByName(mRoute);
        }

//        layer = mMap.getLayerByName(mPoints);
//
//        while (layer != null) {
//            layer.delete();
//            layer = mMap.getLayerByName(mPoints);
//        }

        mMap.save();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }

    private String[] getRouteSteps() {
        TreeMap<Integer, String> steps = new TreeMap<>();

        try {
            JSONObject jsonObj = new JSONObject(getGeoJSON(mPoints));
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

    public String getGeoJSON(String name) {
        String json;

        try {
            InputStream is = getAssets().open(name + ".geojson");
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

    private class MapLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            VectorLayer layer = new VectorLayer(getApplication(), mMap.createLayerStorage());
            layer.setName(mRoute);
            layer.setVisible(true);

            try {
                JSONObject geoJSONObject = new JSONObject(getGeoJSON(mRoute));
                String errorMessage = layer.createFromGeoJSON(geoJSONObject);

                if (TextUtils.isEmpty(errorMessage))
                    mMap.addLayer(layer);

//            layer = new VectorLayer(getApplication(), mMap.createLayerStorage());
//            layer.setName(mPoints);
//            layer.setVisible(false);
//
//            geoJSONObject = new JSONObject(getGeoJSON(mPoints));
//            errorMessage = layer.createFromGeoJSON(geoJSONObject);
//
//            if (TextUtils.isEmpty(errorMessage))
//                mMap.addLayer(layer);

                mMap.save();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mMapView = new MapView(RoutingActivity.this, mMap);

            ((FrameLayout) findViewById(R.id.fl_map)).addView(mMapView, 0, new FrameLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

            mMapView.bringToFront();
            initializeMap();
        }
    }
}
