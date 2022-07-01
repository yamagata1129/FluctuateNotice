package com.example.fluctuatenotice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fluctuatenotice.prefData.PrefData;

public class ListView_PrefSetting extends AppCompatActivity implements AdapterView.OnItemClickListener {

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
        getIntent();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        // 設定ファイルを更新しMainActivityを再生成
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit()
                .putString(getString(R.string.pref_name), pref_name[position])
                .putInt(getString(R.string.pref_position), position)
                .apply();
        Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

}
