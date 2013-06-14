package jp.miku39.android.lib;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class IndeterminateProgressDialogFragment extends DialogFragment {
	
	public static IndeterminateProgressDialogFragment newInstance(String msg){
		IndeterminateProgressDialogFragment dialog = new IndeterminateProgressDialogFragment();
		Bundle args = new Bundle();
		args.putString("title", msg );
		dialog.setArguments(args);
		return dialog;		
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String str = getArguments().getString("title");
		Dialog d = CommonUtils.createIndeterminateProgressDialog( getActivity(), str);
		return d;
	}
	

}
