package com.example.fluctuatenotice.handlers;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fluctuatenotice.MainActivity;
import com.example.fluctuatenotice.R;
import com.example.fluctuatenotice.prefData.PrefData;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // 表示内容の取得
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        // 県名
        int pref_position = sharedPreferences.getInt(getString(R.string.pref_position), MainActivity.DEFAULT_POSITION);
        ((TextView)findViewById(R.id.txtPrefDetail)).setText(PrefData.pref_name[pref_position]);
        ((TextView)findViewById(R.id.txtStationDetail)).setText(PrefData.station_name[pref_position]);

        // 気温
        ((TextView) findViewById(R.id.txtHighAfterDetail)).setText(String.format("%.1f", PrefData.temperatures[0]));
        ((TextView) findViewById(R.id.txtHighBeforeDetail)).setText(String.format("%.1f", PrefData.temperatures[1]));
        ((TextView) findViewById(R.id.txtLowAfterDetail)).setText(String.format("%.1f", PrefData.temperatures[2]));
        ((TextView) findViewById(R.id.txtLowBeforeDetail)).setText(String.format("%.1f", PrefData.temperatures[3]));

        // URL
        ((TextView)findViewById(R.id.txtForecastDetail)).setText(PrefData.URLs[0]);
        ((TextView)findViewById(R.id.txtHTMLDetail)).setText(PrefData.URLs[1]);

        // 通知の消去
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MainActivity.NOTIFICATION_ID);

        // 終了ボタンの設定
        findViewById(R.id.btnConfirm).setOnClickListener( view -> {
            finish();
        });

    }

}