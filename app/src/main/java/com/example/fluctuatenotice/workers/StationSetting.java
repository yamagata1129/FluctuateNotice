package com.example.fluctuatenotice.workers;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StationSetting {
    private static final String DEBUG_TAG = "STATION_SETTING";

    // We don't use namespaces
    private static final String ns = null;

    private String forecast_URL = null;
    private String station_name = null;

    public String parse(InputStream in, String station_name) throws XmlPullParserException, IOException {
        this.station_name = station_name;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            forecast_URL = readFeed(parser);
            return forecast_URL;
        } finally {
            in.close();
        }
    }

    private String readFeed(XmlPullParser parser) throws XmlPullParserException, IOException{
        List<Entry> entries = new ArrayList<>();
        forecast_URL = "";

        /*  read**()の基本動作
            開始**タグから終了**タグまでを順に読み取る
            途中、読み取りたい子タグが出てきたらそのメソッドへ移行（最下層のタグであればreadText()にてテキストを取得）
            無視するタグはskip()で読み飛ばす
         */
        parser.require(XmlPullParser.START_TAG, ns, "feed");
        // XMLフィードよりエントリー（観測データ名、アメダス地点名、アメダス地点コード）を取得
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            String name = parser.getName();
            if(name.equals("entry")){
                entries.add(readEntry(parser));
            } else{
                skip(parser);
            }
        }
        // 選択都道府県の明日の予報が掲載されているXMLフィードのURLを取得
        for(int i=0; i<entries.size(); i++){
            if(entries.get(i).title.equals("府県天気予報（Ｒ１）")){
                if(entries.get(i).author.equals(station_name)) {
                    forecast_URL = entries.get(i).id;
                }
            }
        }
        Log.d(DEBUG_TAG, station_name+", "+forecast_URL);
        return forecast_URL;
    }

    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException{

        String title = "";
        String author = "";
        String id = "";

        parser.require(XmlPullParser.START_TAG, ns, "entry");
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            String tag = parser.getName();
            if(tag.equals("title")){
                title = readTitle(parser);
            } else if(tag.equals("id")){
                id = readID(parser);
            } else if(tag.equals("author")){
                author = readAuthor(parser);
            } else{
                skip(parser);
            }
        }
        Entry entry = new Entry(title, id, author);
        return entry;
    }

    public static class Entry{
        public final String title;
        public final String id;
        public final String author;

        private Entry(String title, String id, String author){
            this.title = title;
            this.id = id;
            this.author = author;
        }

    }

    private String readTitle(XmlPullParser parser) throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    private String readID(XmlPullParser parser) throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, ns, "id");
        String id = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "id");
        return id;
    }

    private String readAuthor(XmlPullParser parser) throws XmlPullParserException, IOException {
        String author = "";
        parser.require(XmlPullParser.START_TAG, ns, "author");
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            String tag = parser.getName();
            if(tag.equals("name")){
                parser.require(XmlPullParser.START_TAG, ns, "name");
                author = readText(parser);
                parser.require(XmlPullParser.END_TAG, ns, "name");
            } else {
                skip(parser);
            }
        }
        return author;
    }

    // タグ内のテキストを取得
    private String readText(XmlPullParser parser) throws XmlPullParserException, IOException{
        String result = "";
        if(parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // タグを読み飛ばす
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
