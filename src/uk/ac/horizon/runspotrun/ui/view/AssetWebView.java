package uk.ac.horizon.runspotrun.ui.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.util.ViewResource;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AssetWebView
extends WebView {

	private final Context context;
	
	private String assetName;
	
	private int crossfadeID;
	
	private float minAlpha = 0f;
	
	private float maxAlpha = 1f;
	
	public AssetWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.context = context;
		init(attrs);
	}

	public AssetWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init(attrs);
	}

	public AssetWebView(Context context) {
		super(context);
		this.context = context;
		init(null);
	}
	
	@Override
	public void setWebViewClient(WebViewClient client) { }
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		ViewParent p = this;
		View v = null;
		while((p = p.getParent()) != null) 
			if(p instanceof View)
				v = (View)p;
		if(v != null && (v = v.findViewById(crossfadeID)) != null) {
			final View l = v;
			ViewCompat.setAlpha(l, maxAlpha);
			ViewCompat.setAlpha(this, minAlpha);
			super.setWebViewClient(new WebViewClient(){
				@Override
				public void onPageFinished(WebView webview, String url) {
					int duration = getResources().getInteger(
							android.R.integer.config_shortAnimTime);
					ViewCompat.animate(AssetWebView.this)
							.alpha(maxAlpha).setDuration(duration);
					ViewCompat.animate(l).alpha(minAlpha).setDuration(duration)
							.setListener(new ViewPropertyAnimatorListener() {
								@Override
								public void onAnimationStart(View v) {}
								@Override
								public void onAnimationEnd(View v) {
									v.setVisibility(View.GONE);
								}
								@Override
								public void onAnimationCancel(View v) {}
							});
				}
				@Override
				public boolean shouldOverrideUrlLoading(WebView v, String url) {
					context.startActivity(new Intent(
							Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				}
			});
		}
		
		if(assetName != null)
			loadAsset(assetName);
	}
	
	private void init(AttributeSet attrs) {
		setBackgroundColor(
				getResources().getColor((android.R.color.transparent)));
		if(attrs != null) {
			String id = attrs.getAttributeValue(App.XMLNS_URI, "crossfade");
			if(id != null)
				crossfadeID = new ViewResource(context, this).getResourceID(id);
			assetName = attrs.getAttributeValue(App.XMLNS_URI, "resource");
			String min = attrs.getAttributeValue(
					App.XMLNS_URI, "crossfade_minalpha");
			if(min != null)
				minAlpha = Float.parseFloat(min);
			String max = attrs.getAttributeValue(
					App.XMLNS_URI, "crossfade_maxalpha");
			if(max != null)
				maxAlpha = Float.parseFloat(max);
		} 
	}
	
	private void loadAsset(String assetName) {
		StringBuilder buf = new StringBuilder();
		BufferedReader r = null;
		
		try {
			r = new BufferedReader(
					new InputStreamReader(context.getAssets().open(assetName)));
			String line;
			while((line = r.readLine()) != null) 
				buf.append(line);
		} catch(IOException e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		} finally {
			if(r != null)
				try {
					r.close();
				} catch (IOException e) {
					Log.e(e);
					throw new RuntimeException(e); // UNCAUGHT
				}
		}		
		
		try {
			loadData(URLEncoder.encode(buf.toString(), "UTF-8")
					.replaceAll("\\+"," "), "text/html", "UTF-8");
		} catch(UnsupportedEncodingException e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}
	}
	
}
