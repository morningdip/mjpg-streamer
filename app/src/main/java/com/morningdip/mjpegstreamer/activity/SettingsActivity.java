package com.morningdip.mjpegstreamer.activity;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.Formatter;

import com.morningdip.mjpegstreamer.BuildConfig;
import com.morningdip.mjpegstreamer.R;
import com.morningdip.mjpegstreamer.upnp.UpnpConstant;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        populateCameras();
        updateSummaries();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        EditTextPreference portPref = (EditTextPreference) findPreference("port");
        try {
            Integer.parseInt(portPref.getText());
        } catch (NumberFormatException nfe) {
            portPref.setText("8080");
            sharedPreferences.edit().putString("port", "8080").apply();
        }

        populateResolutions();
        updateSummaries();
    }

    private void populateCameras() {
        ListPreference camPref = (ListPreference) findPreference("cam");
        int cams = Camera.getNumberOfCameras();
        List<CharSequence> camEntries = new ArrayList<>();
        List<CharSequence> camEntryValues = new ArrayList<>();
        for (int i = 0; i < cams; i++) {
            if (i == 0) {
                camEntries.add(getResources().getString(R.string.camera_0));
            } else {
                camEntries.add(getResources().getString(R.string.camera_1));
            }

            camEntryValues.add(String.valueOf(i));
        }
        CharSequence[] entries = new CharSequence[camEntries.size()],
                entryValues = new CharSequence[camEntryValues.size()];
        entries = camEntries.toArray(entries);
        entryValues = camEntryValues.toArray(entryValues);
        camPref.setEntries(entries);
        camPref.setEntryValues(entryValues);

        populateResolutions();
    }

    private void populateResolutions() {
        ListPreference camPref = (ListPreference) findPreference("cam");
        int camId = Integer.parseInt(camPref.getValue());
        ListPreference resPref = (ListPreference) findPreference("resolution");

        List<Camera.Size> sizes = MainActivity.getCameraSizes().get(camId);
        if (sizes == null) sizes = new ArrayList<>();

        List<CharSequence> resEntries = new ArrayList<>();
        List<CharSequence> resEntryValues = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size s = sizes.get(i);
            resEntries.add(s.width + "x" + s.height);
            resEntryValues.add(s.width + "x" + s.height);
        }
        CharSequence[] entries = new CharSequence[resEntries.size()];
        CharSequence[] entryValues = new CharSequence[resEntryValues.size()];
        entries = resEntries.toArray(entries);
        entryValues = resEntryValues.toArray(entryValues);
        resPref.setEntries(entries);
        resPref.setEntryValues(entryValues);
    }

    private void updateSummaries() {
        EditTextPreference publicPref = (EditTextPreference) findPreference("public_ip");
        publicPref.setSummary(UpnpConstant.externalIpAddress);

        EditTextPreference privatePref = (EditTextPreference) findPreference("private_ip");
        privatePref.setSummary(getIp());

        EditTextPreference portPref = (EditTextPreference) findPreference("port");
        portPref.setSummary(portPref.getText());

        ListPreference camPref = (ListPreference) findPreference("cam");
        if (Integer.parseInt(camPref.getValue()) == 0) {
            camPref.setSummary(R.string.camera_0);
        } else {
            camPref.setSummary(R.string.camera_1);
        }

        ListPreference resPref = (ListPreference) findPreference("resolution");
        resPref.setSummary(resPref.getValue().replace("x", " x "));

        ListPreference rotPref = (ListPreference) findPreference("rotation");
        rotPref.setSummary(rotPref.getEntry());

        EditTextPreference versionPref = (EditTextPreference) findPreference("app_version");
        versionPref.setSummary(BuildConfig.VERSION_NAME);
    }

    public String getIp() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wifiMgr.getConnectionInfo().getIpAddress());
    }
}
