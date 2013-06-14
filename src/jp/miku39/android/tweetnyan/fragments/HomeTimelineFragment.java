package jp.miku39.android.tweetnyan.fragments;

import java.util.ArrayList;

import jp.miku39.android.tweetnyan.Lib;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import android.os.Bundle;
import android.util.Log;

public class HomeTimelineFragment extends BasicTimelineFragment {
	final static String TAG = "HomeTimelineFragment";
	public final static String sSaveFileName = "home.tweets";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		// FIXME テストのため一時的に無効
//		ArrayList<Status> tmp = (ArrayList<Status>) Lib.readUserObject(getActivity(), sSaveFileName);
//		if( tmp!=null ){
//			Log.d(TAG,"Load Home Timeline from storage.");
//			mTweets = tmp;
//		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if( mTweets.size()==0 ){
			refreshTimeline();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Lib.writeUserObject(getActivity(), sSaveFileName, mTweets);
		Log.d(TAG,"Save Home Timeline to storage.");
	}

	@Override
	protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
		Log.d(TAG,"Get Home Timeline");
		return mInterface.getTwitter().getHomeTimeline(p);
	}
}
