package uk.ac.horizon.runspotrun.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class Urls {

	private static final String hostname = "www.runspotrun.co.uk";
	
	private static final String scheme = "https";
	
	private static final String basepath = "/";
	
	private static final String loginpath = "/accounts/login/";
	
	private static final String jsonrpcpath = "/jsonrpc/";
	
	private static final String apipath = "/api/v1/";
	
	private static final String oauthpath = "/oauth2/";
	
	public static final String BASE;
	static {
		try {
			BASE = new URI(scheme, hostname, basepath, "").toString();
		} catch (URISyntaxException e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}
	}
	
	public static final String LOGIN(String next) {
		try {
			String query = "next=" + next;
			return new URI(scheme, hostname, loginpath, query, "").toString();
			
		} catch (URISyntaxException e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}
	}
	
	public static String JSON_RPC(String accessToken) {
		try {
			String query = "oauth_consumer_key=" 
					+ URLEncoder.encode(accessToken, "UTF-8");
			return new URI(scheme, hostname, jsonrpcpath, query, "").toString(); 
		} catch (Exception e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}
	}
	
	public static final String OAUTH_HOSTNAME = hostname;
	
	public static String OAUTH(String path) {
		return OAUTH(path, "");
	}
	
	public static String OAUTH(String path, String query) {
		try {
			String fullpath = String.format("%s%s", oauthpath, path);
			return new URI(scheme, hostname, fullpath, query, "").toString(); 
		} catch (Exception e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}
	}
	
	public static String API(String endpoint, String accessToken) {
		try {
			String query = "oauth_consumer_key=" 
					+ URLEncoder.encode(accessToken, "UTF-8")
					+ "&format=json" ;
			return new URI(scheme, hostname, 
					apipath + endpoint + "/", query, "").toString(); 
		} catch (Exception e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}
	}
	
}
