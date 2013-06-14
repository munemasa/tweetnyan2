package jp.miku39.android.tweetnyan.services;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import twitter4j.Status;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TwitterService extends Service {
	private static final String TAG = "TwitterService";

	volatile HashMap<Long,SoftReference<Status>> mStatusCache = new HashMap<Long,SoftReference<Status>>();

	public Status loadStatusFromCache(long id) {
		SoftReference<Status> ref = mStatusCache.get( (Long)id );
		if( ref!=null ) return ref.get();
		return null;
	}
	public void storeStatusToCache(Status st){
		mStatusCache.put( (Long)st.getId(), new SoftReference<Status>(st) );
	}
	
	private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
    	public TwitterService getService() {
            return TwitterService.this;
        }
    }
    @Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG,"onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"onDestroy");
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Log.d(TAG,"onRebind");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG,"onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG,"onUnbind");
		return super.onUnbind(intent);
	}

}
