package com.netcommlabs.greencontroller.model;

/**
 * Created by Android on 12/13/2017.
 */

public class MdlValveNameStateNdSelect {

    private String valveName;
    private String valveSelected;
    private String valveState;

    public String getValveState() {
        return valveState;
    }

    public void setValveState(String valveState) {
        this.valveState = valveState;
    }

    public MdlValveNameStateNdSelect(String valveName, String valveSelected, String valveState) {
        this.valveName = valveName;
        this.valveSelected = valveSelected;
        this.valveState = valveState;
    }

    public String getValveSelected() {
        return valveSelected;
    }

    public void setValveSelected(String valveSelected) {
        this.valveSelected = valveSelected;
    }

    public String getValveName() {
        return valveName;
    }

    public void setValveName(String valveName) {
        this.valveName = valveName;
    }
}
