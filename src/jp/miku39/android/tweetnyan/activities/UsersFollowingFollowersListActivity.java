package jp.miku39.android.tweetnyan.activities;

/**
 * フォロー・フォロワーのリスト表示
 * 
 * TODO 2000人を越えるフォロー・フォロワーに対応する
 */

import java.util.ArrayList;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.activities.FollowingFollowersListAdapter.StatusWithUser;
import twitter4j.IDs;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;


public class UsersFollowingFollowersListActivity extends ActivityWithTheme implements OnItemClickListener {
	final static String TAG = "ShowFollowingFollowersListActivity";

	User		mUser;
	boolean		isFollower;
	private Thread mThread;
	private int mCursor;

	private boolean mNowLoading;
	Twitter mTwitter;
	private ArrayList<StatusWithUser> mStatus = new ArrayList<StatusWithUser>();
	private FollowingFollowersListAdapter mAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.user_following_or_followers_list_layout);

	    Intent intent = getIntent();
	    mUser = (User)intent.getSerializableExtra("user");
	    isFollower = intent.getBooleanExtra("isfollower", false);

	    ActionBar bar = getSupportActionBar();
	    String str;
	    if( isFollower ){
	    	str = String.format( getString(R.string.a_user_followed), mUser.getScreenName() );
	    }else{
	    	str = String.format( getString(R.string.a_user_following), mUser.getScreenName() );
	    }
	    bar.setSubtitle( str );

		mTwitter = Lib.createTwitter(this);
		mCursor = 0;
	    mNowLoading = false;

	    initViews();
	    load();
	}

	void initViews(){
	    ListView lv = (ListView)findViewById(R.id.listview_following_or_followers);
	    mAdapter= new FollowingFollowersListAdapter(this, R.layout.one_tweet_layout, mStatus);
		lv.addFooterView( Lib.createLoadMoreTextview(this) );
		lv.setFastScrollEnabled(true);
	    lv.setAdapter( mAdapter );
		lv.setOnItemClickListener(this);
	}

	void load(){
		if( mNowLoading==true ) return;
		mNowLoading = true;

		setProgressBarIndeterminateVisibility(true);

		mThread = new Thread( new Runnable() {
			private long[] mIDs;

			@Override
			public void run() {
				final ArrayList<StatusWithUser> tmparray = new ArrayList<StatusWithUser>();
				try {
					if( mIDs==null){
						IDs ids;
						if( isFollower ){
							// このユーザーをフォローしているユーザーのIDを取得(2000件まで)
							ids = mTwitter.getFollowersIDs(mUser.getId(), -1 );
							Log.d(TAG,"get followers IDs");
						}else{
							// このユーザーがフォローしているユーザーのIDを取得(2000件まで)
							ids = mTwitter.getFriendsIDs(mUser.getId(), -1 );
							Log.d(TAG,"get following IDs");
						}
						mIDs = ids.getIDs();
					}
					int cnt = 0;
					cnt = Math.min( mIDs.length-mCursor*100, 100 );
					if( cnt>0 ){
						long lookupids[] = new long[cnt];
						for(int i=0;i<cnt;i++){
							int n = mCursor*100 + i;
							lookupids[i] = mIDs[ n ];
						}
						ResponseList<User> users = mTwitter.lookupUsers( lookupids );
						for(int i=0; i<users.size(); i++){
							StatusWithUser n = new StatusWithUser();
							n.mStatus = users.get(i).getStatus();
							n.mUser = users.get(i);
							tmparray.add( n );
							TweetnyanMainActivity.sIconCacheThread.requestDownload(n.mUser.getProfileImageURL(), n.mUser.getId());
						}
						mCursor++;
					}
				} catch (TwitterException e) {
					e.printStackTrace();
					showToastFromThread("Failed to obtain following/followers list.(code="+e.getStatusCode()+")", Toast.LENGTH_SHORT );

				} finally {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							setProgressBarIndeterminateVisibility(false);
							mStatus.addAll( tmparray );
							mAdapter.notifyDataSetChanged();
							mNowLoading = false;
						}
					});
				}
			}
		});
		mThread.start();
	}

	@Override
	protected void onResume() {
		Log.d(TAG,"onResume");
		super.onResume();

		// FIXME アイコン処理
//		TwitterClientMainActivity.sIconCacheService.registerViewUpdateCallback(mIconCacheListener);
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
		// FIXME アイコン処理
//		TwitterClientMainActivity.sIconCacheService.registerViewUpdateCallback(null);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if( position==mStatus.size() ){
			load();
			return;
		}
		StatusWithUser st = mStatus.get(position);
    	Intent intent = new Intent(this, UserProfileActivity.class);
    	intent.putExtra("user", st.mUser);
		startActivity(intent);
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
