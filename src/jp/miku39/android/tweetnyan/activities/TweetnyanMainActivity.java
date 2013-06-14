package jp.miku39.android.tweetnyan.activities;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.IconCacheThread;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.Prefs;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment.BasicTimelineInterface;
import jp.miku39.android.tweetnyan.fragments.DirectMessageTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.HomeTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.MentionTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.SearchFragment;
import jp.miku39.android.tweetnyan.fragments.UserListTimelineFragment;
import jp.miku39.android.tweetnyan.services.IconCacheService;
import jp.miku39.android.tweetnyan.services.TwitterService;
import twitter4j.RateLimitStatusEvent;
import twitter4j.RateLimitStatusListener;
import twitter4j.Twitter;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


public class TweetnyanMainActivity extends ActivityWithTheme implements TabListener, OnPageChangeListener, BasicTimelineInterface, RateLimitStatusListener {
	final static String TAG = "TweetnyanMainActivity";
	
	final static int NUM_TABS = 5;	// Home,Mentions,DM,List,Search

	private int mProgressBarDisplayCounter = 0;

	private Twitter mTwitter;

	private ViewPager mViewPager;
	private MyPagerAdapter mAdapter;
	private static Fragment[] sFragments;

	public static IconCacheThread sIconCacheThread = null;
    public static TwitterService sTwitterService = null;
    boolean mIsBound;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            sTwitterService = ((TwitterService.LocalBinder)service).getService();
            // Tell the user about this for our demo.
            Log.d(TAG,"Connected Twitter Service.");

            //initFragments();
            initViews();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            sTwitterService = null;
            Log.d(TAG,"Disconnected Twitter Service.");
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, TwitterService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }


    public static class MyPagerAdapter extends FragmentPagerAdapter {
    	final static String TAG = "MyPagerAdapter";
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public Fragment getItem(int position) {
        	Log.d(TAG,"MyPagerAdapter.getItem("+position+")");
        	switch( position ){
        	case 0:// Home
        	case 1:// Mention
        	case 2:// Direct Message
        	case 3:// User List
        	case 4:// Search
        		return sFragments[position];

        	default:
    			Log.d(TAG,"oops!");
                return new BasicTimelineFragment();        			
        	}
        }
    }

    /**
     * Twitterインスタンスを返す.
     * @return Twitterインスタンス
     */
    public Twitter getTwitter(){
    	return mTwitter;
    }

    public void showToast(String text, int duration){
    	Toast.makeText(this, text, duration).show();
    }
    public void showToastFromThread(final String text, final int duration){
    	runOnUiThread( new Runnable() {
			@Override
			public void run() {
		    	Toast.makeText(TweetnyanMainActivity.this, text, duration).show();
			}
		});
    }

    public void startProgressBar(){
    	ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
    	bar.setVisibility(View.VISIBLE);
    	mProgressBarDisplayCounter++;
    }
    public void stopProgressBar(){
    	mProgressBarDisplayCounter--;
    	if( mProgressBarDisplayCounter<=0 ){
    		mProgressBarDisplayCounter=0;
        	ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
        	bar.setVisibility(View.GONE);
    	}
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initFragments();
        //initViews();
        
        mTwitter = Lib.createTwitter( this );
        mTwitter.addRateLimitStatusListener(this);
        
        ActionBar bar = getSupportActionBar();
    	bar.setSubtitle( "@"+Lib.getCurrentAccountScreenName(this) );

    	sIconCacheThread = new IconCacheThread( this );
    	initServices();
    }

    void initFragments(){
    	sFragments = new Fragment[5];
    	sFragments[Consts.TAB_HOME] 	= new HomeTimelineFragment();
    	sFragments[Consts.TAB_MENTION]	= new MentionTimelineFragment();
    	sFragments[Consts.TAB_DM]   	= new DirectMessageTimelineFragment();
    	sFragments[Consts.TAB_LIST] 	= new UserListTimelineFragment();
    	sFragments[Consts.TAB_SEARCH] 	= new SearchFragment();
    }

    void initServices(){
        startService(new Intent(this, TwitterService.class));
        doBindService();
    }

    void initActionBarTabs(){
        ActionBar bar = getSupportActionBar();

        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //bar.setLogo(R.drawable.ic_launcher);
        bar.setHomeButtonEnabled(false);

        Tab tab;
        tab = bar.newTab();
        tab.setIcon(R.drawable.ic_tab_home_unselected);
//        tab.setText( getString(R.string.tab_home) );
        tab.setTabListener(this);
        bar.addTab(tab);

        tab = bar.newTab();
        tab.setIcon(R.drawable.ic_tab_reply_unselected);
//        tab.setText( getString(R.string.tab_mention) );
        tab.setTabListener(this);
        bar.addTab(tab);

        tab = bar.newTab();
        tab.setIcon(R.drawable.ic_tab_directmessage_unselected);
//        tab.setText( getString(R.string.tab_directmessage) );
        tab.setTabListener(this);
        bar.addTab(tab);

        tab = bar.newTab();
        tab.setIcon(R.drawable.ic_tab_list_unselected);
//        tab.setText( getString(R.string.tab_list) );
        tab.setTabListener(this);
        bar.addTab(tab);

        tab = bar.newTab();
        tab.setIcon(R.drawable.ic_tab_search_unselected);
//        tab.setText( getString(R.string.tab_search) );
        tab.setTabListener(this);
        bar.addTab(tab);
    }

    void initViews(){
        mAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(this);

        initActionBarTabs();
    }

	@Override
	protected void onStart() {
		super.onStart();
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		String str = getDefaultPreferenceString("number_of_tweets_to_retrieve", "50");
		Prefs.sNumOfTweets = Integer.parseInt( str );
		Log.d(TAG,"Number of retrieving tweets: "+Prefs.sNumOfTweets);
		
		str = getDefaultPreferenceString("date_format", "0");
		Prefs.sDateFormatType = Integer.parseInt( str );

		str = getDefaultPreferenceString("where_to_move_when_timeline_updated", "0");
		Prefs.sDestinationOnUpdate = Integer.parseInt( str );

		sIconCacheThread.pushCallback( new IconCacheThread.IIconDownloadedCallback() {
			@Override
			public void onIconDownloaded(final long id) {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						// FIXME アイコンDL完了で表示の更新をする処理を入れる
						for(int i=0;i<4;i++){
							IIconDownloadedCallback tmp = (IIconDownloadedCallback) sFragments[i];
							tmp.onIconDownloaded(id);
						}
					}
				});
			}
		} );
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		sIconCacheThread.popCallback();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		sIconCacheThread.destroy();
		uninitServices();
	}

	void uninitServices(){
		doUnbindService();
        stopService(new Intent(this, IconCacheService.class));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG,"change screen orientation.");
	    super.onConfigurationChanged(newConfig);
	}

	// TODO メニュー
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(Menu.NONE, MENU_ID_MENU1, Menu.NONE, getString(R.string.new_tweet) ).setIcon(R.drawable.ic_menu_tweet).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//    	menu.add(Menu.NONE, MENU_ID_MENU2, Menu.NONE, getString(R.string.refresh) ).setIcon(R.drawable.ic_refresh_timeline).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	menu.add(Menu.NONE, Consts.MENU_ID_MY_PROFILE, Menu.NONE, getString(R.string.my_profile) ).setIcon(R.drawable.ic_menu_profile);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    	menu.add(Menu.NONE, Consts.MENU_ID_SWITCH_ACCOUNT, Menu.NONE, getString(R.string.switch_account) ).setIcon(R.drawable.ic_menu_account);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    	menu.add(Menu.NONE, Consts.MENU_ID_SETTINGS, Menu.NONE, getString(R.string.settings) ).setIcon(R.drawable.ic_menu_setting);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    	menu.add(Menu.NONE, Consts.MENU_ID_ABOUT, Menu.NONE, getString(R.string.about) ).setIcon(R.drawable.ic_launcher);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG,"Menu selected:"+item.getItemId());
		Intent intent;
        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);

        case Consts.MENU_ID_MY_PROFILE:
        	intent = new Intent(this, UserProfileActivity.class);
        	intent.putExtra("targetname", Lib.getCurrentAccountScreenName(this) );
        	startActivity(intent);
        	break;

        case Consts.MENU_ID_SWITCH_ACCOUNT:
        	intent = new Intent( this, SelectAccountActivity.class );
        	intent.putExtra("no-skip", true);
        	startActivity(intent);
        	finish();
        	break;
        	
        case Consts.MENU_ID_SETTINGS:
        	intent = new Intent( this, TweetnyanPreferenceActivity.class );
        	startActivity(intent);
        	break;

        case Consts.MENU_ID_ABOUT:
        	intent = new Intent( this, AboutActivity.class );
        	startActivity(intent);
            break;
        }
        return true;
	}


	// タブ選択.
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

	// ページ移動処理
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
	public void onRateLimitReached(RateLimitStatusEvent limit) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onRateLimitStatus(RateLimitStatusEvent limit) {
		String str = "Remain:";
		str += limit.getRateLimitStatus().getRemainingHits();
		int remain = (limit.getRateLimitStatus().getSecondsUntilReset()/60)+1;
		str += "/ Reset: "+remain+"min after";
		showToastFromThread(str, Toast.LENGTH_SHORT);
	}

}
