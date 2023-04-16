package com.example.fluctuatenotice;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.Toast;

import com.example.fluctuatenotice.prefData.PrefData;
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

    // 設定する値
    public static int PREF_POSITION;
    public String PREF_NAME;
    public int NOTIFICATION_TIME;
    public String NOTIFICATION_STATUS;

    // デフォルト値
    public static int DEFAULT_POSITION = 0;
    public static String DEFAULT_PREF = PrefData.pref_name[DEFAULT_POSITION];
    public static String DEFAULT_STATUS = "";
    public static int DEFAULT_TIME = 0;

    // 設定値の表示場所
    private TextView pref_text, time_text, result;

    private WorkManager mWorkManager;
    private ListenableFuture<List<WorkInfo>> workInfo;

    public SharedPreferences sharedPreferences;

    /* 追加箇所 */
    /* ListView_PrefSettingを開くActivityResultLauncher */
    final ActivityResultLauncher<Intent> prefectureActivityLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if(result.getResultCode()==RESULT_OK){
                                Intent data = result.getData();
                                PREF_POSITION = data.getIntExtra(getString(R.string.from_preflist_position), DEFAULT_POSITION);
                                PREF_NAME = data.getStringExtra(getString(R.string.from_preflist_name));
                                sharedPreferences.edit()
                                        .putInt(getString(R.string.pref_position), PREF_POSITION)
                                        .putString(getString(R.string.pref_name), PREF_NAME)
                                        .apply();
                                pref_text.setText(PREF_NAME);
                            } else {
                                Toast.makeText(MainActivity.this, R.string.err_msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

    /* 追加箇所 */
    /* ListView_TimeSettingを開くActivityResultLauncher */
    final ActivityResultLauncher<Intent> timeActivityLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if(result.getResultCode()==RESULT_OK){
                                Intent data = result.getData();
                                NOTIFICATION_TIME = data.getIntExtra(getString(R.string.from_timelist), DEFAULT_TIME);
                                sharedPreferences.edit()
                                        .putInt(getString(R.string.notice_time), NOTIFICATION_TIME)
                                        .apply();
                                time_text.setText(NOTIFICATION_TIME+"時");
                            } else {
                                Toast.makeText(MainActivity.this, R.string.err_msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref_text = findViewById(R.id.pref_text);
        time_text = findViewById(R.id.time_text);
        result = findViewById(R.id.result);

        // 設定内容の取得・表示
        sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        PREF_NAME = sharedPreferences.getString(getString(R.string.pref_name), DEFAULT_PREF);
        PREF_POSITION = sharedPreferences.getInt(getString(R.string.pref_position), DEFAULT_POSITION);
        NOTIFICATION_TIME = sharedPreferences.getInt(getString(R.string.notice_time), DEFAULT_TIME);
        NOTIFICATION_STATUS = sharedPreferences.getString(getString(R.string.notice_status), DEFAULT_STATUS);

        Log.d(DEBUG_TAG, PREF_NAME+", "+PREF_POSITION+", "+NOTIFICATION_TIME+", "+NOTIFICATION_STATUS);

        pref_text.setText(PREF_NAME);
        time_text.setText(NOTIFICATION_TIME+"時");
        result.setText(NOTIFICATION_STATUS);

        mWorkManager = WorkManager.getInstance(getApplication());

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
        prefectureActivityLauncher.launch(intent);
    }

    // ”設定（通知時間）”ボタン
    public void settingtimeButton(View view){
        Intent intent = new Intent(this, ListView_TimeSetting.class);
        timeActivityLauncher.launch(intent);
    }

    // ”通知”ボタン
    public void notificationButton(View view) {

        // 現在時刻の取得
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int delay_time;
        if(hour>NOTIFICATION_TIME){
            delay_time = ((24-hour)+NOTIFICATION_TIME)*60-minute;
        } else{
            delay_time = (NOTIFICATION_TIME-hour)*60-minute;
        }
        Log.d(DEBUG_TAG, (delay_time/60)+":"+(delay_time%60));

        // WorkManagerへの登録
        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
                        .addTag(DEBUG_TAG)
                        .setInitialDelay(delay_time, TimeUnit.MINUTES)
                        .build();

        mWorkManager.enqueueUniquePeriodicWork(
                NOTIFICATION_NAME, ExistingPeriodicWorkPolicy.REPLACE, notificationWorkRequest);
        workInfo = mWorkManager.getWorkInfosForUniqueWork(NOTIFICATION_NAME);

        // 登録内容の確認
        try{
            if(workInfo.get().get(0).getState().toString().equals("ENQUEUED")) {
                sharedPreferences.edit()
                        .putString(getString(R.string.notice_status), NOTIFICATION_STATES[0])
                        .apply();
                Log.d(DEBUG_TAG, workInfo.get().get(0).getState().toString());
            }
        } catch (ExecutionException e) {
            Log.d(DEBUG_TAG, "ExecutionException");
        } catch (InterruptedException e) {
            Log.d(DEBUG_TAG, "InterruptedException");
        }
        result.setText(sharedPreferences.getString(getString(R.string.notice_status), DEFAULT_STATUS));

    }

    // ”停止”ボタン
    public void cancelButton(View view) {
        try {
            if(workInfo!=null
                    && workInfo.get().get(0).getState().toString().equals("ENQUEUED")) {
                mWorkManager.cancelAllWorkByTag(DEBUG_TAG);
                workInfo = mWorkManager.getWorkInfosForUniqueWork(NOTIFICATION_NAME);
                Log.d(DEBUG_TAG, workInfo.get().get(0).getState().toString());
            }
        } catch (ExecutionException e) {
            Log.d(DEBUG_TAG, "ExecutionException");
        } catch (InterruptedException e) {
            Log.d(DEBUG_TAG, "InterruptedException");
        }
        sharedPreferences.edit()
                .putString(getString(R.string.notice_status), NOTIFICATION_STATES[1])
                .apply();
        result.setText(sharedPreferences.getString(getString(R.string.notice_status), DEFAULT_STATUS));
    }

}