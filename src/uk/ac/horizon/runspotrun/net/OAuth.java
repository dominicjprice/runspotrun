package uk.ac.horizon.runspotrun.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;

import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.app.Urls;

public class OAuth {
	
	private static void authorizeClient(HttpClient client)
	throws Exception{
		
		HttpGet get = new HttpGet(
				Urls.OAUTH("authorize", String.format(
						"response_type=code&client_id=%s", App.OAUTH_CLIENT_ID)));
		HttpResponse response = client.execute(get);
		response.getEntity().consumeContent();
	}

	private static String authorizeClient(HttpClient client, String csrf)
	throws Exception{
		String url = Urls.OAUTH("authorize/confirm");
		HttpPost post = new HttpPost(url);
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(
				"csrfmiddlewaretoken", csrf));
		urlParameters.add(new BasicNameValuePair("scope", "read+write"));
		urlParameters.add(new BasicNameValuePair("authorize", "Authorize"));
		urlParameters.add(new BasicNameValuePair("client_id", App.OAUTH_CLIENT_ID));
		post.setEntity(new UrlEncodedFormEntity(urlParameters));
		post.setHeader("Referer", url);
		HttpResponse response = client.execute(post);
		response.getEntity().consumeContent();
		return response.getHeaders("Location")[0].getValue();
	}
	
	private static String getAuthorizationCode(HttpClient client, String url)
	throws Exception {
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		response.getEntity().consumeContent();
		URL result = new URL(response.getHeaders("Location")[0].getValue());
		for(String part : result.getQuery().split("&")) {
			String[] parts = part.split("=");
			if(parts[0].equalsIgnoreCase("code"))
				return parts[1];
		}
		throw new RuntimeException();
	}
	
	private static String getAccessToken(HttpClient client, String authCode)
	throws Exception {
		HttpPost post = new HttpPost(Urls.OAUTH("access_token/"));
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
		urlParameters.add(new BasicNameValuePair("code", authCode));
		urlParameters.add(new BasicNameValuePair("client_id", App.OAUTH_CLIENT_ID));
		urlParameters.add(new BasicNameValuePair("client_secret", App.OAUTH_CLIENT_SECRET));
		post.setEntity(new UrlEncodedFormEntity(urlParameters));
		HttpResponse response = client.execute(post);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()));
		StringBuilder buf = new StringBuilder();
		String line;
		while((line = in.readLine()) != null)
			buf.append(line).append("\n");
		return buf.toString();
	}
	
	public static String getAccessToken(String sessionId, String csrfToken)
	throws Exception {
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setBooleanParameter(
				"http.protocol.handle-redirects", false);
		CookieStore cookies = new BasicCookieStore();
		BasicClientCookie csrfCookie = new BasicClientCookie(
				"csrftoken", csrfToken);
		csrfCookie.setDomain(Urls.OAUTH_HOSTNAME);
		csrfCookie.setPath("/");
		cookies.addCookie(csrfCookie);
		BasicClientCookie sessionCookie = new BasicClientCookie(
				"sessionid", sessionId);
		sessionCookie.setDomain(Urls.OAUTH_HOSTNAME);
		sessionCookie.setPath("/");
		cookies.addCookie(sessionCookie);		
		client.setCookieStore(cookies);
		
		authorizeClient(client);
		return getAccessToken(client, 
				getAuthorizationCode(client, 
						authorizeClient(client, csrfToken)));
	}

}
