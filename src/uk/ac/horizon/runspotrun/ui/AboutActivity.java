package uk.ac.horizon.runspotrun.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import uk.ac.horizon.runspotrun.Constants;
import uk.ac.horizon.runspotrun.R;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.app.Activity;
import android.content.Intent;

public class AboutActivity 
extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		final WebView v = (WebView)findViewById(R.id.activity_about_web_view);
		StringBuilder buf = new StringBuilder();
		try {
			BufferedReader r = new BufferedReader(
					new InputStreamReader(getAssets().open("about.html")));
			String line;
			while((line = r.readLine()) != null) 
				buf.append(line);
			r.close();
		} catch(IOException e) {
			buf = new StringBuilder();
			buf.append("An error occurred, please go back and try again.");
		}
		v.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if(url.toLowerCase().startsWith("mailto")) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		            startActivity(i);
					return true;
				}					
				return super.shouldOverrideUrlLoading(view, url);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				v.setVisibility(View.VISIBLE);
			}
		});
		try {
			v.loadData(URLEncoder.encode(buf.toString(), "UTF-8").replaceAll("\\+"," "),
					"text/html", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(Constants.LOG_TAG, e.getMessage(), e);
		}
	}

}
