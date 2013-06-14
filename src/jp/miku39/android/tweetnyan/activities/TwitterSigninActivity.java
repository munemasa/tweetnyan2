package jp.miku39.android.tweetnyan.activities;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class TwitterSigninActivity extends ActivityWithTheme {
	final static String TAG = "TwitterSigninActivity";

	private Twitter mTwitter;
	private RequestToken mRequestToken;
	private AccessToken mAccessToken;
	private Integer mIndex;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.twitter_authentication_layout);

	    ActionBar bar = getSupportActionBar();
	    bar.setSubtitle( getString(R.string.signin) );

	    if( savedInstanceState!=null ){
	    	Log.d(TAG,"Restore Request Token");
	    	mRequestToken = (RequestToken)savedInstanceState.getSerializable("request-token");
			mIndex = savedInstanceState.getInt("index");
	    }else{
		    mIndex = getIntent().getIntExtra("index", -1);	    	
	    }
	    Log.d(TAG,"Create Account/"+mIndex);

	    initViews();	    
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("request-token", mRequestToken);
		outState.putInt("index", mIndex);
	}

	void initViews(){
		Button btn;
		btn = (Button)findViewById( R.id.btn_get_pin );
		btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtils.hideSoftwareKeyboard( TwitterSigninActivity.this, v);
				getAccessToken();
			}
		});

		btn = (Button)findViewById( R.id.btn_authenticate );
		btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtils.hideSoftwareKeyboard( TwitterSigninActivity.this, v);
				authenticate();
			}
		});
	}

	void getAccessToken(){
		Log.d(TAG,"Get AccessToken");
		mTwitter = Lib.createTwitter();

		setProgressBarIndeterminateVisibility(true);
		final Button btn = (Button)findViewById(R.id.btn_get_pin);
		btn.setEnabled(false);

		Thread th = new Thread(new Runnable(){
			@Override
			public void run() {
			    try {
			    	mRequestToken = mTwitter.getOAuthRequestToken();
					String url = mRequestToken.getAuthorizationURL();

					Log.d(TAG,"Authorization URL="+url);
					Uri uri = Uri.parse(url);
					Intent i = new Intent( Intent.ACTION_VIEW, uri );
					startActivity(i);

			    } catch (TwitterException e) {
					e.printStackTrace();

			    } finally {
			    	runOnUiThread( new Runnable(){
						@Override
						public void run() {
							btn.setEnabled(true);
							setProgressBarIndeterminateVisibility(false);
						}} );					
				}
			}});
    	th.start();
	}

	private void authenticate(){
		Log.d(TAG,"Twitter User Authenticating...");
		final String pin = ((EditText)findViewById(R.id.edit_input_pin)).getText().toString();
		if( pin.length()<=0 ) return;
		if( mRequestToken==null ) return;
		if( mTwitter==null ){
			mTwitter = Lib.createTwitter();
		}

		setProgressBarIndeterminateVisibility(true);
		final Button btn = (Button)findViewById(R.id.btn_authenticate);
		btn.setEnabled(false);

		mAccessToken = null;
		Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
				try {
					mAccessToken = mTwitter.getOAuthAccessToken( mRequestToken, pin );
					Lib.saveAccessToken( TwitterSigninActivity.this, mAccessToken, mIndex );

					Log.d(TAG,"Twitter User Authentication is Successful.");
					Log.d(TAG,"Screen Name:"+mAccessToken.getScreenName());
					mRequestToken = null;

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(TwitterSigninActivity.this, getString(R.string.success_authentication), Toast.LENGTH_SHORT).show();
							finish();
						}
					});
				} catch (TwitterException te) {
			        if(401 == te.getStatusCode()){
			        	Log.d(TAG,"Unable to get the access token.");
		          	}else{
		          		te.printStackTrace();
		          	}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(TwitterSigninActivity.this, getString(R.string.failed_authentication), Toast.LENGTH_SHORT).show();
						}
					});
		        } finally {
			    	runOnUiThread( new Runnable(){
						@Override
						public void run() {
							btn.setEnabled(true);
							setProgressBarIndeterminateVisibility(false);
						}} );
		        }
			} } );
		th.start();
	}

}
