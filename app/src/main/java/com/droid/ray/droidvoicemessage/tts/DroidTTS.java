package com.droid.ray.droidvoicemessage.tts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.droid.ray.droidvoicemessage.R;
import com.droid.ray.droidvoicemessage.activity.MainActivity;
import com.droid.ray.droidvoicemessage.common.DroidCommon;

import java.util.ArrayList;
import java.util.Locale;

import static com.droid.ray.droidvoicemessage.common.DroidCommon.TAG;

/**
 * Created by Robson on 22/09/2017.
 */

public class DroidTTS extends Service implements TextToSpeech.OnInitListener {
    private static final String CHANNEL_ID = "voice_message_playback";
    private static final int FOREGROUND_NOTIFICATION_ID = 2001;
    public static TextToSpeech tts;
    private Context context;
    private boolean ttsReady = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind ");
        return null;
    }

    @Override
    public void onInit(int i) {
        Log.d(TAG, "onInit ");
        ttsReady = true;
        StartLoopIfNeeded();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate ");
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
        InitializeObjects();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart ");
        StartLoopIfNeeded();
        return START_NOT_STICKY;
    }

    private void StartLoopIfNeeded() {
        if (!ttsReady || DroidCommon.LoopingNotification || tts == null || tts.isSpeaking() || !DroidCommon.HasPendingNotifications()) {
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    LoopingNotification();
                } catch (Exception ex) {
                    Log.d(TAG, "StartLoopIfNeeded: " + ex.getMessage());
                }
            }
        }).start();
    }

    private void LoopingNotification() {
        try {
            DroidCommon.LoopingNotification = true;
            ShowNotification();
            while (DroidCommon.HasPendingNotifications()) {
                ArrayList<String> mensagens = DroidCommon.GetPendingNotificationsSnapshot();
                for (String str : mensagens) {
                    if (DroidCommon.inCall || DroidCommon.forceBreak) {
                        return;
                    }

                    Speak(str);
                    DroidCommon.RemoveNotification(str);
                    DroidCommon.TimeSleep(500);
                }
            }
            ShowNotification();
        } finally {
            DroidCommon.LoopingNotification = false;
            if (!DroidCommon.HasPendingNotifications() || DroidCommon.inCall || DroidCommon.forceBreak) {
                stopSelf();
            }
        }

    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDestroy ");
        ResetObject();
        super.onDestroy();
    }


    private void InitializeObjects() {
        try {
            context = getBaseContext();
            tts = new TextToSpeech(context, this);
            tts.setLanguage(Locale.getDefault());
            Log.d(TAG, "InitializeObjects ");
        } catch (Exception ex) {
            Log.d(TAG, "InitializeObjects: " + ex.getMessage());
        }
    }

    private void ResetObject() {
        if (tts != null) {
            try {
                if (tts.isSpeaking()) {
                    tts.stop();
                }
                tts.shutdown();
            } catch (Exception ex) {
                Log.d(TAG, "ZerarObjeto: " + ex.getMessage());
            }
        }
    }


    private void Speak(final String texto) {
        try {
            //   Toast.makeText(context, texto, Toast.LENGTH_SHORT).show();
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null);
            Log.d(TAG, "Speak " + texto);

            while (tts.isSpeaking()) {
                Log.d(TAG, "isSpeaking?: " + texto + " " + tts.isSpeaking());
                DroidCommon.TimeSleep(500);
            }
            DroidCommon.TimeSleep(1000);
            Log.d(TAG, "isSpeaking?: " + texto + " " + tts.isSpeaking());


        } catch (Exception ex) {
            Log.d(TAG, "Speak: " + ex.getMessage());
        }
    }

    private static void ShowNotification() {
        Log.d(TAG, "--------------------------------------------");
        for (String str : DroidCommon.GetPendingNotificationsSnapshot()) {
            Log.d(TAG, "ShowNotification: " + str);
        }
    }

    private Notification buildForegroundNotification() {
        createNotificationChannel();

        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Lendo mensagens em voz alta")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Voice Message playback",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Foreground service used while reading messages aloud.");
        notificationManager.createNotificationChannel(channel);
    }

}
