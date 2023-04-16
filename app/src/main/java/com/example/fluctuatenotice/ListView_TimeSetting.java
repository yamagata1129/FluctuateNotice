package com.example.fluctuatenotice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class ListView_TimeSetting extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private String selected_time;
    private static final String[] times = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
            "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        setContentView(listView);

        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, times);

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        selected_time = times[position];
        finish();
    }

    @Override
    public void finish() {
        int result;
        if(selected_time==null){
            result = RESULT_CANCELED;
        } else {
            result = RESULT_OK;
        }
        Intent intent = new Intent();
        intent.putExtra(getString(R.string.from_timelist), Integer.parseInt(selected_time));
        setResult(result, intent);
        super.finish();
    }

}