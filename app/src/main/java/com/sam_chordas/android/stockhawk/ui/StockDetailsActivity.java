package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StockDetailsActivity extends AppCompatActivity {
    private static final String EXTRA_QUOTE = "com.sam_chordas.android.stockhawk.ui.EXTRA_QUOTE";
    private static final String LOG_TAG = StockDetailsActivity.class.getSimpleName();
    private static final String BUNDLE_RESPONSE = "BUNDLE_RESPONSE";
    private String quoteName;
    private OkHttpClient okHttpClient;
    private LineChartView chart;
    private TextView tvAverage;
    private Boolean isTablet = false;
    private String endDate;
    private String startDate;
    private String[] labels;
    private float[] data;
    private Pair<Integer,Integer> minMaxValues;
    private  String result;


    public static Intent getStockDetailsIntent(Context context,String quote){
        Intent intent = new Intent(context,StockDetailsActivity.class);
        intent.putExtra(EXTRA_QUOTE,quote);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);
        chart = (LineChartView) findViewById(R.id.linechart);
        TextView tvQuoteName = (TextView) findViewById(R.id.text_quote_name);
        tvAverage = (TextView) findViewById(R.id.text_average);
        Intent intent  = getIntent();
        quoteName = intent.getStringExtra(EXTRA_QUOTE);
        tvQuoteName.setText(quoteName);

        if (savedInstanceState == null){
            setDates();
            fetchDetails(buildQuery());
        }else {
            if (savedInstanceState.getString(BUNDLE_RESPONSE) == null){
                setDates();
                fetchDetails(buildQuery());
            }else {
                result = savedInstanceState.getString(BUNDLE_RESPONSE);
                try {
                    setQuoteDetails(result);
                    setChartDetails();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void fetchDetails(Request request) {
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(LOG_TAG,"request failed",e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                result = response.body().string();
                try {
                    setQuoteDetails(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setChartDetails();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_RESPONSE,result);
    }

    private void setChartDetails() {
        LineSet lineSet = new LineSet(labels,data);
        lineSet.setColor(getResources().getColor(android.R.color.white));
        chart.setLabelsColor(getResources().getColor(android.R.color.white));
        chart.setAxisColor(getResources().getColor(android.R.color.white));
        chart.setAxisLabelsSpacing(10f);
        chart.setAxisBorderValues(minMaxValues.first-1,minMaxValues.second+1);
        if (minMaxValues.second - minMaxValues.first > 30) {
            chart.setStep(10);
        }
        chart.addData(lineSet);
        chart.show();
    }


    private void setAverageValue(final float averageValue){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isTablet) {
                    tvAverage.setText(getString(R.string.text_average_value_tablet, averageValue));
                }else {
                    tvAverage.setText(getString(R.string.text_average_value_phone, averageValue));
                }
            }
        });
    }
    private void setDates(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        endDate = dateFormat.format(calendar.getTime());
        isTablet = Utils.isScreenSW(600);
        if (isTablet){
            calendar.add(Calendar.MONTH, -1);
        }else {
            calendar.add(Calendar.DAY_OF_MONTH, -15);
        }
        startDate = dateFormat.format(calendar.getTime());
    }

    private Request buildQuery() {
        okHttpClient = new OkHttpClient();
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://query.yahooapis.com/v1/public/yql?q=");
        try {
            urlString.append(URLEncoder.encode("select Date,Close from yahoo.finance.historicaldata where symbol "
                    + " = ", "UTF-8"));
            urlString.append(URLEncoder.encode("\"" + quoteName + "\" ","UTF-8"));
            urlString.append(URLEncoder.encode("and startDate = ","UTF-8"));
            urlString.append(URLEncoder.encode("\"" + startDate + "\"","UTF-8"));
            urlString.append(URLEncoder.encode("and endDate = ","UTF-8"));
            urlString.append(URLEncoder.encode("\"" + endDate + "\" ","UTF-8"));
            urlString.append(URLEncoder.encode("| sort(field=\"Date\")","UTF-8"));
            urlString.append("&format=json&diagnostics=true&env=http%3A%2F%2Fdatatables.org%2Falltables.env&callback=");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String detailsUrl =  urlString.toString();
        return new Request.Builder().url(detailsUrl).build();
    }
    private void setQuoteDetails(String result) throws JSONException {
        labels = Utils.jsonToLabels(result);
        data = Utils.jsonToClosedValue(result);
        minMaxValues = Utils.getMinMaxValue(data);
        float averageValue = Utils.getAverageValue(data);
        setAverageValue(averageValue);
    }

}
