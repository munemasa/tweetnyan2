package jp.miku39.android.lib;

import jp.miku39.android.tweetnyan.R;
import android.app.AlertDialog;
import android.content.Context;

public class MyAlertDialog extends AlertDialog {
	public static MyAlertDialog newInstance(Context context){
		MyAlertDialog d = new MyAlertDialog(context, R.style.MyDialogTheme_Light);
		return d;
	}

	public MyAlertDialog(Context context) {  
		super(context);  
	}

	public MyAlertDialog(Context context, int theme) {  
		super(context, theme);  
	}
}
