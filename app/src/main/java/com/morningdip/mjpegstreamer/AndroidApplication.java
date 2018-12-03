package com.morningdip.mjpegstreamer;

import android.app.Application;
import android.os.Handler;
import android.os.Message;

import com.morningdip.mjpegstreamer.utils.MappingEntity;

import org.cybergarage.upnp.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AndroidApplication extends Application {
    private static AndroidApplication instance;
    private ConcurrentHashMap<String, Handler> mHandlerMap = new ConcurrentHashMap<>();
    public static Device curDevice = null;
    public static List<MappingEntity> itemList = new ArrayList<MappingEntity>();

    public static AndroidApplication getInstance(){
        return instance;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
    }

    public static void sendMessage(Message msg) {
        for (Handler handler : getInstance().getHandlerMap().values()) {
            handler.sendMessage(Message.obtain(msg));
        }
    }

    public ConcurrentHashMap<String, Handler> getHandlerMap() {
        return mHandlerMap;
    }
}
