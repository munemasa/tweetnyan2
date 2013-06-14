package jp.miku39.android.tweetnyan.activities;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.tweetnyan.R;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;


public class AboutActivity extends ActivityWithTheme {
	final static String TAG = "AboutActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.about);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG,"Menu selected:"+item.getItemId());
		Intent intent;
        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);

        case android.R.id.home:
        	intent = new Intent( this, TweetnyanMainActivity.class );
        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	startActivity(intent);
        	break;
        }
        return true;
	}

}
