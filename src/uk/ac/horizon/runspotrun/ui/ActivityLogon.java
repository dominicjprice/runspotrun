package uk.ac.horizon.runspotrun.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.alexd.jsonrpc.JSONRPCException;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.horizon.runspotrun.R;
import uk.ac.horizon.runspotrun.app.AccessToken;
import uk.ac.horizon.runspotrun.app.Log;
import uk.ac.horizon.runspotrun.app.Urls;
import uk.ac.horizon.runspotrun.net.CookieParser;
import uk.ac.horizon.runspotrun.net.JsonRpc;
import uk.ac.horizon.runspotrun.net.OAuth;
import uk.ac.horizon.runspotrun.ui.view.LogonWebView;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;

public class ActivityLogon
extends Activity {
	
	private final AsyncTask<String, Void, Boolean> oauthTask = new AsyncTask<String, Void, Boolean>() {

		@Override
		protected void onPostExecute(Boolean result) {
			Toast.makeText(ActivityLogon.this, 
					result 
					? R.string.activity_logon_success_text
					: R.string.activity_logon_failure_text, Toast.LENGTH_LONG).show();
			close();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			Map<String, String> cookies = CookieParser.parse(params[0]);
			try {
				String code = OAuth.getAccessToken(
						cookies.get("sessionid"), cookies.get("csrftoken"));
				ObjectMapper mapper = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<Object, Object> map = mapper.readValue(code, Map.class);
				AccessToken at = new AccessToken(ActivityLogon.this);
				at.set(map.get("access_token").toString());
				return true;
			} catch(Exception e) {
				Log.e(e);
				return false;
			}
		}
		
	};
	
	private final AsyncTask<Void, Void, Boolean> logonTask = new AsyncTask<Void, Void, Boolean>(){

		@Override
		protected void onPostExecute(Boolean result) {
			if(result.booleanValue()) {
				Toast.makeText(ActivityLogon.this, 
						R.string.activity_logon_success_text, Toast.LENGTH_SHORT).show();
				close();
			} else {
				final String redirect = "/dev/android/logon_complete";
				setContentView(R.layout.activity_logon);	
				final LogonWebView view = (LogonWebView)findViewById(R.id.logon_web_view);
				view.setOnPageFinishedHandler(new LogonWebView.OnPageFinishedHandler() {
					@Override
					public boolean onPageFinished(String url) {
						URL purl;
						try {
							purl = new URL(url);
						} catch(MalformedURLException e) {
							Log.e(e);
							throw new RuntimeException(e); // UNCAUGHT
						}
						if(purl.getPath().startsWith(redirect)) {
							view.setVisibility(View.INVISIBLE);
							oauthTask.execute(CookieManager.getInstance().getCookie(url));
							return true;
						} else
							return false;
					}
				});
				view.loadUrl(Urls.LOGIN(redirect));
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			String accessToken = new AccessToken(ActivityLogon.this).get();
			if(accessToken == null)
				return false;
			try {
				return new JsonRpc(accessToken).dummy();
			} catch(JSONRPCException e) {
				return false;
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override 
	protected void onResume() {
		super.onResume();
		if(!checkNetwork()) {
			Toast.makeText(ActivityLogon.this, 
					R.string.activity_logon_no_network_text, Toast.LENGTH_LONG).show();
			close();
		} else
			logonTask.execute((Void[])null);
	}
	
	public void onClickClose(View v) {
		close();
	}
	
	private void close() {
		finish();
	}
	
	private boolean checkNetwork() {
		try {
			ConnectivityManager cm = (ConnectivityManager)
					getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.getActiveNetworkInfo().isConnected();
		} catch(Exception e) {
			return false;	
		}
	}
	
}
