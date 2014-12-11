package uk.ac.horizon.runspotrun.ui.view;

import uk.ac.horizon.runspotrun.R;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class StyledListPreference 
extends ListPreference {

	private final Context context;
	
	private int selectedIndex;
	
	private AlertDialog dialog;

	public StyledListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public StyledListPreference(Context context) {
		super(context);
		this.context = context;
	}
	
	@Override
	public Dialog getDialog() {
		return dialog;
	}
	
	@Override
    protected void onPrepareDialogBuilder(Builder builder) {		
        View title = View.inflate(context, 
				R.layout.layout_settings_dialog_title, null);
		((TextView)title.findViewById(
				R.id.layout_settings_dialog_title)).setText(getTitle());
		builder.setCustomTitle(title);	
		builder.setNegativeButton(null, null);
		builder.setPositiveButton(null, null);
		builder.setNeutralButton(null, null);
    }
	
	@Override
    protected View onCreateDialogView() {
        View view = View.inflate(
        		getContext(), R.layout.layout_settings_dialog, null);
        ListView list = (ListView)view.findViewById(android.R.id.list);
        
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
        		context, R.layout.layout_settings_radio, getEntries());
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setItemChecked(findIndexOfValue(getValue()), true);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(
					AdapterView<?> parent, View view, int position, long id) {
				selectedIndex = position;
				StyledListPreference.this.onClick(
						getDialog(), DialogInterface.BUTTON_POSITIVE);
				getDialog().dismiss();
			}
		});

        return view;
    }
	
	@Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if(positiveResult) {
        	String value = getEntryValues()[selectedIndex].toString();
        	if(callChangeListener(value))
                setValue(value);
        }
    }
	
	@Override
	protected void showDialog(Bundle state) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		View view = onCreateDialogView();
		onBindDialogView(view);
		onPrepareDialogBuilder(builder);
		dialog = builder.create();
		if(state != null)
			dialog.onRestoreInstanceState(state);
		dialog.setOnDismissListener(this);
		dialog.setView(view, 0, 0, 0, 0);
		dialog.show();
	}
	
}
