package jp.miku39.android.tweetnyan.fragments;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import android.os.Bundle;
import android.util.Log;

public class MentionTimelineFragment extends BasicTimelineFragment {
	final static String TAG = "MentionTimelineFragment";
	public final static String sSaveFileName = "mention.tweets";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		if( mTweets.size()==0 ){
			refreshTimeline();
		}
	}

	@Override
	protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
		Log.d(TAG,"Get Mentions");
		return mInterface.getTwitter().getMentionsTimeline(p);
	}

}
