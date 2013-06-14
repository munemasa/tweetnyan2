package jp.miku39.android.tweetnyan.activities;

import java.io.File;

import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.R.drawable;
import jp.miku39.android.tweetnyan.R.string;
import jp.miku39.android.tweetnyan.R.xml;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class TweetnyanPreferenceActivity extends PreferenceActivity {
	final static String TAG = "TweetnyanPreferenceActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
//		setTheme(R.style.Theme_HoloEverywhereLight_Sherlock);
//		setTheme(R.style.Theme_Sherlock);
		setTheme( android.R.style.Theme_Black );
	    super.onCreate(savedInstanceState);

	    addPreferencesFromResource( R.xml.preferences1 );

	    Preference pref = (Preference)findPreference("clear_iconcache");
	    pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d(TAG,"Clear Icon Cache.");
				confirmClearIconCache();
				return false;
			}
		});
	}
	void confirmClearIconCache(){
    	new AlertDialog.Builder(this)
    	.setIcon(R.drawable.icon)
    	.setMessage( getString(R.string.confirm_clear_iconcache) )
    	.setPositiveButton( getString(android.R.string.yes), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	    	clearIconCache();
    	    }
    	})
    	.setNegativeButton( getString(android.R.string.no), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	        // Nothing to do
    	    }
    	})
    	.show();
	}

	void clearIconCache(){
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage( getString(R.string.now_clear_iconcache) );
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();

        Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
		        File dir = getExternalCacheDir();
		        final File[] files = dir.listFiles();
		        runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog.setMax( files.length );
					}
				});
		        CommonUtils.sleep(200);
		        for( int i=0; i<files.length; i++){
		        	Log.d(TAG,"Delete "+files[i].getAbsolutePath());
		        	files[i].delete();

	        		final int ii = i;
		            runOnUiThread( new Runnable() {
						@Override
						public void run() {
							dialog.setProgress( ii );
						}
					});		        		
		        }
		        dialog.dismiss();
			}
		});
        th.start();
	}

}
