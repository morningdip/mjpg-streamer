package com.morningdip.mjpegstreamer.utils;

import android.os.Handler;

import com.morningdip.mjpegstreamer.AndroidApplication;

public class MyController {
    private String mName;

    public MyController(String name, Handler handler) {
        AndroidApplication.getInstance().getHandlerMap().put(name, handler);
        this.mName = name;
    }

    public void destroy() {
        AndroidApplication.getInstance().getHandlerMap().remove(mName);
    }
}
