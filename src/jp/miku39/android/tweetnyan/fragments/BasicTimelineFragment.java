package jp.miku39.android.tweetnyan.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.lib.MyAlertDialog;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.FavoriteStatusWrapper;
import jp.miku39.android.tweetnyan.IconCacheThread;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.Prefs;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.activities.CheckTweetSelectedInterface;
import jp.miku39.android.tweetnyan.activities.CreateNewTweetActivity;
import jp.miku39.android.tweetnyan.activities.StatusListViewAdapter;
import jp.miku39.android.tweetnyan.activities.TweetnyanMainActivity;
import jp.miku39.android.tweetnyan.activities.UserProfileActivity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import yanzm.products.quickaction.lib.ActionItem;
import yanzm.products.quickaction.lib.QuickAction;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class BasicTimelineFragment extends Fragment implements CheckTweetSelectedInterface, OnItemLongClickListener, OnItemClickListener, IconCacheThread.IIconDownloadedCallback {
	final static String TAG = "BasicTimelineFragment";

	protected PullToRefreshListView mPullToRefreshListView;
	protected ListView mListView;
	protected ArrayList<Status> mTweets = new ArrayList<Status>();

	protected BasicTimelineInterface mInterface;
	protected StatusListViewAdapter mAdapter;

	protected HashMap<Long,Status> mSelectedStatus = new HashMap<Long,Status>();
	protected ActionMode mActionMode;

	private volatile boolean mIsRunning = false;	// ツイート取得処理が走行中

	private MenuItem mRefreshMenuItem;

	public interface BasicTimelineInterface {
	    public void startProgressBar();
	    public void stopProgressBar();
	    public Twitter getTwitter();
	    public void showToastFromThread(final String text, final int duration);
	    public void showToast(String text, int duration);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mInterface = (BasicTimelineInterface) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d( this.getClass().getName(), "onCreate");
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d( this.getClass().getName(), "onCreateView");
        View v = inflater.inflate(R.layout.basic_timeline_layout, container, false);
        mPullToRefreshListView = (PullToRefreshListView) v.findViewById(R.id.basic_timeline_listview);
        mListView = mPullToRefreshListView.getRefreshableView();
        mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mListView.addFooterView( Lib.createLoadMoreTextview(getActivity()) );

		mPullToRefreshListView.setOnRefreshListener( new com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshTimeline();
			}
		});

        mAdapter = new StatusListViewAdapter(getActivity(), R.layout.one_tweet_layout, mTweets ,this);
        mListView.setAdapter(mAdapter);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d( this.getClass().getName(), "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		Log.d( this.getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		Log.d( this.getClass().getName(), "onDestroyView");
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		Log.d( this.getClass().getName(), "onDetach");
		super.onDetach();
	}

	@Override
	public void onPause() {
		Log.d( this.getClass().getName(), "onPause");
		super.onPause();
	}

	@Override
	public void onResume() {
		Log.d( this.getClass().getName(), "onResume");
		super.onResume();
	}

	@Override
	public void onStart() {
		Log.d( this.getClass().getName(), "onStart");
		super.onStart();
	}

	@Override
	public void onStop() {
		Log.d( this.getClass().getName(), "onStop");
		super.onStop();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d( this.getClass().getName(), "onViewCreated");
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		position--;
		if( position==mTweets.size() ){
			loadMore();
			return;
		}

		Status tmp = mTweets.get(position);
    	if( tmp.isRetweet() ){
    		tmp = tmp.getRetweetedStatus();
    	}
    	final Status clicked_tweet = tmp;

    	// ここからクイックアクションを作成
		final QuickAction qa = new QuickAction(view);
		ActionItem item;

		// リプライ
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_reply) );
		item.setTitle( getString(R.string.qa_do_reply) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Status st = clicked_tweet;
	        	Intent intent = new Intent(getActivity(), CreateNewTweetActivity.class);
	        	if( st.isRetweet() ){
	        		st = st.getRetweetedStatus();
	        	}
	        	intent.putExtra("in-reply-to", st);
	        	startActivity(intent);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

		// Retweet
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_retweet) );
		item.setTitle( getString(R.string.qa_do_retweet) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MyAlertDialog d = MyAlertDialog.newInstance(getActivity());
	        	d.setIcon(R.drawable.ic_launcher);
	        	d.setMessage( getString(R.string.qa_confirm_retweet) );
	        	d.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
	        	    public void onClick(DialogInterface dialog, int whichButton) {
	        	    	retweet( clicked_tweet );
	    	        	qa.dismiss();
	        	    }
	        	});
	        	d.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
	        	    public void onClick(DialogInterface dialog, int whichButton) {
	        	        // Nothing to do
	    	        	qa.dismiss();
	        	    }
	        	});
	        	d.show();
			}
		});
		qa.addActionItem(item);

		// Retweet with Comment
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_retweet_informal) );
		item.setTitle( getString(R.string.qa_do_retweet_with_comment) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Status st;
	        	Intent intent = new Intent(getActivity(), CreateNewTweetActivity.class);
	        	st = clicked_tweet;
	        	if( st.isRetweet() ){
	        		st = st.getRetweetedStatus();
	        	}
	        	String str = " RT @"+st.getUser().getScreenName()+": "+st.getText();
	        	intent.putExtra("text", str);
	    		intent.putExtra("move_last",false);
	        	startActivity(intent);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

		// お気に入り追加と削除
		item = new ActionItem();
		// TODO お気に入り追加したときにお気に入りフラグが立たないので仮対処
//		if( !clicked_tweet.isFavorited() ){
		if( !FavoriteStatusWrapper.isFavorited(clicked_tweet) ){
			item.setIcon( getResources().getDrawable(R.drawable.ic_fav_add) );
			item.setTitle( getString(R.string.qa_do_favorite) );
			item.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					addToFavorite(clicked_tweet);
		        	qa.dismiss();
				}
			});
		}else{
			item.setIcon( getResources().getDrawable(R.drawable.ic_fav_del) );
			item.setTitle( getString(R.string.qa_do_unfavorite) );
			item.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteFromFavorite(clicked_tweet);
		        	qa.dismiss();
				}
			});
		}
		qa.addActionItem(item);

		// in-reply-to があれば会話を表示する
		if( clicked_tweet.getInReplyToStatusId()>0 ){
			item = new ActionItem();
			item.setIcon( getResources().getDrawable(R.drawable.ic_chat) );
			item.setTitle( getString(R.string.qa_show_talking) );
			item.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showTalkDialog( clicked_tweet );
		        	qa.dismiss();
				}
			});
			qa.addActionItem(item);
		}

		long myid = Lib.getCurrentAccountUserId(getActivity());
		if( clicked_tweet.getUser().getId()==myid ){
			// 削除
			item = new ActionItem();
			item.setIcon( getResources().getDrawable(R.drawable.ic_delete) );
			item.setTitle( getString(R.string.qa_do_delete) );
			item.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					MyAlertDialog d = MyAlertDialog.newInstance(getActivity());
		        	d.setIcon(R.drawable.ic_launcher);
		        	d.setMessage( getString(R.string.qa_confirm_delete_my_tweet) );
		        	d.setButton( AlertDialog.BUTTON_POSITIVE, getString(R.string.qa_do_delete), new DialogInterface.OnClickListener() {
		        	    public void onClick(DialogInterface dialog, int whichButton) {
				        	deleteMyTweet(clicked_tweet);
		        	    }
		        	});
		        	d.setButton( AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
		        	    public void onClick(DialogInterface dialog, int whichButton) {
		        	        // Nothing to do
		        	    }
		        	});
		        	d.show();
		        	qa.dismiss();
				}
			});
			qa.addActionItem(item);
		}
		
		// 地図を開く
		if( clicked_tweet.getGeoLocation()!=null ){
			item = new ActionItem();
			item.setIcon( getResources().getDrawable(android.R.drawable.ic_menu_mapmode) );
			item.setTitle( getString(R.string.qa_open_googlemap) );
			item.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					// geo:latitude,longitude?z=zoom
					double lat = clicked_tweet.getGeoLocation().getLatitude();
					double lng = clicked_tweet.getGeoLocation().getLongitude();
					intent.setData( Uri.parse("geo:0,0?q="+lat+","+lng+"&z=16") );
					startActivity(intent);
		        	qa.dismiss();
				}
			});
			qa.addActionItem(item);
		}

		// プロフィール
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_profile) );
		item.setTitle( getString(R.string.profile) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Status st = clicked_tweet;
	        	if( st.isRetweet() ){
	        		st = st.getRetweetedStatus();
	        	}
	        	Intent intent = new Intent(getActivity(), UserProfileActivity.class);
	        	intent.putExtra("user", st.getUser());
	        	startActivity(intent);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

		qa.show();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// ツイートを選択する
		position--;
		if( position==mTweets.size() ){
			return true;
		}

		Status clicked_tweet = mTweets.get(position);
    	if( clicked_tweet.isRetweet() ){
    		clicked_tweet = clicked_tweet.getRetweetedStatus();
    	}

		Status b = mSelectedStatus.get( clicked_tweet.getId() );
		if( b!=null ){
			mSelectedStatus.remove( clicked_tweet.getId() );
		}else{
			mSelectedStatus.put(clicked_tweet.getId(), clicked_tweet);
		}
		mAdapter.notifyDataSetChanged();
		
		int n = mSelectedStatus.keySet().size();

		// 全員に返信する
		ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		        menu.add(Menu.NONE, Consts.MENU_ID_NEW_TWEET, Menu.NONE, "Reply All" )
//		        	.setIcon(R.drawable.ic_reply)
		        	.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				return true;
			}
			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}
			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch( item.getItemId() ){
				case Consts.MENU_ID_NEW_TWEET:
		        	Intent intent = new Intent(getActivity(), CreateNewTweetActivity.class);
		        	String str = "";
					Status st;
	                Long o;
	                Iterator<Long> it = mSelectedStatus.keySet().iterator();
	                while( it.hasNext() ){
	                    o = it.next();
	                    st = mSelectedStatus.get( o );
	                    str += "@"+st.getUser().getScreenName()+" ";
	                }
		        	intent.putExtra("text", str);
		    		intent.putExtra("move_last",true);
		        	startActivity(intent);
					return true;
				}
				return false;
			}
			@Override
			public void onDestroyActionMode(ActionMode mode) {
				Log.d(TAG,"Clear Selection");
				mActionMode = null;
				mSelectedStatus.clear();
				mAdapter.notifyDataSetChanged();
			}
		};
		if( n>0 ){
			if( mActionMode==null ){
				mActionMode = getActivity().startActionMode(mActionModeCallback);
			}
			String str = String.format( getString(R.string.n_selected), n);
			mActionMode.setTitle( str );
		}else{
			mActionMode.finish();
		}
		return true;
	}

	public void showTalkDialog(final Status st){
	    // DialogFragment.show() will take care of adding the fragment
	    // in a transaction.  We also want to remove any currently showing
	    // dialog, so make our own transaction and take care of that here.
	    FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
	    Fragment prev = getActivity().getFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DialogFragment newFragment = TalkDialogFragment.newInstance( st );
	    newFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme_Light);
	    newFragment.show(ft, "dialog");

	}
	
    public void retweet(final Status st){
    	mInterface.startProgressBar();
    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
					final Status newst = mInterface.getTwitter().retweetStatus(st.getId());
					getActivity().runOnUiThread( new Runnable() {
						@Override
						public void run() {
							replaceTweet(st, newst);
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format( getString(R.string.error_retweet), e.getStatusCode());
					mInterface.showToastFromThread( str, Toast.LENGTH_SHORT );

				} finally {
					getActivity().runOnUiThread( new Runnable(){
						@Override
						public void run() {
							mInterface.stopProgressBar();
						}} );
				}
			}} );
    	th.start();
    }

    public void addToFavorite(Status st0){
    	if( st0.isRetweet() ){
    		st0 = st0.getRetweetedStatus();
    	}
    	final Status st = st0;

    	Log.d(TAG,"Adding to favorite...");
    	mInterface.startProgressBar();
    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
		        	final Status newst = mInterface.getTwitter().createFavorite(st.getId());
		        	Log.d(TAG,(newst.isFavorited()?"Faved":"Non-faved"));
		        	getActivity().runOnUiThread( new Runnable() {
						@Override
						public void run() {
							replaceTweet(st, newst);
							// TODO お気に入り追加したときにお気に入りフラグが立たないので仮対処
							FavoriteStatusWrapper.setFavorite(newst, true);
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_favorite), e.getStatusCode());
			    	mInterface.showToastFromThread(errmsg, Toast.LENGTH_SHORT);

				} finally {
					getActivity().runOnUiThread( new Runnable(){
						@Override
						public void run() {
							mInterface.stopProgressBar();
						}} );
				}
			}} );
    	th.start();
    }

    public void deleteFromFavorite(Status st0){
    	if( st0.isRetweet() ){
    		st0 = st0.getRetweetedStatus();
    	}
    	final Status st = st0;

    	Log.d(TAG,"Deleting favorite...");
    	mInterface.startProgressBar();
    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
		        	final Status newst = mInterface.getTwitter().destroyFavorite(st.getId());
		        	getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							replaceTweet(st, newst);
							// TODO お気に入り追加したときにお気に入りフラグが立たないので仮対処
							if( newst.isFavorited()==false ){
								FavoriteStatusWrapper.setFavorite(newst, false);
							}
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_unfavorite), e.getStatusCode());
			    	mInterface.showToastFromThread(errmsg,Toast.LENGTH_SHORT);

				} finally {
					getActivity().runOnUiThread( new Runnable(){
						@Override
						public void run() {
							mInterface.stopProgressBar();
						}} );
				}
			}} );
    	th.start();
    }

	public void deleteMyTweet(final Status st) {
		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.qa_now_deleting_my_tweet) );
		dialog.setCancelable(false);
		FragmentManager manager = getActivity().getFragmentManager();
		dialog.show(manager, "delete-tweet");

    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
		    		mInterface.getTwitter().destroyStatus( st.getId() );
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							for(int i=0;i<mTweets.size();i++){
								if( mTweets.get(i).getId()==st.getId() ){
									mTweets.remove(i);
									break;
								}
							}
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_to_del_tweet), e.getStatusCode());
			    	mInterface.showToastFromThread(errmsg, Toast.LENGTH_SHORT );

				} finally {
					getActivity().runOnUiThread( new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}
					});
				}
			}} );
    	th.start();
	}

    private void replaceTweet(Status oldtweet, Status newtweet){
    	for(int i=0; i<mTweets.size(); i++){
    		Status s = mTweets.get(i);
    		if( s.getId()==oldtweet.getId() ){
    			Log.d(TAG,"Tweet replaced.");
    			mTweets.set(i,  newtweet);
    			return;
    		}
    	}
    }

	/**
	 * Tweetを取得する.
	 * 継承先で実装すること
	 * @param p
	 * @return
	 * @throws TwitterException
	 */
	protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
		return null;
	}

	public void startRefreshMenuAnimation() {
		/* Attach a rotating ImageView to the refresh item as an ActionView */
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
		
		// FIXME 仮対処
		if( mRefreshMenuItem!=null ){
			Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
			rotation.setRepeatCount(Animation.INFINITE);
			iv.startAnimation(rotation);
			mRefreshMenuItem.setActionView(iv);
		}
 	}

	public void completeRefreshMenuAnimation() {
		// FIXME 仮対処
		if( mRefreshMenuItem!=null && mRefreshMenuItem.getActionView()!=null ){
			mRefreshMenuItem.getActionView().clearAnimation();
			mRefreshMenuItem.setActionView(null);
		}
	}

	protected void refreshTimeline(){
		if( mIsRunning ) return;

		mIsRunning  = true;
		Log.d(TAG,"Updating Timeline...");

		// TODO DL中の見せ方を考える
		//startRefreshMenuAnimation();
		mInterface.startProgressBar();
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				final ArrayList<Status> tmparray = new ArrayList<Status>();
	    		ResponseList<Status> st;
				long maxId = 0;
	    		Paging p = new Paging();
				if( mTweets.size()!=0 ){
					maxId = mTweets.get(0).getId();
					p.setSinceId( maxId );
				}
	    		p.setCount( Prefs.sNumOfTweets );
				try {
					st = getTweets(p);

					for(int i=0; i<st.size(); i++){
						Status tmp = st.get(i);
						tmparray.add(tmp);
						if( tmp.isRetweet() ) tmp = tmp.getRetweetedStatus();
						TweetnyanMainActivity.sIconCacheThread.requestDownload( tmp.getUser().getProfileImageURL(), tmp.getUser().getId() );
					}

					getActivity().runOnUiThread( new Runnable() {
						@Override
						public void run() {
							mPullToRefreshListView.onRefreshComplete(); // ヘッダ操作あたりが絡んできて先に呼んでおかないとダメ

							// 0: no-move 1:top-of-readed 2:top
					    	int now_pos = 0;
					    	int offset = 0;
					    	now_pos = mListView.getFirstVisiblePosition();
					    	Log.d(TAG,"moving scroll position... now getFirstVisiblePosition() is :"+ now_pos);
					    	if( mListView.getChildAt(0)!=null ){
					    		offset = mListView.getChildAt(0).getTop();
					    	}
							mTweets.addAll(0, tmparray);
			        		mAdapter.notifyDataSetChanged();

			        		int nomove_pos = now_pos + tmparray.size();
							int readed_pos = 0; // FIXME 既読位置の処理
					    	if( now_pos==0 ){
					    		// ヘッダが表示されているなら、移動しないポジションは未読の先頭.
					    		nomove_pos = Math.max( tmparray.size(), 1);
					    		offset = -1;
					    	}
							changeViewPointofListView(mListView, readed_pos, nomove_pos, offset);
						}
					});

				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format( getString(R.string.error_get_tweets), e.getStatusCode());
					mInterface.showToastFromThread(str, Toast.LENGTH_SHORT);

				} finally {
		        	mIsRunning = false;
				}

				getActivity().runOnUiThread( new Runnable() {
					@Override
					public void run() {
						Log.d(TAG,"onRefreshComplete");
						mPullToRefreshListView.onRefreshComplete();
						// TODO DL中の見せ方を考える
						//completeRefreshMenuAnimation();
						mInterface.stopProgressBar();
					}
				});
			}
		});
		th.start();
	}

	public void loadMore(){
		if( mIsRunning ) return;

		mIsRunning  = true;
		Log.d(TAG,"Load more tweets...");

		//startRefreshMenuAnimation();
		mInterface.startProgressBar();
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				final ArrayList<Status> tmparray = new ArrayList<Status>();
	    		ResponseList<Status> st;
	    		long maxId = 0;
	    		if( mTweets.size()!=0 ){
	    			maxId = mTweets.get( mTweets.size()-1 ).getId() - 1;
	    		}
	    		Paging p = new Paging();
	    		p.setCount( Prefs.sNumOfTweets );
	    		if( maxId>0) p.setMaxId(maxId);
				try {
					st = getTweets(p);

					for(int i=0; i<st.size(); i++){
						Status tmp = st.get(i);
						tmparray.add(tmp);
						if( tmp.isRetweet() ) tmp = tmp.getRetweetedStatus();
						TweetnyanMainActivity.sIconCacheThread.requestDownload( tmp.getUser().getProfileImageURL(), tmp.getUser().getId() );
					}

					mListView.post( new Runnable() {						
						@Override
						public void run() {
							Log.d(TAG,"loaded "+tmparray.size()+" tweets.");
							mTweets.addAll(tmparray);
							int num_of_tweets = mTweets.size();
							if( num_of_tweets > Consts.sMaxTweets ){
								for( int i=2000; i<num_of_tweets; i++){
									mTweets.remove(Consts.sMaxTweets);
								}
							}
			        		mAdapter.notifyDataSetChanged();
						}
					});

				} catch (TwitterException e) {
					e.printStackTrace();
					String str = String.format( getString(R.string.error_get_tweets), e.getStatusCode());
					mInterface.showToastFromThread(str, Toast.LENGTH_SHORT);

				} finally {
		        	mIsRunning = false;
				}

				getActivity().runOnUiThread( new Runnable() {
					@Override
					public void run() {
						mPullToRefreshListView.onRefreshComplete();
						//completeRefreshMenuAnimation();
						mInterface.stopProgressBar();
					}
				});
			}
		});
		th.start();
	}

	/**
	 * 指定の位置まで移動する.
	 * @param lv
	 * @param top_of_readed
	 * @param move_none
	 * @param offset
	 */
    private void changeViewPointofListView(final ListView lv, int top_of_readed, int move_none, int offset){
    	Log.d(TAG,"changeViewPointofListView("+Prefs.sDestinationOnUpdate+"):"+top_of_readed+"/"+move_none+"/"+offset);
    	switch( Prefs.sDestinationOnUpdate ){
    	case Consts.MOVE_NONE:
        	if( move_none>=0 ){
        		if( top_of_readed<0 ){
        			Log.d(TAG, "No readed item.");
        			move_none = 1; // 既読がない状態
        			offset = -1;
        			lv.post( new Runnable() {
						@Override
						public void run() {
							lv.setSelectionFromTop(1, -1);
						}
					});
        		}else{
        			lv.setSelectionFromTop(move_none, offset);
        		}
        	}
    		break;
    	case Consts.MOVE_TOP_OF_READED:
    		// 既読の先頭まで
        	if( top_of_readed>=0 ){
        		lv.setSelectionFromTop(top_of_readed, -1);
        	}else{
        		// 既読がない状態のときはとりあえず先頭にもってきておく
    			Log.d(TAG, "No readed item.");
    			lv.post( new Runnable() {
					@Override
					public void run() {
						lv.setSelectionFromTop(1, -1);
					}
				});
        	}
    		break;
    	case Consts.MOVE_TOP:
			lv.post( new Runnable() {
				@Override
				public void run() {
					lv.setSelectionFromTop(1, -1);
				}
			});
    		break;
    	}
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, Consts.MENU_ID_NEW_TWEET, Menu.NONE, getString(R.string.new_tweet) ).setIcon(R.drawable.ic_menu_tweet).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	mRefreshMenuItem = menu.add(Menu.NONE, Consts.MENU_ID_REFRESH_TIMELINE, Menu.NONE, getString(R.string.refresh) );
    	mRefreshMenuItem.setIcon(R.drawable.ic_refresh_timeline);
    	mRefreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG,"Menu selected:"+item.getItemId());
		Intent intent;
        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);

        case Consts.MENU_ID_NEW_TWEET:
        	// New Tweet
        	intent = new Intent( getActivity(), CreateNewTweetActivity.class );
        	startActivity(intent);
            break;
        case Consts.MENU_ID_REFRESH_TIMELINE:
        	refreshTimeline();
            break;
        }
        return true;
	}

	@Override
	public boolean isSelected(Long id) {
		return mSelectedStatus.containsKey(id);
	}

	@Override
	public void onIconDownloaded(final long id) {
		if( mAdapter!=null ){
			getActivity().runOnUiThread( new Runnable() {
				@Override
				public void run() {
					// アイコン表示していない場合は通知不要なのでチェック
					// 前後+1の範囲に該当ユーザーがいれば通知する感じでいいかな
					int first_pos = mListView.getFirstVisiblePosition();
					int last_pos = mListView.getLastVisiblePosition();
					Log.d(TAG,""+getClass().getName()+" = "+first_pos+"-"+last_pos);

					first_pos--;
					last_pos++;
					if( first_pos<0 ) first_pos=0;
					if( last_pos >= mTweets.size() ) last_pos = mTweets.size()-1;

					boolean doNotify = false;
					for(int i=first_pos; i<=last_pos; i++ ){
						if( mTweets.get(i).getUser().getId()==id ){
							doNotify = true;
							break;
						}
					}
					if( doNotify ){
						Log.d(TAG,"Icon Updated.");
						mAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	}

}
