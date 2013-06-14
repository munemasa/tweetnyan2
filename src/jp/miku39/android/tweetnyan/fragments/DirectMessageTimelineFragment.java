package jp.miku39.android.tweetnyan.fragments;

// FIXME load moreを実装する

import java.util.ArrayList;
import java.util.Collections;

import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.lib.MyAlertDialog;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.IconCacheThread;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.activities.CreateDirectMessageActivity;
import jp.miku39.android.tweetnyan.activities.DirectMessageListViewAdapter;
import jp.miku39.android.tweetnyan.activities.TweetnyanMainActivity;
import jp.miku39.android.tweetnyan.activities.UserProfileActivity;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment.BasicTimelineInterface;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.TwitterException;
import yanzm.products.quickaction.lib.ActionItem;
import yanzm.products.quickaction.lib.QuickAction;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class DirectMessageTimelineFragment extends Fragment implements OnItemClickListener, IconCacheThread.IIconDownloadedCallback {
	final static String TAG = "DirectMessageTimelineFragment";
	public final static String sSaveFileName = "directmessage";

	private PullToRefreshListView mPullToRefreshListView;
	private ListView mListView;
    private ArrayList<DirectMessage> mItems = new ArrayList<DirectMessage>();

	private BasicTimelineInterface mInterface;
	private DirectMessageListViewAdapter mAdapter;

	private volatile boolean mIsRunning = false;	// ツイート取得処理が走行中
	private MenuItem mRefreshMenuItem;

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG,"onAttach");
		super.onAttach(activity);
		mInterface = (BasicTimelineInterface) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.basic_timeline_layout, container, false);
        mPullToRefreshListView = (PullToRefreshListView) v.findViewById(R.id.basic_timeline_listview);
        mListView = mPullToRefreshListView.getRefreshableView();
        mListView.setOnItemClickListener(this);
        mPullToRefreshListView.setOnRefreshListener( new com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshDirectMessageTimeline();
			}
		});

        mAdapter = new DirectMessageListViewAdapter(getActivity(), R.layout.one_directmessage_layout, mItems);
        mListView.setAdapter(mAdapter);
		return v;
	}

	@Override
	public void onDestroyView() {
		Log.d(TAG,"onDestroyView");
		super.onDestroyView();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		position--;
    	final DirectMessage dm = mItems.get(position);

    	final QuickAction qa = new QuickAction(view);
		ActionItem item;

		// 返信
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_reply) );
		item.setTitle( getString(R.string.qa_do_reply) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent;
		    	intent = new Intent(getActivity(), CreateDirectMessageActivity.class);
		    	Log.d(TAG,"DM Reply-To: "+dm.getSenderScreenName());
		    	intent.putExtra("to", dm.getSenderScreenName());
		    	startActivity(intent);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

		// 削除
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(android.R.drawable.ic_menu_delete) );
		item.setTitle( getString(R.string.qa_do_delete) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MyAlertDialog d = MyAlertDialog.newInstance(getActivity());
	        	d.setIcon(R.drawable.ic_launcher);
	        	d.setMessage( getString(R.string.qa_confirm_delete_directmessage) );
	        	d.setButton( AlertDialog.BUTTON_POSITIVE, getString(R.string.qa_do_delete), new DialogInterface.OnClickListener() {
	        	    public void onClick(DialogInterface dialog, int whichButton) {
			        	deleteDirectMessage( dm );
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

		// プロフィール
		item = new ActionItem();
		item.setIcon( getResources().getDrawable(R.drawable.ic_profile) );
		item.setTitle( getString(R.string.profile) );
		item.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	        	Intent intent = new Intent(getActivity(), UserProfileActivity.class);
	        	intent.putExtra("user", dm.getSender());
	        	startActivity(intent);
	        	qa.dismiss();
			}
		});
		qa.addActionItem(item);

//		qa.setLayoutStyle(QuickAction.STYLE_LIST);
		qa.show();
	}
	
	void deleteDirectMessage( final DirectMessage dm ){
		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.qa_now_deleting_directmessage) );
		dialog.setCancelable(false);
		FragmentManager manager = getActivity().getFragmentManager();
		dialog.show(manager, "delete-dm");

    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
		    	try {
					mInterface.getTwitter().destroyDirectMessage(dm.getId());
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mItems.remove(dm);
							mAdapter.notifyDataSetChanged();
						};
					});
				} catch (TwitterException e) {
					e.printStackTrace();
			    	String errmsg = String.format( getString(R.string.error_to_del_directmessage), e.getStatusCode());
					mInterface.showToastFromThread(errmsg, Toast.LENGTH_SHORT);

				} finally {
					getActivity().runOnUiThread( new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}} );
				}
			}} );
    	th.start();		
	}

	private void refreshDirectMessageTimeline(){
		if( mIsRunning ) return;
		mIsRunning = true;
		Log.d(TAG,"Updating DirectMessage Timeline...");

		// TODO DL中の見せ方を考える
		//startRefreshMenuAnimation();
		mInterface.startProgressBar();
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
	    		final ArrayList<DirectMessage> tmparray = new ArrayList<DirectMessage>();
		    	try {
		    		Paging p = new Paging();
	    			long maxId = 0;
	    			long myid = Lib.getCurrentAccountUserId(getActivity());
	    			ArrayList<DirectMessage> dmlist = mItems;

	    			// 受け取ったDMの取得
	    			if( dmlist.size()!=0 ){
	    				for( int i=0; i<dmlist.size(); i++ ){
	    					if( dmlist.get(i).getSenderId()!=myid ){
	    	    				maxId = dmlist.get(i).getId();
	    	    				p.setSinceId(maxId);
	    	    				break;
	    					}
	    				}
	    			}
	    			// FIXME DM取得数
		    		p.setCount( Consts.sNumOfTweets );
	    			ResponseList<DirectMessage> dm = mInterface.getTwitter().getDirectMessages(p);
					for(int i=0; i<dm.size(); i++){
						DirectMessage tmp = dm.get(i);
						if( tmp.getId() > maxId ){
							tmparray.add(tmp);
							TweetnyanMainActivity.sIconCacheThread.requestDownload( tmp.getSender().getProfileImageURL(), tmp.getSender().getId() );
						}
					}
					
					// 送ったDMの取得
					p = new Paging();
	    			// FIXME DM取得数
		    		p.setCount( Consts.sNumOfTweets );
	    			if( dmlist.size()!=0 ){
	    				for( int i=0; i<dmlist.size(); i++ ){
	    					if( dmlist.get(i).getSenderId()==myid ){
	    	    				maxId = dmlist.get(i).getId();
	    	    				p.setSinceId(maxId);
	    	    				break;
	    					}
	    				}
	    			}
					dm = mInterface.getTwitter().getSentDirectMessages(p);
					for(int i=0; i<dm.size(); i++){
						DirectMessage tmp = dm.get(i);
						if( tmp.getId() > maxId ){
							tmparray.add(tmp);
							TweetnyanMainActivity.sIconCacheThread.requestDownload( tmp.getSender().getProfileImageURL(), tmp.getSender().getId() );
						}
					}
					
					mListView.post( new Runnable() {
						@Override
						public void run() {
							mPullToRefreshListView.onRefreshComplete(); // ヘッダ操作あたりが絡んできて先に呼んでおかないとダメ

							// 並べ替え
							class DMComparator implements java.util.Comparator<DirectMessage> {
								@Override
								public int compare(DirectMessage arg0, DirectMessage arg1) {
									return (int) (arg1.getCreatedAt().getTime()/1000 - arg0.getCreatedAt().getTime()/1000);
								}
							}
					    	int now_pos = 0;
					    	int offset = 0;
					    	now_pos = mListView.getFirstVisiblePosition();
					    	if( mListView.getChildAt(0)!=null ){
					    		offset = mListView.getChildAt(0).getTop();
					    	}
							mItems.addAll(tmparray);
							Collections.sort( mItems, new DMComparator() );
							int nomove_pos = now_pos + tmparray.size();
				        	if( nomove_pos>=0 ){
				        		mListView.setSelectionFromTop(nomove_pos, offset);
				        	}							
						}
					});

		    	} catch (TwitterException e) {
					e.printStackTrace();

				}finally{
					mIsRunning = false;
				}

				getActivity().runOnUiThread( new Runnable() {
					@Override
					public void run() {
						// TODO DL中の見せ方を考える
						//completeRefreshMenuAnimation();
						mInterface.stopProgressBar();
						mPullToRefreshListView.onRefreshComplete();
					}
				});
			}
		});
		th.start();
	}

	public void startRefreshMenuAnimation() {
		/* Attach a rotating ImageView to the refresh item as an ActionView */
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
		
		Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
		rotation.setRepeatCount(Animation.INFINITE);
		iv.startAnimation(rotation);
		mRefreshMenuItem.setActionView(iv);
 	}

	public void completeRefreshMenuAnimation() {
		mRefreshMenuItem.getActionView().clearAnimation();
		mRefreshMenuItem.setActionView(null);
	}

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, Consts.MENU_ID_NEW_TWEET, Menu.NONE, getString(R.string.new_tweet) ).setIcon(R.drawable.ic_new_directmessage).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
        	// New DirectMessage
        	intent = new Intent( getActivity(), CreateDirectMessageActivity.class );
        	startActivity(intent);
            break;
        case Consts.MENU_ID_REFRESH_TIMELINE:
        	refreshDirectMessageTimeline();
            break;
        }
        return true;
	}

	@Override
	public void onIconDownloaded(long id) {
		if( mAdapter!=null ){
			getActivity().runOnUiThread( new Runnable() {
				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();			
				}
			});
		}
	}

}
