package jp.miku39.android.tweetnyan.activities;

/**
 * 検索したツイート表示のアダプタ
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import jp.miku39.android.tweetnyan.R;
import twitter4j.Status;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchedTweetListAdapter extends ArrayAdapter<Status> {

	Activity cxt;
	private LayoutInflater mInflater;
	private List<Status> mItems;

	public SearchedTweetListAdapter(Activity context, int textViewResourceId, List<Status> objects) {
		super(context, textViewResourceId, objects);
		cxt = context;
		mItems = objects;
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {  
		View view = convertView;  
		if (view == null) {  
			view = mInflater.inflate(R.layout.one_tweet_layout, null);  
//			view.setBackgroundResource(android.R.drawable.status_bar_item_background);  
		}

		Status item = (Status)mItems.get(position);  
		TextView text;
		String str;
		ImageView image;

		// RT記号
		image = (ImageView)view.findViewById(R.id.img_retweeted);
		image.setVisibility(View.GONE);			
		// お気に入り記号表示
		image = (ImageView)view.findViewById(R.id.img_faved);
		image.setVisibility(View.GONE);
		// リツイート元を表示
		text = (TextView)view.findViewById(R.id.tweet_retweeted_by);
		text.setVisibility(View.GONE);

		image = (ImageView)view.findViewById(R.id.user_icon);
		Bitmap bmp = null;
		bmp = TweetnyanMainActivity.sIconCacheThread.loadIcon( item.getUser().getProfileImageURL(), item.getUser().getId() );
		if( bmp!=null ){
			image.setImageBitmap(bmp);
		}else{
			image.setImageResource(R.drawable.ic_dummy);
		}

		// 名前
		text = (TextView)view.findViewById(R.id.twitter_user_name);
		text.setText("name");
		text.setVisibility(View.GONE);

		// 鍵アイコンはなし
		image = (ImageView)view.findViewById(R.id.locked_user);
		image.setVisibility(View.GONE);

		// スクリーン名
		text = (TextView)view.findViewById(R.id.twitter_screen_name);  
		text.setText( item.getUser().getScreenName() );

		// ツイート本体
		text = (TextView)view.findViewById(R.id.tweet_body);  
//		text.setOnTouchListener( mLinkHandler );
		str = item.getText();
//		str = Lib.encodeHTML(str);
//		str = Lib.addLink(str);
		text.setText( str );
		text.setFocusable(false);

		// 日付とクライアント名
		text = (TextView)view.findViewById(R.id.tweet_date_and_client);
		str = Pattern.compile("<.*?>").matcher(item.getSource()).replaceAll("");
		text.setText(item.getCreatedAt().toLocaleString()+" from "+str);
		return view;
	}
}
