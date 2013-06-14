package jp.miku39.android.tweetnyan.activities;

import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.UserList;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


public class CreateNewListActivity extends ActivityWithTheme {
	final static String TAG = "CreateNewListActivity";

	UserList	mCurrentList = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.create_new_list);

	    ActionBar bar = getActionBar();
	    bar.setSubtitle( getString(R.string.create_new_list_title) );

	    // 文字数カウント
		EditText et = (EditText)findViewById(R.id.list_description);
	    et.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
			    TextView v = (TextView)findViewById(R.id.numof_characters);
			    v.setText(s.length()+" characters");
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}});

	    mCurrentList = (UserList)getIntent().getSerializableExtra("current-list");
	    if( mCurrentList!=null){
	    	Log.d(TAG,"Edit this list.");
	    	fillForm();
	    }

	    setupButton();
	}

	void fillForm(){
		EditText et;
		et = (EditText)findViewById(R.id.list_name);
		et.setText(mCurrentList.getName());

		et = (EditText)findViewById(R.id.list_description);
		et.setText(mCurrentList.getDescription());
		
		RadioGroup radioGroup = (RadioGroup)findViewById(R.id.list_disclosure_level);
		radioGroup.check( mCurrentList.isPublic()?R.id.list_public:R.id.list_private );
	}

	void setupButton(){
		Button btn;
		btn = (Button)findViewById(R.id.btn_create);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if( mCurrentList==null ){
					createNewList();
				}else{
					updateNewList();
				}
			}
		});

		btn = (Button)findViewById(R.id.btn_cancel);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
	
	void createNewList(){
		final Twitter twitter = Lib.getTwitter();

		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_creating_list) );
		dialog.setCancelable(false);
		FragmentManager manager = getFragmentManager();
		dialog.show(manager, "create-new-list");

		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				EditText et;
				et = (EditText)findViewById(R.id.list_name);
				String list_name = et.getEditableText().toString();
				Log.d(TAG,"List Name="+list_name);

				et = (EditText)findViewById(R.id.list_description);
				String list_description = et.getEditableText().toString();
				Log.d(TAG,"Description="+list_description);

				RadioGroup radioGroup = (RadioGroup)findViewById(R.id.list_disclosure_level);
				int checked = radioGroup.getCheckedRadioButtonId();
				if( checked==R.id.list_public ){
					Log.d(TAG,"List is public.");
				}
				if( checked==R.id.list_private ){
					Log.d(TAG,"List is private.");
				}

				try {
					final UserList l = twitter.createUserList(list_name, checked==R.id.list_public, list_description);

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// FIXME 新しいリストを追加する
							//TwitterClientMainActivity.sTwitterService.mUserLists.add(l);					
						}
					});

					// 作成したリストの情報を返す
					Intent intent = new Intent();
					intent.putExtra("new-list", l);
					setResult( RESULT_OK, intent);
					finish();
				} catch (final TwitterException e) {
					e.printStackTrace();
					showToastFromThread("Failed to create a list.(code="+e.getStatusCode()+")", Toast.LENGTH_SHORT);

				} finally {
					runOnUiThread( new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}} );
				}
			}
		});
		th.start();
	}

	void updateNewList(){
		final Twitter twitter = Lib.getTwitter();

		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_updating_list_description) );
		dialog.setCancelable(false);
		FragmentManager manager = getFragmentManager();
		dialog.show(manager, "update-new-list");

		Thread th = new Thread( new Runnable() {
			@Override
			public void run() {
				EditText et;
				et = (EditText)findViewById(R.id.list_name);
				String list_name = et.getEditableText().toString();
				Log.d(TAG,"List Name="+list_name);

				et = (EditText)findViewById(R.id.list_description);
				String list_description = et.getEditableText().toString();
				Log.d(TAG,"Description="+list_description);

				RadioGroup radioGroup = (RadioGroup)findViewById(R.id.list_disclosure_level);
				int checked = radioGroup.getCheckedRadioButtonId();
				if( checked==R.id.list_public ){
					Log.d(TAG,"List is public.");
				}
				if( checked==R.id.list_private ){
					Log.d(TAG,"List is private.");
				}

				try {
					final UserList l = twitter.updateUserList(mCurrentList.getId(), list_name, checked==R.id.list_public, list_description);
					// 作成したリストの情報を返す
					Intent intent = new Intent();
					intent.putExtra("updated-list", l);
					setResult(RESULT_OK, intent);
					finish();
				} catch (final TwitterException e) {
					e.printStackTrace();
					String str = String.format(getString(R.string.error_update_list), e.getStatusCode());
					showToastFromThread( str );

				} finally {
					runOnUiThread( new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}} );
				}
			}
		});
		th.start();
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
