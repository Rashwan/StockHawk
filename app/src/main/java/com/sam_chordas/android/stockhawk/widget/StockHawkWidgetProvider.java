package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by rashwan on 8/10/16.
 */

public class StockHawkWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_SYMBOL = "com.sam_chordas.android.stockhawk.widget.EXTRA_SYMBOL";
    public static final String EXTRA_PRICE = "com.sam_chordas.android.stockhawk.widget.EXTRA_PRICE";
    public static final String ACTION_DETAILS_ACTIVITY = "com.sam_chordas.android.stockhawk.widget.ACTION_DETAILS_ACTIVITY";
    public static final String ACTION_MY_STOCKS_ACTIVITY = "ccom.sam_chordas.android.stockhawk.widget.ACTION_MY_STOCKS_ACTIVITY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_DETAILS_ACTIVITY)){
            String symbol = intent.getStringExtra(EXTRA_SYMBOL);
            String price = intent.getStringExtra(EXTRA_PRICE);
            Intent stockDetailsIntent = StockDetailsActivity.getStockDetailsIntent(context,symbol,price);
            stockDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            stockDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(stockDetailsIntent);
        }else if (intent.getAction().equals(ACTION_MY_STOCKS_ACTIVITY)){
            Intent myStocksIntent = new Intent(context, MyStocksActivity.class);
            myStocksIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myStocksIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(myStocksIntent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId: appWidgetIds) {
            Intent intent = new Intent(context,StockHawkWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
            rv.setRemoteAdapter(R.id.list_view_widget,intent);

            Intent detailsIntent = new Intent(context,StockHawkWidgetProvider.class);
            detailsIntent.setAction(ACTION_DETAILS_ACTIVITY);
            detailsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);
            detailsIntent.setData(Uri.parse(detailsIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent detailsPendingIntent = PendingIntent.getBroadcast(context,0,detailsIntent
                    ,PendingIntent.FLAG_UPDATE_CURRENT);

            Intent myStocksIntent = new Intent(context,StockHawkWidgetProvider.class);
            myStocksIntent.setAction(ACTION_MY_STOCKS_ACTIVITY);
            myStocksIntent.setData(Uri.parse(myStocksIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent myStocksPendingIntent = PendingIntent.getBroadcast(context,1,myStocksIntent
            ,PendingIntent.FLAG_UPDATE_CURRENT);

            rv.setPendingIntentTemplate(R.id.list_view_widget,detailsPendingIntent);
            rv.setOnClickPendingIntent(R.id.text_widget_title,myStocksPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId,rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    public static void refreshWidget(Context context){
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context,StockHawkWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(cn);
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetIds,R.id.list_view_widget);
    }
}
