package uk.ac.horizon.runspotrun.net;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import uk.ac.horizon.runspotrun.app.Log;

public class CookieParser {

	private static enum ReadNameResponseType {
		END_OF_STREAM,
		COOKIE,
		COOKIE_NAME;	
	}
	
	private static enum ReadValueResponseType {
		END_OF_STREAM,
		COOKIE_VALUE;	
	}
	
	private static class ReadNameResponse {
		public ReadNameResponseType type;
		public String value;
		public ReadNameResponse(ReadNameResponseType type, String value) {
			this.type = type;
			this.value = value;
		}
	}
	
	private static class ReadValueResponse {
		public ReadValueResponseType type;
		public String value;
		public ReadValueResponse(ReadValueResponseType type, String value) {
			this.type = type;
			this.value = value;
		}
	}
	
	public static Map<String, String> parse(String data) {
		BufferedReader r = new BufferedReader(new StringReader(data));
		HashMap<String, String> map = new HashMap<String, String>();
		try {
			parse(r, map);	
			r.close();
		} catch(Exception e) {
			Log.e(e);
			throw new RuntimeException(e); // UNCAUGHT
		}		
		return map;
	}
	
	private static void parse(Reader r, Map<String, String> map)
	throws Exception {
		ReadNameResponse name = parseName(r);
		switch(name.type) {
		case END_OF_STREAM:
			map.put(name.value, null);
			return;
		case COOKIE:
			map.put(name.value, null);
			break;
		case COOKIE_NAME:
			ReadValueResponse value = parseValue(r);
			switch(value.type) {
			case END_OF_STREAM:
				map.put(name.value, value.value);
				return;
			case COOKIE_VALUE:
				map.put(name.value, value.value);
				break;
			}
			break;
		}
		parse(r, map);
	}
	
	private static ReadNameResponse parseName(Reader r)
	throws Exception {
		int c;
		StringBuilder buf = new StringBuilder();
		while((c = r.read()) != -1 
				&& (c != ((int)'=')) 
				&& (c != ((int)';')))
			buf.append((char)c);
		String name = buf.toString().trim().toLowerCase(Locale.getDefault());
		switch(c) {
		case -1:
			return new ReadNameResponse(ReadNameResponseType.END_OF_STREAM, name);
		case ((int)';'):
			return new ReadNameResponse(ReadNameResponseType.COOKIE, name);
		default:
			return new ReadNameResponse(ReadNameResponseType.COOKIE_NAME, name);
		}
	}
	
	private static ReadValueResponse parseValue(Reader r)
	throws Exception {
		int c;
		StringBuilder buf = new StringBuilder();
		while((c = r.read()) != -1  
				&& (c != ((int)';')))
			buf.append((char)c);
		String value = buf.toString().trim();
		switch(c) {
		case -1:
			return new ReadValueResponse(ReadValueResponseType.END_OF_STREAM, value);
		default:
			return new ReadValueResponse(ReadValueResponseType.COOKIE_VALUE, value);
		}
	}

}
