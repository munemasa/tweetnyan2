package jp.miku39.android.tweetnyan.activities;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import br.com.dina.ui.widget.UITableView;
import br.com.dina.ui.widget.UITableView.ClickListener;


public class UserProfileActivity extends ActivityWithTheme implements ClickListener, IIconDownloadedCallback {

	private User mTargetUser;
	private long mCurrentAccountUserId;
	private String mTargetName;
	private Twitter mTwitter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.userprofile_layout);

	    ActionBar bar = getSupportActionBar();
	    bar.setSubtitle( getString(R.string.profile) );

	    Intent intent = getIntent();
	    mTargetUser = (User)intent.getSerializableExtra("user");
	    mTargetName = intent.getStringExtra("targetname");
	    mCurrentAccountUserId = Lib.getCurrentAccountUserId(this);

	    if( mTargetName!=null ){
	    	getUserProfile();
	    }else{
	    	initView();
	    	TweetnyanMainActivity.sIconCacheThread.requestDownload( mTargetUser.getProfileImageURL(), mTargetUser.getId() );
	    }

	    TweetnyanMainActivity.sIconCacheThread.pushCallback( this );
	}

	@Override
	public void onIconDownloaded(long id) {
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				ImageView imgview;
				Bitmap icon = TweetnyanMainActivity.sIconCacheThread.loadIcon(mTargetUser.getProfileImageURL(), mTargetUser.getId());
				if( icon!=null ){
					imgview = (ImageView)findViewById(R.id.profile_icon);
					imgview.setImageBitmap(icon);
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	    TweetnyanMainActivity.sIconCacheThread.popCallback();
	}

	void getUserProfile(){
		setProgressBarIndeterminateVisibility(true);
		
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				mTwitter = Lib.createTwitter(UserProfileActivity.this);
				try {
					mTargetUser = mTwitter.showUser( mTargetName );
			    	TweetnyanMainActivity.sIconCacheThread.requestDownload( mTargetUser.getProfileImageURL(), mTargetUser.getId() );
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							initView();
						}
					});

				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format(getString(R.string.error_get_profile), e.getStatusCode());
					showToastFromThread(str, Toast.LENGTH_SHORT);

				} finally {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							setProgressBarIndeterminateVisibility(false);
						}
					});
				}
			}
		});
		th.start();

	}

	void initView(){
		ImageView imgview;

		Bitmap icon = TweetnyanMainActivity.sIconCacheThread.loadIcon(mTargetUser.getProfileImageURL(), mTargetUser.getId());
		if( icon!=null ){
			imgview = (ImageView)findViewById(R.id.profile_icon);
			imgview.setImageBitmap(icon);
		}

		imgview = (ImageView)findViewById(R.id.protected_user_icon);
		imgview.setVisibility( mTargetUser.isProtected()?View.VISIBLE:View.GONE );

		imgview = (ImageView)findViewById(R.id.verified_icon);
		imgview.setVisibility( mTargetUser.isVerified()?View.VISIBLE:View.GONE );

		TextView txtview;
		txtview = (TextView)findViewById(R.id.txt_screen_name);
		txtview.setText(mTargetUser.getScreenName());

		txtview = (TextView)findViewById(R.id.txt_user_name);
		txtview.setText(mTargetUser.getName());

		txtview = (TextView)findViewById(R.id.txt_profile);
		try{
			txtview.setText(mTargetUser.getDescription());
		}catch(Exception e){
			txtview.setVisibility(View.GONE);			
		}

		txtview = (TextView)findViewById(R.id.txt_location);
		try{
			txtview.setText(mTargetUser.getLocation());
		}catch(Exception e){
			txtview.setVisibility(View.GONE);
		}

		txtview = (TextView)findViewById(R.id.txt_url);
		try{
			txtview.setAutoLinkMask(Linkify.WEB_URLS);
			txtview.setText(mTargetUser.getURL().toString());
		}catch(Exception e){
			txtview.setVisibility(View.GONE);
		}

        UITableView tableView = (UITableView) findViewById(R.id.tableView);
        tableView.setClickListener(this);

        String str;
        str = String.format( getString(R.string.numof_tweets), mTargetUser.getStatusesCount());
        tableView.addBasicItem(str);
        str = String.format( getString(R.string.numof_favorite), mTargetUser.getFavouritesCount());
        tableView.addBasicItem(str);
        str = String.format( getString(R.string.numof_follow), mTargetUser.getFriendsCount());
        tableView.addBasicItem(str);
        str = String.format( getString(R.string.numof_follower), mTargetUser.getFollowersCount());
        tableView.addBasicItem(str);
        str = getString( R.string.list );
        tableView.addBasicItem(str);
        str = getString( R.string.search_related_tweets );
        tableView.addBasicItem(str);

        if( mTargetUser.getId()==mCurrentAccountUserId ){
        	str = getString( R.string.retweet_by_others );
            tableView.addBasicItem(str);
        	str = getString( R.string.retweet_by_you );
            tableView.addBasicItem(str);
        	str = getString( R.string.your_tweet_rewteeted );
            tableView.addBasicItem(str);
        }

        tableView.commit();

		// DM作成ボタン
	    Button btn = (Button)findViewById(R.id.btn_create_dm);
	    btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent;
		    	intent = new Intent(UserProfileActivity.this, CreateDirectMessageActivity.class);
		    	intent.putExtra("to", mTargetUser.getScreenName());
		    	startActivity(intent);
			}
		});

		Button btn1, btn2, btn3;
	    btn1 = (Button)findViewById(R.id.btn_add_list);
		btn2 = (Button)findViewById(R.id.btn_follow_or_unfollow);
		btn3 = (Button)findViewById(R.id.btn_edit);
		if( mTargetUser.getId()==mCurrentAccountUserId ){
			btn1.setVisibility(View.GONE);		// リストに追加
			btn2.setVisibility(View.GONE);		// フォロー・アンフォロー 
			btn3.setVisibility(View.VISIBLE);	// 編集
			btn3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// 編集画面へ
					Intent intent = new Intent(UserProfileActivity.this, EditProfileActivity.class);
					intent.putExtra( "profile", mTargetUser );
					startActivity(intent);
				}
			});
		}
	}

	@Override
	public void onClick(int index) {
		Intent intent;
		switch( index ){
		case 0:
			// ツイート
			intent = new Intent(this, UsersHomeTimelineActivity.class);
			intent.putExtra("targetuser", mTargetUser);
			intent.putExtra("targetname", mTargetName);
			startActivity(intent);
			break;
		case 1:
			// お気に入り
			intent = new Intent(this, UserFavoritesActivity.class);
			intent.putExtra("targetuser", mTargetUser);
			intent.putExtra("targetname", mTargetName);
			startActivity(intent);			
			break;
		case 2:
			// フォロー
			intent = new Intent(UserProfileActivity.this, UsersFollowingFollowersListActivity.class);
			intent.putExtra("user", mTargetUser);
			intent.putExtra("isfollower", false);
			startActivity(intent);
			break;
		case 3:
			// フォロワー
			intent = new Intent(UserProfileActivity.this, UsersFollowingFollowersListActivity.class);
			intent.putExtra("user", mTargetUser);
			intent.putExtra("isfollower", true);
			startActivity(intent);
			break;
		case 4:
			// リスト
			intent = new Intent(UserProfileActivity.this, UsersListManagementActivity.class);
			intent.putExtra("user", mTargetUser);
			startActivity(intent);
			break;
		case 5:
			// 関連ツイートを検索
			intent = new Intent(this, SearchResultActivity.class);
			intent.putExtra( "search-word", "@"+mTargetUser.getScreenName() );
			startActivity(intent);
			break;
		default:
			break;
		}
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, Consts.MENU_ID_BLOCK, Menu.NONE, getString(R.string.block) ).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    	menu.add(Menu.NONE, Consts.MENU_ID_REPORT_SPAM, Menu.NONE, getString(R.string.report_spam) ).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return super.onCreateOptionsMenu(menu);
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
        case Consts.MENU_ID_BLOCK:
        	// ブロック
            break;
        case Consts.MENU_ID_REPORT_SPAM:
        	// スパムとして報告
            break;
        }
        return true;
	}

}
