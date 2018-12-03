package com.morningdip.mjpegstreamer.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.morningdip.mjpegstreamer.AndroidApplication;
import com.morningdip.mjpegstreamer.R;
import com.morningdip.mjpegstreamer.mjpeg.MjpegServer;
import com.morningdip.mjpegstreamer.upnp.UpnpCommand;
import com.morningdip.mjpegstreamer.upnp.UpnpConstant;
import com.morningdip.mjpegstreamer.upnp.UpnpService;
import com.morningdip.mjpegstreamer.utils.MyController;
import com.morningdip.mjpegstreamer.utils.Rotator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder holder;
    private Camera camera;
    private boolean previewRunning = false;
    private int camId = 0;

    private static String port;

    private ByteArrayOutputStream previewStream = new ByteArrayOutputStream();
    private int rotationSteps = 0;
    private boolean aboveLockScreen = true;

    private static MjpegServer mjpegServer = new MjpegServer();
    private static Thread serverThread = new Thread(mjpegServer);

    private static HashMap<Integer, List<Camera.Size>> cameraSizes = new HashMap<>();
    private static ReentrantReadWriteLock frameLock = new ReentrantReadWriteLock();
    private static byte[] jpegFrame;

    public static byte[] getJpegFrame() {
        try {
            frameLock.readLock().lock();
            return jpegFrame;
        } finally {
            frameLock.readLock().unlock();
        }
    }

    public static HashMap<Integer, List<Camera.Size>> getCameraSizes() {
        return cameraSizes;
    }

    private static void setJpegFrame(ByteArrayOutputStream stream) {
        try {
            frameLock.writeLock().lock();
            jpegFrame = stream.toByteArray();
        } finally {
            frameLock.writeLock().unlock();
        }
    }

    private void cacheResolutions() {
        int cams = Camera.getNumberOfCameras();
        for (int i = 0; i < cams; i++) {
            Camera cam = Camera.open(i);
            Camera.Parameters params = cam.getParameters();
            cameraSizes.put(i, params.getSupportedPreviewSizes());
            cam.release();
        }
    }

    private static final int ANIMATION_DURATION = 300;
    private static final float ROTATION_ANGLE = -90f;
    private AnimatorSet mOpenAnimatorSet;
    private AnimatorSet mCloseAnimatorSet;
    private FloatingActionMenu menu;
    private FloatingActionButton fab_camera;
    private FloatingActionButton fab_setting;
    private FloatingActionButton fab_remove_port;

    private MyController myController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cacheResolutions();
        setFabMenuAnimation();
        setFabButtons();

        Handler handler = new MainHandler(MainActivity.this);
        myController = new MyController(MainActivity.class.getName(), handler);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surfaceView);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    private void setFabButtons() {
        fab_camera = (FloatingActionButton) findViewById(R.id.fab_camera);
        fab_setting = (FloatingActionButton) findViewById(R.id.fab_setting);
        fab_remove_port = (FloatingActionButton) findViewById(R.id.fab_remove_port);

        final Drawable camera_front = ContextCompat.getDrawable(MainActivity.this,
                R.drawable.camera_front);
        final Drawable camera_rear = ContextCompat.getDrawable(MainActivity.this,
                R.drawable.camera_rear);

        fab_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int cams = Camera.getNumberOfCameras();
                camId++;
                if (camId > cams - 1) camId = 0;
                if (previewRunning) stopPreview();
                if (camera != null) camera.release();
                camera = null;

                if (camId == 0) {
                    fab_camera.setImageDrawable(camera_rear);
                } else {
                    fab_camera.setImageDrawable(camera_front);
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                preferences.edit().putString("cam", String.valueOf(camId)).apply();

                openCamAndPreview();
            }
        });

        fab_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        fab_remove_port.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    public void run() {
                        UpnpCommand.DeletePortMapping(AndroidApplication.curDevice, port);
                    }
                }.start();
            }
        });


    }

    private void setFabMenuAnimation() {
        menu = (FloatingActionMenu) findViewById(R.id.floating_menu);
        mOpenAnimatorSet = new AnimatorSet();
        mCloseAnimatorSet = new AnimatorSet();

        ObjectAnimator collapseAnimator =  ObjectAnimator.ofFloat(menu.getMenuIconView(),
                "rotation",
                -90f + ROTATION_ANGLE, 0f);
        ObjectAnimator expandAnimator = ObjectAnimator.ofFloat(menu.getMenuIconView(),
                "rotation",
                0f, -90f + ROTATION_ANGLE);

        final Drawable clearDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.clear);
        final Drawable menuDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.menu);

        expandAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                menu.getMenuIconView().setImageDrawable(clearDrawable);
                menu.setIconToggleAnimatorSet(mCloseAnimatorSet);
            }
        });

        collapseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                menu.getMenuIconView().setImageDrawable(menuDrawable);
                menu.setIconToggleAnimatorSet(mOpenAnimatorSet);
            }
        });

        mOpenAnimatorSet.play(expandAnimator);
        mCloseAnimatorSet.play(collapseAnimator);

        mOpenAnimatorSet.setDuration(ANIMATION_DURATION);
        mCloseAnimatorSet.setDuration(ANIMATION_DURATION);

        menu.setIconToggleAnimatorSet(mOpenAnimatorSet);
    }

    public static String getIp() {
        WifiManager wifiMgr = (WifiManager) AndroidApplication.getInstance().getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wifiMgr.getConnectionInfo().getIpAddress());
    }

    private static class MainHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        public MainHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = weakReference.get();
            if (activity == null)
                return;
            if (activity.isFinishing())
                return;

            switch (msg.what) {
                case UpnpConstant.MSG.ip_done: {
                    new Thread() {
                        public void run() {
                            UpnpCommand.addPortMapping(AndroidApplication.curDevice, port, getIp());
                        }
                    }.start();

                    break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (UpnpConstant.internalIpAddress != null && port != null) {
            new Thread() {
                public void run() {
                    UpnpCommand.DeletePortMapping(AndroidApplication.curDevice, port);
                }
            }.start();
        }

        stopService(new Intent(MainActivity.this, UpnpService.class));
        startService(new Intent(MainActivity.this, UpnpService.class));

        loadPreferences();
        openCamAndPreview();

        if (!serverThread.isAlive()) {
            serverThread.start();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (aboveLockScreen)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }


    @Override
    public void onBackPressed() {
        new Thread() {
            public void run() {
                UpnpCommand.DeletePortMapping(AndroidApplication.curDevice, port);
            }
        }.start();

        stopService(new Intent(MainActivity.this, UpnpService.class));

        this.finish();
        System.exit(0);
    }

    private void openCamAndPreview() {
        try {
            if (camera == null) camera = Camera.open(camId);
            startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        camId = Integer.parseInt(preferences.getString("cam", "0"));

        port = preferences.getString("port", "8080");

        Integer rotDegrees = Integer.parseInt(preferences.getString("rotation", "0"));
        rotationSteps = rotDegrees / 90;
        Integer port = Integer.parseInt(preferences.getString("port", "8080"));
        MjpegServer.setPort(port);
        aboveLockScreen = preferences.getBoolean("above_lock_screen", aboveLockScreen);
        Boolean allIps = preferences.getBoolean("allow_all_ips", false);
        MjpegServer.setAllIpsAllowed(allIps);
    }

    private void startPreview() {
        if (previewRunning) stopPreview();

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0) {
            camera.setDisplayOrientation(90);
        } else if (display.getRotation() == Surface.ROTATION_270) {
            camera.setDisplayOrientation(180);
        } else {
            camera.setDisplayOrientation(0);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String res = preferences.getString("resolution", "640x480");
        String[] resParts = res.split("x");

        Camera.Parameters params = camera.getParameters();

        // Set Preview Size
        params.setPreviewSize(Integer.parseInt(resParts[0]), Integer.parseInt(resParts[1]));

        // Set Auto Focus
        params.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
        camera.setParameters(params);

        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.startPreview();
        holder.addCallback(this);

        previewRunning = true;
    }

    private void stopPreview() {
        if (!previewRunning) return;

        holder.removeCallback(this);
        camera.stopPreview();
        camera.setPreviewCallback(null);

        previewRunning = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        stopPreview();

        if (camera != null) camera.release();
        camera = null;

        openCamAndPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        openCamAndPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopPreview();
        if (camera != null) camera.release();
        camera = null;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        previewStream.reset();
        Camera.Parameters p = camera.getParameters();

        int previewHeight = p.getPreviewSize().height;
        int previewWidth = p.getPreviewSize().width;

        switch(rotationSteps) {
            case 1:
                bytes = Rotator.rotateYUV420Degree90(bytes, previewWidth, previewHeight);
                break;
            case 2:
                bytes = Rotator.rotateYUV420Degree180(bytes, previewWidth, previewHeight);
                break;
            case 3:
                bytes = Rotator.rotateYUV420Degree270(bytes, previewWidth, previewHeight);
                break;
        }

        if (rotationSteps == 1 || rotationSteps == 3) {
            int tmp = previewHeight;
            previewHeight = previewWidth;
            previewWidth = tmp;
        }

        int format = p.getPreviewFormat();
        YuvImage image = new YuvImage(bytes, format, previewWidth, previewHeight, null);
        image.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), 30, previewStream);

        setJpegFrame(previewStream);
    }
}
