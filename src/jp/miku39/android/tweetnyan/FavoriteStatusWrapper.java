package jp.miku39.android.tweetnyan;

import java.util.HashMap;

import twitter4j.Status;

public class FavoriteStatusWrapper {
	public static HashMap<Long, Boolean> sIsFavorite = new HashMap<Long, Boolean>();
	
	public static boolean isFavorited( Status st ){
		if( st.isFavorited()==false ){
			Boolean b = sIsFavorite.get( st.getId() );
			if( b!=null ) return b;
		}
		return st.isFavorited();
	}

	public static void setFavorite( Status st, boolean b ){
		sIsFavorite.put( st.getId(), b );
	}
}
