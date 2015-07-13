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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.nextgis.maplibui.GISApplication;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String[] addresses = new String[] {
                getString(R.string.address1),
                getString(R.string.address2)
        };

        ListView lvAddresses = (ListView)findViewById(R.id.lv_addresses);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_step, addresses);
        lvAddresses.setAdapter(adapter);

        lvAddresses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                Intent routingActivity = new Intent(MainActivity.this, RoutingActivity.class);
                routingActivity.putExtra(Constants.BUNDLE_ROUTE_ID, position);
                routingActivity.putExtra(Constants.BUNDLE_ROUTE_NAME, adapter.getItem(position));
                startActivity(routingActivity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                ((GISApplication) getApplication()).showSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
