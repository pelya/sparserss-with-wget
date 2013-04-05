/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package de.shandschuh.sparserss.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;
import de.shandschuh.sparserss.MainTabActivity;
import de.shandschuh.sparserss.R;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.provider.FeedData;

public class SparseRSSAppWidgetProviderSmall extends SparseRSSAppWidgetProvider {
	
	@Override
	void updateAppWidget(Context context, int appWidgetId, boolean hideRead, String entryCount, String feedIds, int backgroundColor) {
		updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, feedIds);
	}
	
	private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String feedIds) {
		StringBuilder selection = new StringBuilder();
		
		selection.append(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL);
		
		if (feedIds.length() > 0) {
			if (selection.length() > 0) {
				selection.append(Strings.DB_AND);
			}
			selection.append(FeedData.EntryColumns.FEED_ID).append(" IN ("+feedIds).append(')');
		}
		
		Cursor cursor = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {FeedData.EntryColumns._ID}, selection.toString(), null, null);
		
		int k = 0;
		while (cursor.moveToNext()) {
			k++;
		}
		cursor.close();
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.homescreenwidget_small);
		views.setOnClickPendingIntent(R.id.news_counter_layout, PendingIntent.getActivity(context, 0, new Intent(context, MainTabActivity.class), 0));
		//views.setOnClickPendingIntent(R.id.feed_icon_small, PendingIntent.getActivity(context, 0, new Intent(context, MainTabActivity.class), 0));
		//views.setOnClickPendingIntent(R.id.news_counter, PendingIntent.getActivity(context, 0, new Intent(context, MainTabActivity.class), 0));
		views.setTextViewText(R.id.news_counter, String.valueOf(k));
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}
