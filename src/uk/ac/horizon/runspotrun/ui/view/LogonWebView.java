package uk.ac.horizon.runspotrun.ui.view;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LogonWebView
extends WebView {

	public interface OnPageFinishedHandler {
		public boolean onPageFinished(String url);
	}
	
	private OnPageFinishedHandler onPageFinishedHandler;
	
	public LogonWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public LogonWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public LogonWebView(Context context) {
		super(context);
		init(context, null);
	}
	
	@Override
	public boolean performClick() {
		return super.performClick();
	}
	
	@Override
	public void setWebViewClient(WebViewClient client) { }
	
	public void setOnPageFinishedHandler(OnPageFinishedHandler onPageFinishedHandler) {
		this.onPageFinishedHandler = onPageFinishedHandler;
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	private void init(final Context context, AttributeSet attrs) {
		setBackgroundColor(getResources().getColor((android.R.color.transparent)));
		WebSettings s = getSettings();
		s.setJavaScriptEnabled(true);
		s.setSaveFormData(false);
		s.setSavePassword(false);
		
		setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				v.performClick();
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
		
		super.setWebViewClient(new WebViewClient() {
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if(url.toLowerCase(Locale.getDefault()).startsWith("mailto")) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		            context.startActivity(i);
					return true;
				}					
				return super.shouldOverrideUrlLoading(view, url);
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				if(onPageFinishedHandler == null)
					super.onPageFinished(view, url);
				else if(!onPageFinishedHandler.onPageFinished(url))
					super.onPageFinished(view, url);
			}
			
		});
	}

}
