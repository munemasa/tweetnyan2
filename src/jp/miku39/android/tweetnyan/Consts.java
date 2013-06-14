package jp.miku39.android.tweetnyan;

import com.actionbarsherlock.view.Menu;


public class Consts {
	public static final String sTwitpicAPIKey = "8b074f2aeaebf08e7894deee34c0f67e";
	
	public static final int sPicUploadTwitter = 0;
	public static final int sPicUploadTwitpic = 1;

	public static final int sNumOfTweets = 200;
	public static final int sMaxAccounts = 10;
	public static final int sMaxTweets = 2000;

	public static final int TAB_HOME 	= 0;
	public static final int TAB_MENTION = 1;
	public static final int TAB_DM 		= 2;
	public static final int TAB_LIST 	= 3;
	public static final int TAB_SEARCH 	= 4;	
	
	public final static int MOVE_NONE = 0;
	public final static int MOVE_TOP_OF_READED = 1;
	public final static int MOVE_TOP = 2;

	public static final String sPrefsCurrentAccountKey = "current-account";

	public static final int MENU_ID_NEW_TWEET 		= (Menu.FIRST + 1);	// New Tweet/DirectMessage
	public static final int MENU_ID_REFRESH_TIMELINE= (Menu.FIRST + 2);	// Refresh Timeline
	public static final int MENU_ID_MY_PROFILE 		= (Menu.FIRST + 3);	// My Profile
	public static final int MENU_ID_SWITCH_ACCOUNT 	= (Menu.FIRST + 4);	// Switch Account
	public static final int MENU_ID_SETTINGS 		= (Menu.FIRST + 5);	// Settings
	public static final int MENU_ID_ABOUT 			= (Menu.FIRST + 6);	// About
	public static final int MENU_ID_SAVE_THIS_SEARCH= (Menu.FIRST + 7); // Save This Search
	public static final int MENU_ID_DEL_THIS_SEARCH = (Menu.FIRST + 8); // Delete This Search
	public static final int MENU_ID_BLOCK 			= (Menu.FIRST + 9); // Block
	public static final int MENU_ID_REPORT_SPAM 	= (Menu.FIRST + 8); // Report spam
	public static final int MENU_ID_TAKE_PHOTO 		= (Menu.FIRST + 9); // Take a photo
	public static final int MENU_ID_ATTACH_PIC 		= (Menu.FIRST + 10); // Attach a picture
	public static final int MENU_ID_GET_GEOLOC 		= (Menu.FIRST + 11); // Get geolocation
	public static final int MENU_ID_CREATE_LIST 	= (Menu.FIRST + 12); // Create List
	public static final int MENU_ID_FOLLOW_THIS_LIST= (Menu.FIRST + 13); // Follow this List
	public static final int MENU_ID_UNFOLLOW_THIS_LIST=(Menu.FIRST + 14); // Unollow this List
	public static final int MENU_ID_DELETE_THIS_LIST= (Menu.FIRST + 15); // Delete this List
	public static final int MENU_ID_LIST_FOLLOWING  = (Menu.FIRST + 16); // Users List Following

	
}
