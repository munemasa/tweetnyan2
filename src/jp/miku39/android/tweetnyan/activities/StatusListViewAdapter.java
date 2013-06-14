package jp.miku39.android.tweetnyan.activities;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import jp.miku39.android.tweetnyan.FavoriteStatusWrapper;
import jp.miku39.android.tweetnyan.Lib;
import jp.miku39.android.tweetnyan.Prefs;
import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.R.drawable;
import jp.miku39.android.tweetnyan.R.id;
import jp.miku39.android.tweetnyan.R.layout;
import jp.miku39.android.tweetnyan.R.string;

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

public class StatusListViewAdapter extends ArrayAdapter<Status> {
	final static String TAG = "TweetListViewAdapter";

	private List<Status> mItems;
	private LayoutInflater mInflater;
	private Context mContext;
	private CheckTweetSelectedInterface mCheckTweetSelected;

	public StatusListViewAdapter(Context context, int textViewResourceId, List<Status> objects, CheckTweetSelectedInterface parent) {
		super(context, textViewResourceId, objects);
		
		mItems = objects;
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mContext = context;
		mCheckTweetSelected = parent;
	}

	// 高速化用のViewHolder
	class ViewHolder {
    	ImageView 	img_retweeted;
    	ImageView	img_favorited;
    	ImageView	img_icon;
		ImageView	img_locked;
		ImageView	img_in_reply_to;
		ImageView	img_mapdrop;
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
			holder.img_in_reply_to = (ImageView)view.findViewById(R.id.tweet_have_in_reply_to);
			holder.img_mapdrop = (ImageView)view.findViewById(R.id.tweet_have_geolocation);
			holder.txt_retweeted_by = (TextView)view.findViewById(R.id.tweet_retweeted_by);
			holder.txt_date_and_client = (TextView)view.findViewById(R.id.tweet_date_and_client);
			holder.txt_user_name = (TextView)view.findViewById(R.id.twitter_user_name);
			holder.txt_screen_name = (TextView)view.findViewById(R.id.twitter_screen_name);
			holder.txt_tweet_body = (TextView)view.findViewById(R.id.tweet_body);
			holder.txt_tweet_body.setLinkTextColor( 0xff3333ee );
			view.setTag(holder);
		}else{
			holder = (ViewHolder)view.getTag();
		}

		Status item = (Status)mItems.get(position);  
		TextView text;
		String str;
		ImageView image;

		User user = item.getUser();

		// リツイート記号表示
		holder.img_retweeted.setVisibility( item.isRetweet()?View.VISIBLE:View.GONE );
		// お気に入り記号表示
		// TODO お気に入り追加したときにお気に入りフラグが立たないので仮対処
//		holder.img_favorited.setVisibility( item.isFavorited()?View.VISIBLE:View.GONE );
		holder.img_favorited.setVisibility( FavoriteStatusWrapper.isFavorited(item)?View.VISIBLE:View.GONE );
		// in-reply-toアイコン表示
		holder.img_in_reply_to.setVisibility( item.getInReplyToStatusId()>0 ? View.VISIBLE:View.GONE );

		// リツイート元を表示
		text = holder.txt_retweeted_by;
		if( item.isRetweet() ){
			long n = item.getRetweetCount();
			if( n>1 ){
				String screenname = user.getScreenName();
				if( n>100 ){
					// str = screenname+"と100+人がリツイート";
					str = String.format(getContext().getString(R.string.over100users_retweet), screenname);
				}else{
					// str = user.getScreenName()+"と"+n+"人がリツイート";
					str = String.format(getContext().getString(R.string.n_users_retweet), screenname, n );
				}
			}else{
				// str = user.getScreenName()+"がリツイート";
				str = String.format(getContext().getString(R.string.a_user_retweet), user.getScreenName());
			}
			text.setText(str);
			user = item.getRetweetedStatus().getUser();
			item = item.getRetweetedStatus();
			text.setVisibility(View.VISIBLE);
		}else{
			text.setVisibility(View.GONE);
		}

		// 鍵付きユーザ表示
		holder.img_locked.setVisibility( user.isProtected()?View.VISIBLE:View.GONE );

		// アイコン
		image = holder.img_icon;
		Bitmap bmp = null;
		bmp = TweetnyanMainActivity.sIconCacheThread.loadIcon( item.getUser().getProfileImageURL(), item.getUser().getId() );
		if( bmp!=null ){
			image.setImageBitmap(bmp);
		}else{
			image.setImageResource(R.drawable.ic_dummy);
		}
		holder.position = position;
		image.setTag( view );

		// 名前
		text = holder.txt_user_name;
		text.setText(user.getName());

		// スクリーン名
		text = holder.txt_screen_name;  
		text.setText(user.getScreenName());

		// ツイート本体
		text = holder.txt_tweet_body;
		str = item.getText();
		str = Lib.encodeHTML(str);
		str = Lib.addLink(str);
		text.setText( Html.fromHtml(str) );

		// ツイートの文字色
//		if( is_readed ) text.setTextColor(0xFFa0a0a0);
//		else text.setTextColor(0xFFd8d8d8);
		
		// ジオロケーションタグ付き
		holder.img_mapdrop.setVisibility( item.getGeoLocation()==null ? View.GONE : View.VISIBLE );

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
		text.setText(date+" via "+str);

		if( mCheckTweetSelected!=null){
			if( mCheckTweetSelected.isSelected( item.getId() ) ){
				// 薄い青色
				view.setBackgroundColor(0x809ed2e5);
			}else{
				view.setBackgroundColor(0x00000000);
			}
		}

		// 自分宛を背景色グレーに
//		String myname = "amano_rox";	// FIXME 自分宛ての背景色指定について
//		if( item.getText().contains(myname) ){
//			view.setBackgroundColor(0x80555555);
//		}else{
//			view.setBackgroundColor(0x00000000);
//		}

		// TODO カラーラベルの設定
		text = (TextView)view.findViewById(R.id.color_label);
		text.setBackgroundColor( 0xff222222 );
		return view;
	}

}
