package com.morningdip.mjpegstreamer.upnp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import com.morningdip.mjpegstreamer.AndroidApplication;
import com.morningdip.mjpegstreamer.utils.NetworkUtils;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.device.DeviceChangeListener;

public class UpnpService extends Service {
    private UpnpThread thread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.destroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        thread = new UpnpThread();
        thread.start();
    }

    private static class UpnpThread extends Thread {
        private ControlPoint controlPoint;

        @Override
        public void destroy() {
            controlPoint.stop();
        }

        @Override
        public void run() {
            controlPoint = new ControlPoint();
            controlPoint.start();

            controlPoint.addDeviceChangeListener(new DeviceChangeListener() {
                @Override
                public void deviceAdded(Device dev) {
                    if (dev == null)
                        return;

                    String deviceType = dev.getDeviceType();

                    if (deviceType.equals(UpnpConstant.IGD)) {
                        AndroidApplication.curDevice = dev;

                        UpnpConstant.internalIpAddress = NetworkUtils.getLocalIp(AndroidApplication.getInstance());
                        UpnpConstant.externalIpAddress = UpnpCommand.GetExternalIPAddress(dev);

                        Message msg = Message.obtain();
                        msg.what = UpnpConstant.MSG.ip_done;
                        AndroidApplication.getInstance().sendMessage(msg);
                    }
                }

                @Override
                public void deviceRemoved(Device dev) {

                }
            });
        }
    }
}
