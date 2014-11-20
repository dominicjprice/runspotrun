package uk.ac.horizon.runspotrun.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import uk.ac.horizon.runspotrun.Constants;
import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.service.LogUploader;
import uk.ac.horizon.runspotrun.service.VideoUploader;
import uk.ac.horizon.runspotrun.util.Cookies;
import uk.ac.horizon.runspotrun.util.Session;

public class LogonActivity
extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(this, LogUploader.class));
		startService(new Intent(this, VideoUploader.class));
		
		final Session session = new Session(this);		
		(new AsyncTask<Void, Void, Boolean>(){

			@SuppressLint("SetJavaScriptEnabled")
			@Override
			protected void onPostExecute(Boolean result) {
				if(result.booleanValue()) {
					startActivity(new Intent(LogonActivity.this, HomeActivity.class));
					finish();
				} else {
					setContentView(R.layout.activity_logon);	
					final WebView view = (WebView)findViewById(R.id.logon_web_view);
					view.getSettings().setBuiltInZoomControls(true);
					view.getSettings().setSupportZoom(true);
					view.getSettings().setJavaScriptEnabled(true);					    
					view.clearSslPreferences();
					view.setOnTouchListener(new View.OnTouchListener() {
						
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
							case MotionEvent.ACTION_UP:
								if(!v.hasFocus())
									v.requestFocus();
		                        break;
							}
							return false;
						}
						
					});
					view.setWebViewClient(new WebViewClient(){
						
						@Override
						public boolean shouldOverrideUrlLoading(WebView view, String url) {
							if(url.toLowerCase(Locale.getDefault()).startsWith("mailto")) {
								Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					            startActivity(i);
								return true;
							}					
							return super.shouldOverrideUrlLoading(view, url);
						}
						
						@Override
						public void onPageStarted(WebView view, String url, Bitmap favicon) {
							view.setVisibility(View.INVISIBLE);
							super.onPageStarted(view, url, favicon);
						}

						@Override
						public void onPageFinished(WebView view, String url) {
							
							if(url.equals(Constants.SERVER_BASE_URL)
									|| url.startsWith(Constants.SERVER_BASE_URL + "#")) {
								CookieManager cm = CookieManager.getInstance();
								Cookies cookies = Cookies.parse(cm.getCookie(url));
								session.set(cookies.get("sessionid"), cookies.get("csrftoken"));
								startActivity(new Intent(LogonActivity.this, HomeActivity.class));
								finish();
							} else {
								view.setVisibility(View.VISIBLE);
								super.onPageFinished(view, url);
							}
						}
						
					});
					//view.loadUrl(Constants.SERVER_LOGIN_URL);
					
					StringBuilder buf = new StringBuilder();
					try {
						BufferedReader r = new BufferedReader(
								new InputStreamReader(getAssets().open("consent.html")));
						String line;
						while((line = r.readLine()) != null) 
							buf.append(line);
						r.close();
					} catch(IOException e) {
						buf = new StringBuilder();
						buf.append("An error occurred, please go back and try again.");
					}
					try {
						view.loadData(URLEncoder.encode(buf.toString(), "UTF-8").replaceAll("\\+"," "),
								"text/html", "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Log.e(Constants.LOG_TAG, e.getMessage(), e);
					}
				}
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(Constants.SERVER_TEST_LOGIN_URL);
				get.addHeader("Cookie", "csrftoken=" + session.getCsrfToken() 
						+ "; sessionid=" + session.getSessionId());
				get.addHeader("X-CSRFToken", session.getCsrfToken());
				
				boolean val = false;
				
				try {
					HttpResponse r = client.execute(get);
					r.getEntity().consumeContent();
					val = r.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
				} catch (Exception e) {
					Log.e(Constants.LOG_TAG, e.getMessage(), e);
					val = false;
				}
				
				if(!val) {
					get = new HttpGet(Constants.SERVER_LOGOUT_URL);
					get.addHeader("Cookie", "csrftoken=" + session.getCsrfToken() 
							+ "; sessionid=" + session.getSessionId());
					get.addHeader("X-CSRFToken", session.getCsrfToken());
					try {
						HttpResponse r = client.execute(get);
						r.getEntity().consumeContent();
					} catch (Exception e) {
						Log.w(Constants.LOG_TAG, e.getMessage(), e);
					}
				}
				
				return val;
			}
			
		}).execute(null, null);
	}
	
}
