package jp.miku39.android.tweetnyan.activities;

/**
 * プロフィールの編集
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class EditProfileActivity extends ActivityWithTheme implements IIconDownloadedCallback {
	final static String TAG = "EditProfile";
	
	final static int REQUEST_GALLERY = 0;

	private User mUser;
	private Twitter mTwitter;
	private Thread mThread;
	
	boolean mIconSelected = false;
	Uri		mIconFile;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    getWindow().setSoftInputMode( LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
	    
	    setContentView(R.layout.edit_profile_layout);

	    mIconSelected = false;
	    mTwitter = Lib.createTwitter(this);
	    mUser = (User)getIntent().getSerializableExtra("profile");

	    ActionBar bar = getActionBar();
	    bar.setSubtitle( String.format( getString(R.string.edit_profile), mUser.getScreenName() ) );

	    TweetnyanMainActivity.sIconCacheThread.pushCallback( this );

	    initViews();
	    setup();
	}

	@Override
	public void onIconDownloaded(long id) {
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				setup();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	    TweetnyanMainActivity.sIconCacheThread.popCallback();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
			InputStream in;
			try {
				mIconFile = data.getData();
				in = getContentResolver().openInputStream(data.getData());
				final Bitmap img = BitmapFactory.decodeStream(in);
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						// 選択した画像を表示
						ImageView imgview;
						imgview = (ImageView)findViewById(R.id.profile_icon);
						imgview.setImageBitmap(img);
						mIconSelected = true;
					}
				});
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	void initViews(){
	    Button btn;
	    btn = (Button)findViewById(R.id.btn_save);
	    btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),0);
				saveProfile();
			}
		});

	    btn = (Button)findViewById(R.id.btn_cancel);
	    btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	    btn = (Button)findViewById(R.id.select_icon);
	    btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, REQUEST_GALLERY);
			}
		});

	    EditText et = (EditText)findViewById(R.id.edit_self_introduction);
	    // 文字数カウント
	    et.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
			    TextView v = (TextView)findViewById(R.id.numof_characters);
			    v.setText(""+s.length()+" characters");
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}});
	}

	private void setup(){
		Bitmap icon = TweetnyanMainActivity.sIconCacheThread.loadIcon( mUser.getProfileImageURL(), mUser.getId() );
		if( icon!=null ){
			ImageView imgview;
			imgview = (ImageView)findViewById(R.id.profile_icon);
			imgview.setImageBitmap(icon);
		}else{
			TweetnyanMainActivity.sIconCacheThread.requestDownload( mUser.getProfileImageURL(), mUser.getId() );
		}

		EditText et;

		et = (EditText)findViewById(R.id.edit_user_name);
	    et.setText(mUser.getName());

	    et = (EditText)findViewById(R.id.edit_current_location);
	    et.setText(mUser.getLocation());

	    et = (EditText)findViewById(R.id.edit_web);
	    et.setText(mUser.getURL().toString());

	    et = (EditText)findViewById(R.id.edit_self_introduction);
	    et.setText(mUser.getDescription());
	}

	private void saveProfile(){
		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_saving) );
		dialog.setCancelable(false);
		FragmentManager manager = getFragmentManager();
		dialog.show(manager, "save-profile");

	    EditText et;
	    et = (EditText)findViewById(R.id.edit_user_name);
	    final String name = et.getEditableText().toString();

	    et = (EditText)findViewById(R.id.edit_current_location);
	    final String location = et.getEditableText().toString();

	    et = (EditText)findViewById(R.id.edit_web);
	    final String url = et.getEditableText().toString();

	    et = (EditText)findViewById(R.id.edit_self_introduction);
	    final String description = et.getEditableText().toString();

		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
			    try {
			    	Log.d(TAG,"profile updating...");
					mUser = mTwitter.updateProfile(name, url, location, description);
					if( mIconSelected ){
						Log.d(TAG,"profile icon updating...");
						InputStream in;
						try {
							in = getContentResolver().openInputStream( mIconFile );
							mUser = mTwitter.updateProfileImage(in);
							in.close();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
					    	String errmsg = getString(R.string.error_save_profile_icon);
					    	showToastFromThread(errmsg);

						} catch (IOException e) {
							e.printStackTrace();
					    	String errmsg = getString(R.string.error_save_profile_icon);
					    	showToastFromThread(errmsg);
						}
					}
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_save_profile), e.getStatusCode());
			    	showToastFromThread(errmsg, Toast.LENGTH_SHORT);

				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
							setup();
						}
					});
				}
			}
		});
		mThread.start();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
