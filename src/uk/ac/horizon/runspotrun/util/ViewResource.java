package uk.ac.horizon.runspotrun.util;

import android.content.Context;
import android.view.View;

public class ViewResource {

	private final Context context;
	
	private final View view;
	
	public ViewResource(Context context, View view) {
		this.context = context;
		this.view = view;
	}
	
	public int getResourceID(String id) {
		if(id.charAt(0) == '@')
			id = context.getPackageName() + ":" + id.substring(1);
		return view.getResources().getIdentifier(id, null, null);
	}
	
}
