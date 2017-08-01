package com.tokbox.android.IB.events;

public class PrivateCall {
    private String isWith = "";

    public PrivateCall() {}

    public PrivateCall(String isWith) {
        this.isWith = isWith;
    }

    public String getIsWith() {
        return isWith;
    }

    public void setIsWith(String isWith) {
        this.isWith = isWith;
    }

}
