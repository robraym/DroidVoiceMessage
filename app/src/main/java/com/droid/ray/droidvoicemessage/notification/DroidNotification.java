package com.droid.ray.droidvoicemessage.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.droid.ray.droidvoicemessage.common.DroidCommon;
import com.droid.ray.droidvoicemessage.common.DroidPreferences;
import com.droid.ray.droidvoicemessage.tts.DroidTTS;

import static com.droid.ray.droidvoicemessage.common.DroidCommon.TAG;

/**
 * Created by Robson on 03/02/2016.
 */


public class DroidNotification extends DroidBaseNotification {
    private String msg;


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        processStatusBarNotification(sbn, "onNotificationPosted");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        try {
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            if (activeNotifications != null) {
                for (StatusBarNotification activeNotification : activeNotifications) {
                    processStatusBarNotification(activeNotification, "onListenerConnected");
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "onListenerConnected: " + ex.getMessage());
        }
    }

    private void processStatusBarNotification(StatusBarNotification sbn, String source) {
        try {
            msg = getNotificationKitKat(sbn);
            if (DroidCommon.QueueNotification(msg)) {
                Log.d(TAG, source + ": " + msg);
                DroidCommon.WaitingEndCallEndService(getBaseContext());
            }
        } catch (Exception ex) {
            Log.d(TAG, source + ": " + ex.getMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getNotificationKitKat(StatusBarNotification mStatusBarNotification) {
        String tit;
        String notif = "";
        String pack = mStatusBarNotification.getPackageName();// Package Name
        if (pack.contains("com.whatsapp") ||
                pack.contains("com.android.mms") ||
                pack.contains("com.facebook.orca")) {
            Bundle extras = mStatusBarNotification.getNotification().extras;
            if (extras == null) {
                return "";
            }

            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            if (title == null) {
                return "";
            }

            tit = title.toString().trim().replace(":", ""); // Title

            if (DroidCommon.FilterTitleNotification(tit)) {
                CharSequence desc = extras.getCharSequence(Notification.EXTRA_TEXT); // / Description
                if (desc == null) {
                    desc = "";
                }
                String notificationContactName = getNotificationContactName(extras);

                try {
                    Bundle bigExtras = mStatusBarNotification.getNotification().extras;
                    CharSequence[] descArray = bigExtras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                    if (descArray != null && descArray.length > 0) {
                        notif = descArray[descArray.length - 1].toString();
                    } else {
                        notif = desc.toString();
                    }

                } catch (Exception ex) {
                    notif = desc.toString();
                }

                if (!TextUtils.isEmpty(notif)) {
                    notif = DroidCommon.SetNotification(getBaseContext(), tit, notif, notificationContactName);
                }

            }
        }
        return notif;
    }

    private String getNotificationContactName(Bundle extras) {
        String contactName = getMessagingSenderName(extras);
        if (!TextUtils.isEmpty(contactName)) {
            return contactName;
        }

        CharSequence conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
        if (conversationTitle != null) {
            return conversationTitle.toString();
        }

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        return title == null ? "" : title.toString();
    }

    private String getMessagingSenderName(Bundle extras) {
        try {
            Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (messages == null || messages.length == 0) {
                return "";
            }

            for (int index = messages.length - 1; index >= 0; index--) {
                if (!(messages[index] instanceof Bundle)) {
                    continue;
                }

                Bundle message = (Bundle) messages[index];
                CharSequence sender = message.getCharSequence("sender");
                if (!TextUtils.isEmpty(sender)) {
                    return sender.toString();
                }

                String senderPersonName = getSenderPersonName(message);
                if (!TextUtils.isEmpty(senderPersonName)) {
                    return senderPersonName;
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "getMessagingSenderName: " + ex.getMessage());
        }

        return "";
    }

    private String getSenderPersonName(Bundle message) {
        try {
            Object person = message.getParcelable("sender_person");
            if (person == null) {
                return "";
            }

            Object name = person.getClass().getMethod("getName").invoke(person);
            return name == null ? "" : name.toString();
        } catch (Exception ex) {
            Log.d(TAG, "getSenderPersonName: " + ex.getMessage());
            return "";
        }
    }
}


