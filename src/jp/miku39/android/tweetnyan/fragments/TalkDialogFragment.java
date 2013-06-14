package jp.miku39.android.tweetnyan.fragments;

import java.util.ArrayList;

import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.lib.MyAlertDialog;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.activities.CheckTweetSelectedInterface;
import jp.miku39.android.tweetnyan.activities.CreateNewTweetActivity;
import jp.miku39.android.tweetnyan.activities.StatusListViewAdapter;
import jp.miku39.android.tweetnyan.activities.TweetnyanMainActivity;
import jp.miku39.android.tweetnyan.activities.UserProfileActivity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import yanzm.products.quickaction.lib.ActionItem;
import yanzm.products.quickaction.lib.QuickAction;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;


public class TalkDialogFragment extends DialogFragment implements CheckTweetSelectedInterface, OnItemClickListener {
	final static String TAG = "TalkDialogFragment";
	Status mTweet;
	ListView mListView;
	ArrayList<Status> mTweets = new ArrayList<Status>();
	StatusListViewAdapter mAdapter;
	
	volatile boolean mFinished = false;

	public static TalkDialogFragment newInstance(Status msg){
		TalkDialogFragment dialog = new TalkDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable( "tweet", msg );
		dialog.setArguments(args);
		return dialog;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        mTweet = (Status) getArguments().getSerializable("tweet");
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        MyAlertDialog myDialog = new MyAlertDialog( getActivity(), R.style.MyDialogTheme_Light );
		myDialog.setIcon(R.drawable.ic_launcher);
		myDialog.setCancelable(true);
		mListView = new ListView( getActivity() );
		mTweets.add( mTweet );
        mAdapter = new StatusListViewAdapter( getActivity(), R.layout.one_tweet_layout, mTweets ,this);
		mListView.setAdapter( mAdapter );
		mListView.setOnItemClickListener( this );
		myDialog.setView( mListView );
		return myDialog;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mFinished = true;
	}

	void getInReplyToTweets(){
		if( mTweets.size()>1 ) return;

		final Twitter twitter = Lib.getTwitter();

		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				Status st = mTweet;
				CommonUtils.sleep(300);

				while( st!=null && st.getInReplyToStatusId()>0 ){
		    		if( mFinished ) break;

		        	Status rep = TweetnyanMainActivity.sTwitterService.loadStatusFromCache( st.getInReplyToStatusId() );
		        	if( rep!=null ){
	            		if( getActivity()!=null ){
	            			final Status tmp = rep;
		    		    	getActivity().runOnUiThread( new Runnable(){
		    					@Override
		    					public void run() {
				            		mTweets.add(tmp);
				            		mAdapter.notifyDataSetChanged();
		    					}} );
	            		}
		        		st = rep;
		        	}else{
		        		try {
							rep = twitter.showStatus( st.getInReplyToStatusId() );
			            	if( rep!=null ){
			            		TweetnyanMainActivity.sTwitterService.storeStatusToCache( rep );
			            		st = rep;
			            		final Status tmp = rep;
			            		if( getActivity()!=null ){
				    		    	getActivity().runOnUiThread( new Runnable(){
				    					@Override
				    					public void run() {
						            		mTweets.add(tmp);
						            		mAdapter.notifyDataSetChanged();
				    					}} );
			            		}
			    		    	CommonUtils.sleep(200);
			            	}else break;
						} catch (TwitterException e) {
							e.printStackTrace();
							break;
						}
		        	}
		    	}
		    	Log.d(TAG,"Thread finished.");
			}
		});

    	th.start();
	}

	@Override
	public void onStart() {
		Log.d(TAG,"onStart");
		super.onStart();

		getInReplyToTweets();
	}

	@Override
	public boolean isSelected(Long id) {
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// BasicTimelineFragmentと同じなので、整理したいところ.
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
		if( !clicked_tweet.isFavorited() ){
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

		/*
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
		*/

		/*
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
		*/

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

	public void showToast(String text, int duration){
		Toast.makeText( getActivity(), text, duration).show();
	}

	public void showToastFromThread(final String text){
		showToastFromThread(text, Toast.LENGTH_SHORT);
	}
	public void showToastFromThread(final String text, final int duration){
		if( getActivity()==null ) return;
		getActivity().runOnUiThread( new Runnable() {
			@Override
			public void run() {
				showToast(text,duration);
			}
		});
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

    public void retweet(final Status st){
    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
					final Status newst = Lib.getTwitter().retweetStatus(st.getId());
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
					showToastFromThread( str, Toast.LENGTH_SHORT );
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
    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
		        	final Status newst = Lib.getTwitter().createFavorite(st.getId());
		        	Log.d(TAG,(newst.isFavorited()?"Faved":"Non-faved"));
		        	getActivity().runOnUiThread( new Runnable() {
						@Override
						public void run() {
							replaceTweet(st, newst);
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_favorite), e.getStatusCode());
			    	showToastFromThread(errmsg, Toast.LENGTH_SHORT);
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
    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
		        	final Status newst = Lib.getTwitter().destroyFavorite(st.getId());
		        	getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							replaceTweet(st, newst);
							mAdapter.notifyDataSetChanged();
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_unfavorite), e.getStatusCode());
			    	showToastFromThread(errmsg,Toast.LENGTH_SHORT);

				}
			}} );
    	th.start();
    }

}
