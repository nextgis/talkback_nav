package com.nextgis.rehacompdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


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
}
