package com.example.fluctuatenotice.workers;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForecastScanner {
    private static final String DEBUG_TAG = "TEMPERATURE";

    // We don't use namespaces
    private static final String ns = null;

    public double[] parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readInfo(parser);
        } finally {
            in.close();
        }
    }

    public static class Item{
        public final String name;
        public final String code;
        public final List<Temperature> temp_list;

        public Item(String name, String code, List<Temperature> temp_list){
            this.name = name;
            this.code = code;
            this.temp_list = temp_list;
        }
    }

    private double[] readInfo(XmlPullParser parser) throws XmlPullParserException, IOException{
        List<Item> items = new ArrayList<>();

        // Itemタグが見つかるまで何度も階層を下げる
        parser.require(XmlPullParser.START_TAG, ns, "Report");
        while(parser.next()!=XmlPullParser.END_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if(parser.getName().equals("Body")) {
                parser.require(XmlPullParser.START_TAG, ns, "Body");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    if(parser.getName().equals("MeteorologicalInfos")
                            && parser.getAttributeValue(0).equals("地点予報")) {
                        parser.require(XmlPullParser.START_TAG, ns, "MeteorologicalInfos");
                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG) {
                                continue;
                            }
                            if (parser.getName().equals("TimeSeriesInfo")) {
                                if (parser.getEventType() != XmlPullParser.START_TAG) {
                                    continue;
                                }
                                while (parser.next() != XmlPullParser.END_TAG) {
                                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                                        continue;
                                    }
                                    if (parser.getName().equals("Item")) {
                                        items.add(readItem(parser));
                                    } else {
                                        skip(parser);
                                    }
                                }
                            } else {
                                skip(parser);
                            }
                        }
                    } else {
                        skip(parser);
                    }
                }
            } else {
                skip(parser);
            }
        }

        // 地点予報が２箇所存在するので、現在のフィードにあわせ１個目のみを解析
        List<Double> temperatures = new ArrayList<>();
        for(int i=0; i<items.get(0).temp_list.size(); i++){
            temperatures.add(items.get(0).temp_list.get(i).temperature);
        }
        double high_temperature = Collections.max(temperatures);
        double low_temperature = Collections.min(temperatures);
        double[] high_low_temperature = {high_temperature, low_temperature};

        Log.d(DEBUG_TAG, high_temperature+", "+low_temperature);

        return high_low_temperature;

    }

    private Item readItem(XmlPullParser parser) throws XmlPullParserException, IOException{

        String name = "";
        String code = "";
        List<Temperature> temp_list = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "Item");
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            if(parser.getName().equals("Kind")){
                parser.require(XmlPullParser.START_TAG, ns, "Kind");
                while(parser.next()!=XmlPullParser.END_TAG){
                    if(parser.getEventType()!=XmlPullParser.START_TAG){
                        continue;
                    }
                    if(parser.getName().equals("Property")){
                        parser.require(XmlPullParser.START_TAG, ns, "Property");
                        temp_list.add(readProperty(parser));
                    } else{
                        skip(parser);
                    }
                }
            } else if(parser.getName().equals("Station")){
                String[] station = readStation(parser);
                name = station[0];
                code = station[1];
            } else{
                skip(parser);
            }
        }
        Item item = new Item(name, code, temp_list);
        return item;

    }

    // 気温の取得
    public static class Temperature{
        public final String type;
        public final double temperature;

        public Temperature(String type, double temperature){
            this.type = type;
            this.temperature = temperature;
        }
    }

    private Temperature readProperty(XmlPullParser parser) throws XmlPullParserException, IOException{

        Temperature temperature = new Temperature("初期値", 0.0);

        parser.require(XmlPullParser.START_TAG, ns, "Property");
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            if(parser.getName().equals("TemperaturePart")){
                parser.require(XmlPullParser.START_TAG, ns, "TemperaturePart");
                temperature = readTemperaturePart(parser);
            } else{
                skip(parser);
            }
        }
        return temperature;

    }

    private Temperature readTemperaturePart(XmlPullParser parser) throws XmlPullParserException, IOException{

        double temperature = 0.0;
        String type = "";

        parser.require(XmlPullParser.START_TAG, ns, "TemperaturePart");
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            if(parser.getName().equals("jmx_eb:Temperature")){
                parser.require(XmlPullParser.START_TAG, ns, "jmx_eb:Temperature");
                type = parser.getAttributeValue(2);
                temperature = readJmx_eb(parser);
            } else{
                skip(parser);
            }
        }
        Temperature temperature_set = new Temperature(type, temperature);
        return temperature_set;

    }

    private double readJmx_eb(XmlPullParser parser) throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, ns, "jmx_eb:Temperature");
        double temperature = Double.parseDouble(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, "jmx_eb:Temperature");
        return temperature;
    }

    // 観測地点の取得（念の為）
    private String[] readStation(XmlPullParser parser) throws XmlPullParserException, IOException{

        String name = "";
        String code = "";

        parser.require(XmlPullParser.START_TAG, ns, "Station");
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.getEventType()!=XmlPullParser.START_TAG){
                continue;
            }
            if(parser.getName().equals("Name")){
                name = readName(parser);
            } else if(parser.getName().equals("Code")){
                code = readCode(parser);
            } else{
                skip(parser);
            }
        }

        String[] station = {name, code};
        return station;

    }

    private String readName(XmlPullParser parser) throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, ns, "Name");
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Name");
        return name;
    }

    private String readCode(XmlPullParser parser) throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, ns, "Code");
        String code = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Code");
        return code;
    }

    private String readText(XmlPullParser parser) throws XmlPullParserException, IOException{
        String result = "";
        if(parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

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
