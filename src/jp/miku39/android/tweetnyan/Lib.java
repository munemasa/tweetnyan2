package jp.miku39.android.tweetnyan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import jp.miku39.android.lib.CommonUtils;
import jp.miku39.android.tweetnyan.fragments.DirectMessageTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.HomeTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.MentionTimelineFragment;
import jp.miku39.android.tweetnyan.fragments.UserListTimelineFragment;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

public class Lib {
	final static String TAG = "Lib";
	
	static Twitter sTwitter;

	static volatile private int sCurrentAccountInex = -1;

	public static void setCurrentAccountIndex(Context ctx, int i){
		// アカウント選択で選択したときに呼び出される
		sCurrentAccountInex = i;
		CommonUtils.setIntegerValue(ctx, Consts.sPrefsCurrentAccountKey, i);
	}
	public static int getCurrentAccountIndex(Context ctx){
		if( sCurrentAccountInex<0 ){
			Log.d(TAG,"Get Current Account Index from Preferences");
			sCurrentAccountInex = CommonUtils.getIntegerValue(ctx, Consts.sPrefsCurrentAccountKey);
		}
		return sCurrentAccountInex;
	}
	public static String getCurrentAccountScreenName(Context ctx){
		int n = Lib.getCurrentAccountIndex(ctx);
		return CommonUtils.getStringValue(ctx, "username-"+n );
	}
	public static long getCurrentAccountUserId(Context ctx){
		int n = Lib.getCurrentAccountIndex(ctx);
		return CommonUtils.getLongValue(ctx, "userid-"+n);
	}

	/**
	 * Twitterインスタンスの生成.
	 * @return
	 */
	public static Twitter createTwitter(){
		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer( OAuthParam.sConsumerKey, OAuthParam.sConsumerSecret );
		return twitter;
	}
	/**
	 * Twitterインスタンスの生成.
	 * @param accesstoken
	 * @return
	 */
	public static Twitter createTwitter(AccessToken accesstoken){
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(OAuthParam.sConsumerKey)
		  .setOAuthConsumerSecret(OAuthParam.sConsumerSecret)
		  .setOAuthAccessToken(accesstoken.getToken())
		  .setOAuthAccessTokenSecret(accesstoken.getTokenSecret())
		  .setHttpConnectionTimeout( 20*1000 )
		  .setHttpReadTimeout( 60*1000 );

		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();	
		return twitter;
	}
	/**
	 * Twitterインスタンスの生成.
	 * @param ctx
	 * @return
	 */
	public static Twitter createTwitter(Context ctx){
	    int tmp = Lib.getCurrentAccountIndex(ctx);
	    AccessToken accesstoken = Lib.loadAccessToken(ctx, tmp);
	    Twitter twitter = Lib.createTwitter(accesstoken);
	    sTwitter = twitter;
	    return twitter;
	}

	public static Twitter getTwitter(){
		return sTwitter;
	}

	/**
	 * AccessTokenを保存.
	 * 認証に成功したときに呼び出される。
	 * @param ctx
	 * @param accesstoken
	 * @param n
	 */
	public static void saveAccessToken(Context ctx, AccessToken accesstoken, int n){
		//String token = accesstoken.getToken();
		//String tokenSecret = accesstoken.getTokenSecret();
		//Log.d(TAG,"Save AccessToken:"+token+" / "+tokenSecret);

		CommonUtils.writeObject( ctx, "accesstoken-"+n, accesstoken );
		CommonUtils.setStringValue(ctx, "username-"+n, accesstoken.getScreenName() );
		CommonUtils.setLongValue(ctx, "userid-"+n, accesstoken.getUserId() );
		Log.d(TAG,"Save Access Token (ID="+accesstoken.getUserId()+")");
	}

	public static AccessToken loadAccessToken(Context ctx, int n){
		AccessToken accesstoken = (AccessToken) CommonUtils.readObject(ctx, "accesstoken-"+n);
		if( accesstoken!=null ){
			Log.d(TAG,"Load Access Token");
			//String token = accesstoken.getToken();
			//String tokenSecret = accesstoken.getTokenSecret();
			//Log.d(TAG,"Load AccessToken:"+token+" / "+tokenSecret);
			return accesstoken;
		}
		return null;
	}
	
	public static void removeAccessToken(Context ctx, int n){
		CommonUtils.setStringValue(ctx, "username-"+n, "");
		CommonUtils.setLongValue(ctx, "userid-"+n, -1);
		CommonUtils.deleteFile(ctx, "accesstoken-"+n);

		// なんか微妙な方法で消す
		CommonUtils.deleteFile(ctx, HomeTimelineFragment.sSaveFileName+n);
		CommonUtils.deleteFile(ctx, MentionTimelineFragment.sSaveFileName+n);
		CommonUtils.deleteFile(ctx, DirectMessageTimelineFragment.sSaveFileName+n);
		CommonUtils.deleteFile(ctx, UserListTimelineFragment.sSaveFileName+n);
		CommonUtils.deleteFile(ctx, UserListTimelineFragment.sSaveFileNameUserList+n);
	}

	// Twitterのユーザー単位で保存する用
	// ファイル名が name-n となる
	public static void writeUserObject(Context ctx, String name, Object obj){
		int n = Lib.getCurrentAccountIndex(ctx);
		CommonUtils.writeObject(ctx, name+n, obj);
	}
	public static Object readUserObject(Context ctx, String name){
		int n = Lib.getCurrentAccountIndex(ctx);
		return CommonUtils.readObject(ctx, name+n);
	}
	public static void removeUserObject(Context ctx, String name){
		int n = Lib.getCurrentAccountIndex(ctx);
		CommonUtils.deleteFile(ctx, name+n);
	}
	public static FileOutputStream openUserFileOutput(Context ctx, String name) throws FileNotFoundException{
		int n = Lib.getCurrentAccountIndex(ctx);
		return ctx.openFileOutput(name+n, Context.MODE_PRIVATE);
	}
	public static FileInputStream openUserFileInput(Context ctx, String name) throws FileNotFoundException {
		int n = Lib.getCurrentAccountIndex(ctx);
		return ctx.openFileInput(name+n);
	}

	public static TextView createLoadMoreTextview(Context cxt){
    	TextView tv = new TextView(cxt);
    	tv.setText( cxt.getString(R.string.load_more) );
    	tv.setGravity(Gravity.CENTER);
    	tv.setHeight(100);
    	return tv;
    }

	public static String encodeHTML(String str){
		str = str.replaceAll("&", "&amp;");
		str = str.replaceAll("\"", "&quot;");
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		return str;
	}

	public static String addLink(String str){
		str = str.replaceAll("((http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+)", "<a href=\"$0\">$0</a>");
		str = str.replaceAll("([^A-Za-z\\d_])@([A-Za-z\\d_]+)", "$1<a href=\"tweetnyanprof2:@$2\">@$2</a>");
		str = str.replaceAll("^@([A-Za-z\\d_]+)", "<a href=\"tweetnyanprof2:$0\">$0</a>");
		str = str.replaceAll("#(\\w+)", "<a href=\"tweetnyansearch2:$0\">$0</a>");
		return str;
	}

}
