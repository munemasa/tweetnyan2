package jp.miku39.android.tweetnyan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class IconCacheThread  {
	private static final String TAG = "IconCacheThread";

	// アイコンダウンロード完了通知を受け取るためのインターフェイス
	public interface IIconDownloadedCallback {
		void onIconDownloaded( long id );
	}
	Stack<IIconDownloadedCallback> mIconDownloadCallback = new Stack<IIconDownloadedCallback>();
	public void pushCallback( IIconDownloadedCallback icallback ){
		mIconDownloadCallback.push( icallback );
	}
	public void popCallback(){
		try{
			mIconDownloadCallback.pop();
		}catch( EmptyStackException e ){
			e.printStackTrace();
		}
	}

	File mCacheDir;
	volatile boolean mIsThreadFinish = false;

	class IconCache {
		URI mUri;
		long mUserId;
		public IconCache(URI uri, long user_id) {
			mUri = uri;
			mUserId = user_id;
		}
	}
	ArrayList<IconCache> mDownloadIconQueue = new ArrayList<IconCache>();

	private Thread mThread;

	private Runnable mDownloaderThread = new Runnable(){
		@Override
		public void run() {
			while( true ){
				Log.d(TAG,"Wait icon downloading request...");
				synchronized(mDownloadIconQueue){
					try {
						mDownloadIconQueue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if( mIsThreadFinish ) break;

				while( true ){
					IconCache download;
					synchronized(mDownloadIconQueue){
						// キューが空になるまでひたすらループ
						try{
							download = mDownloadIconQueue.remove(0);
						}catch(IndexOutOfBoundsException e){
							break;
						}
					}

					File dir = mCacheDir;
					File f = new File( dir , ((Long)download.mUserId).toString()+download.mUri.toString().substring( download.mUri.toString().lastIndexOf("/")+1) );
					//Log.d(TAG,"Download and save icon: "+f.getAbsolutePath());
					if( f.exists() ){
						//Log.d(TAG,"Icon already exists.");
						continue;
					}

					HttpGet httpget;
					httpget = new HttpGet( download.mUri );
					DefaultHttpClient client = new DefaultHttpClient();
					try {
						// HTTP Download
						HttpResponse response = client.execute( httpget );
						int status = response.getStatusLine().getStatusCode();
						if ( status != HttpStatus.SC_OK ) continue;
	
						InputStream is = response.getEntity().getContent();
						OutputStream out=new FileOutputStream(f);
						byte buf[]=new byte[1024];
						int len;
						while((len=is.read(buf))>0){
							out.write(buf,0,len);
						}
						out.close();
						is.close();

						if( !mIconDownloadCallback.empty() ){
							//Log.d(TAG,"An icon download finished.");
							mIconDownloadCallback.peek().onIconDownloaded( download.mUserId );
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			Log.d(TAG,"Icon Downloader Thread is done.");
		}
	};

	/**
	 * ダウンロードを要求する.
	 * @param url アイコンのURL
	 * @param user_id ユーザーID
	 */
	public void requestDownload( String spec, long user_id ){
		IconCache icon;
		URL url;
		try {
			url = new URL(spec);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		try {
			synchronized(mDownloadIconQueue){
				URI uri = url.toURI();
				for(int i=0;i<mDownloadIconQueue.size(); i++){
					IconCache tmp = mDownloadIconQueue.get(i);
					if( tmp.mUri.compareTo( uri )==0 ){
						Log.d(TAG,"Icon downloading skipped.");
						return;
					}
				}
				icon = new IconCache( uri, user_id );
				mDownloadIconQueue.add( icon );
				mDownloadIconQueue.notifyAll();
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	HashMap<String,SoftReference<Bitmap>> mIconCache = new HashMap<String,SoftReference<Bitmap>>();
	public Bitmap loadIcon(String spec, long id){
		Bitmap icon;
		String idstr = ((Long)id).toString();
		URL url;
		try {
			url = new URL(spec);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		String fname = url.toString().substring( url.toString().lastIndexOf("/"));
		String key = idstr+"/"+fname;

		// メモリキャッシュをチェック
		SoftReference<Bitmap> ref = mIconCache.get(key);
		if( ref!=null ){
			icon = ref.get();
			if( icon!=null ) return icon;
		}

		File dir = mCacheDir;
		File f = new File( dir , ((Long)id).toString()+url.toString().substring( url.toString().lastIndexOf("/")+1) );

		//Log.d(TAG,"Icon file loading: "+f.toString());
		InputStream is;
		try {
			is = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
			return null;
		}
		icon = BitmapFactory.decodeStream( is );
		mIconCache.put(key, new SoftReference<Bitmap>(icon));
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return icon;
	}

	public IconCacheThread(Context cxt) {
		Log.d(TAG,"Construct IconCacheThread");

		mIsThreadFinish = false;

		mCacheDir = cxt.getExternalCacheDir();
		Log.d(TAG,"External Cache Dir: "+mCacheDir.getAbsolutePath());

		mThread = new Thread( mDownloaderThread );
		mThread.start();
	}

	public void destroy() {
		// TODO スレッドの停止処理
		synchronized(mDownloadIconQueue){
			mIsThreadFinish = true;
			mDownloadIconQueue.notify();
		}
		try {
			mThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.d(TAG,"destroy");
	}

}
