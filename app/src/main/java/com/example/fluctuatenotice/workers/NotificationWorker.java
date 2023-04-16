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
import com.example.fluctuatenotice.handlers.NotificationActivity;
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
import java.util.Locale;

public class NotificationWorker extends Worker {

    private final String DEBUG_TAG = "HTMLActivity";

    private static final String[] pref_name = PrefData.pref_name;
    private static final String[] station_name = PrefData.station_name;
    private static final String[] pref_code = PrefData.pref_code;
    private static final String[] station_code = PrefData.station_code;

    private Context context;
    private SharedPreferences sharedPreferences;

    private String pref_selected = null;

    private int day = 0;

    private float high_tmp_before = 0;
    private float high_tmp_after = 0;
    private float low_tmp_before = 0;
    private float low_tmp_after = 0;

    public String station_URL = "https://www.data.jma.go.jp/developer/xml/feed/regular_l.xml";
    private final String html_URL_template
            = "https://www.data.jma.go.jp/obd/stats/etrn/view/daily_s1.php?prec_no=pref_code&block_no=station_code&year=now_year&month=now_month&day=&view=";
    private String forecast_URL = "";
    private String html_URL = "";

    public NotificationWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork(){
        context = getApplicationContext();
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        // 変数の設定
        int PREF_POSITION = sharedPreferences.getInt(context.getString(R.string.pref_position), 0);
        this.pref_selected = pref_name[PREF_POSITION];
        String pref_code_selected = pref_code[PREF_POSITION];
        String station_selected = station_name[PREF_POSITION];
        String station_code_selected = station_code[PREF_POSITION];

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

        // 選択都道府県のXMLフィードURLの取得→予報気温の取得、格納
        try {
            forecast_URL = new StationSetting().parse(generateUrl(station_URL), station_selected);
            float[] tmp_after
                    = new ForecastScanner().parse(generateUrl(forecast_URL));
            high_tmp_after = tmp_after[0];
            low_tmp_after = tmp_after[1];
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "IOException");
        } catch (XmlPullParserException e) {
            Log.d(DEBUG_TAG, "XmlPullParserException");
        }

        // 「過去の気象データ」より気温の取得
        downloadHTML(html_URL);

        // 通知の生成
        generateNotification();

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
            Elements tds_yesterdays = table_rows.get(day+2).getElementsByTag("td");
            high_tmp_before = Float.parseFloat(tds_yesterdays.get(7).text());
            low_tmp_before = Float.parseFloat(tds_yesterdays.get(8).text());
            Log.d(DEBUG_TAG, "Connected: "+high_tmp_before+", "+low_tmp_before);
        } catch (IOException e){
            Log.d(DEBUG_TAG, "Disconnect: "+url);
        }
    }

    // 指定したURLよりXMLフィードの取得
    private InputStream generateUrl(String urlString) throws IOException {
        java.net.URL url = new URL(urlString);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(100000 /* ミリ秒 */);
        conn.setReadTimeout(100000 /* ミリ秒 */);
        conn.setRequestProperty("User-Agent", "Android");
        conn.setRequestProperty("Accept-Language", Locale.getDefault().toString());
        conn.setRequestMethod("GET");
        conn.setDoOutput(false);
        conn.setDoInput(true);
        conn.connect();

        Log.d(DEBUG_TAG, "URL:"+urlString);
        Log.d(DEBUG_TAG, "HttpStatusCode:"+conn.getResponseCode());

        return conn.getInputStream();

    }

    // 通知の作成
    private void generateNotification() {

        // 表示内容の設定
        String high_sing = "+";
        String high_notice = "";
        String low_sing = "+";
        String low_notice = "";
        if(high_tmp_after<=high_tmp_before){
            high_sing = "";
        }
        if(Math.abs(high_tmp_after-high_tmp_before)>=3){
            high_notice = "！";
        }
        if(low_tmp_after<=low_tmp_before){
            low_sing = "";
        }
        if(Math.abs(low_tmp_after-low_tmp_before)>=3){
            low_notice = "！";
        }

        String high_differ = String.format("%.1f" ,high_tmp_after-high_tmp_before);
        String low_differ = String.format("%.1f" ,low_tmp_after-low_tmp_before);

        // 苦肉の策
        PrefData.temperatures[0] = high_tmp_after;
        PrefData.temperatures[1] = high_tmp_before;
        PrefData.temperatures[2] = low_tmp_after;
        PrefData.temperatures[3] = low_tmp_before;
        PrefData.URLs[0] = forecast_URL;
        PrefData.URLs[1] = html_URL;

        // intentの生成
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // 通知の作成
        Notification builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
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
