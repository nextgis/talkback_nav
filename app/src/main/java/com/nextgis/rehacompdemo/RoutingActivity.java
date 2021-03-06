/*
 * Project:  RehacompDemo
 * Purpose:  Routing for the blind using TalkBack and TTS.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.rehacompdemo;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.nextgis.maplib.api.IGeometryCache;
import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Geo;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.overlay.CurrentLocationOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RoutingActivity extends AppCompatActivity implements LocationListener {
    private StepAdapter mAdapter;
    private ListView mSteps;
    private String mRoute, mPoints;
    private MapDrawable mMap;
    private MapViewOverlays mMapView;
    private CurrentLocationOverlay mCurrentLocationOverlay;
    private LocationManager mLocationManager;
    private int mActivationDistance;
    private List<GeoLineString> mRouteSegments;
    private List<IGeometryCacheItem> mAllPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        String sRadius = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_RADIUS, "6");
        mActivationDistance = Integer.parseInt(sRadius);

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
        mSteps.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        mSteps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                mSteps.requestFocusFromTouch();
                mSteps.setSelection(position);
            }
        });

        mMap = (MapDrawable) ((GISApplication) getApplication()).getMap();
        new MapLoader().execute();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.GPS_MIN_TIME, Constants.GPS_MIN_DIST, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCurrentLocationOverlay != null)
            mCurrentLocationOverlay.stopShowingCurrentLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCurrentLocationOverlay != null)
            mCurrentLocationOverlay.startShowingCurrentLocation();
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
    public boolean dispatchPopulateAccessibilityEvent(@NonNull AccessibilityEvent event) {
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        int position;

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    position = mSteps.getSelectedItemPosition();
                    if (position == AdapterView.INVALID_POSITION)
                        position = 0;
                    else
                        position--;

                    mSteps.requestFocusFromTouch();
                    mSteps.setSelection(position);
                    break;
                } else
                    return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    position = mSteps.getSelectedItemPosition();
                    if (position == AdapterView.INVALID_POSITION)
                        position = 0;
                    else
                        position++;

                    mSteps.requestFocusFromTouch();
                    mSteps.setSelection(position);
                    break;
                } else
                    return true;
            default:
                return super.dispatchKeyEvent(event);
        }

        return true;
    }

    private TreeMap<Long, String> getRouteSteps() {
        TreeMap<Long, String> steps = new TreeMap<>();

        try {
            JSONObject jsonObj = new JSONObject(getGeoJSON(mPoints));
            JSONArray jsonStepsArray = jsonObj.getJSONArray("features");
            JSONObject jsonStep;

            for (int i = 0; i < jsonStepsArray.length(); i++) {
                jsonStep = jsonStepsArray.getJSONObject(i).getJSONObject("properties");
                steps.put(jsonStepsArray.getJSONObject(i).getLong("id"), jsonStep.getString("instr"));
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
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return json;
    }

    // Thanks StackOverflow!
    // http://stackoverflow.com/questions/1459368/snap-point-to-a-line-java
    public static Location snapToLine(Location pointA, Location pointB, Location pointP, boolean clampToSegment) {
        Location projection = new Location(LocationManager.GPS_PROVIDER);

        double ax = pointA.getLatitude();
        double ay = pointA.getLongitude();
        double bx = pointB.getLatitude();
        double by = pointB.getLongitude();

        // no line, locations are similar
        if (ax == bx && ay == by)
            return pointA;

        double px = pointP.getLatitude();
        double py = pointP.getLongitude();

        double apx = px - ax;
        double apy = py - ay;
        double abx = bx - ax;
        double aby = by - ay;

        double ab2 = abx * abx + aby * aby;
        double ap_ab = apx * abx + apy * aby;
        double t = ap_ab / ab2;

        if (clampToSegment) {
            if (t < 0) {
                t = 0;
            } else if (t > 1) {
                t = 1;
            }
        }

        projection.setLatitude(ax + abx * t);
        projection.setLongitude(ay + aby * t);

        return projection;
    }

    private double getOffsetLatitude(Location location, double offset) {
        return location.getLatitude() + (180 / Math.PI) * (offset / 6378137.0);
    }

    private double getOffsetLongitude(Location location, double offset) {
        return location.getLongitude() + (180 / Math.PI) * (offset / 6378137.0) / Math.cos(Math.PI / 180 * location.getLatitude());
    }

    private GeoEnvelope getArea(Location currentLocation) {
        GeoEnvelope area = new GeoEnvelope();
        Location offset = new Location(currentLocation);

        offset.setLatitude(getOffsetLatitude(currentLocation, -Constants.ENVELOPE_SIDE));
        offset.setLongitude(getOffsetLongitude(currentLocation, -Constants.ENVELOPE_SIDE));
        area.setMin(Geo.wgs84ToMercatorSphereX(offset.getLongitude()), Geo.wgs84ToMercatorSphereY(offset.getLatitude()));

        offset.setLatitude(getOffsetLatitude(currentLocation, Constants.ENVELOPE_SIDE));
        offset.setLongitude(getOffsetLongitude(currentLocation, Constants.ENVELOPE_SIDE));
        area.setMax(Geo.wgs84ToMercatorSphereX(offset.getLongitude()), Geo.wgs84ToMercatorSphereY(offset.getLatitude()));

        return area;
    }

    @Override
    public void onLocationChanged(Location currentLocation) {
        GeoEnvelope area = getArea(currentLocation);
        Location nextLocation = new Location(LocationManager.GPS_PROVIDER);
        Location previousLocation = new Location(LocationManager.GPS_PROVIDER);

        TreeMap<Float, Location> perpendiculars = new TreeMap<>();
        HashMap<Location, GeoLineString> snapsToSegments = new HashMap<>();
        for (GeoLineString segment : mRouteSegments)
            if (area.intersects(segment.getEnvelope()) || area.contains(segment.getEnvelope())) {
                previousLocation.setLongitude(Geo.mercatorToWgs84SphereX(segment.getPoint(0).getX()));
                previousLocation.setLatitude(Geo.mercatorToWgs84SphereY(segment.getPoint(0).getY()));
                nextLocation.setLongitude(Geo.mercatorToWgs84SphereX(segment.getPoint(1).getX()));
                nextLocation.setLatitude(Geo.mercatorToWgs84SphereY(segment.getPoint(1).getY()));
                Location snap = snapToLine(previousLocation, nextLocation, currentLocation, false);
                Float perpendicular = currentLocation.distanceTo(snap);
                perpendiculars.put(perpendicular, snap);
                snapsToSegments.put(snap, segment);
            }

        if (perpendiculars.size() > 0 && snapsToSegments.size() > 0) {
            Location snappedLocation = perpendiculars.firstEntry().getValue();
            GeoLineString segment = snapsToSegments.get(snappedLocation);
            previousLocation.setLongitude(Geo.mercatorToWgs84SphereX(segment.getPoint(0).getX()));
            previousLocation.setLatitude(Geo.mercatorToWgs84SphereY(segment.getPoint(0).getY()));
            nextLocation.setLongitude(Geo.mercatorToWgs84SphereX(segment.getPoint(1).getX()));
            nextLocation.setLatitude(Geo.mercatorToWgs84SphereY(segment.getPoint(1).getY()));

            GeoPoint point = segment.getPoint(1);
            if (snappedLocation.distanceTo(previousLocation) < snappedLocation.distanceTo(nextLocation)) {
                point = segment.getPoint(0);
                nextLocation.setLongitude(previousLocation.getLongitude());
                nextLocation.setLatitude(previousLocation.getLatitude());
            }

            if (snappedLocation.distanceTo(nextLocation) <= mActivationDistance) {
                long id = -1;
                for (IGeometryCacheItem cachePoint : mAllPoints) {
                    if (point.equals(cachePoint.getGeometry()))
                        id = cachePoint.getFeatureId();
                }

                int position = mAdapter.getItemPosition(id);
                if (position != -1) {
                    mSteps.requestFocusFromTouch();
                    mSteps.setSelection(position);
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
        private Map<Long, Integer> mData;

        public StepAdapter(Context context, int resource, Map<Long, String> data) {
            super(context, resource, data.values().toArray(new String[data.size()]));

            mData = new HashMap<>();
            int position = 0;
            for (Map.Entry<Long, String> entry : data.entrySet())
                mData.put(entry.getKey(), position++);
        }

        public int getItemPosition(long id) {
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

            VectorLayer allPointsLayer = (VectorLayer) mMap.getLayerByName(mPoints);
            IGeometryCache pointsCache = allPointsLayer.getGeometryCache();
            mAllPoints = pointsCache.getAll();
            mRouteSegments = new ArrayList<>();

            Collections.sort(mAllPoints, new Comparator<IGeometryCacheItem>() {
                @Override
                public int compare(IGeometryCacheItem v1, IGeometryCacheItem v2) {
                    return Long.valueOf(v1.getFeatureId()).compareTo(v2.getFeatureId());
                }
            });

            for (int i = 0; i < mAllPoints.size() - 1; i++) {
                GeoLineString segment = new GeoLineString();
                segment.add((GeoPoint) mAllPoints.get(i).getGeometry());
                segment.add((GeoPoint) mAllPoints.get(i + 1).getGeometry());
                mRouteSegments.add(segment);
            }

            mMapView = new MapViewOverlays(RoutingActivity.this, mMap);
            mCurrentLocationOverlay = new CurrentLocationOverlay(RoutingActivity.this, mMapView);
            mCurrentLocationOverlay.setStandingMarker(R.drawable.ic_location_standing);
            mCurrentLocationOverlay.setMovingMarker(R.drawable.ic_location_moving);
            mCurrentLocationOverlay.startShowingCurrentLocation();
            mMapView.addOverlay(mCurrentLocationOverlay);

            ((FrameLayout) findViewById(R.id.fl_map)).addView(mMapView, 0, new FrameLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

            mMapView.bringToFront();
            initializeMap();
        }
    }
}

