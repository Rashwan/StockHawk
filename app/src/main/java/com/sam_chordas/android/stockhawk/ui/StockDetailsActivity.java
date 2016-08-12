package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
    private static final String LOG_TAG = StockDetailsActivity.class.getSimpleName();
    private static final String EXTRA_SYMBOL = "com.sam_chordas.android.stockhawk.ui.EXTRA_SYMBOL";
    private static final String EXTRA_PRICE = "com.sam_chordas.android.stockhawk.ui.EXTRA_PRICE";
    private static final String BUNDLE_RESPONSE = "BUNDLE_RESPONSE";
    private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String SELECT = "select Date,Close from yahoo.finance.historicaldata where symbol = ";
    private static final String START_DATE = "and startDate = ";
    private static final String END_DATE = "and endDate = ";
    private static final String SORT = "| sort(field=\"Date\")";
    private static final String FORMAT = "&format=json&diagnostics=true&env=http%3A%2F%2Fdatatables.org%2Falltables.env&callback=";
    private static final String ENCODING = "UTF-8";
    private String quoteName;
    private String currentPrice;
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
    private ProgressBar progressBar;
    private TextView tvError;
    private Button buttonRefresh;
    private LinearLayout layoutOffline;
    private TextView tvCurrentPrice;

    public static Intent getStockDetailsIntent(Context context,String symbol,String price){
        Intent intent = new Intent(context,StockDetailsActivity.class);
        intent.putExtra(EXTRA_SYMBOL,symbol);
        intent.putExtra(EXTRA_PRICE,price);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        chart = (LineChartView) findViewById(R.id.linechart);
        tvAverage = (TextView) findViewById(R.id.text_average);
        tvCurrentPrice = (TextView) findViewById(R.id.text_current_price);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        tvError = (TextView) findViewById(R.id.text_error);
        buttonRefresh = (Button) findViewById(R.id.button_refresh);
        layoutOffline = (LinearLayout) findViewById(R.id.layout_offline);
        Intent intent  = getIntent();
        quoteName = intent.getStringExtra(EXTRA_SYMBOL);
        currentPrice = intent.getStringExtra(EXTRA_PRICE);
        getSupportActionBar().setTitle(quoteName);
        isTablet = Utils.isScreenSW(600);

        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isNetworkAvailable(getApplication())){
                    setDates();
                    fetchDetails(buildQuery());
                }
            }
        });

        if (savedInstanceState == null){
            determineLayout();
        }else {
            if (savedInstanceState.getString(BUNDLE_RESPONSE) == null){
                determineLayout();
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

    private void determineLayout() {
        if (Utils.isNetworkAvailable(getApplication())){
            setDates();
            fetchDetails(buildQuery());
        }
        else {
            setErrorLayout(true);
        }
    }

    private void setErrorLayout(final boolean offline) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (offline) {
                    tvError.setText(R.string.msg_no_internet_connection);
                }else {
                    tvError.setText(R.string.msg_server_error);
                }
                progressBar.setVisibility(View.GONE);
                layoutOffline.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchDetails(Request request) {
        layoutOffline.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setErrorLayout(false);
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


    private void populateStockDetails(final float averageValue){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                layoutOffline.setVisibility(View.GONE);
                tvCurrentPrice.setText(getString(R.string.text_current_price,currentPrice));
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
        urlString.append(BASE_URL);
        try {
            urlString.append(URLEncoder.encode(SELECT, ENCODING));
            urlString.append(URLEncoder.encode("\"" + quoteName + "\" ", ENCODING));
            urlString.append(URLEncoder.encode(START_DATE, ENCODING));
            urlString.append(URLEncoder.encode("\"" + startDate + "\"", ENCODING));
            urlString.append(URLEncoder.encode(END_DATE, ENCODING));
            urlString.append(URLEncoder.encode("\"" + endDate + "\" ", ENCODING));
            urlString.append(URLEncoder.encode(SORT, ENCODING));
            urlString.append(FORMAT);
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
        populateStockDetails(averageValue);
    }

}
