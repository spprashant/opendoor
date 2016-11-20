package com.home.pr.opendoor;

/**
 * Created by Pr on 28/10/2016.
 */
public class DoorValue {
    private int value;

    public DoorValue (int x) {
        this.value=x;
    }
    public void setLock (int x) {
        this.value=x;
    }
    public int getLock (int x) {
        return this.value;
    }
}

