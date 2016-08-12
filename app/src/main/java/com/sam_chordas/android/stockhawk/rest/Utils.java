package com.sam_chordas.android.stockhawk.rest;

import android.app.Application;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Pair;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    if (!jsonObject.getString("Bid").equals("null")){
                        batchOperations.add(buildBatchOperation(jsonObject));
                    }else {
                        throw new IllegalArgumentException(jsonObject.getString("symbol"));
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {

            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String[] jsonToLabels(String json) throws JSONException {
        SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat chartDateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.length() != 0) {
            JSONArray results = jsonObject.getJSONObject("query").getJSONObject("results")
                    .getJSONArray("quote");
            String[] dates = new String[results.length()];
            for (int i = 0; i < results.length(); i++) {
                try {
                    Date date = jsonDateFormat.parse(results.getJSONObject(i).getString("Date"));
                    calendar.setTime(date);
                    String todayDate = chartDateFormat.format(calendar.getTime());
                    dates[i] = todayDate;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            Log.d(LOG_TAG, Arrays.toString(dates));
            return dates;
        } else {
            return new String[0];
        }
    }

    public static float[] jsonToClosedValue(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.length() != 0) {
            JSONArray results = jsonObject.getJSONObject("query").getJSONObject("results")
                    .getJSONArray("quote");
            float[] closedValues = new float[results.length()];
            for (int i = 0; i < results.length(); i++) {
                closedValues[i] = Float.valueOf(results.getJSONObject(i).getString("Close"));
            }
            Log.d(LOG_TAG, Arrays.toString(closedValues));
            return closedValues;
        } else {
            return new float[0];
        }
    }

    public static Pair<Integer,Integer> getMinMaxValue(float[] values){
        int min = Math.round(values[0]);
        int max = Math.round(values[0]);
        for (float value: values) {
            min = Math.round(Math.min(min,value));
            max = Math.round(Math.max(max,value));
        }
        return new Pair<>(min,max);
    }
    public static float getAverageValue(float[] values){
        float sum = 0;
        for (float value: values) {
            sum += value;
        }
        return sum/values.length;
    }
    public static Boolean isScreenSW(int smallestWidthDp){
        Configuration config = Resources.getSystem().getConfiguration();
        return config.smallestScreenWidthDp >= smallestWidthDp;
    }
    public static Boolean isNetworkAvailable(Application application){
        ConnectivityManager connectivityManager  = (ConnectivityManager)
                application.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format(Locale.getDefault(),"%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    private static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format(Locale.getDefault(),"%.2f", round);
        StringBuilder changeBuffer = new StringBuilder(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    private static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }
}
