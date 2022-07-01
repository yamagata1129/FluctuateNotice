package com.example.fluctuatenotice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.fluctuatenotice.workers.NotificationWorker;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String DEBUG_TAG = "FluctuateNotice";
    public static final String NOTIFICATION_NAME = "NotifWorker";
    public static final int NOTIFICATION_ID = 888;
    public static final String CHANNEL_ID = "channel_reminder_1";
    public static final String[] NOTIFICATION_STATES = {"通知設定中", ""};

    public String PREF_NAME;
    public static int PREF_POSITION;
    public int NOTIFICATION_TIME;
    public String NOTIFICATION_STATUS;
    public static String DEFAULT_PREF = "滋賀県";
    public static String DEFAULT_STATUS = "";
    public static int DEFAULT_POSITION = 0;
    public static int DEFAULT_TIME = 0;

    private WorkManager mWorkManager;
    private ListenableFuture<List<WorkInfo>> workInfo;

    public SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 共通設定ファイルの作成
        if(sharedPreferences==null){
            sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        } else{}

        // 設定内容の取得・表示
        PREF_NAME = sharedPreferences.getString(getString(R.string.pref_name), DEFAULT_PREF);
        PREF_POSITION = sharedPreferences.getInt(getString(R.string.pref_position), DEFAULT_POSITION);
        NOTIFICATION_TIME = sharedPreferences.getInt(getString(R.string.notice_time), DEFAULT_TIME);
        NOTIFICATION_STATUS = sharedPreferences.getString(getString(R.string.notice_status), DEFAULT_STATUS);

        Log.d(DEBUG_TAG, PREF_NAME+", "+PREF_POSITION+", "+NOTIFICATION_TIME+", "+NOTIFICATION_STATUS);

        ((TextView)findViewById(R.id.pref_title)).setText(PREF_NAME);
        ((TextView)findViewById(R.id.time_text)).setText(NOTIFICATION_TIME+"時");
        ((TextView)findViewById(R.id.result)).setText(NOTIFICATION_STATUS);

        mWorkManager = WorkManager.getInstance(getApplication());
        if(NOTIFICATION_STATUS.equals(NOTIFICATION_STATES[0])){
            workInfo = mWorkManager.getWorkInfosForUniqueWork(NOTIFICATION_NAME);
            try {
                Log.d(DEBUG_TAG, workInfo.get().get(0).getState().toString());
            } catch (ExecutionException e) {
                Log.d(DEBUG_TAG, "ExecutionException");
            } catch (InterruptedException e) {
                Log.d(DEBUG_TAG, "InterruptedException");
            }
        }

        // 通知チャネルの作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Forecast notification", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("毎日、設定時間に最低・最高気温と前日比を通知します。");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    // ”設定（県名）”ボタン
    public void settingprefButton(View view){
        Intent intent = new Intent(this, ListView_PrefSetting.class);
        startActivity(intent);
    }

    // ”設定（通知時間）”ボタン
    public void settingtimeButton(View view){
        Intent intent = new Intent(this, ListView_TimeSetting.class);
        startActivity(intent);
    }

    // ”通知”ボタン
    public void notificationButton(View view) {

        // 現在時刻の取得
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int delay_time = 0;
        if(hour>NOTIFICATION_TIME){
            delay_time = ((24-hour)+NOTIFICATION_TIME)*60-minute;
        } else if(hour<=NOTIFICATION_TIME){
            delay_time = (NOTIFICATION_TIME-hour)*60-minute;
        } else{}
        Log.d(DEBUG_TAG, (delay_time/60)+":"+(delay_time%60));

        // WorkManagerへの登録
        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 30, TimeUnit.MINUTES)
                        .addTag(DEBUG_TAG)
                        .setInitialDelay(delay_time, TimeUnit.MINUTES)
                        .build();

        mWorkManager.enqueueUniquePeriodicWork(
                NOTIFICATION_NAME, ExistingPeriodicWorkPolicy.REPLACE, notificationWorkRequest);

        // 登録内容の確認
        try{
            workInfo = mWorkManager.getWorkInfosForUniqueWork(NOTIFICATION_NAME);
            if(workInfo.get().get(0).getState().toString().equals("ENQUEUED")) {
                getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit()
                        .putString(getString(R.string.notice_status), NOTIFICATION_STATES[0])
                        .apply();
                Log.d(DEBUG_TAG, workInfo.get().get(0).getState().toString());
            }
        } catch (ExecutionException e) {
            Log.d(DEBUG_TAG, "ExecutionException");
        } catch (InterruptedException e) {
            Log.d(DEBUG_TAG, "InterruptedException");
        }
        ((TextView) findViewById(R.id.result))
                .setText(sharedPreferences.getString(getString(R.string.notice_status), DEFAULT_STATUS));

    }

    // ”停止”ボタン
    public void cancelButton(View view) {
        try {
            if(workInfo!=null
                    && workInfo.get().get(0).getState().toString().equals("ENQUEUED")) {
                mWorkManager.cancelAllWorkByTag(DEBUG_TAG);
                workInfo = mWorkManager.getWorkInfosForUniqueWork(NOTIFICATION_NAME);
                getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit()
                        .putString(getString(R.string.notice_status), NOTIFICATION_STATES[1])
                        .apply();
                Log.d(DEBUG_TAG, workInfo.get().get(0).getState().toString());
            }
        } catch (ExecutionException e) {
            Log.d(DEBUG_TAG, "ExecutionException");
        } catch (InterruptedException e) {
            Log.d(DEBUG_TAG, "InterruptedException");
        }
        ((TextView) findViewById(R.id.result))
                .setText(sharedPreferences.getString(getString(R.string.notice_status), DEFAULT_STATUS));
    }

}