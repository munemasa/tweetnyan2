package jp.miku39.android.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

import jp.miku39.android.tweetnyan.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class CommonUtils {
	final static String sPrefsName = "prefs";
	
	public static void sleep(long ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static ProgressDialog createIndeterminateProgressDialog(Context ctx, String msg){
        ProgressDialog dialog = new ProgressDialog(ctx , R.style.MyDialogTheme_Light);
//        ProgressDialog dialog = new ProgressDialog(cxt , R.style.MyDialogTheme_Dark);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage( msg );
		return dialog;
	}

	public static void hideSoftwareKeyboard(Context ctx, View v){
		InputMethodManager inputMethodManager = (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}
	
	public static String getStringValue(Context ctx, String key){
		return ctx.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).getString(key, "");
	}
	public static void setStringValue(Context ctx, String key, String value){
		Editor edit = ctx.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).edit();
		edit.putString(key, value);
		edit.commit();
	}

	/**
	 * 設定から整数値を読み込む.
	 * @param ctx
	 * @param key
	 * @return 正の整数値を返す。値がkeyに保存されていなければ負の値を返す
	 */
	public static Integer getIntegerValue(Context ctx, String key){
		return ctx.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).getInt(key, -1);
	}
	public static void setIntegerValue(Context ctx, String key, Integer value){
		Editor edit = ctx.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).edit();
		edit.putInt(key, value);
		edit.commit();
	}

	public static Long getLongValue(Context ctx, String key){
		return ctx.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).getLong(key, -1);
	}
	public static void setLongValue(Context ctx, String key, long value) {
		Editor edit = ctx.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).edit();
		edit.putLong(key, value);
		edit.commit();
	}

	public static void deleteFile(Context ctx, String name){
		ctx.deleteFile(name);
	}

	public static void writeObject(Context ctx, String name, Object obj) {
		try {
			OutputStream os = ctx.openFileOutput( name, Context.MODE_PRIVATE );
			ObjectOutputStream oos;
			oos = new ObjectOutputStream( os );
			oos.writeObject( obj );
			oos.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Object readObject(Context ctx, String name) {
		Object obj = null;
		try {
			InputStream is = ctx.openFileInput( name );
			ObjectInputStream ois;
			ois = new ObjectInputStream( is );
			obj = ois.readObject();
			ois.close();
			is.close();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return obj;
	}

}
