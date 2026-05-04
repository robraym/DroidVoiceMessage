package com.droid.ray.droidvoicemessage.service;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.droid.ray.droidvoicemessage.activity.MainActivity;
import com.droid.ray.droidvoicemessage.common.DroidCommon;
import com.droid.ray.droidvoicemessage.common.DroidPreferences;

/**
 * Created by Robson on 29/01/2018.
 */

public class DroidPhoneService extends Service {
    private static final String TAG = "DroidVoiceMessage";
    private DroidPhoneReceiver phoneReceiver;

    //bluetouch
    private static final int NOTE_ID = 1000;

    public static final String ACTION_STARTVOICE = "com.example.BluetoothAudioProxy.action.STARTVOICE";
    public static final String ACTION_STOPVOICE = "com.example.BluetoothAudioProxy.action.STOPVOICE";

    BluetoothAdapter mBluetoothAdapter;
    BluetoothHeadset mBluetoothHeadset;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        phoneReceiver = new DroidPhoneReceiver();

        //bluetouch
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
        //Monitor profile events
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mProfileReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(phoneReceiver, filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(phoneReceiver);

        //bluetouch
        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
        unregisterReceiver(mProfileReceiver);
    }

    private class DroidPhoneReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                String message= "";
                switch (state) {
                    case 0:
                        if ("1".equals(DroidPreferences.GetString(context, "foneOuvido"))) {
                            DroidPreferences.SetString(context, "foneOuvido", "0");
                            DroidCommon.ShowListener(context);
                            message = "Fone de ouvido desconectado";
                        }
                        break;
                    case 1:
                        DroidCommon.ShowListener(context);
                        message = "Fone de ouvido conectado";
                        DroidPreferences.SetString(context, "foneOuvido", "1");
                        break;
                    default:
                        Log.d(TAG, "Houve algum problema ao obter o estado do fone de ouvido");
                }
                if (!message.isEmpty()) {
               //     Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    //bluetouch
    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(TAG, "Connecting HeadsetService...");
                mBluetoothHeadset = (BluetoothHeadset) proxy;
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(TAG, "Unexpected Disconnect of HeadsetService...");
                mBluetoothHeadset = null;
            }
        }
    };

    private BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED");
                notifyConnectState(context, intent);
            }

        }
    };


    private void notifyConnectState(Context context, Intent intent) {
        final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
        String message = "";
        switch (state) {
            case BluetoothHeadset.STATE_CONNECTED:
                message = "Dispositivo bluetouch conectado";
                DroidPreferences.SetString(context, "foneBlueTouch", "1");
                DroidCommon.ShowListener(context);
                break;
            case BluetoothHeadset.STATE_CONNECTING:
                message = "Conectando o dispositivo bluetouch";
                break;
            case BluetoothHeadset.STATE_DISCONNECTING:
                message = "Disconectano o dispositivo bluetouch";
                break;
            case BluetoothHeadset.STATE_DISCONNECTED:
                if ("1".equals(DroidPreferences.GetString(context, "foneBlueTouch"))) {
                    DroidPreferences.SetString(context, "foneBlueTouch", "0");
                    DroidCommon.ShowListener(context);
                    message = "Dispositivo bluetouch desconectado";
                }
                break;
            default:
                message = "Dispositivo desconhecidos";
                break;
        }

        if (!message.isEmpty()) {
      //      Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            //   buildNotification(message);
        }
    }
}
