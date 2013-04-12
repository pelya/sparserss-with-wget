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

package de.shandschuh.sparserss.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Date;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import de.shandschuh.sparserss.R;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.provider.FeedData;

public class WgetDownloader {
	
	private static final String MOBILE_USERAGENT = "--user-agent=Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; GT-N7000 Build/IMM76L; CyanogenMod-9.1.0) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	
	private static final String DESKTOP_USERAGENT = "--user-agent=Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:19.0) Gecko/20100101 Firefox/19.0";
	
	private static final String[] WGET_ARGS = { "-p", "-k", "-H", "-N", "--timeout=60", "--limit-rate=100k", "--wait=0.3", "--restrict-file-names=windows", "--tries=3", "--no-check-certificate" };
	
	public void download(Context context, String feedId, SharedPreferences preferences) {
		
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
			return;
		}

		// proxy = new Proxy(ZERO.equals(preferences.getString(Strings.SETTINGS_PROXYTYPE, ZERO)) ? Proxy.Type.HTTP : Proxy.Type.SOCKS, new InetSocketAddress(preferences.getString(Strings.SETTINGS_PROXYHOST, Strings.EMPTY), Integer.parseInt(preferences.getString(Strings.SETTINGS_PROXYPORT, Strings.DEFAULTPROXYPORT))));

		String selection = new StringBuilder(FeedData.FeedColumns.SAVEPAGES).append("=1").toString();

		Cursor cursor = context.getContentResolver().query(feedId == null ? FeedData.FeedColumns.CONTENT_URI : FeedData.FeedColumns.CONTENT_URI(feedId), null, selection, null, null);
		
		int urlPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);
		
		int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);
		
		int lastUpdatePosition = cursor.getColumnIndex(FeedData.FeedColumns.REALLASTUPDATE);
		
		while(cursor.moveToNext()) {
			String id = cursor.getString(idPosition);
			String feedUrl = cursor.getString(urlPosition);
		}
		cursor.close();
	}
}
