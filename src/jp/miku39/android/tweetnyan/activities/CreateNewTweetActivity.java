package jp.miku39.android.tweetnyan.activities;

/**
 * 新規ツイートからリプまで、ツイート送信はここ。
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.lib.MyAlertDialog;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.UserMentionEntity;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;
import twitter4j.media.MediaProvider;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class CreateNewTweetActivity extends ActivityWithTheme implements LocationListener {
	final static String TAG = "CreateNewTweetActivity";
	private Twitter mTwitter;
	private Status mInReplyToTweet;

	protected static final int REQUEST_GALLERY = 0;
	protected static final int REQUEST_CAMERA = 1;

	protected boolean mMediaSelected = false;
	protected File mTemporalFile;
	protected Location mCurrentLocation;
	protected Uri mMediaUriToUpload;
	private String mTempFullPath;
	private int mPicUploadDestination;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mMediaSelected = false;

		if(requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
			mMediaUriToUpload = data.getData();
			mMediaSelected = true;
			Log.d(TAG,"upload from gallery: "+mMediaUriToUpload.toString());
			deleteTemporalFile();

	        ImageView imageView = (ImageView)findViewById(R.id.attachment_thumbnail);
	        imageView.setImageURI( mMediaUriToUpload );
	        imageView.setVisibility(View.VISIBLE);
		}
		if(requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
	        mMediaSelected = true;
	        Log.d(TAG, "upload from camera: "+mTemporalFile.toString() );

	        ImageView imageView = (ImageView)findViewById(R.id.attachment_thumbnail);
	        imageView.setImageURI( Uri.fromFile(mTemporalFile) );
	        imageView.setVisibility(View.VISIBLE);
		}
	}
    protected void deleteTemporalFile(){
		if( mTemporalFile!=null ){
			Log.d(TAG,"Delete temporal file.");
			mTemporalFile.delete();
			mTemporalFile = null;
		}
	}
    
    void startCamera(){
		Intent intent = new Intent();
		intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

		mTemporalFile = getExternalCacheDir();
		if( mTemporalFile==null ) return;
		try {
			mTemporalFile = File.createTempFile("tmp", ".jpg", mTemporalFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG,"Photo file="+mTemporalFile.getAbsolutePath());
		mTempFullPath = mTemporalFile.getAbsolutePath();

		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile( mTemporalFile ));
		startActivityForResult(intent, REQUEST_CAMERA);
    }
    
    void startGallery(){
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		startActivityForResult(intent, REQUEST_GALLERY);
    }

    protected void startGPS(){
		LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);		
	}
	protected void stopGPS(){
		LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locManager.removeUpdates(this);		
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    mTwitter = Lib.createTwitter( this );

	    ActionBar bar = getActionBar();
//		bar.setSubtitle(R.string.create_new_tweet);
	    bar.setTitle(R.string.create_new_tweet);
	    bar.setSubtitle( "@"+Lib.getCurrentAccountScreenName( this ) );
		bar.setDisplayHomeAsUpEnabled(true);

	    Intent intent = getIntent();
	    mInReplyToTweet = (Status) intent.getSerializableExtra("in-reply-to");
		if( mInReplyToTweet==null ){
		    setContentView(R.layout.create_newtweet_layout);
		}else{
			setContentView(R.layout.create_replytweet_layout);
		}

		if( savedInstanceState!=null ){
			mTempFullPath = savedInstanceState.getString("temp-path");
			if( mTempFullPath!=null ){
				mTemporalFile = new File(mTempFullPath);
			}
		}

		// 画像アップロード先の設定
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mPicUploadDestination = Integer.parseInt( pref.getString("picture_upload_target", "0") );
		Log.d(TAG,"Picture Upload Target:"+mPicUploadDestination );
		initViews();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("temp-path", mTempFullPath);
	}
	
	@Override
	protected void onDestroy() {
		deleteTemporalFile();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopGPS();
	}

	void initViews(){
		Button btn;
		
		if( mInReplyToTweet!=null ){
			// リプ先スクリーン名
		    TextView tv = (TextView)findViewById(R.id.text_in_reply_to);
		    String userNameToReply = mInReplyToTweet.getUser().getScreenName();
		    String str = String.format( getString(R.string.reply_to_user), "@"+userNameToReply);
		    tv.setText( str );

		    // リプ元メッセージ
		    tv = (TextView)findViewById(R.id.referenced_text);
		    str = "@"+userNameToReply+": "+mInReplyToTweet.getText();
		    tv.setText( str );

			str = "@"+userNameToReply+" ";
			int start = str.length();
			UserMentionEntity[] tmp = mInReplyToTweet.getUserMentionEntities();
			String name = Lib.getCurrentAccountScreenName(this);
			HashMap<String,Boolean> flags = new HashMap<String,Boolean>();
			flags.put(userNameToReply, true);
			for(int i=0; i<tmp.length; i++){
				if( !name.equals(tmp[i].getScreenName() ) ){
					String n = tmp[i].getScreenName();
					Boolean b = flags.get(n);
					if( b==null || b==false ){
						str += "@"+n+" ";
					}
					flags.put(n, true);
				}
			}
		    EditText et = (EditText)findViewById(R.id.edit_tweet);
			et.setText(str);
			int end = str.length();
			et.setSelection( start, end );
		}

		btn = (Button)findViewById(R.id.btn_send);
		btn.setEnabled(false);
		btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtils.hideSoftwareKeyboard( CreateNewTweetActivity.this, v);
				if( mMediaSelected ){
					if( mPicUploadDestination==Consts.sPicUploadTwitpic ){
						uploadToTwitpicAndTweet();
						return;
					}
				}
				sendTweet();
			}
		});

		btn = (Button)findViewById(R.id.btn_cancel);
		btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtils.hideSoftwareKeyboard( CreateNewTweetActivity.this, v);
				finish();
			}
		});

	    EditText et = (EditText)findViewById(R.id.edit_tweet);
	    // 文字数カウント
	    et.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
			    TextView v = (TextView)findViewById(R.id.numof_characters);
			    v.setText(""+s.length());

			    Button btn = (Button)findViewById(R.id.btn_send);
			    btn.setEnabled( s.length()!=0?true:false );
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}});

    	Intent intent = getIntent();
    	String str = intent.getStringExtra("text");
    	if( str!=null ){
    		et.setText(str);
    	}
    	
    	// 共有から
    	Bundle extras = intent.getExtras();
    	if( extras!=null ){
    		CharSequence subject = extras.getCharSequence(Intent.EXTRA_SUBJECT);
			CharSequence msg = extras.getCharSequence(Intent.EXTRA_TEXT);
			String tmp = "";
			if( subject!=null ){
				tmp += subject;
			}
			if( msg!=null ){
				if( subject!=null ) str += " ";
				tmp += msg;
				et.setText(tmp);
			}
    	}

    	if( intent.getBooleanExtra("move_last", false) ){
    		int end = str.length();
    		et.setSelection( end, end );
    	}

        ImageView imageView = (ImageView)findViewById(R.id.attachment_thumbnail);
        imageView.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				unattachPicture();
			}
		});
        
        imageView = (ImageView)findViewById(R.id.geolocation_thumbnail);
        imageView.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				unattachGeoTag();
			}
		});
	}
	
	void unattachPicture(){
		MyAlertDialog d = MyAlertDialog.newInstance(this);
    	d.setIcon(R.drawable.ic_launcher);
    	d.setMessage( getString(R.string.confirm_cancel_attach_pic) );
    	d.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
				mMediaSelected = false;
				deleteTemporalFile();
		        ImageView imageView = (ImageView)findViewById(R.id.attachment_thumbnail);
		        imageView.setVisibility(View.GONE);
     	    }
    	});
    	d.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	        // Nothing to do
    	    }
    	});
    	d.show();
	}
	void unattachGeoTag(){
		MyAlertDialog d = MyAlertDialog.newInstance(this);
    	d.setIcon(R.drawable.ic_launcher);
    	d.setMessage( getString(R.string.confirm_cancel_geotag) );
    	d.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
				mCurrentLocation = null;
				ImageView iv = (ImageView)findViewById(R.id.geolocation_thumbnail);
				iv.setVisibility(View.GONE);
     	    }
    	});
    	d.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	        // Nothing to do
    	    }
    	});
    	d.show();
	}

	void sendTweet(){
		final String str = ((EditText)findViewById(R.id.edit_tweet)).getText().toString();
		if( str.length()<=0 ) return;

		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_sending) );
		dialog.setCancelable(false);
		FragmentManager manager = getFragmentManager();
		dialog.show(manager, "sendtweet");

        Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
				try {
					StatusUpdate update = new StatusUpdate(str);
					if( mCurrentLocation!=null ){
						double lat = mCurrentLocation.getLatitude();
						double lng = mCurrentLocation.getLongitude();
						GeoLocation location = new GeoLocation(lat, lng);
						update.setLocation(location);
					}
					if( mInReplyToTweet!=null ){
						update.setInReplyToStatusId( mInReplyToTweet.getId() );
					}
					if( mMediaSelected && mPicUploadDestination==Consts.sPicUploadTwitter ){
						// Twitterのツイートに画像添付
						File imagefile = mTemporalFile;
						if( imagefile==null ){
				            ContentResolver cr = getContentResolver();  
				            String[] columns = {MediaStore.Images.Media.DATA };  
				            Cursor c = cr.query(mMediaUriToUpload, columns, null, null, null);  
				            c.moveToFirst();  
				            imagefile = new File(c.getString(0));
						}
						update.media(imagefile);
					}
					Status st = mTwitter.updateStatus(update);
					Log.d(TAG,"Successfully updated the status to [" + st.getText() + "].");
					finish();

				} catch (final TwitterException e) {
					e.printStackTrace();

					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							String str = String.format( getString(R.string.error_tweeting), e.getStatusCode() );
							Toast.makeText(CreateNewTweetActivity.this, str, Toast.LENGTH_SHORT).show();
						}
					});
				} finally {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
						}
					});
				}
			}} );
		th.start();		
	}

	private void uploadToTwitpicAndTweet(){
		Log.d(TAG,"Now uploading to Twitpic.");
		// TODO 今のところtwitpicのみ
		final MediaProvider mediaprovider = MediaProvider.TWITPIC;

		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_uploading_picture) );
		dialog.setCancelable(false);
		FragmentManager manager = getFragmentManager();
		dialog.show(manager, "sendpic");

        Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					ConfigurationBuilder configurationbuilder = new ConfigurationBuilder()
						.setOAuthConsumerKey(mTwitter.getConfiguration().getOAuthConsumerKey())
						.setOAuthConsumerSecret(mTwitter.getConfiguration().getOAuthConsumerSecret())
						.setOAuthAccessToken(mTwitter.getOAuthAccessToken().getToken())
						.setOAuthAccessTokenSecret(mTwitter.getOAuthAccessToken().getTokenSecret());
					configurationbuilder.setMediaProviderAPIKey( Consts.sTwitpicAPIKey );

					ImageUploadFactory imageuploadfactory = new ImageUploadFactory(configurationbuilder.build());

					ImageUpload imageupload = imageuploadfactory.getInstance(mediaprovider);

					File imagefile = mTemporalFile;
					if( imagefile==null ){
			            ContentResolver cr = getContentResolver();  
			            String[] columns = {MediaStore.Images.Media.DATA };  
			            Cursor c = cr.query(mMediaUriToUpload, columns, null, null, null);  
			            c.moveToFirst();  
			            imagefile = new File(c.getString(0));
					}
					final String str = ((EditText)findViewById(R.id.edit_tweet)).getText().toString();
					final String url = imageupload.upload(imagefile, str);
					Log.d(TAG,"uploaded:"+url);

					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							EditText et = (EditText)findViewById(R.id.edit_tweet);
							et.setText(str+" "+url);
							sendTweet();
						}
					});

				} catch (TwitterException e) {
					e.printStackTrace();
					
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							String str = getString(R.string.error_uploading_pic);
							Toast.makeText(CreateNewTweetActivity.this, str, Toast.LENGTH_SHORT).show();
						}
					});

				} finally {
					runOnUiThread( new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}} );
				}
			}
		});
		th.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Consts.MENU_ID_TAKE_PHOTO, Menu.NONE, getString(R.string.take_a_picture) ).setIcon(android.R.drawable.ic_menu_camera).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	menu.add(Menu.NONE, Consts.MENU_ID_ATTACH_PIC, Menu.NONE, getString(R.string.select_a_picture) ).setIcon(android.R.drawable.ic_menu_gallery).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	menu.add(Menu.NONE, Consts.MENU_ID_GET_GEOLOC, Menu.NONE, getString(R.string.get_geolocation) ).setIcon(android.R.drawable.ic_menu_mylocation).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);
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

        case Consts.MENU_ID_TAKE_PHOTO:
        	// 写真撮影
        	startCamera();
        	break;
        case Consts.MENU_ID_ATTACH_PIC:
        	// 画像添付
        	startGallery();
            break;
        case Consts.MENU_ID_GET_GEOLOC:
        	// GPS測定
        	startGPS();
            break;
        }
        return true;
	}
	
	
	/**
	 * Google Static Mapsから現在地地図を取得して表示する.
	 */
	protected void getGoogleMapsStaticImage(){
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				URI uri;
				try {
					// http://maps.google.com/maps/api/staticmap?sensor=true&size=100x100&center=35.69715,139.78350&zoom=14&markers=color:blue|size:mid|35.69715,139.78350|
					double lat = mCurrentLocation.getLatitude();
					double lng = mCurrentLocation.getLongitude();
					String url = "http://maps.google.com/maps/api/staticmap?sensor=true&size=75x75&center="+lat+","+lng+"&zoom=14&markers=color%3Ablue%7Csize%3Amid%7C"+lat+","+lng+"%7C&mobile=true";
					Log.d(TAG,url);
					uri = new URI(url);
					HttpGet get = new HttpGet(uri);
			        DefaultHttpClient client = new DefaultHttpClient();
			        try {
						HttpResponse response = client.execute( get );
						int status = response.getStatusLine().getStatusCode();
						if ( status != HttpStatus.SC_OK ) return;
						InputStream is = response.getEntity().getContent();
						final Bitmap bmp = BitmapFactory.decodeStream(is);
						runOnUiThread(new Runnable() {							
							@Override
							public void run() {
								ImageView iv = (ImageView)findViewById(R.id.geolocation_thumbnail);
								iv.setImageBitmap(bmp);
								iv.setVisibility(View.VISIBLE);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}

				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
		th.start();
	}

	@Override
	public void onLocationChanged(Location location) {
		mCurrentLocation = location;
		Log.d(TAG,"latitude="+location.getLatitude());
		Log.d(TAG,"longitude="+location.getLongitude());
		stopGPS();

		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				getGoogleMapsStaticImage();
			}
		});
	}
	@Override
	public void onProviderDisabled(String provider) {
	}
	@Override
	public void onProviderEnabled(String provider) {
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
