package com.tomclaw.appsend_rb.main.item;


/**
 * Created by ivsolkin on 09.01.17.
 */
public abstract class BaseItem {

    public static final int APP_ITEM = 0x00001;
    public static final int DONATE_ITEM = 0x01000;
    public static final int COUCH_ITEM = 0x10000;

    public abstract int getType();
}
