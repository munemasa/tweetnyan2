package jp.miku39.android.tweetnyan.activities;

/**
 * フォロー・フォロワーリストを表示するアクティビティ
 */

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import jp.miku39.android.tweetnyan.Prefs;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.R.drawable;
import jp.miku39.android.tweetnyan.R.id;
import jp.miku39.android.tweetnyan.R.layout;
import jp.miku39.android.tweetnyan.activities.FollowingFollowersListAdapter.StatusWithUser;
import twitter4j.Status;
import twitter4j.User;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FollowingFollowersListAdapter extends ArrayAdapter<StatusWithUser> {
	final static String TAG = "FollowingFollowersListAdapter";
	Context cxt;

	public static class StatusWithUser{
		Status	mStatus;
		User	mUser;
	}

	private LayoutInflater mInflater;
	private List<StatusWithUser> mItems;

	public FollowingFollowersListAdapter(Context context, int textViewResourceId, List<StatusWithUser> objects) {
		super(context, textViewResourceId, objects);
		mItems = objects;
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		cxt = context;
	}

    class ViewHolder {
    	ImageView 	img_retweeted;
    	ImageView	img_favorited;
    	ImageView	img_icon;
		ImageView	img_locked;
    	TextView	txt_retweeted_by;
    	TextView	txt_date_and_client;
    	TextView	txt_user_name;
    	TextView	txt_screen_name;
    	TextView	txt_tweet_body;
    	int			position;
    }

	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View view = convertView;  
		if (view == null) {  
			view = mInflater.inflate(R.layout.one_tweet_layout, null);  

			holder = new ViewHolder();
			holder.img_retweeted = (ImageView)view.findViewById(R.id.img_retweeted);
			holder.img_favorited = (ImageView)view.findViewById(R.id.img_faved);
			holder.img_icon = (ImageView)view.findViewById(R.id.user_icon);
			holder.img_locked = (ImageView)view.findViewById(R.id.locked_user);
			holder.txt_retweeted_by = (TextView)view.findViewById(R.id.tweet_retweeted_by);
			holder.txt_date_and_client = (TextView)view.findViewById(R.id.tweet_date_and_client);
			holder.txt_user_name = (TextView)view.findViewById(R.id.twitter_user_name);
			holder.txt_screen_name = (TextView)view.findViewById(R.id.twitter_screen_name);
			holder.txt_tweet_body = (TextView)view.findViewById(R.id.tweet_body);
			view.setTag(holder);
		}else{
			holder = (ViewHolder)view.getTag();
		}

		Status item = ((StatusWithUser)mItems.get(position)).mStatus;
		User user = ((StatusWithUser)mItems.get(position)).mUser;

		TextView text;
		String str;
		ImageView image;

		// リツイート記号表示はなしで
		holder.img_retweeted.setVisibility( View.GONE );

		// お気に入り記号表示はなしで
		holder.img_favorited.setVisibility( View.GONE );

		// 鍵付きユーザ表示
		holder.img_locked.setVisibility( user.isProtected()?View.VISIBLE:View.GONE );

		// リツイート元表示はなしで.
		text = holder.txt_retweeted_by;
		text.setVisibility(View.GONE);

		image = holder.img_icon;
		Bitmap bmp = TweetnyanMainActivity.sIconCacheThread.loadIcon(user.getProfileImageURL(), user.getId());;
		if( bmp!=null ){
			image.setImageBitmap(bmp);
		}else{
			image.setImageResource(R.drawable.ic_dummy);
		}
		holder.position = position;
		image.setTag( view );

		// TODO フォロー・フォロワーリスト表示
//		image.setOnClickListener(this);
//		image.setOnLongClickListener(this);
//		image.setOnTouchListener(this);

		// 名前
		text = holder.txt_user_name;
		text.setText(user.getName());

		// スクリーン名
		text = holder.txt_screen_name;  
		text.setText(user.getScreenName());

		if( item!=null ){
			// ツイート本体
			text = holder.txt_tweet_body;
			str = item.getText();
//			str = Lib.encodeHTML(str);
//			str = str.replaceAll("((http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+)", "<a href=\"$0\">$0</a>");
//			str = str.replaceAll("(\\s+)@([A-Za-z\\d_]+)", "$1<a href=\"tweetnyanprof:@$2\">@$2</a>");
//			str = str.replaceAll("^@([A-Za-z\\d_]+)", "<a href=\"tweetnyanprof:$0\">$0</a>");
			text.setText( Html.fromHtml(str) );
			text.setFocusable(false);
	
			// ツイートの文字色
//			text.setTextColor(0xFFc8c8c8);

			// 日付とクライアント名
			text = holder.txt_date_and_client; 
			str = Pattern.compile("<.*?>").matcher(item.getSource()).replaceAll("");
			String date;
			if( Prefs.sDateFormatType==0 ){
				Date now = new Date();
				long elapse_seconds = (now.getTime()/1000 - item.getCreatedAt().getTime()/1000);
				if( elapse_seconds < 60 ){
					date = elapse_seconds+"秒前";
				}else if( elapse_seconds < 60*60 ){
					date = (elapse_seconds/60)+"分前";
				}else if( elapse_seconds < 60*60*24 ){
					date = (elapse_seconds/60/60)+"時間前";				
				}else{
					long days = elapse_seconds/60/60/24;
					date = days+"日前";
				}
			}else{
				date = item.getCreatedAt().toLocaleString();
			}
			text.setText(date+" from "+str);

			holder.txt_tweet_body.setVisibility(View.VISIBLE);
			holder.txt_date_and_client.setVisibility(View.VISIBLE);
		}else{
			holder.txt_tweet_body.setVisibility(View.GONE);
			holder.txt_date_and_client.setVisibility(View.GONE);
		}

//		view.setBackgroundColor(0x00000000);

		// カラーラベルの設定
		text = (TextView)view.findViewById(R.id.color_label);
		text.setBackgroundColor( 0xff222222 );
		return view;
	}

}
