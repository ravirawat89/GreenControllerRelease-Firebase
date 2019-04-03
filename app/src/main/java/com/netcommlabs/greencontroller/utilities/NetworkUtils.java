package com.netcommlabs.greencontroller.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Netcomm on 10/21/2016.
 */

public class NetworkUtils {

    public static boolean isConnected(Context mContext) {
        if (mContext != null) {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        } else
            return false;

    }

}
