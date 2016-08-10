package com.sam_chordas.android.stockhawk;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * Created by rashwan on 8/10/16.
 */

public class StockHawkWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId: appWidgetIds) {
            Intent intent = new Intent(context,StockHawkWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews rv = new RemoteViews(context.getPackageName(),R.layout.widget);
            rv.setRemoteAdapter(R.id.list_view_widget,intent);

            appWidgetManager.updateAppWidget(appWidgetId,rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
