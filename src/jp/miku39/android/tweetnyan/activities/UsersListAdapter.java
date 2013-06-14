package jp.miku39.android.tweetnyan.activities;

import java.util.List;

import jp.miku39.android.tweetnyan.R;
import jp.miku39.android.tweetnyan.R.drawable;
import jp.miku39.android.tweetnyan.R.id;
import jp.miku39.android.tweetnyan.R.layout;

import twitter4j.User;
import twitter4j.UserList;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UsersListAdapter extends ArrayAdapter<UserList>{

	final static String TAG = "UserListAdapter";
	Context cxt;

	private LayoutInflater mInflater;
	private List<UserList> mItems;

	public UsersListAdapter(Context context, int textViewResourceId, List<UserList> objects) {
		super(context, textViewResourceId, objects);
		mItems = objects;
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		cxt = context;
	}

	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;  
		if (view == null) {  
			view = mInflater.inflate(R.layout.one_list_layout, null);  
		}

		UserList item = mItems.get(position);
		User user = item.getUser();

		ImageView image = (ImageView)view.findViewById(R.id.profile_icon);
		Bitmap bmp = TweetnyanMainActivity.sIconCacheThread.loadIcon(user.getProfileImageURL(), user.getId());
		if( bmp!=null ){
			image.setImageBitmap(bmp);
		}else{
			image.setImageResource(R.drawable.ic_dummy);
		}

		TextView tv;
		tv = (TextView)view.findViewById(R.id.list_name);
		String str = item.getFullName();
		tv.setText( str );

		image = (ImageView)view.findViewById(R.id.icon_private);
		image.setVisibility( item.isPublic()?View.GONE:View.VISIBLE);

		tv = (TextView)view.findViewById(R.id.list_description);
		tv.setText( item.getDescription() );
		return view;
	}

}
