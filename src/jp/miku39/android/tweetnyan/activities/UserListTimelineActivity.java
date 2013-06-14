package jp.miku39.android.tweetnyan.activities;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.lib.MyAlertDialog;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment.BasicTimelineInterface;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.UserList;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * 指定のリストのタイムラインを表示する.
 * @author amano
 *
 */

/*
 * TODO フォローしているユーザーのメニューを作る
 */

public class UserListTimelineActivity extends ActivityWithTheme implements BasicTimelineInterface, IIconDownloadedCallback {
	final static String TAG = "UserListTimelineActivity";

	private Twitter mTwitter;
	private UserList mUserList;

	private ListTimelineFragment mFragment;

	private boolean mWorkingOnUserListTL = false;

	public class ListTimelineFragment extends BasicTimelineFragment {
		@Override
		public void onStart() {
			super.onStart();
			if( mTweets.size()==0 ){
				refreshTimeline();
			}
		}

		@Override
		protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
			ResponseList<Status> st = mTwitter.getUserListStatuses(mUserList.getId(), p);
			return st;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.list_timeline_layout);

	    mTwitter = Lib.getTwitter();
		mUserList = (UserList)getIntent().getSerializableExtra("userlist");

		ActionBar bar = getSupportActionBar();
		bar.setSubtitle( R.string.list );

		String str = mUserList.getFullName();
	    if( !mUserList.isPublic()){
	    	str = "[private] "+str;
	    }
		TextView tv = (TextView)findViewById(R.id.list_name);
		tv.setText( str );

	    FragmentManager manager = getSupportFragmentManager();
	    FragmentTransaction transaction = manager.beginTransaction();
	    mFragment = new ListTimelineFragment();
	    transaction.add( R.id.list_timeline_layout_root, mFragment, "list-timeline" );
	    transaction.commit();
	    
	    TweetnyanMainActivity.sIconCacheThread.pushCallback( this );
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TweetnyanMainActivity.sIconCacheThread.popCallback();
	}

    public void startProgressBar(){
    	ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
    	bar.setVisibility(View.VISIBLE);
    }
    public void stopProgressBar(){
    	ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar_main);
    	bar.setVisibility(View.GONE);
    }

	@Override
	public Twitter getTwitter() {
		return mTwitter;
	}
	
    void followThisList(){
    	if( mWorkingOnUserListTL ) return;
    	mWorkingOnUserListTL  = true;

    	startProgressBar();
    	Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
		    	try {
					mUserList = mTwitter.createUserListSubscription( mUserList.getId() );
				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format(getString(R.string.error_failed_to_follow_list), e.getStatusCode());
					showToastFromThread(str);

				} finally {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							mWorkingOnUserListTL = false;
							stopProgressBar();
						}
					});
				}
			}
		});
    	th.start();
    }

    void unfollowThisList(){
    	if( mWorkingOnUserListTL ) return;
    	mWorkingOnUserListTL = true;

    	startProgressBar();
    	Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
		    	try {
		    		mUserList = mTwitter.destroyUserListSubscription( mUserList.getId() );

		    	} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format(getString(R.string.error_failed_to_unfollow_list), e.getStatusCode());
					showToastFromThread( str );

				} finally {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							mWorkingOnUserListTL = false;
							stopProgressBar();
						}
					});
				}
			}
		});
    	th.start();
    }

    void confirmToDeleteThisList(){
		MyAlertDialog d = MyAlertDialog.newInstance( this );
    	d.setIcon(R.drawable.ic_launcher);
    	d.setMessage( getString(R.string.confirm_delete_list) );
    	d.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	    	deleteThisList();
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
    void deleteThisList(){
		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_deleting_list) );
		dialog.setCancelable(false);
		FragmentManager manager = getSupportFragmentManager();
		dialog.show(manager, "delete-list");

        Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
		    	try {
		    		mUserList = mTwitter.destroyUserList( mUserList.getId() );

		    		Intent intent = new Intent();
		    		intent.putExtra("deleted-list", mUserList);
		    		setResult(RESULT_OK, intent);
		    		finish();

		    	} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format(getString(R.string.error_delete_list), e.getStatusCode());
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

    void showFollowingUser(){
		Intent intent = new Intent(this, ShowFollowingUserOfListActivity.class);
		intent.putExtra("userlist", mUserList);
		startActivity(intent);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Drawable tmpicon = getResources().getDrawable(R.drawable.icon);
        menu.add(Menu.NONE, Consts.MENU_ID_FOLLOW_THIS_LIST, Menu.NONE, getString(R.string.follow_this_list) );
        menu.add(Menu.NONE, Consts.MENU_ID_UNFOLLOW_THIS_LIST, Menu.NONE, getString(R.string.unfollow_this_list) );
    	menu.add(Menu.NONE, Consts.MENU_ID_DELETE_THIS_LIST, Menu.NONE, getString(R.string.delete_this_list) );
    	menu.add(Menu.NONE, Consts.MENU_ID_LIST_FOLLOWING, Menu.NONE, getString(R.string.user_following_this_list) );
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(Consts.MENU_ID_DELETE_THIS_LIST).setVisible(false);
    	if( mUserList.isFollowing() ){
    		menu.findItem(Consts.MENU_ID_FOLLOW_THIS_LIST).setVisible(false);
    		menu.findItem(Consts.MENU_ID_UNFOLLOW_THIS_LIST).setVisible(true);
		}else{
    		menu.findItem(Consts.MENU_ID_FOLLOW_THIS_LIST).setVisible(true);
    		menu.findItem(Consts.MENU_ID_UNFOLLOW_THIS_LIST).setVisible(false);
		}
    	// 自分のリストの場合は、削除メニューを表示
    	if( mUserList.getUser().getScreenName().equals( Lib.getCurrentAccountScreenName(this) ) ){
    		menu.findItem(Consts.MENU_ID_FOLLOW_THIS_LIST).setVisible(false);
    		menu.findItem(Consts.MENU_ID_UNFOLLOW_THIS_LIST).setVisible(false);
    		menu.findItem(Consts.MENU_ID_DELETE_THIS_LIST).setVisible(true);
    	}
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);

        case android.R.id.home:
        	Intent intent = new Intent( this, TweetnyanMainActivity.class );
        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	startActivity(intent);
        	break;
        	
        case Consts.MENU_ID_FOLLOW_THIS_LIST:
        	// このリストをフォローする
        	followThisList();
        	break;

        case Consts.MENU_ID_UNFOLLOW_THIS_LIST:
        	// このリストをアンフォローする
        	unfollowThisList();
        	break;
        	
        case Consts.MENU_ID_DELETE_THIS_LIST:
        	// このリストを削除する
        	confirmToDeleteThisList();
        	break;

        case Consts.MENU_ID_LIST_FOLLOWING:
        	// このリストがフォローしているユーザー
        	showFollowingUser();
        	break;

        }
        return true;
	}

	@Override
	public void onIconDownloaded(final long id) {
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				IIconDownloadedCallback tmp = (IIconDownloadedCallback) mFragment;
				tmp.onIconDownloaded(id);
			}
		});
	}

}
