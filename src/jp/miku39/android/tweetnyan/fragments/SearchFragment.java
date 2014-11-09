package jp.miku39.android.tweetnyan.fragments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;

import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.activities.SearchResultActivity;
import jp.miku39.android.tweetnyan.activities.TweetnyanMainActivity;
import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class SearchFragment extends Fragment {
	final static String TAG = "SearchFragment";

	private TweetnyanMainActivity mMainActivity;

	ArrayList<SavedSearch> mSavedSearch = new ArrayList<SavedSearch>();
	ArrayList<String> mSavedSearchString = new ArrayList<String>();
	ArrayList<String> mTrends = new ArrayList<String>();

	private boolean mBusySavedSearch;
	private boolean mBusyTrends;

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG,"onAttach");
		super.onAttach(activity);
		mMainActivity = (TweetnyanMainActivity)activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"onCreate");
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		loadTrendsFromFile();
		loadSavedSearchFromFile();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.search_fragment_layout, container, false);

		// 検索ボタン
	    ImageButton ib = (ImageButton)v.findViewById(R.id.do_search);
	    ib.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText et = (EditText)(getView().findViewById(R.id.search_text));
				String str = et.getEditableText().toString();
				CommonUtils.hideSoftwareKeyboard( getActivity(), v);

				Intent intent = new Intent(getActivity(), SearchResultActivity.class);
				intent.putExtra("search-word", str);
				startActivity(intent);
			}
		});
	    // 検索文字入力
	    EditText et = (EditText)v.findViewById(R.id.search_text);
	    et.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if( actionId == EditorInfo.IME_ACTION_SEARCH ){
					EditText et = (EditText)(getView().findViewById(R.id.search_text));
					String str = et.getEditableText().toString();
					CommonUtils.hideSoftwareKeyboard(getActivity(), v);

					Intent intent = new Intent(getActivity(), SearchResultActivity.class);
					intent.putExtra("search-word", str);
					startActivity(intent);
					return true;
				}
				return false;
			}
		});
	    ImageButton btn = (ImageButton)v.findViewById(R.id.btn_reload_saved_search);
	    btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG,"Reload Saved Search");
				v.startAnimation( AnimationUtils.loadAnimation(getActivity(), R.anim.rotate) );
				loadSavedSearch();
			}
		});
	    btn = (ImageButton)v.findViewById(R.id.btn_reload_trends);
	    btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG,"Reload Trends");
				v.startAnimation( AnimationUtils.loadAnimation(getActivity(), R.anim.rotate) );
				loadTrends();
			}
		});

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(TAG,"onViewCreated");
		super.onViewCreated(view, savedInstanceState);

		if( mTrends.size()!=0 ){
			initTrends();
		}else{
		    loadTrends();
		}
		if( mSavedSearchString.size()!=0 ){
			initSavedSearch();
		}else{
		    loadSavedSearch();			
		}
	}

	void initSavedSearch(){
	    ArrayAdapter<String> adapter;
	    final Spinner sp = (Spinner)(getView().findViewById(R.id.saved_search));
    	adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	sp.setAdapter(adapter);
    	adapter.add(getString(R.string.saved_search));
    	for(int i=0;i<mSavedSearchString.size();i++){
    		String str = mSavedSearchString.get(i);
        	adapter.add(str);
    	}
    	sp.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if(pos!=0){
		    		String str = mSavedSearchString.get(pos-1);
		    		SavedSearch saved_search = mSavedSearch.get(pos-1);
		    		
		    		sp.setSelection(0);

		    		// 検索結果アクティビティを起動
		    		Intent intent = new Intent(getActivity(), SearchResultActivity.class);
					intent.putExtra("search-word", str);
					intent.putExtra("saved-search", saved_search);
					startActivity(intent);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}});		
	}
	
	void initTrends(){
	    ArrayAdapter<String> adapter;

		final Spinner sp = (Spinner)(getView().findViewById(R.id.trends));
    	adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	sp.setAdapter(adapter);

    	adapter.add(getString(R.string.trends));
    	for(int i=0;i<mTrends.size();i++){
    		String str = mTrends.get(i);
        	adapter.add(str);
    	}
    	sp.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if(pos!=0){
		    		String str = mTrends.get(pos-1);
		    		
		    		sp.setSelection(0);

		    		// 検索結果アクティビティを起動
		    		Intent intent = new Intent(getActivity(), SearchResultActivity.class);
					intent.putExtra("search-word", str);
					startActivity(intent);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}});		
	}

	public void loadSavedSearch(){
		if( mBusySavedSearch ) return;
		mBusySavedSearch = true;

		if( mMainActivity.getTwitter()==null ) return;

		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					final ResponseList<SavedSearch> res = mMainActivity.getTwitter().getSavedSearches();
					// UIの更新
					getActivity().runOnUiThread( new Runnable() {
						@Override
						public void run() {
							mSavedSearch.clear();
							mSavedSearchString.clear();
							for(int i=0; i<res.size(); i++){
								SavedSearch s = res.get(i);
								mSavedSearchString.add( s.getQuery() );
								mSavedSearch.add(s);
							}
							saveSavedSearchToFile();
							if( getView()!=null ){
								initSavedSearch();
							}
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
				} finally {
					mBusySavedSearch = false;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if( getView()!=null ){
								ImageButton btn = (ImageButton)getView().findViewById(R.id.btn_reload_saved_search);
								btn.clearAnimation();
								btn.setAnimation(null);
							}
						}
					});
				}
			}
		});
		th.start();		
	}

	public void loadTrends(){
		if( mBusyTrends ) return;
		mBusyTrends = true;

		if( mMainActivity.getTwitter()==null ) return;

		Thread th = new Thread( new Runnable() {			
			@Override
			public void run() {
				try {
					final Trends trends = mMainActivity.getTwitter().getPlaceTrends(23424856);

					// UI更新
					getActivity().runOnUiThread( new Runnable() {
						@Override
						public void run() {
							Trend trend[] = trends.getTrends();
							Log.d("Search",""+trend.length);
							mTrends.clear();
							for(int i=0; i<trend.length; i++){
								mTrends.add( trend[i].getName() );
							}
							saveTrendsToFile();
							if( getView()!=null ){
								initTrends();
							}
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
				} finally {
					mBusyTrends = false;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if( getView()!=null ){
								ImageButton btn = (ImageButton)getView().findViewById(R.id.btn_reload_trends);
								btn.clearAnimation();
								btn.setAnimation(null);
							}
						}
					});
				}
			}
		});
		th.start();
	}

	private void loadTrendsFromFile() {
		try {
			FileInputStream os = Lib.openUserFileInput(getActivity(),"trends");
			ObjectInputStream inObject = new ObjectInputStream(os);
			mTrends = (ArrayList<String>) inObject.readObject();
			inObject.close();
			os.close();
			Log.d(TAG,"Load Trends from File.");
		} catch (OptionalDataException e) {
			e.printStackTrace();
			mTrends = new ArrayList<String>();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			mTrends = new ArrayList<String>();
		} catch (IOException e) {
			e.printStackTrace();
			mTrends = new ArrayList<String>();
		} catch (ClassCastException e){
			e.printStackTrace();
			mTrends = new ArrayList<String>();
		}
	}

	private void saveTrendsToFile() {
		Log.d(TAG,"Save Trends.");
		try {
			FileOutputStream os = Lib.openUserFileOutput(getActivity(),"trends");
			ObjectOutputStream outObject = new ObjectOutputStream(os);
			outObject.writeObject( mTrends );
			outObject.close();
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void loadSavedSearchFromFile(){
		try {
			FileInputStream os = Lib.openUserFileInput(getActivity(), "savedsearch");
			ObjectInputStream inObject = new ObjectInputStream(os);
			mSavedSearchString = (ArrayList<String>) inObject.readObject();
			mSavedSearch = (ArrayList<SavedSearch>) inObject.readObject();
			inObject.close();
			os.close();
			Log.d(TAG,"Load saved search from file.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mSavedSearchString = new ArrayList<String>();
			mSavedSearch = new ArrayList<SavedSearch>();
		} catch (IOException e) {
			e.printStackTrace();
			mSavedSearchString = new ArrayList<String>();
			mSavedSearch = new ArrayList<SavedSearch>();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			mSavedSearchString = new ArrayList<String>();
			mSavedSearch = new ArrayList<SavedSearch>();
		} catch (ClassCastException e){
			e.printStackTrace();
			mSavedSearchString = new ArrayList<String>();
			mSavedSearch = new ArrayList<SavedSearch>();
		}
	}

	private void saveSavedSearchToFile(){
		Log.d(TAG,"Save Saved Search.");
		try {
			FileOutputStream os = Lib.openUserFileOutput(getActivity(),"savedsearch");
			ObjectOutputStream outObject = new ObjectOutputStream(os);
			outObject.writeObject( mSavedSearchString );
			outObject.writeObject( mSavedSearch );
			outObject.close();
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
