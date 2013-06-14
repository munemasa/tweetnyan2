package jp.miku39.android.tweetnyan.activities;


import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.BasicTimelineFragment.BasicTimelineInterface;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public class UserFavoritesActivity extends ActivityWithTheme implements BasicTimelineInterface {
	User mTargetUser;
	String mTargetName;
	private Twitter mTwitter;

	public class FavoritesFragment extends BasicTimelineFragment {

		@Override
		public void onStart() {
			super.onStart();
			if( mTweets.size()==0 ){
				refreshTimeline();
			}
		}

		protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
			ResponseList<Status> st = mTwitter.getFavorites(mTargetUser.getScreenName(), p);
//			ResponseList<Status> st = mTwitter.getFavorites(mTargetUser.getScreenName());
			return st;
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.user_favorites_layout);

    	mTwitter = Lib.createTwitter(this);

    	mTargetUser = (User) getIntent().getSerializableExtra("targetuser");
    	mTargetName = getIntent().getStringExtra("targetname");

	    ActionBar bar = getSupportActionBar();
	    String str = String.format(getString(R.string.subtitle_user_favorites), mTargetUser.getScreenName());
	    bar.setSubtitle(str);

	    initViews();
	}

	void initViews(){
	    FragmentManager manager = getSupportFragmentManager();
	    FragmentTransaction transaction = manager.beginTransaction();
	    Fragment fragment = new FavoritesFragment();
	    transaction.add(R.id.user_favorites_frame_root, fragment, "favorites");
	    transaction.commit();
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
		    	Toast.makeText(UserFavoritesActivity.this, text, duration).show();
			}
		});
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
        }
        return true;
	}
}
