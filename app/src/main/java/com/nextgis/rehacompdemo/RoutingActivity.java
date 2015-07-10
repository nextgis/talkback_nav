package com.nextgis.rehacompdemo;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.mapui.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.nextgis.maplib.util.Constants.FIELD_ID;

public class RoutingActivity extends AppCompatActivity implements LocationListener {
    private StepAdapter mAdapter;
    private ListView mSteps;
    private String mRoute, mPoints;
    private MapDrawable mMap;
    private MapView mMapView;
    private LocationManager mLocationManager;

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

        mSteps = (ListView) findViewById(R.id.lv_steps);
        mAdapter = new StepAdapter(this, R.layout.item_step, getRouteSteps());
        mSteps.setAdapter(mAdapter);

        mSteps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
        });

        mMap = (MapDrawable) ((GISApplication) getApplication()).getMap();
        new MapLoader().execute();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 3, this);
    }

    private void initializeMap() {
        setHardwareAccelerationOff();

        GeoEnvelope geoEnvelope = new GeoEnvelope(4188874.25, 4190170.25, 7514486.5, 7515398.5);

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

        layer = mMap.getLayerByName(mPoints);
        while (layer != null) {
            layer.delete();
            layer = mMap.getLayerByName(mPoints);
        }

        mMap.save();

        mLocationManager.removeUpdates(this);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }

    private TreeMap<Integer, String> getRouteSteps() {
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

        return steps;//.values().toArray(new String[steps.size()]);
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

    @Override
    public void onLocationChanged(Location currentLocation) {
        GeoPoint nextPoint;
        VectorLayer allPoints = (VectorLayer) mMap.getLayerByName(mPoints);
        Location nextLocation = new Location(LocationManager.GPS_PROVIDER);
        Cursor data;

        for (VectorCacheItem point : allPoints.getVectorCache()) {
            nextPoint = (GeoPoint) point.getGeoGeometry().copy();
            nextPoint.project(GeoConstants.CRS_WGS84);
            nextLocation.setLongitude(nextPoint.getX());
            nextLocation.setLatitude(nextPoint.getY());

            if (currentLocation.distanceTo(nextLocation) <= Constants.POINT_RADIUS) {
                data = allPoints.query(new String[]{Constants.POINT_ID}, FIELD_ID + " = ?", new String[]{point.getId() + ""}, null);

                if (data.moveToFirst()) {
                    int id = data.getInt(0);
                    id = mAdapter.getItemPosition(id);

                    if (id != -1) {
                        mSteps.setSelection(id);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public class StepAdapter extends ArrayAdapter<String> {
        private Map<Integer, Integer> mData;

        public StepAdapter(Context context, int resource, Map<Integer, String> data) {
            super(context, resource, data.values().toArray(new String[data.size()]));

            mData = new HashMap<>();
            int position = 0;
            for (Map.Entry<Integer, String> entry : data.entrySet())
                mData.put(entry.getKey(), position++);
        }

        public int getItemPosition(int id) {
            Integer result = mData.get(id);
            result = result == null ? -1 : result;
            return result;
        }
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

                layer = new VectorLayer(getApplication(), mMap.createLayerStorage());
                layer.setName(mPoints);
                layer.setVisible(false);

                geoJSONObject = new JSONObject(getGeoJSON(mPoints));
                errorMessage = layer.createFromGeoJSON(geoJSONObject);

                if (TextUtils.isEmpty(errorMessage))
                    mMap.addLayer(layer);

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
