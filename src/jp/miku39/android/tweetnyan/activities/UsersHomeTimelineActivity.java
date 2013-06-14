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
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UsersHomeTimelineActivity extends ActivityWithTheme implements BasicTimelineInterface {
	User mTargetUser;
	String mTargetName;
	private Twitter mTwitter;

	@SuppressLint("ValidFragment")
	public class UserHomeTimelineFragment extends BasicTimelineFragment {
		@Override
		public void onStart() {
			super.onStart();
			if( mTweets.size()==0 ){
				refreshTimeline();
			}
		}

		@Override
		protected ResponseList<Status> getTweets(Paging p) throws TwitterException {
			ResponseList<Status> st = mTwitter.getUserTimeline(mTargetUser.getId(), p);
			return st;
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.user_home_timeline_layout);

    	mTwitter = Lib.getTwitter();

    	mTargetUser = (User) getIntent().getSerializableExtra("targetuser");
    	mTargetName = getIntent().getStringExtra("targetname");

    	initViews();
	}

	void initViews(){
		ImageView imgview;
		Bitmap icon = TweetnyanMainActivity.sIconCacheThread.loadIcon(mTargetUser.getProfileImageURL(), mTargetUser.getId());
		if( icon!=null ){
			imgview = (ImageView)findViewById(R.id.profile_icon);
			imgview.setImageBitmap(icon);
		}

    	TextView tv;
    	tv = (TextView)findViewById(R.id.txt_user_name);
    	tv.setText( mTargetUser.getName() );
    	tv = (TextView)findViewById(R.id.txt_screen_name);
    	tv.setText( mTargetUser.getScreenName() );

	    FragmentManager manager = getSupportFragmentManager();
	    FragmentTransaction transaction = manager.beginTransaction();
	    Fragment fragment = new UserHomeTimelineFragment();
	    transaction.add(R.id.user_home_timeline_layout_root, fragment, "userhome-timeline");
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
		    	Toast.makeText(UsersHomeTimelineActivity.this, text, duration).show();
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
