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
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URL;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Build;

import de.shandschuh.sparserss.R;
import de.shandschuh.sparserss.Strings;
import de.shandschuh.sparserss.provider.FeedDataContentProvider;
import de.shandschuh.sparserss.provider.FeedData;

public class WgetDownloader {
	
	private static final String WGET = "wget";
	
	private static final String MOBILE_USERAGENT = "--user-agent=Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; GT-N7000 Build/IMM76L; CyanogenMod-9.1.0) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	
	private static final String DESKTOP_USERAGENT = "--user-agent=Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:19.0) Gecko/20100101 Firefox/19.0";
	
	private static final String[] WGET_ARGS = { "--tries=3", "--retry-connrefused", "--timeout=60", "--limit-rate=30k", "--wait=0.3", "--restrict-file-names=windows", "--no-check-certificate", "--timestamping", "--force-directories", "--convert-links", "--page-requisites", "--span-hosts" };
	
	private static final String WGET_BINARY_URL = "https://github.com/pelya/wget-android/blob/master/android/wget?raw=true";
	
	private static final String WGET_BINARY_ARCH = "armeabi";
	
	private static final String WGET_TARGET_FILE_LOG = "\nSaving to: ";
	
	private static final String ZERO = "0";
	
	private static boolean running = false;
	
	public static boolean architectureSupported() {
		if (WGET_BINARY_ARCH.equals(Build.CPU_ABI) || WGET_BINARY_ARCH.equals(Build.CPU_ABI2)) {
			return true;
		}
		return false;
	}
	
	public static void download(final Context context, final String feedId, final SharedPreferences preferences) {
		if (running) {
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					running = true;
					downloadThread(context, feedId, preferences);
				} finally {
					running = false;
				}
			}
		}).start();
	}
	
	private static void downloadThread(Context context, String feedId, SharedPreferences preferences) {
		
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if ( !checkWifi(connectivityManager) || !downloadWgetBinary(context) ) {
			return;
		}
		
		deleteWebCacheIfNeeded(preferences);
		
		new File(FeedDataContentProvider.WEBCACHEFOLDER).mkdirs();
		
		Cursor feedCursor = context.getContentResolver().query(feedId == null ? FeedData.FeedColumns.CONTENT_URI : FeedData.FeedColumns.CONTENT_URI(feedId),
				new String[] {FeedData.FeedColumns._ID, FeedData.FeedColumns.SAVEPAGESDESKTOP}, FeedData.FeedColumns.SAVEPAGES + " = 1", null, null);
		
		while (feedCursor.moveToNext()) {
			boolean requestDesktopPage = (feedCursor.getInt(1) == 1);
			
			Cursor entryCursor = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI(feedCursor.getString(0)),
					new String[] {FeedData.EntryColumns._ID, FeedData.EntryColumns.LINK},
					FeedData.EntryColumns.SAVEDPAGE + " IS NULL AND ( " + // Screw StringBuilder! It makes code unreadable
					FeedData.EntryColumns.READDATE + " IS NULL OR " +
					FeedData.EntryColumns.FAVORITE + " = 1 )", null, null);
			
			while (entryCursor.moveToNext()) {
				
				if ( !checkWifi(connectivityManager) ) {
					entryCursor.close();
					feedCursor.close();
					return;
				}
				
				ArrayList<String> args = new ArrayList<String>();
				args.add(context.getFilesDir() + "/wget");
				args.addAll(Arrays.asList(WGET_ARGS));
				args.add(requestDesktopPage ? DESKTOP_USERAGENT : MOBILE_USERAGENT);
				args.add(entryCursor.getString(1));
				
				//System.out.println("Launching wget for page: " + entryCursor.getString(1));
				StringBuilder log = new StringBuilder();
				int i1 = -1, i2 = -1;
				
				Process process = null;
				try {
					ProcessBuilder processBuilder = new ProcessBuilder().directory(new File(FeedDataContentProvider.WEBCACHEFOLDER)).command(args).redirectErrorStream(true);
					
					// SOCKS proxy is not supported
					if (preferences.getBoolean(Strings.SETTINGS_PROXYENABLED, false) && ZERO.equals(preferences.getString(Strings.SETTINGS_PROXYTYPE, ZERO))) {
						processBuilder.environment().put("http_proxy", "http://" + preferences.getString(Strings.SETTINGS_PROXYHOST, Strings.EMPTY) + ":" + preferences.getString(Strings.SETTINGS_PROXYPORT, Strings.DEFAULTPROXYPORT) + "/");
						processBuilder.environment().put("https_proxy", "http://" + preferences.getString(Strings.SETTINGS_PROXYHOST, Strings.EMPTY) + ":" + preferences.getString(Strings.SETTINGS_PROXYPORT, Strings.DEFAULTPROXYPORT) + "/");
					}

					process = processBuilder.start();
					while (true) {
						InputStream out = process.getInputStream();
						byte buf[] = new byte[256];
						int len = out.read(buf);
						if (len < 0) {
							break;
						}
						if (i1 <= 0 || i2 <= 0) {
							log.append(new String(buf, 0, len));
							i1 = log.indexOf("\nSaving to: ");
							i2 = i1 > 0 ? log.indexOf("\n", i1 + 1) : -1;
						}
						//System.out.println("wget log: " + new String(buf, 0, len));
						if ( !checkWifi(connectivityManager) ) {
							entryCursor.close();
							feedCursor.close();
							return;
						}
					}
				} catch (Exception e) {
				} finally {
					if (process != null) {
						try {
							process.getInputStream().close();
							process.getOutputStream().close();
							process.destroy();
						} catch (Exception e) {
						}
					}
				}
				
				ContentValues values = new ContentValues();
				if (i1 > 0 && i2 > 0) {
					values.put(FeedData.EntryColumns.SAVEDPAGE, FeedDataContentProvider.WEBCACHEFOLDER + "/" + log.substring(i1 + WGET_TARGET_FILE_LOG.length() + 1, i2 - 1));
					//System.out.println("wget target file: " + log.substring(i1 + WGET_TARGET_FILE_LOG.length() + 1, i2 - 1));
				} else {
					values.put(FeedData.EntryColumns.SAVEDPAGE, "");
					//System.out.println("wget - failed to get target file");
				}
				context.getContentResolver().update(FeedData.EntryColumns.ENTRY_CONTENT_URI(entryCursor.getString(0)), values, null, null);
			}
			entryCursor.close();
		}
		feedCursor.close();
	}
	
	private static boolean checkWifi(ConnectivityManager connectivityManager) {
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
			return false;
		}
		return true;
	}

	private static boolean downloadWgetBinary(Context context) {
		
		if( !architectureSupported() ) {
			//System.out.println("Cannot download wget - incompatible architecture, only " + WGET_BINARY_ARCH + " is supported");
			return false;
		}
		
		File wgetPath = new File(context.getFilesDir(), WGET);
		
		if( wgetPath.exists() /* && wgetPath.canExecute() */ ) {
			return true;
		}
		context.getFilesDir().mkdirs();
		//System.out.println("Downloading wget from: " + WGET_BINARY_URL + " to: " + wgetPath);
		BufferedOutputStream out = null;
		try {
			InputStream in = new URL(WGET_BINARY_URL).openStream();
			out = new BufferedOutputStream(new FileOutputStream(wgetPath));
			byte[] buf = new byte[1024];
			int len = 0;
			while (true) {
				len = in.read(buf);
				if (len < 0) {
					break;
				}
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			//System.out.println("Error downloading wget from: " + WGET_BINARY_URL + " to: " + wgetPath + " : " + e);
			if (out != null) {
				try {
					out.close();
				} catch (Exception ee) {
				}
			}
			wgetPath.delete();
			return false;
		}
		
		try {
			Process process = new ProcessBuilder().command("chmod", "755", wgetPath.toString()).redirectErrorStream(true).start();
			byte buf[] = new byte[1024];
			process.getInputStream().read(buf);
			process.getInputStream().close();
			process.waitFor();
		} catch (Exception e) {
			//System.out.println("Downloading wget from: " + WGET_BINARY_URL + " to: " + wgetPath + " : error setting permissions");
			wgetPath.delete();
			return false;
		}
		//System.out.println("Downloading wget from: " + WGET_BINARY_URL + " to: " + wgetPath + " : done");
		return true;
	}
	
	private static void deleteWebCacheIfNeeded(SharedPreferences preferences) {
		long keepTime = Long.parseLong(preferences.getString(Strings.SETTINGS_KEEPTIME, "4"))*86400000l;
		if (new File(FeedDataContentProvider.WEBCACHEFOLDER).lastModified() + keepTime < System.currentTimeMillis() / 1000) {
			deleteRecursively(new File(FeedDataContentProvider.WEBCACHEFOLDER));
		}
	}
	
	public static boolean deleteRecursively(File dir)
	{
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteRecursively(new File(dir, children[i]));
				if (!success)
					return false;
			}
		}
		return dir.delete();
	}
}
