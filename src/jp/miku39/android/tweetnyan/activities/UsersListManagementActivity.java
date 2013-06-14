package jp.miku39.android.tweetnyan.activities;

import java.util.ArrayList;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.lib.MyAlertDialog;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserList;
import yanzm.products.quickaction.lib.ActionItem;
import yanzm.products.quickaction.lib.QuickAction;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


public class UsersListManagementActivity extends ActivityWithTheme implements TabListener, OnPageChangeListener, IIconDownloadedCallback {
	final static String TAG = "UsersListActivity";
	final static int sTabs = 3;

	public final static int REQ_CODE_NEW_LIST = 1;
	public final static int REQ_CODE_EDIT_EDIT = 2;
	public final static int REQ_CODE_SHOW_LIST_TIMELINE = 3;

	User mTargetUser;
	Twitter mTwitter;

	private ArrayList<UserList> mCreatedList = new ArrayList<UserList>();
	private ArrayList<UserList> mFollowingList = new ArrayList<UserList>();
	private ArrayList<UserList> mFollowerList = new ArrayList<UserList>();
	long mCursorOfCreatedList = -1;
	long mCursorOfFollowingList = -1;

	private UsersListAdapter mCreatedListAdapter;
	private UsersListAdapter mFollowingListAdapter;
	private UsersListAdapter mFollowerListAdapter;

	View mLayoutViews[] = new View[ sTabs ];
	private ViewPager mViewPager;
	private int mCounter;

	public class MyPagerAdapter extends PagerAdapter{

		@Override
	    public int getCount() {
		    return sTabs;
	    }

	    /**
	     * Create the page for the given position.  The adapter is responsible
	     * for adding the view to the container given here, although it only
	     * must ensure this is done by the time it returns from
	     * {@link #finishUpdate()}.
	     *
	     * @param container The containing View in which the page will be shown.
	     * @param position The page position to be instantiated.
	     * @return Returns an Object representing the new page.  This does not
	     * need to be a View, but can be some other container of the page.
	     */
	    @Override
		public Object instantiateItem(View collection, int position) {
	    	try{
		    	View v = mLayoutViews[position];
		    	((ViewPager) collection).addView(v,0);
		    	return v;

	    	}catch(Exception e){
		        TextView tv = new TextView( UsersListManagementActivity.this );
		        tv.setText("Page " + position);
		        tv.setTextColor(Color.WHITE);
		        tv.setTextSize(30);
		        ((ViewPager) collection).addView(tv,0);
		    	return tv;
	    	}
	    }

	    /**
	     * Remove a page for the given position.  The adapter is responsible
	     * for removing the view from its container, although it only must ensure
	     * this is done by the time it returns from {@link #finishUpdate()}.
	     *
	     * @param container The containing View from which the page will be removed.
	     * @param position The page position to be removed.
	     * @param object The same object that was returned by
	     * {@link #instantiateItem(View, int)}.
	     */
	    @Override
	    public void destroyItem(View collection, int position, Object view) {
	        ((ViewPager) collection).removeView((View) view);
	    }

	    @Override
	    public boolean isViewFromObject(View view, Object object) {
	        return view==((View)object);
	    }

	    /**
	     * Called when the a change in the shown pages has been completed.  At this
	     * point you must ensure that all of the pages have actually been added or
	     * removed from the container as appropriate.
	     * @param container The containing View which is displaying this adapter's
	     * page views.
	     */
	    @Override
	    public void finishUpdate(View arg0) {}
	    
	    @Override
	    public void restoreState(Parcelable arg0, ClassLoader arg1) {}

	    @Override
	    public Parcelable saveState() {
	            return null;
	    }

	    @Override
	    public void startUpdate(View arg0) {}
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("created-list", mCreatedList);
		outState.putSerializable("following-list", mFollowingList);
		outState.putSerializable("followers-list", mFollowerList);
		outState.putLong("follower-cursor", mCursorOfFollowingList);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.user_list_layout);

	    mCounter = 0;

	    mTwitter = Lib.createTwitter(this);
	    mTargetUser = (User) getIntent().getSerializableExtra("user");

	    if( savedInstanceState!=null ){
	    	mCreatedList = (ArrayList<UserList>) savedInstanceState.getSerializable("created-list");
	    	mFollowingList = (ArrayList<UserList>) savedInstanceState.getSerializable("following-list");
	    	mFollowerList = (ArrayList<UserList>) savedInstanceState.getSerializable("followers-list");
	    	mCursorOfFollowingList = savedInstanceState.getLong("follower-cursor");
	    }

        initViews();

	    ActionBar bar = getSupportActionBar();
	    String str = String.format( getString(R.string.a_users_list), mTargetUser.getScreenName());
	    bar.setSubtitle(str);

	    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        Tab tab;
        tab = bar.newTab();
        tab.setText( getString(R.string.list) );
        tab.setTabListener(this);
        bar.addTab(tab);

        tab = bar.newTab();
        tab.setText( getString(R.string.follow) );
        tab.setTabListener(this);
        bar.addTab(tab);

        tab = bar.newTab();
        tab.setText( getString(R.string.follower) );
        tab.setTabListener(this);
        bar.addTab(tab);

	    if( savedInstanceState==null ){
	        getList();
	        getFollowerList();
	    }
	    TweetnyanMainActivity.sIconCacheThread.pushCallback( this );
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TweetnyanMainActivity.sIconCacheThread.popCallback();
	}

	void initViews(){
		ListView lv;

		// ユーザーが作成したリスト
		lv = new ListView( this );
//		lv.setEmptyView( findViewById(R.id.empty_text) ); // TODO 失敗
		// TODO cursorを使わないAPIを使用しているので、現在、load moreはない
//        lv.addFooterView(Lib.createLoadMoreTextview(this));
    	mLayoutViews[0] = lv;
        mCreatedListAdapter = new UsersListAdapter(this, R.layout.one_list_layout, mCreatedList);
        lv.setAdapter(mCreatedListAdapter);
        lv.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if( position==mCreatedList.size() ){
				}else{
					showUserListTweet( mCreatedList.get(position) );
				}
			}
		});
        // Tweetnyanの使用者の作成したリストでは、ロングタップによる操作メニューを出す
		if( mTargetUser.getScreenName().equals( Lib.getCurrentAccountScreenName(this) ) ){
        	lv.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if( position==mCreatedList.size() ){
						// 何もすることがない
					}else{
						quickActionLongClick(parent, view ,position, id);
					}					
					return false;
				}
			});
        }

        // ユーザーがfollowしているリスト
		lv = new ListView( this );
//		lv.setEmptyView( findViewById(R.id.empty_text) ); // TODO 失敗
		// TODO cursorを使わないAPIを使用しているので
//        lv.addFooterView(Lib.createLoadMoreTextview(this));
    	mLayoutViews[1] = lv;
        mFollowingListAdapter = new UsersListAdapter(this, R.layout.one_list_layout, mFollowingList);
        lv.setAdapter(mFollowingListAdapter);
        lv.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if( position==mFollowingList.size() ){
				}else{
					showUserListTweet( mFollowingList.get(position) );
				}
			}
		});

        // ユーザーをfollowingしているリスト
        lv = new ListView( this );
//		lv.setEmptyView( findViewById(R.id.empty_text) ); // TODO 失敗
    	mLayoutViews[2] = lv;
        lv.addFooterView(Lib.createLoadMoreTextview(this));
        mFollowerListAdapter = new UsersListAdapter(this, R.layout.one_list_layout, mFollowerList);
        lv.setAdapter(mFollowerListAdapter);
        lv.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if( position==mFollowerList.size() ){
					getFollowerList();
				}else{
					showUserListTweet( mFollowerList.get(position) );
				}
			}
		});

        MyPagerAdapter adapter = new MyPagerAdapter();
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(this);
	}

	void startProgressBar(){
		mCounter++;
		ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
		bar.setVisibility(View.VISIBLE);
	}
	void stopProgressBar(){
		mCounter--;
		if( mCounter<=0 ){
			ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
			bar.setVisibility(View.GONE);
			mCounter = 0;
		}
	}
	
	void getList(){
		startProgressBar();
		
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				final ResponseList<UserList> list2;
				
				try {
					list2 = null;//mTwitter.getAllUserLists( mTargetUser.getId() );

					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							showUsersCreatedList(list2);
						}
					});
				} finally {
					runOnUiThread( new Runnable() {
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

	// getUserLists
	void showUsersCreatedList(ResponseList<UserList> list) {
		boolean first = true;
		for(int i=0; i<list.size(); i++){
			UserList l = list.get(i);
			User user = l.getUser();
			if( user.getId()==mTargetUser.getId() ){
				mCreatedList.add( l );
				if( first ){
					TweetnyanMainActivity.sIconCacheThread.requestDownload(user.getProfileImageURL(), user.getId());
					first = false;
				}
			}else{
				mFollowingList.add( l );
				TweetnyanMainActivity.sIconCacheThread.requestDownload(user.getProfileImageURL(), user.getId());
			}
		}
		mCreatedListAdapter.notifyDataSetChanged();
		mFollowingListAdapter.notifyDataSetChanged();

		if( mCreatedList.size()==0 ){
//			((TextView)findViewById(R.id.no_list)).setVisibility(View.VISIBLE);
//			((ListView)findViewById(R.id.created_list)).setVisibility(View.GONE);
		}
		if( mFollowingList.size()==0 ){
//			((TextView)findViewById(R.id.no_following_list)).setVisibility(View.VISIBLE);
//			((ListView)findViewById(R.id.following_list)).setVisibility(View.GONE);
		}
	}

	void getFollowerList(){
		startProgressBar();
		
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				final PagableResponseList<UserList> list;
				try {
					list = mTwitter.getUserListMemberships( mTargetUser.getId(), mCursorOfFollowingList );
					mCursorOfFollowingList = list.getNextCursor();
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							showUsersFollwoingList(list);
						}
					});
				} catch (TwitterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					runOnUiThread( new Runnable() {
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
	void showUsersFollwoingList(PagableResponseList<UserList> list) {
		for(int i=0; i<list.size(); i++){
			mFollowerList.add( list.get(i) );
			User user = list.get(i).getUser();
			TweetnyanMainActivity.sIconCacheThread.requestDownload(user.getProfileImageURL(), user.getId());
		}
		mFollowerListAdapter.notifyDataSetChanged();
		if( mFollowerList.size()==0 ){
			// TODO フォロワーリストがないときの処理
//			((TextView)findViewById(R.id.no_follower_list)).setVisibility(View.VISIBLE);
//			((ListView)findViewById(R.id.follower_list)).setVisibility(View.GONE);
		}
	}

	/**
	 * リストのTLを表示する.
	 * @param userList
	 */
	protected void showUserListTweet(UserList userList) {
		Intent intent = new Intent(this, UserListTimelineActivity.class);
		intent.putExtra("userlist", userList);
		startActivityForResult( intent, REQ_CODE_SHOW_LIST_TIMELINE );
	}

	protected void quickActionLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final QuickAction qa = new QuickAction(view);
		ActionItem item;

		// リストの詳細を編集
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(android.R.drawable.ic_menu_edit) );
		item.setTitle( getString(R.string.edit_this_list) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	        	Intent intent = new Intent(UsersListManagementActivity.this, CreateNewListActivity.class);
	        	intent.putExtra("current-list", mCreatedList.get(position));
	        	startActivityForResult(intent, REQ_CODE_EDIT_EDIT);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

		// リストの削除
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_delete) );
		item.setTitle( getString(R.string.delete_this_list) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				confirmToDeleteThisList(position);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

		qa.show();
	}

    /**
     * このリストを削除しますか？
     */
    void confirmToDeleteThisList(final int position){
		MyAlertDialog d = MyAlertDialog.newInstance( this );
    	d.setIcon(R.drawable.ic_launcher);
    	d.setMessage( getString(R.string.confirm_delete_list) );
    	d.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	    	deleteThisList( position );
    	    }
    	});
    	d.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	        // Nothing to do
    	    }
    	});
    	d.show();
    }

    /**
     * このリストを削除する.
     */
    void deleteThisList(final int position){
    	final Twitter twitter = mTwitter;
    	final UserList l = mCreatedList.get(position);

		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_deleting_list) );
		dialog.setCancelable(false);
		FragmentManager manager = getSupportFragmentManager();
		dialog.show(manager, "delete-list");

        Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
		    	try {
		    		twitter.destroyUserList( l.getId() );
		    		runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mCreatedList.remove(position);
							mCreatedListAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format( getString(R.string.error_delete_list), e.getStatusCode());
					showToastFromThread( str );

				} finally {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
				    		dialog.dismiss();
						}
					});
				}
			}
		});
    	th.start();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQ_CODE_NEW_LIST){
			if( resultCode == RESULT_OK ) {
				UserList l = (UserList)data.getSerializableExtra("new-list");
				mCreatedList.add(l);
				Log.d(TAG,"Create a new list.");
				mCreatedListAdapter.notifyDataSetChanged();
			}
		}
		if( requestCode == REQ_CODE_EDIT_EDIT ){
			if( resultCode == RESULT_OK ) {
				Log.d(TAG,"Update the list description.");
				UserList l = (UserList)data.getSerializableExtra("updated-list");
				for(int i=0; i<mCreatedList.size(); i++){
					if( mCreatedList.get(i).getId()==l.getId() ){
						mCreatedList.set( i, l );
						break;
					}
				}
				mCreatedListAdapter.notifyDataSetChanged();
			}
		}
		if(requestCode==REQ_CODE_SHOW_LIST_TIMELINE && resultCode==RESULT_OK){
			UserList deleted = (UserList)data.getSerializableExtra("deleted-list");
			for(int i=0;i<mCreatedList.size();i++){
				UserList l = mCreatedList.get(i);
				if( l.getId()==deleted.getId()){
					mCreatedList.remove(i);
					break;
				}
			}
			mCreatedListAdapter.notifyDataSetChanged();
		}
	}


	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem( tab.getPosition() );
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		ActionBar bar = getSupportActionBar();
		bar.setSelectedNavigationItem(position);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if( mTargetUser.getScreenName().equals( Lib.getCurrentAccountScreenName(this) ) ){
			menu.add(Menu.NONE, Consts.MENU_ID_CREATE_LIST, Menu.NONE, getString(R.string.create_list) ).setIcon( android.R.drawable.ic_menu_add ).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
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
        
        case Consts.MENU_ID_CREATE_LIST:
        	intent = new Intent( this, CreateNewListActivity.class );
        	startActivityForResult( intent, REQ_CODE_NEW_LIST );
        	break;
        }
        return true;
	}

	@Override
	public void onIconDownloaded(long id) {
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				mCreatedListAdapter.notifyDataSetChanged();
				mFollowingListAdapter.notifyDataSetChanged();
				mFollowerListAdapter.notifyDataSetChanged();
			}
		});
	}
}
