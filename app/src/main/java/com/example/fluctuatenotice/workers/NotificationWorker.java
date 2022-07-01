package com.example.fluctuatenotice.workers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fluctuatenotice.MainActivity;
import com.example.fluctuatenotice.R;
import com.example.fluctuatenotice.handlers.BigTextMainActivity;
import com.example.fluctuatenotice.prefData.PrefData;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class NotificationWorker extends Worker {

    private final String DEBUG_TAG = "HTMLActivity";
    private int PREF_POSITION = 0;

    private static final String[] pref_name = PrefData.pref_name;
    private static final String[] station_name = PrefData.station_name;
    private static final String[] pref_code = PrefData.pref_code;
    private static final String[] station_code = PrefData.station_code;

    private String pref_selected = null;
    private String station_selected = null;
    private String pref_code_selected = null;
    private String station_code_selected = null;

    private int year = 0;
    private int month = 0;
    private int day = 0;

    private double high_tmp_before = 0;
    private double high_tmp_after = 0;
    private double low_tmp_before = 0;
    private double low_tmp_after = 0;

    public String station_URL = "https://www.data.jma.go.jp/developer/xml/feed/regular_l.xml";
    private final String html_URL_template
            = "https://www.data.jma.go.jp/obd/stats/etrn/view/daily_s1.php?prec_no=pref_code&block_no=station_code&year=now_year&month=now_month&day=&view=";
    private String html_URL;

    public NotificationWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork(){
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        // 変数の設定
        this.PREF_POSITION = sharedPreferences.getInt(context.getString(R.string.pref_position), 0);
        this.pref_selected = pref_name[PREF_POSITION];
        this.pref_code_selected = pref_code[PREF_POSITION];
        this.station_selected = station_name[PREF_POSITION];
        this.station_code_selected = station_code[PREF_POSITION];

        Log.d(DEBUG_TAG, pref_selected+", "+PREF_POSITION+", "+pref_code_selected+", "+station_selected+", "+station_code_selected);

        // 日時の取得
        Calendar calendar = Calendar.getInstance();
        int notification_time = sharedPreferences.getInt(context.getString(R.string.notice_time), 0);
        if(notification_time<=23){
            calendar.add(Calendar.DATE, -1);
        }
        day = calendar.get(Calendar.DAY_OF_MONTH);

        // 「過去の気象データ」用URLの作成
        html_URL = html_URL_template.replaceAll("pref_code", pref_code_selected)
                .replaceAll("station_code", station_code_selected)
                .replaceAll("now_year", Integer.toString(calendar.get(Calendar.YEAR)))
                .replaceAll("now_month", Integer.toString(calendar.get(Calendar.MONTH)+1));
        Log.d(DEBUG_TAG, html_URL);

        try {
            // 選択都道府県のXMLフィードURLの取得→予報気温の取得、格納
            double[] tmp_after
                    = new ForecastScanner().parse(generateUrl(new StationSetting().parse(generateUrl(station_URL), station_selected)));
            high_tmp_after = tmp_after[0];
            low_tmp_after = tmp_after[1];
            // 「過去の気象データ」より気温の取得
            downloadHTML(html_URL);
        } catch (IOException e) {
        } catch (XmlPullParserException e) {
        }
        generateBigTextStyleNotification();

        return Result.success();
    }

    // HTMLデータの取得（前日の気温：「過去の気象データ」より）
    private void downloadHTML(String url){
        try {
            Log.d("HTMLTask", "start HTML");
            // get all html documents
            Document doc = Jsoup.connect(url).get();
            // select id="tablefix1" and each row input element
            Elements table_rows = doc.select("#tablefix1 tr");
            // 日付を指定してdoc_rowsの中から必要な行だけ取り出す
            Elements tds_yesterdays = table_rows.get(day+3).getElementsByTag("td");
            high_tmp_before = Double.parseDouble(tds_yesterdays.get(7).text());
            low_tmp_before = Double.parseDouble(tds_yesterdays.get(8).text());
        } catch (IOException e){
            Log.d(DEBUG_TAG, "Disconnect: "+url);
        }
    }

    // 指定したURLよりXMLフィードの取得
    private InputStream generateUrl(String urlString) throws IOException {
        java.net.URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    // 通知の作成
    private void generateBigTextStyleNotification() {

        Intent intent = new Intent(this.getApplicationContext(), BigTextMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, 0);

        // 表示内容の設定
        String high_sing = "+";
        String high_notice = "";
        String low_sing = "+";
        String low_notice = "";
        if(high_tmp_after<=high_tmp_before){
            high_sing = "";
        } else {}
        if(Math.abs(high_tmp_after-high_tmp_before)>=3){
            high_notice = "！";
        } else {}
        if(low_tmp_after<=low_tmp_before){
            low_sing = "";
        } else {}
        if(Math.abs(low_tmp_after-low_tmp_before)>=3){
            low_notice = "！";
        } else {}

        String high_differ = String.format("%.1f" ,high_tmp_after-high_tmp_before);
        String low_differ = String.format("%.1f" ,low_tmp_after-low_tmp_before);

        // 通知の作成
        Notification builder = new NotificationCompat.Builder(this.getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("現在値："+pref_selected)
                .setContentText(high_notice+"最高 "+high_tmp_after+"℃（"+high_sing+high_differ+"℃）、"
                        +low_notice+"最低 "+low_tmp_after+"℃（"+low_sing+low_differ+"℃）")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat mNotificationManagerCompat = NotificationManagerCompat.from(this.getApplicationContext());
        mNotificationManagerCompat.notify(MainActivity.NOTIFICATION_ID, builder);

    }

}
