package com.tomclaw.appsend_rb.core;

import java.lang.ref.WeakReference;

/**
 * Created by solkin on 16.05.14.
 */
@SuppressWarnings("WeakerAccess")
public abstract class WeakObjectTask<W> extends Task {

    private final WeakReference<W> weakObject;

    public WeakObjectTask(W object) {
        this.weakObject = new WeakReference<W>(object);
    }

    public W getWeakObject() {
        return weakObject.get();
    }
}
