package com.nextgis.rehacompdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvAddresses = (ListView)findViewById(R.id.lv_addresses);

        final String[] addresses = new String[] {
                getString(R.string.address1),
                getString(R.string.address2)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, addresses);
        lvAddresses.setAdapter(adapter);

        lvAddresses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent routingActivity = new Intent(MainActivity.this, RoutingActivity.class).putExtra(Constants.BUNDLE_ROUTE, position);
                startActivity(routingActivity);
            }
        });
    }
}
