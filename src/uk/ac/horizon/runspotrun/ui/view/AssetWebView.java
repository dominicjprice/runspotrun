package uk.ac.horizon.runspotrun.ui.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Log;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class AssetWebView
extends WebView {

	private final Context context;
	
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
	
	public void loadAsset(String assetName) {
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
	
	private void init(AttributeSet attrs) {
		setBackgroundColor(getResources().getColor((android.R.color.transparent)));
		if(attrs != null) {
			String assetName = attrs.getAttributeValue(App.XMLNS_URI, "resource");
			if(assetName != null)
				loadAsset(assetName);
		}
	}

}
