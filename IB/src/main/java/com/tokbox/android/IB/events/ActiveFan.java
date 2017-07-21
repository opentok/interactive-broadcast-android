package com.tokbox.android.IB.events;

public class ActiveFan {
    private String id;

    public ActiveFan() {}


    public ActiveFan(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
