package jp.miku39.android.tweetnyan.activities;

/**
 * DMを表示するアダプタ
 */

import java.util.List;

import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.R.drawable;
import jp.miku39.android.tweetnyan.R.id;
import jp.miku39.android.tweetnyan.R.layout;

import twitter4j.DirectMessage;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DirectMessageListViewAdapter extends ArrayAdapter<DirectMessage> {
	final static String TAG = "DirectMessageListAdapter";

	private List<DirectMessage> mItems;
	private LayoutInflater mInflater;
	private Context mContext;


	public DirectMessageListViewAdapter(Context context, int textViewResourceId, List<DirectMessage> objects) {
		super(context, textViewResourceId, objects);

		mContext = context;
		mItems = objects;  
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {  
		View view = convertView;  
		if (view == null) {  
			view = mInflater.inflate(R.layout.one_directmessage_layout, null);  
		}

		DirectMessage item = (DirectMessage)mItems.get(position);  
		TextView text;
		Bitmap icon;

		ImageView image = (ImageView)view.findViewById(R.id.dm_user_icon);
		icon = TweetnyanMainActivity.sIconCacheThread.loadIcon( item.getSender().getProfileImageURL(), item.getSenderId());
		if( icon==null ){
			image.setImageResource(R.drawable.ic_dummy);
		}else{
			image.setImageBitmap(icon);
		}

		text = (TextView)view.findViewById(R.id.dm_user_name);  
		text.setText(item.getSender().getName() );

		text = (TextView)view.findViewById(R.id.dm_screen_name);  
		text.setText(item.getSenderScreenName() + " > " + item.getRecipientScreenName() );

		text = (TextView)view.findViewById(R.id.dm_message_boxy);  
		String str = item.getText();
//		str = Lib.encodeHTML(str);
//		str = Lib.addLink(str);
		text.setText( str );
		text.setFocusable(false);

//		if( is_readed ) text.setTextColor(0xFFa0a0a0);
//		else text.setTextColor(0xFFd8d8d8);

		text = (TextView)view.findViewById(R.id.dm_date);
		text.setText(item.getCreatedAt().toLocaleString());
		return view;
	}
}
