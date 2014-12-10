package uk.ac.horizon.runspotrun.ui.view;

import java.util.Locale;

import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.util.ViewResource;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LogonWebView
extends WebView {

	public interface OnPageFinishedHandler {
		public boolean onPageFinished(String url);
	}
	
	private final Context context;
	
	private OnPageFinishedHandler onPageFinishedHandler;
	
	private int busyID;
	
	private View busy;
	
	public LogonWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.context = context;
		init(attrs);
	}

	public LogonWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init(attrs);
	}

	public LogonWebView(Context context) {
		super(context);
		this.context = context;
		init(null);
	}
	
	@Override
	public boolean performClick() {
		return super.performClick();
	}
	
	@Override
	public void setWebViewClient(WebViewClient client) { }
	
	public void setOnPageFinishedHandler(
			OnPageFinishedHandler onPageFinishedHandler) {
		this.onPageFinishedHandler = onPageFinishedHandler;
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();	
		ViewParent p = this;
		View v = null;
		while((p = p.getParent()) != null) 
			if(p instanceof View)
				v = (View)p;
		busy = v.findViewById(busyID);
		
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	private void init(AttributeSet attrs) {
		setBackgroundColor(
				getResources().getColor((android.R.color.transparent)));
		WebSettings s = getSettings();
		s.setJavaScriptEnabled(true);
		s.setSaveFormData(false);
		s.setSavePassword(false);
		
		if(attrs != null) {
			String id = attrs.getAttributeValue(App.XMLNS_URI, "busy");
			if(id != null)
				busyID = new ViewResource(context, this).getResourceID(id);
		}
		
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
			public void onLoadResource(WebView view, String url) { 
				super.onLoadResource(view, url);
				if(busy != null)
					busy.setVisibility(View.VISIBLE);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if(!url.toLowerCase(Locale.getDefault()).startsWith("http")) {
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
				else if(!onPageFinishedHandler.onPageFinished(url)) {
					super.onPageFinished(view, url);
					if(busy != null)
						busy.setVisibility(View.GONE);
				}
			}
			
		});
	}

}
