package com.netcommlabs.greencontroller.Interfaces;

import android.location.Location;

/**
 * Created by Android on 12/7/2017.
 */

public interface LocationDecetor {

        void OnLocationChange(Location location);
        void onErrors(String msg);

}
