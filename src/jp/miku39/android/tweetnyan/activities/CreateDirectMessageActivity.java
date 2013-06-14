package jp.miku39.android.tweetnyan.activities;

/**
 * ダイレクトメッセージを作成するアクティビティ
 */



import jp.miku39.android.lib.ActivityWithTheme;
import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.lib.IndeterminateProgressDialogFragment;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CreateDirectMessageActivity extends ActivityWithTheme {
	final static String TAG = "CreateDirectMessageActivity";
	Twitter mTwitter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.create_directmessage_layout);

	    mTwitter = Lib.createTwitter(this);

	    ActionBar bar = getActionBar();
	    bar.setTitle(R.string.directmessage);
	    bar.setSubtitle( "@"+Lib.getCurrentAccountScreenName( this ) );
		bar.setDisplayHomeAsUpEnabled(true);

		initViews();
	}
	
	void initViews(){
	    EditText edit = (EditText)findViewById(R.id.edit_direct_message_to);
	    try{
		    Intent intent = getIntent();
		    String to = intent.getStringExtra("to");
		    Log.d(TAG,"To:"+to);
		    edit.setText( to );
		    ((EditText)findViewById(R.id.edit_direct_message_body)).requestFocus();
	    }catch(Exception e){
	    	e.printStackTrace();
	    	edit.setText("");
	    }

	    ((EditText)findViewById(R.id.edit_direct_message_body)).addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
			    TextView v = (TextView)findViewById(R.id.numof_dm_character);
			    v.setText(""+s.length());
			    Button btn = (Button)findViewById(R.id.btn_send_direct_message);
			    btn.setEnabled( s.length()!=0?true:false );
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}});

	    Button btn;
	    // 送信
	    btn = (Button)findViewById(R.id.btn_send_direct_message);
	    btn.setEnabled(false);
	    btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CommonUtils.hideSoftwareKeyboard(CreateDirectMessageActivity.this, v);
				sendDirectMessage();
			}});
	    // キャンセル
	    btn = (Button)findViewById(R.id.btn_cancel_direct_message);
	    btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				finish();
			}});		
	}

	private void createAutoCompleteList(){
		// TODO DM送付先のオートコンプリートリストを作る
	    AutoCompleteTextView edit = (AutoCompleteTextView)findViewById(R.id.edit_direct_message_to);
	    Twitter tw = mTwitter;
	}

	private void showErrorDialog(TwitterException te){
		te.printStackTrace();
		Log.d(TAG,"On New Tweet: "+te.getStatusCode());
		String str = String.format( getString(R.string.failed_to_send_dm), te.getStatusCode() );
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	private void sendDirectMessage(){
		final String screenname = ((EditText)findViewById(R.id.edit_direct_message_to)).getEditableText().toString();
		final String txt = ((EditText)findViewById(R.id.edit_direct_message_body)).getEditableText().toString();
		
		if( screenname.length()<=0 || txt.length()<=0 ){
			Log.d(TAG,"No Destination Screen Name or Text");
			return;
		}
		
		final IndeterminateProgressDialogFragment dialog = IndeterminateProgressDialogFragment.newInstance( getString(R.string.now_sending) );
		dialog.setCancelable(false);
		FragmentManager manager = getFragmentManager();
		dialog.show(manager, "send_dm");

		Thread th = new Thread( new Runnable(){
			@Override
			public void run() {
				try {
					mTwitter.sendDirectMessage(screenname, txt);
					finish();
				} catch (final TwitterException te) {
					runOnUiThread( new Runnable(){
						@Override
						public void run() {
							showErrorDialog(te);
						}} );
				} finally {
					runOnUiThread( new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}} );
				}
			}} );
		th.start();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG,"Menu selected:"+item.getItemId());
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
