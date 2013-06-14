package jp.miku39.android.lib;

import jp.miku39.android.tweetnyan.R;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.Toast;

public class ActivityWithTheme extends Activity {
	final static String TAG = "ActivityWithTheme";

	SharedPreferences mDefPreference;
	
	public FragmentManager getSupportFragmentManager(){
		return getFragmentManager();
	}
	
	public ActionBar getSupportActionBar(){
		return getActionBar();
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	mDefPreference = PreferenceManager.getDefaultSharedPreferences(this);
    	requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
//    	setTheme( R.style.Theme_HoloEverywhereDark_Sherlock );
    	setTheme( R.style.Theme_HoloEverywhereLight_Sherlock );
        super.onCreate(savedInstanceState);
		setProgressBarIndeterminateVisibility(false);
    }

	@Override
	protected void onStart() {
		super.onStart();
		// 初期状態から表示しっぱなしになるので、この辺で表示止めておく
		setProgressBarIndeterminateVisibility(false);
	}
	
	public String getDefaultPreferenceString( String key, String defValue ){
		return mDefPreference.getString(key, defValue);
	}
	public int getDefaultPreferenceInt( String key, int defValue ){
		return mDefPreference.getInt(key, defValue);
	}

	public void showToast(String text, int duration){
		Toast.makeText( this, text, duration).show();
	}

	public void showToastFromThread(final String text){
		showToastFromThread(text, Toast.LENGTH_SHORT);
	}
	public void showToastFromThread(final String text, final int duration){
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				showToast(text,duration);
			}
		});
	}
}
