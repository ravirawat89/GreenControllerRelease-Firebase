package com.netcommlabs.greencontroller.utilities;

/**
 * Created by Android on 7/12/2016.
 */
public class Navigation_Drawer_Data {

    int flat_icon_drawer;
    String label_drawer;


    public Navigation_Drawer_Data(int flat_icon_drawer, String label_drawer) {

        this.flat_icon_drawer=flat_icon_drawer;
        this.label_drawer=label_drawer;
    }

    public int getflat_icon_drawer() {
        return flat_icon_drawer;
    }

    public String getlabel_drawer() {
        return label_drawer;
    }
}
