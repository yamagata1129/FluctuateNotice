package com.example.fluctuatenotice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fluctuatenotice.prefData.PrefData;

public class ListView_PrefSetting extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private int selected_position;
    private String selected_pref;
    private static final String[] pref_name = PrefData.pref_name;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        setContentView(listView);

        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pref_name);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        selected_position = position;
        selected_pref = pref_name[position];
        finish();
    }

    @Override
    public void finish() {
        int result;
        if(selected_pref==null){
            result = RESULT_CANCELED;
        } else {
            result = RESULT_OK;
        }
        Intent intent = new Intent();
        intent.putExtra(getString(R.string.from_preflist_name), selected_pref);
        intent.putExtra(getString(R.string.from_preflist_position), selected_position);
        setResult(result, intent);
        super.finish();
    }
}
