package jp.miku39.android.tweetnyan.activities;

import java.util.ArrayList;
import java.util.List;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserList;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * リストがフォローしているユーザーの一覧.
 * @author amano
 *
 */

public class ShowFollowingUserOfListActivity extends ActivityWithTheme implements OnItemClickListener, IIconDownloadedCallback {
	final static String TAG = "ShowFollowingUserOfListActivity";

	public class ListUserAdapter extends ArrayAdapter<User>{
		Context cxt;
		private LayoutInflater mInflater;
		private List<User> mItems;

		public ListUserAdapter(Context context, int textViewResourceId, List<User> objects) {
			super(context, textViewResourceId, objects);
			mItems = objects;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			cxt = context;
		}

		@Override  
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;  
			if (view == null) {  
				view = mInflater.inflate(R.layout.one_list_layout, null);  
			}

			User user = (User)mItems.get(position);

			ImageView image = (ImageView)view.findViewById(R.id.profile_icon);
			Bitmap bmp = TweetnyanMainActivity.sIconCacheThread.loadIcon(user.getProfileImageURL(), user.getId());
			if( bmp!=null ){
				image.setImageBitmap(bmp);
			}else{
				image.setImageResource(R.drawable.ic_dummy);
			}

			image = (ImageView)view.findViewById(R.id.icon_private);
			image.setVisibility( user.isProtected()?View.VISIBLE:View.GONE );

			TextView tv;
			tv = (TextView)view.findViewById(R.id.list_name);
			String str = user.getScreenName();
			tv.setText( str );

			tv = (TextView)view.findViewById(R.id.list_description);
			tv.setText( user.getName() );
			return view;
		}
	}
	
	ListUserAdapter  mAdapter;
	private ListView mListView;
	private UserList mUserList;
	
	ArrayList<User> mUsers = new ArrayList<User>();

	private long mCursor = -1;
	private boolean mBusy;
	private Twitter mTwitter;

	
	public void startProgressBar(){
		ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
		bar.setVisibility(View.VISIBLE);
	}
	public void stopProgressBar(){
		ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
		bar.setVisibility(View.GONE);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.show_following_or_followers_list_layout);

	    mListView = (ListView)findViewById(R.id.listview_following_or_followers);
	    mListView.addFooterView( Lib.createLoadMoreTextview(this) );
	    mListView.setOnItemClickListener(this);
	    mUserList = (UserList)getIntent().getSerializableExtra("userlist");
	    mAdapter = new ListUserAdapter(this, R.layout.one_list_layout, mUsers);
	    mListView.setAdapter(mAdapter);

	    ActionBar bar = getSupportActionBar();
	    bar.setSubtitle( mUserList.getFullName() );

		mTwitter = Lib.getTwitter();

	    loadUsers();
	}

	@Override
	protected void onResume() {
		Log.d(TAG,"onResume");
		super.onResume();
	    TweetnyanMainActivity.sIconCacheThread.pushCallback( this );
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
	    TweetnyanMainActivity.sIconCacheThread.popCallback();
	}

	public void loadUsers(){
		if( mBusy ) return;
		mBusy = true;

		startProgressBar();
		Thread th = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final PagableResponseList<User> users = mTwitter.getUserListMembers( mUserList.getId(), mCursor );
					mCursor = users.getNextCursor();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							for(int i=0; i<users.size(); i++){
								User user = users.get(i);
								mUsers.add( user );
								TweetnyanMainActivity.sIconCacheThread.requestDownload( user.getProfileImageURL(), user.getId() );
							}
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format(getString(R.string.error_failed_to_get_following_list), e.getStatusCode());
					showToastFromThread( str );

				} finally {
					mBusy = false;

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							stopProgressBar();
						}
					});
				}
			}
		});
		th.start();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if( position == mUsers.size() ){
			loadUsers();
		}else{
			User user = mUsers.get(position);
			Intent intent = new Intent(this,UserProfileActivity.class);
			intent.putExtra("user", user);
			startActivity(intent);
		}
	}

	@Override
	public void onIconDownloaded(long id) {
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		});
	}

}
