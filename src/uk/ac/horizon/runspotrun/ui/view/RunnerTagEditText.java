package uk.ac.horizon.runspotrun.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class RunnerTagEditText
extends EditText {

	public RunnerTagEditText(Context context) {
		super(context);
	}

	public RunnerTagEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RunnerTagEditText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public void clearRunnerTag() {
		setText("");
	}
	
	public String getRunnerTag() {
		return getText().toString();
	}
	
	public String getAndClearRunnerTag() {
		String t = getText().toString();
		setText("");
		return t;
	}
	
	public void backspaceRunnerTag() {
		String s = getText().toString();
		if(s.length() > 0) {
			s = s.substring(0, s.length() - 1);
			setText(s);
		}
	}
	
	public void appendRunnerTag(String text) {
		append(text);
	}
}
