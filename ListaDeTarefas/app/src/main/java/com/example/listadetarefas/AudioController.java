package com.example.listadetarefas;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.Settings;

public class AudioController {

    private final Context context;
    private final AudioManager audioManager;

    public interface AudioConnectionListener {
        void onBluetoothConnected();
        void onBluetoothDisconnected();
    }

    public AudioController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean audioOutputAvailable(int type) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false;
        }

        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public void monitorarDispositivosAudio(final AudioConnectionListener listener) {
        audioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                if (audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
                    if (listener != null) listener.onBluetoothConnected();
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                if (!audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
                    if (listener != null) listener.onBluetoothDisconnected();
                }
            }
        }, null);
    }

    public void abrirConfiguracoesBluetooth() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_CONNECTION_ONLY", true);
        intent.putExtra("EXTRA_CLOSE_ON_CONNECT", true);
        intent.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1);
        context.startActivity(intent);
    }

    public void tocarSomAlerta(int somResId) {
        boolean temAltoFalante = audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        boolean temBluetooth = audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);

        if (temAltoFalante || temBluetooth) {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, somResId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });
                mediaPlayer.start();
            }
        }
    }
}