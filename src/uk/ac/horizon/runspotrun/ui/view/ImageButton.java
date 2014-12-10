package uk.ac.horizon.runspotrun.ui.view;

import uk.ac.horizon.runspotrun.app.App;
import uk.ac.horizon.runspotrun.util.ViewResource;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.StateSet;
import android.widget.Button;

public class ImageButton
extends Button {

	private static enum State { OFF, ON }
	
	private final ViewResource viewResource;
	
	public ImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		viewResource = new ViewResource(context, this);
		init(attrs);
	}

	public ImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		viewResource = new ViewResource(context, this);
		init(attrs);
	}

	public ImageButton(Context context) {
		super(context);
		viewResource = new ViewResource(context, this);
		init(null);
	}
	
	private void init(AttributeSet attrs) {
		if(attrs != null) {
			Drawable dl = getStateList(attrs, "drawableLeft");
			Drawable dr = getStateList(attrs, "drawableRight");
			Drawable dt = getStateList(attrs, "drawableTop");
			Drawable db = getStateList(attrs, "drawableBottom");
			setCompoundDrawablesWithIntrinsicBounds(dl, dt, dr, db);
		}
	}
	
	private StateListDrawable getStateList(AttributeSet attrs, String name) {
		String value = attrs.getAttributeValue(App.XMLNS_URI, name);
		if(value == null)
			return null;
		StateListDrawable d = new StateListDrawable();
		d.addState(new int[]{ 
				android.R.attr.state_focused }, 
				getDrawable(value, State.ON));
		d.addState(new int[]{ 
				android.R.attr.state_pressed }, 
				getDrawable(value, State.ON));
		d.addState(StateSet.WILD_CARD, 
				getDrawable(value, State.OFF));
		return d;
	}
	
	private Drawable getDrawable(String name, State state) {
		String ext;
		switch(state) {
		case OFF:
			ext = "_off";
			break;
		case ON:
			ext = "_on";
			break;
		default: throw new RuntimeException(); // UNCAUGHT
		} 
		return getResources().getDrawable(
				viewResource.getResourceID("@drawable/" + name + ext));
	}

}
