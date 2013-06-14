package jp.miku39.android.tweetnyan.activities;

import java.util.ArrayList;
import java.util.List;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.IconCacheThread.IIconDownloadedCallback;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.SavedSearch;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class SearchResultActivity extends ActivityWithTheme implements IIconDownloadedCallback {
	final static String TAG = "SearchResultActivity";

	String	mSearchWord;

	SavedSearch mSavedSearch;

	private SearchedTweetListAdapter mListAdapter;
	private ArrayList<Status> mSearchedTweets = new ArrayList<Status>();

	private boolean mDoSearching;
	private Twitter mTwitter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        mTwitter = Lib.createTwitter( this );
        
        TweetnyanMainActivity.sIconCacheThread.pushCallback( this );

        super.onCreate(savedInstanceState);
        
        ActionBar bar = getActionBar();
        bar.setSubtitle( getString(R.string.search_result) );

	    getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	    setContentView(R.layout.search_result_layout);

	    Intent intent = getIntent();
	    mSearchWord = intent.getStringExtra("search-word");
	    if( mSearchWord==null ){
	    	mSearchWord = intent.getDataString().substring( ("tweetnyansearch:").length() );
	    }

	    mSavedSearch = (SavedSearch)intent.getSerializableExtra("saved-search");

	    Log.d(TAG,"search keyword="+mSearchWord);

	    EditText et = (EditText)findViewById(R.id.search_text);
	    et.setText( mSearchWord );
	    et.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if( actionId == EditorInfo.IME_ACTION_SEARCH ){
					CommonUtils.hideSoftwareKeyboard( SearchResultActivity.this, v);

					EditText et = (EditText)findViewById(R.id.search_text);
					String str = et.getEditableText().toString();
					searchString(str);
					return true;
				}
				return false;
			}
		});

	    ImageButton btn = (ImageButton)findViewById(R.id.do_search);
	    btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtils.hideSoftwareKeyboard( SearchResultActivity.this, v);

				EditText et = (EditText)findViewById(R.id.search_text);
				String str = et.getEditableText().toString();
				searchString(str);
			}
		});

	    ListView lv = (ListView)findViewById(R.id.search_result_listview);
	    mListAdapter = new SearchedTweetListAdapter(this, R.layout.one_tweet_layout, mSearchedTweets);
    	lv.setAdapter(mListAdapter);
    	lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			@Override
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("Search","position="+position);
			}});

    	searchString( mSearchWord );
	}

	@Override
	public void onIconDownloaded(long id) {
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				mListAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TweetnyanMainActivity.sIconCacheThread.popCallback();
	}

	@Override
	protected void onResume() {
		Log.d(TAG,"onResume");
		super.onResume();

//		TwitterClientMainActivity.sIconCacheService.registerViewUpdateCallback(mIconCacheListener);
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
//		TwitterClientMainActivity.sIconCacheService.registerViewUpdateCallback(null);
	}

	public void searchString(final String str){
		Log.d("Search","search:"+str);
		if( str.length()<=0 ) return;

		if( mDoSearching ) return;
		mDoSearching = true;

		setProgressBarIndeterminateVisibility(true);

		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				Query q = new Query();
				QueryResult res = null;
				q.setQuery(str);
				try {
					res = mTwitter.search(q);
					final List<Status> tweets = res.getTweets();
					for(int i=0;i<tweets.size();i++){
						Status t = tweets.get(i);
						TweetnyanMainActivity.sIconCacheThread.requestDownload( t.getUser().getProfileImageURL(), t.getUser().getId() );
					}
					// UI更新
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							setProgressBarIndeterminateVisibility(false);
							mSearchedTweets.clear();
							mSearchedTweets.addAll( tweets );
							mListAdapter.notifyDataSetChanged();

							TextView tv = (TextView)findViewById(R.id.search_result_message);
							tv.setVisibility( tweets.size()==0?View.VISIBLE:View.GONE);
						}
					});
				} catch (TwitterException e) {
					// TODO エラーメッセージ表示
					e.printStackTrace();
				} finally {
					// progressを止める
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							setProgressBarIndeterminateVisibility(false);
							mDoSearching = false;
						}
					});
				}
			}
		});
		th.start();
	}

	void newTweet(){
	    EditText et = (EditText)findViewById(R.id.search_text);
		Intent intent = new Intent(this, CreateNewTweetActivity.class);
		String str = et.getEditableText().toString();
		if( str.indexOf("#")==0 || str.indexOf("＃")==0 ){
			intent.putExtra("text", " "+str);
		}
		intent.putExtra("move_last",false);
		startActivity(intent);
	}
	
	void saveThisSearch(){
		setProgressBarIndeterminateVisibility(true);
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					mSavedSearch = mTwitter.createSavedSearch(mSearchWord);
				} catch (TwitterException e) {
					e.printStackTrace();
					mSavedSearch = null;
					
					String str = String.format( getString(R.string.error_add_saved_search), e.getStatusCode());
					showToastFromThread( str, Toast.LENGTH_SHORT );

				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setProgressBarIndeterminateVisibility(false);
						}
					});
				}
			}
		});
		th.start();
	}

	void deleteThisSavedSearch(){
		if( mSavedSearch==null ) return;

		setProgressBarIndeterminateVisibility(true);
		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					mTwitter.destroySavedSearch( mSavedSearch.getId() );
					mSavedSearch = null;
				} catch (TwitterException e) {
					e.printStackTrace();

					String str = String.format( getString(R.string.error_remove_saved_search), e.getStatusCode());
					showToastFromThread( str, Toast.LENGTH_SHORT );
					
				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setProgressBarIndeterminateVisibility(false);
						}
					});
				}
			}
		});
		th.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Consts.MENU_ID_NEW_TWEET, Menu.NONE, getString(R.string.new_tweet) ).setIcon(R.drawable.ic_menu_tweet).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, Consts.MENU_ID_SAVE_THIS_SEARCH, Menu.NONE, getString(R.string.save_this_search_result) ).setIcon(android.R.drawable.ic_menu_save);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, Consts.MENU_ID_DEL_THIS_SEARCH, Menu.NONE, getString(R.string.delete_this_saved_search) ).setIcon(R.drawable.ic_delete);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	changeActionMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    private void changeActionMenu(Menu menu){
		menu.findItem(Consts.MENU_ID_SAVE_THIS_SEARCH).setVisible( mSavedSearch==null );    		
		menu.findItem(Consts.MENU_ID_DEL_THIS_SEARCH).setVisible( mSavedSearch!=null );    		
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

        case Consts.MENU_ID_NEW_TWEET:
        	newTweet();
            break;

        case Consts.MENU_ID_SAVE_THIS_SEARCH:
        	saveThisSearch();
            break;
        case Consts.MENU_ID_DEL_THIS_SEARCH:
        	deleteThisSavedSearch();
            break;
        }
        return true;
	}

}
