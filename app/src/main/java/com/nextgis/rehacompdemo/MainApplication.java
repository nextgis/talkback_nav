package com.nextgis.rehacompdemo;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.File;

import static com.nextgis.maplib.util.Constants.MAP_EXT;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;

public class MainApplication extends GISApplication {
    @Override
    public MapBase getMap()
    {
        if (null != mMap) {
            return mMap;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        File defaultPath = getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(getFilesDir(), KEY_PREF_MAP);
        }

        String mapPath = sharedPreferences.getString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        String mapName = sharedPreferences.getString(SettingsConstantsUI.KEY_PREF_MAP_NAME, "default");
        File mapFullPath = new File(mapPath, mapName + MAP_EXT);
        final Bitmap bkBitmap = BitmapFactory.decodeResource(getResources(), com.nextgis.maplibui.R.drawable.bk_tile);

        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI());
        mMap.setName(mapName);
        mMap.load();

        return mMap;
    }

    @Override
    public String getAuthority()
    {
        return Constants.AUTHORITY;
    }

    @Override
    public void showSettings()
    {
//        Intent intentSet = new Intent(this, SettingsActivity.class);
//        intentSet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intentSet);
    }

    @Override
    protected void onFirstRun()
    {
        //add OpenStreetMap layer on application first run
        String layerName = getString(R.string.osm);
        String layerURL = getString(R.string.osm_url);
        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(getApplicationContext(), mMap.createLayerStorage());
        layer.setName(layerName);
        layer.setURL(layerURL);
        layer.setTMSType(TMSTYPE_OSM);
        layer.setVisible(true);

        mMap.addLayer(layer);
        mMap.moveLayer(0, layer);

        mMap.save();
    }
}
