package jp.miku39.android.lib;

import jp.miku39.android.tweetnyan.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;


public class YesNoDialogFragment extends DialogFragment {
	final static String TAG = "YesNoDialogFragment";

	private String mMsg;
	
	public interface YesNoDialogFragmentCallback {
		public void doPositiveClick();
		public void doNegativeClick();
	};

	public static YesNoDialogFragment newInstance(String msg){
		YesNoDialogFragment dialog = new YesNoDialogFragment();
		Bundle args = new Bundle();
		args.putString("msg", msg );
		dialog.setArguments(args);
		return dialog;		
	}


    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        mMsg = getArguments().getString("msg");
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        MyAlertDialog myDialog = new MyAlertDialog( getActivity(), R.style.MyDialogTheme_Light );
		myDialog.setIcon(R.drawable.ic_launcher);
		myDialog.setMessage( mMsg );
	    myDialog.setButton(Dialog.BUTTON_POSITIVE, getString(android.R.string.yes),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	((YesNoDialogFragmentCallback)getActivity()).doPositiveClick();
                }
            }
        );
	    myDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(android.R.string.no),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	((YesNoDialogFragmentCallback)getActivity()).doNegativeClick();
                }
            }
        );
		return myDialog;
	}

}
