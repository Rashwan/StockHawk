package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;

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
    private static final String STOCKDETAILSTAG = StockDetailsActivity.class.getSimpleName();
    private String quoteName;
    private OkHttpClient okHttpClient;
    private LineChartView chart;

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
        Intent intent  = getIntent();
        quoteName = intent.getStringExtra(EXTRA_QUOTE);
        okHttpClient = new OkHttpClient();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String todayDate = dateFormat.format(calendar.getTime());
        Log.d("TODAY DATE",todayDate);

        //// TODO: 8/7/16 Make it dependant on screen size
        calendar.add(Calendar.DAY_OF_MONTH,-15);
        String minusMonthDate = dateFormat.format(calendar.getTime());
        Log.d("Minus Month DATE",minusMonthDate);

        StringBuilder urlString = new StringBuilder();
        urlString.append("https://query.yahooapis.com/v1/public/yql?q=");
        try {
            urlString.append(URLEncoder.encode("select Date,Close from yahoo.finance.historicaldata where symbol "
                    + " = ", "UTF-8"));
            urlString.append(URLEncoder.encode("\"" + quoteName + "\" ","UTF-8"));
            urlString.append(URLEncoder.encode("and startDate = ","UTF-8"));
            urlString.append(URLEncoder.encode("\"" + minusMonthDate + "\"","UTF-8"));
            urlString.append(URLEncoder.encode("and endDate = ","UTF-8"));
            urlString.append(URLEncoder.encode("\"" + todayDate + "\" ","UTF-8"));
            urlString.append(URLEncoder.encode("| sort(field=\"Date\")","UTF-8"));
            urlString.append("&format=json&diagnostics=true&env=http%3A%2F%2Fdatatables.org%2Falltables.env&callback=");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String detailsUrl = urlString.toString();
        Log.d("Query URL",detailsUrl);
        Request request = new Request.Builder().url(detailsUrl).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(STOCKDETAILSTAG,"request failed",e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Log.d(STOCKDETAILSTAG,result);
                try {
                    String[] labels = Utils.jsonToDates(result);
                    float[] data = Utils.jsonToClosedValue(result);
                    Pair<Integer,Integer> minMaxValues = Utils.getMinValue(data);
                    chart.setAxisBorderValues(minMaxValues.first-1,minMaxValues.second+1);
                    if (minMaxValues.second - minMaxValues.first > 30) {
                        chart.setStep(10);
                    }
                    LineSet lineSet = new LineSet(labels,data);
                    lineSet.setColor(getResources().getColor(android.R.color.white));
                    chart.setAxisLabelsSpacing(10f);
                    chart.setLabelsColor(getResources().getColor(android.R.color.white));
                    chart.addData(lineSet);
                    chart.setAxisColor(getResources().getColor(android.R.color.white));
                    chart.show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
