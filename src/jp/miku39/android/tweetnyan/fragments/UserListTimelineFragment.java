package jp.miku39.android.tweetnyan.fragments;

import java.util.ArrayList;

import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.R.anim;
import jp.miku39.android.tweetnyan.R.id;
import jp.miku39.android.tweetnyan.R.layout;
import jp.miku39.android.tweetnyan.activities.StatusListViewAdapter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.UserList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class UserListTimelineFragment extends BasicTimelineFragment {
	final static String TAG = "ListTimelineFragment";
	public final static String sSaveFileName = "userlist.tweets";
	public final static String sSaveFileNameUserList = "userlist";

	private ArrayAdapter<String> mUserListSpinnerAdapter;
	private ArrayList<UserList> mUserList = new ArrayList<UserList>();

	private Spinner mSpinner;
	protected int mSpinnerPosition;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ArrayList<?> tmp = (ArrayList<Status>) Lib.readUserObject( getActivity(), sSaveFileNameUserList);
		if( tmp!=null ){
			Log.d(TAG,"Load UserList");
			mUserList = (ArrayList<UserList>) tmp;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// リストタブはリストを選択するスピナーがあるレイアウトなので、オーバーライドして別途。
		// あまり違いがないので整理の余地あり。
        View v = inflater.inflate(R.layout.userlist_fragment_layout, container, false);
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

		mSpinner = (Spinner)v.findViewById(R.id.select_list_spinner);

        initUserListSpinnerData();

        mAdapter = new StatusListViewAdapter(getActivity(), R.layout.one_tweet_layout, mTweets ,this);
        mListView.setAdapter(mAdapter);

        Button btn = (Button)v.findViewById(R.id.btn_list_update);
        btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loadUserList();
			}
		});
		return v;
	}

	/**
     * sTwitterService.mUserListの中身に合わせてリストのスピナーを初期化する.
     */
    void initUserListSpinnerData(){
    	mUserListSpinnerAdapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_spinner_item );
		mUserListSpinnerAdapter.add("");
    	mUserListSpinnerAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );

		for( int i=0; i<mUserList.size(); i++ ){
    		UserList l = mUserList.get(i);
    		String str;

    		str = l.getFullName();
    		if( !l.isPublic() ){
    			str = "[private] "+str;
    		}
    		mUserListSpinnerAdapter.add( str );
    	}

		mSpinner.setAdapter(mUserListSpinnerAdapter);
    	// 選択するとTLを読む
    	mSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				Log.d(TAG,"Select Spinner #"+pos);
				if( pos>0 ){
					mTweets.clear();
					mAdapter.notifyDataSetChanged();
					mSpinnerPosition = pos;
					refreshTimeline();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}});
    }
    
    /**
     * Twitterのリストの全一覧を取得して、スピナーを更新する
     */
    private void loadUserList(){
    	Log.d(TAG,"Retrieve UserList");
        final Button btn = (Button)(getView().findViewById(R.id.btn_list_update));
        btn.startAnimation( AnimationUtils.loadAnimation(getActivity(), R.anim.rotate) );

    	Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
				long id = 0;
		    	try {
					id = Lib.getCurrentAccountUserId(getActivity());
			    	final ResponseList<UserList> lists = null;//mInterface.getTwitter().getAllUserLists( id );

			    	getView().post( new Runnable() {						
						@Override
						public void run() {
					    	mUserList.clear();
					    	mUserList.addAll( lists );
							Lib.writeUserObject( getActivity(), sSaveFileNameUserList, mUserList);
							Log.d(TAG,"Save UserList");
					    	initUserListSpinnerData();
						}
					});

		    	} finally {
					btn.post( new Runnable() {
						@Override
						public void run() {
							btn.clearAnimation();
							btn.setAnimation(null);
						}
					});
				}
			}} );
    	th.start();
	}

	protected void refreshTimeline(){
		int pos = mSpinner.getSelectedItemPosition();
		if( pos==0 ) return;
		super.refreshTimeline();
	}

	@Override
	protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
		Log.d(TAG,"Get UserList Timeline");
		int pos = mSpinner.getSelectedItemPosition();
		long id = 0;
		if( pos>0 ){
			pos--;
			id = mUserList.get(pos).getId();
		}
		return mInterface.getTwitter().getUserListStatuses(id, p);
	}
}
