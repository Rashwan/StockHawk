package com.sam_chordas.android.stockhawk;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by rashwan on 8/10/16.
 */

public class StockHawkWidgetService extends RemoteViewsService{
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoteViewsFactory(this.getApplicationContext(),intent);
    }
}
class WidgetRemoteViewsFactory implements  RemoteViewsService.RemoteViewsFactory{

    private Context context;
    private int widgetId;
    private Cursor cursor;
    private String symbol;
    private String bidPrice;
    public WidgetRemoteViewsFactory(Context context,Intent intent) {
        this.context = context;
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        if (cursor!= null){
            cursor.close();
        }
        cursor = context.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,new String[]{
                QuoteColumns.SYMBOL,QuoteColumns.BIDPRICE}, QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},null);
    }

    @Override
    public void onDestroy() {
        if (cursor != null){
            cursor.close();
        }
    }

    @Override
    public int getCount() {
        Log.d("WIDGET",cursor.getCount() + "");
        return cursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (cursor.moveToPosition(position)){
            final int symbolColumnIndex = cursor.getColumnIndex(QuoteColumns.SYMBOL);
            final int priceColumnIndex = cursor.getColumnIndex(QuoteColumns.BIDPRICE);
            symbol = cursor.getString(symbolColumnIndex);
            bidPrice = cursor.getString(priceColumnIndex);
        }
        RemoteViews rv = new RemoteViews(context.getPackageName(),R.layout.widget_list_item);
        rv.setTextViewText(R.id.text_widget_symbol,symbol);
        rv.setTextViewText(R.id.text_widget_stock_price,bidPrice);

        Bundle bundle = new Bundle();
        bundle.putString(StockHawkWidgetProvider.EXTRA_SYMBOL,symbol);
        bundle.putString(StockHawkWidgetProvider.EXTRA_PRICE,bidPrice);
        Intent detailsIntent = new Intent();
        detailsIntent.putExtras(bundle);
        rv.setOnClickFillInIntent(R.id.layout_widget_item,detailsIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
