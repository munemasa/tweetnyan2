package jp.miku39.android.tweetnyan.activities;

import java.util.ArrayList;
import java.util.HashMap;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.lib.YesNoDialogFragment;
import jp.miku39.android.lib.YesNoDialogFragment.YesNoDialogFragmentCallback;
import jp.miku39.android.tweetnyan.Consts;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


public class SelectAccountActivity extends ActivityWithTheme implements OnItemClickListener, OnItemLongClickListener, YesNoDialogFragmentCallback {
	final static String TAG = "SelectAccountActivity";
	private ArrayAdapter<String> mAdapter;
	private HashMap<String,Integer> mAccountIndexMap = new HashMap<String,Integer>();
	private ArrayList<String> mAccounts;
	private int mPos;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"onCreate");
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.select_account_layout);

	    if( !getIntent().getBooleanExtra("no-skip", false) ){
	    	int n = Lib.getCurrentAccountIndex(this);
	    	if( n>=0 ){
			    Intent intent = new Intent( SelectAccountActivity.this, TweetnyanMainActivity.class );
			    startActivity(intent);
			    finish();
			    return;
	    	}
	    }

	    ActionBar bar = getActionBar();
	    bar.setSubtitle( getString(R.string.select_account) );
	    bar.setHomeButtonEnabled(false);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	    initViews();
	}

	void initViews(){
	    mAccounts = new ArrayList<String>();
	    for(int i=1; i<=Consts.sMaxAccounts; i++){
	    	String str = CommonUtils.getStringValue(this, "username-"+i);
	    	if( str.length()>0 ){
	    		Log.d(TAG,"Select User("+i+") = "+str);
	    		mAccountIndexMap.put( str, i );
	    		mAccounts.add(str);
	    	}
	    }
	    mAccounts.add( getString(R.string.add_account) );

	    ListView lv = (ListView)findViewById(R.id.select_account_listview);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mAccounts);
	    lv.setAdapter(mAdapter);
	    lv.setOnItemClickListener( this );
	    lv.setOnItemLongClickListener( this );
	}

	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Integer i = mAccountIndexMap.get( mAccounts.get(position) );
		Log.d(TAG,"Select "+i+"/"+mAccounts.get(position));

		if( i!=null ){
			Lib.setCurrentAccountIndex( SelectAccountActivity.this, i );
		    Intent intent = new Intent( SelectAccountActivity.this, TweetnyanMainActivity.class );
		    startActivity(intent);
		    finish();
		}else{
			// 新規追加
		    Intent intent = new Intent( SelectAccountActivity.this, TwitterSigninActivity.class );
		    int j;
			for(j=1;j<=Consts.sMaxAccounts;j++){
				if( !mAccountIndexMap.containsValue(j) ){
					intent.putExtra("index",j);
					break;
				}
			}
			if( j<=Consts.sMaxAccounts ){
				startActivity(intent);
			}else{
				// アカウントは10個まで
				Toast.makeText(this, getString(R.string.warn_max_accounts), Toast.LENGTH_LONG).show();
			}
		}
	}

	public void doPositiveClick() {
		String screen_name = mAccounts.get(mPos);
		Integer current = Lib.getCurrentAccountIndex(this);
		Integer n = mAccountIndexMap.get(screen_name);
		if( n!=null ){
			if( current==n ){
				Lib.setCurrentAccountIndex(this, -1);
			}
			Lib.removeAccessToken(this, n);
			mAccountIndexMap.remove(screen_name);
			mAccounts.remove(mPos);
			mAdapter.notifyDataSetChanged();
		}
	}

	public void doNegativeClick() {
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// 登録したアカウントのサインイン情報を削除
		mPos = position;

	    // DialogFragment.show() will take care of adding the fragment
	    // in a transaction.  We also want to remove any currently showing
	    // dialog, so make our own transaction and take care of that here.
	    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DialogFragment newFragment = YesNoDialogFragment.newInstance( getString(R.string.confirm_delete) );
	    newFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme_Light);
	    newFragment.show(ft, "dialog");
		return false;
	}

}
