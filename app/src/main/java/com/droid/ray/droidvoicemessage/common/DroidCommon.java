package com.droid.ray.droidvoicemessage.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Window;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

import com.droid.ray.droidvoicemessage.R;
import com.droid.ray.droidvoicemessage.service.DroidPhoneService;
import com.droid.ray.droidvoicemessage.tts.DroidTTS;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Robson on 04/08/2017.
 */

public class DroidCommon {
    private static Context contextCommon;
    public static final String TAG = "VoiceMessage";
    public static final String ACTION_CONTACTS_CHANGED = "com.droid.ray.droidvoicemessage.ACTION_CONTACTS_CHANGED";
    public static ArrayList<String> Notification = new ArrayList<>();
    public static ArrayList<String> AllNotification = new ArrayList<>();
    private static final Object NOTIFICATION_LOCK = new Object();
    private static final int MAX_RECENT_NOTIFICATIONS = 120;
    private static final String CONTACT_GROUP_UNKNOWN = "?";
    private static final HashMap<String, String> contactPhoneByDuplicateKey = new HashMap<>();
    public static Boolean inCall = false;
    public static Boolean InThread = false;
    public static Boolean LoopingNotification = false;

    public static final int PERMISSION_ALL = 2;
    public static final String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE};
    public static boolean forceBreak = false;
    private static final HashSet<String> expandedContactGroups = new HashSet<>();

    public static void EnviaMsg(Context context) {
        try {
            if (context != null) {
                contextCommon = context.getApplicationContext();
            }
            if (HasPendingNotifications()) {
                Log.d(TAG, "EnviaMsg - startService");
                StartService();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Erro EnviaMsg " + ex.getMessage());
        }
    }

    private static void StopService() {
        try {
            Log.d(TAG, "StopService");
            contextCommon.stopService(new Intent(contextCommon, DroidTTS.class));
            TimeSleep(500);
        } catch (Exception ex) {
            Log.d(TAG, "stopService: " + ex.getMessage());
        }
    }

    public static void StartService() {

        try {
            Log.d(TAG, "StartService");
            ContextCompat.startForegroundService(contextCommon, new Intent(contextCommon, DroidTTS.class));
            TimeSleep(500);
        } catch (Exception ex) {
            Log.d(TAG, "startService: " + ex.getMessage());
        }
    }

    public static void WaitingEndCallEndService(final Context context) {
        if (DroidCommon.inCall) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (DroidCommon.inCall) {
                            Log.d(TAG, "WaitingEndCall");
                            DroidCommon.TimeSleep(2000);
                        }
                    } catch (Exception ex) {
                        Log.d(TAG, "WaitingEndCall: " + ex.getMessage());
                    }
                }
            }).start();
        } else if (DroidTTS.tts != null && DroidTTS.tts.isSpeaking()) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        DroidCommon.InThread = true;
                        if (DroidTTS.tts != null) {
                            while (DroidTTS.tts.isSpeaking()) {
                                Log.d(TAG, "WaitingEndService");
                                DroidCommon.TimeSleep(500);
                            }
                        }
                        //  StopService();
                    } finally {
                        DroidCommon.InThread = false;
                    }
                }
            }).start();
        } else if (!DroidCommon.InThread) {
            EnviaMsg(context);
        }
    }

    public static String SetNotification(String tit, String notif) {
        String contato = "";
        if (contextCommon == null || TextUtils.isEmpty(tit) || TextUtils.isEmpty(notif)) {
            return "";
        }

        if (!tit.toLowerCase().equals("whatsapp")) {
            int indexGroup;
            int indexVideo;
            try {
                indexVideo = notif.indexOf("Vídeo (");
                if (indexVideo > 0)
                {
                    try {
                        notif = notif.substring(0, indexVideo + 7) + "00:" + notif.substring(indexVideo + 7);
                    }
                    catch (Exception ex)
                    {
                    }
                }
                indexGroup = tit.indexOf("@");
                if (indexGroup > 0) {
                    contato = tit.substring(0, indexGroup).trim();
                } else contato = tit;
                String resolvedContact = resolveContactName(contextCommon, contato);
                if (!TextUtils.isEmpty(resolvedContact) && !resolvedContact.equals(contato)) {
                    if (indexGroup > 0) {
                        tit = resolvedContact + tit.substring(indexGroup);
                    } else {
                        tit = resolvedContact;
                    }
                    contato = resolvedContact;
                }
                tit = tit.replace("@", " grupo ");
                notif = tit + " " + notif;
                String PrefNotif = DroidPreferences.GetString(contextCommon, contato);
                if (PrefNotif.equals("") && !notif.trim().isEmpty()) {
                    if (!contato.contains("(")) {
                        DroidPreferences.SetString(contextCommon, contato, "N");
                        NotifyContactsChanged(contextCommon);
                    }
                }
                if (!PrefNotif.equals("S")) {
                    notif = "";
                }

            } catch (Exception ex) {
                Log.d(TAG, "DroidPreferences: " + ex.getMessage());
                notif = "";
            }
        } else notif = "";

        String notifLower = notif.toLowerCase();
        if (WasNotificationRecentlyQueued(notif) || notifLower.equals("procurando novas mensagens") || notifLower.equals("mensagens está em execução")) {
            notif = "";
        }
        return notif;
    }

    public static String SetNotification(Context context, String tit, String notif) {
        if (context != null) {
            contextCommon = context.getApplicationContext();
        }
        return SetNotification(tit, notif);
    }

    public static String SetNotification(Context context, String tit, String notif, String notificationContactName) {
        if (context != null) {
            contextCommon = context.getApplicationContext();
        }

        if (!TextUtils.isEmpty(notificationContactName)) {
            String titleContact = getNotificationContactTitle(tit);
            notificationContactName = sanitizeContactName(notificationContactName);
            if (isLikelyPhoneNumber(titleContact) && !isLikelyPhoneNumber(notificationContactName)) {
                rememberContactPhone(notificationContactName, titleContact);
                migratePhoneNumberPreference(contextCommon, titleContact, notificationContactName);
                tit = replaceNotificationContactTitle(tit, titleContact, notificationContactName);
            }
        }

        return SetNotification(tit, notif);
    }

    public static boolean FilterTitleNotification(String msg) {
        String titleMsg = msg.toLowerCase();
        return !titleMsg.equals("ícones de bate-papo ativos") &&
                !titleMsg.contains("mensagens):") &&
                !titleMsg.contains("whatsapp web") &&
                !titleMsg.equals("mensagens está em execução");

    }

    public static void ShowListener(Context context) {
        Intent mIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mIntent);
    }

    public static void StartPhoneService(Context context) {
        Intent intent = new Intent(context, DroidPhoneService.class);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void NotifyContactsChanged(Context context) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(ACTION_CONTACTS_CHANGED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public static void TimeSleep(Integer seg) {
        try {
            Thread.sleep(seg);
        } catch (Exception ex) {
        }
    }

    public static void AddNotification(String notif) {
        try {
            synchronized (NOTIFICATION_LOCK) {
                if (!TextUtils.isEmpty(notif) && !DroidCommon.Notification.contains(notif)) {
                    Notification.add(notif);
                    Log.d(TAG, "AddNotification: " + notif);
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error AddNotification: " + ex.getMessage());
        }
    }

    public static void AddAllNotification(String notif) {
        try {
            synchronized (NOTIFICATION_LOCK) {
                if (!TextUtils.isEmpty(notif) && !DroidCommon.AllNotification.contains(notif)) {
                    AllNotification.add(notif);
                    trimRecentNotificationsLocked();
                    Log.d(TAG, "AddAllNotification: " + notif);
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error AddAllNotification: " + ex.getMessage());
        }
    }

    public static boolean QueueNotification(String notif) {
        try {
            synchronized (NOTIFICATION_LOCK) {
                if (TextUtils.isEmpty(notif) || DroidCommon.Notification.contains(notif) || DroidCommon.AllNotification.contains(notif)) {
                    return false;
                }

                Notification.add(notif);
                AllNotification.add(notif);
                trimRecentNotificationsLocked();
                Log.d(TAG, "QueueNotification: " + notif);
                return true;
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error QueueNotification: " + ex.getMessage());
            return false;
        }
    }

    public static void RemoveNotification(String msg) {
        try {
            synchronized (NOTIFICATION_LOCK) {
                int pos = Notification.indexOf(msg);
                if (pos >= 0) {
                    Notification.remove(pos);
                    Log.d(TAG, "RemoveNotification: " + msg);
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error RemoveNotification: " + ex.getMessage());
        }

    }

    public static void RemovePendingNotificationsForContact(String contactName) {
        contactName = sanitizeContactName(contactName);
        if (TextUtils.isEmpty(contactName)) {
            return;
        }

        try {
            synchronized (NOTIFICATION_LOCK) {
                String prefix = contactName + " ";
                for (int index = Notification.size() - 1; index >= 0; index--) {
                    String pendingNotification = Notification.get(index);
                    if (pendingNotification.equals(contactName) || pendingNotification.startsWith(prefix)) {
                        Notification.remove(index);
                        Log.d(TAG, "RemovePendingNotificationsForContact: " + contactName);
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error RemovePendingNotificationsForContact: " + ex.getMessage());
        }
    }

    public static ArrayList<String> GetPendingNotificationsSnapshot() {
        synchronized (NOTIFICATION_LOCK) {
            return new ArrayList<>(Notification);
        }
    }

    public static boolean HasPendingNotifications() {
        synchronized (NOTIFICATION_LOCK) {
            return !Notification.isEmpty();
        }
    }

    private static boolean WasNotificationRecentlyQueued(String notif) {
        synchronized (NOTIFICATION_LOCK) {
            return !TextUtils.isEmpty(notif) && AllNotification.contains(notif);
        }
    }

    private static void trimRecentNotificationsLocked() {
        while (AllNotification.size() > MAX_RECENT_NOTIFICATIONS) {
            AllNotification.remove(0);
        }
    }

    @SuppressLint("ResourceAsColor")
    public static void ShowLayout(Context context, ViewGroup layout) {
        contextCommon = context;
        layout.removeAllViews();

        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);
        sv.setClipToPadding(false);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        sv.setLayoutParams(scrollParams);

        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(context, 16), dp(context, 26), dp(context, 16), dp(context, 22));
        sv.addView(ll);

        ll.addView(createHeader(context));

        ll.addView(createSpacer(context, 14));

        LinearLayout contactsCard = createCardContainer(context);
        boolean hasContacts = false;

        Map<String, String> map = new TreeMap<String, String>(DroidPreferences.GetAllString(context));
        TreeMap<String, TreeMap<String, String>> groupedContacts = groupContacts(context, map);
        Set set2 = groupedContacts.entrySet();
        Iterator iterator2 = set2.iterator();
        while (iterator2.hasNext()) {
            Map.Entry key = (Map.Entry) iterator2.next();
            if (hasContacts) {
                contactsCard.addView(createDivider(context));
            }
            hasContacts = true;
            contactsCard.addView(createContactGroup(
                    context,
                    layout,
                    key.getKey().toString(),
                    (TreeMap<String, String>) key.getValue()
            ));
        }

        if (!hasContacts) {
            TextView empty = new TextView(context);
            empty.setText(context.getString(R.string.vm_contacts_empty));
            empty.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            empty.setPadding(0, dp(context, 4), 0, dp(context, 4));
            contactsCard.addView(empty);
        }

        ll.addView(contactsCard);
        layout.addView(sv);
    }

    private static LinearLayout createHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.vm_header_title));
        title.setTextColor(context.getResources().getColor(R.color.oneUiTextPrimary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(title);

        TextView summary = new TextView(context);
        summary.setText(context.getString(R.string.vm_header_summary));
        summary.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        summary.setPadding(0, dp(context, 6), 0, 0);
        header.addView(summary);

        return header;
    }

    private static LinearLayout createNotificationCard(Context context, boolean isEnabled) {
        LinearLayout card = createCardContainer(context);
        card.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowListener(contextCommon);
            }
        });

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 40));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowListener(contextCommon);
            }
        });

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.vm_notifications_title));
        title.setTextColor(context.getResources().getColor(R.color.oneUiTextPrimary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        final SwitchCompat toggle = new SwitchCompat(context);
        toggle.setMinWidth(dp(context, 48));
        toggle.setShowText(false);
        toggle.setChecked(isEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toggle.setThumbTintList(ContextCompat.getColorStateList(context, R.color.switch_thumb_tint));
            toggle.setTrackTintList(ContextCompat.getColorStateList(context, R.color.switch_track_tint));
        }
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowListener(contextCommon);
                toggle.setChecked(isEnabled);
            }
        });
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        switchParams.leftMargin = dp(context, 12);
        row.addView(toggle, switchParams);
        card.addView(row);

        return card;
    }

    private static LinearLayout createSectionHeader(Context context, String titleText, String summaryText) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(context.getResources().getColor(R.color.oneUiTextPrimary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        section.addView(title);

        TextView summary = new TextView(context);
        summary.setText(summaryText);
        summary.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        summary.setPadding(0, dp(context, 2), 0, 0);
        section.addView(summary);

        return section;
    }

    private static TreeMap<String, TreeMap<String, String>> groupContacts(Context context, Map<String, String> contacts) {
        contacts = mergeDuplicateContacts(context, contacts);
        TreeMap<String, TreeMap<String, String>> groupedContacts = new TreeMap<>();
        Set set = contacts.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String contactName = entry.getKey().toString();
            String value = entry.getValue().toString();
            if (!value.equals("S") && !value.equals("N")) {
                continue;
            }
            String groupKey = getContactGroupKey(contactName);
            TreeMap<String, String> group = groupedContacts.get(groupKey);
            if (group == null) {
                group = new TreeMap<>();
                groupedContacts.put(groupKey, group);
            }
            group.put(contactName, value);
        }
        return groupedContacts;
    }

    private static Map<String, String> mergeDuplicateContacts(Context context, Map<String, String> contacts) {
        TreeMap<String, String> uniqueContacts = new TreeMap<>();
        HashMap<String, String> contactNameByDuplicateKey = new HashMap<>();

        Set set = contacts.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String originalContactName = entry.getKey().toString();
            String contactName = sanitizeContactName(originalContactName);
            String value = entry.getValue().toString();
            if (TextUtils.isEmpty(contactName) || (!value.equals("S") && !value.equals("N"))) {
                continue;
            }

            String duplicateKey = getContactDuplicateKey(contactName);
            String phoneNumber = contactPhoneByDuplicateKey.get(duplicateKey);
            if (TextUtils.isEmpty(duplicateKey) || (TextUtils.isEmpty(phoneNumber) && !isLikelyPhoneNumber(contactName))) {
                continue;
            }

            String existingContactName = contactNameByDuplicateKey.get(duplicateKey);
            if (existingContactName == null) {
                contactNameByDuplicateKey.put(duplicateKey, contactName);
                uniqueContacts.put(contactName, value);
                if (!originalContactName.equals(contactName)) {
                    DroidPreferences.SetString(context, contactName, value);
                    DroidPreferences.RemoveString(context, originalContactName);
                }
            } else {
                String existingValue = uniqueContacts.get(existingContactName);
                String mergedValue = value.equals("S") || "S".equals(existingValue) ? "S" : "N";
                uniqueContacts.put(existingContactName, mergedValue);
                DroidPreferences.SetString(context, existingContactName, mergedValue);
                if (!originalContactName.equals(existingContactName)) {
                    DroidPreferences.RemoveString(context, originalContactName);
                    RemovePendingNotificationsForContact(originalContactName);
                }
            }
        }

        return uniqueContacts;
    }

    private static String getContactGroupKey(String contactName) {
        if (TextUtils.isEmpty(contactName)) {
            return "#";
        }

        if (isLikelyPhoneNumber(contactName)) {
            return CONTACT_GROUP_UNKNOWN;
        }

        String normalized = Normalizer.normalize(contactName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        if (TextUtils.isEmpty(normalized)) {
            return "#";
        }

        char firstChar = Character.toUpperCase(normalized.charAt(0));
        if (firstChar >= 'A' && firstChar <= 'Z') {
            return String.valueOf(firstChar);
        }
        return "#";
    }

    private static LinearLayout createContactGroup(Context context, ViewGroup parentLayout, String groupKey, TreeMap<String, String> contacts) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.VERTICAL);

        boolean isExpanded = expandedContactGroups.contains(groupKey);
        group.addView(createContactGroupHeader(context, parentLayout, groupKey, contacts.size(), isExpanded));

        if (isExpanded) {
            Set set = contacts.entrySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                group.addView(createDivider(context));
                group.addView(createContactRow(
                        context,
                        parentLayout,
                        entry.getKey().toString(),
                        getContactPhoneNumber(entry.getKey().toString()),
                        entry.getValue().equals("S")
                ));
            }
        }

        return group;
    }

    private static LinearLayout createContactGroupHeader(Context context, ViewGroup parentLayout, String groupKey, int contactCount, boolean isExpanded) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 40));
        row.setClickable(true);
        row.setFocusable(true);

        TextView badge = new TextView(context);
        badge.setText(groupKey);
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(context.getResources().getColor(R.color.oneUiSection));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setBackground(createRoundedDrawable(
                context.getResources().getColor(R.color.oneUiSurfacePressed),
                Color.TRANSPARENT,
                14
        ));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                dp(context, 36),
                dp(context, 28)
        );
        badgeParams.rightMargin = dp(context, 12);
        row.addView(badge, badgeParams);

        TextView title = new TextView(context);
        if (groupKey.equals(CONTACT_GROUP_UNKNOWN)) {
            title.setText(context.getString(
                    contactCount == 1 ? R.string.vm_contacts_unknown_group_count_one : R.string.vm_contacts_unknown_group_count_many,
                    contactCount
            ));
        } else {
            title.setText(context.getString(
                    contactCount == 1 ? R.string.vm_contacts_group_count_one : R.string.vm_contacts_group_count_many,
                    contactCount
            ));
        }
        title.setTextColor(context.getResources().getColor(R.color.oneUiTextPrimary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        row.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView indicator = new TextView(context);
        indicator.setText(isExpanded ? "v" : ">");
        indicator.setGravity(Gravity.CENTER);
        indicator.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
        indicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        row.addView(indicator, new LinearLayout.LayoutParams(
                dp(context, 24),
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (expandedContactGroups.contains(groupKey)) {
                    expandedContactGroups.remove(groupKey);
                } else {
                    expandedContactGroups.add(groupKey);
                }
                ShowLayout(context, parentLayout);
            }
        });

        return row;
    }

    private static LinearLayout createContactRow(Context context, ViewGroup parentLayout, String contactName, String phoneNumber, boolean isChecked) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, TextUtils.isEmpty(phoneNumber) ? 40 : 48));
        row.setPadding(dp(context, 48), dp(context, 2), 0, dp(context, 2));
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);

        String displayContactName = isLikelyPhoneNumber(contactName) ? sanitizePhoneNumber(contactName) : contactName;
        TextView name = new TextView(context);
        name.setText(displayContactName);
        name.setTextColor(context.getResources().getColor(R.color.oneUiTextPrimary));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setSingleLine(false);
        textColumn.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        if (!TextUtils.isEmpty(phoneNumber)) {
            TextView phone = new TextView(context);
            phone.setText(phoneNumber);
            phone.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
            phone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            phone.setSingleLine(true);
            phone.setEllipsize(TextUtils.TruncateAt.END);
            phone.setPadding(0, dp(context, 1), 0, 0);
            textColumn.addView(phone, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        final CheckBox checkBox = new CheckBox(context);
        if (Build.VERSION.SDK_INT < 21) {
            CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(context.getResources().getColor(R.color.oneUiSection)));
        } else {
            checkBox.setButtonTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.oneUiSection)));
        }
        checkBox.setText("");
        checkBox.setChecked(isChecked);
        checkBox.setPadding(dp(context, 8), 0, 0, 0);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContactReadPreference(contactName, checkBox.isChecked());
            }
        });
        row.addView(checkBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBox.setChecked(!checkBox.isChecked());
                setContactReadPreference(contactName, checkBox.isChecked());
            }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showRemoveContactDialog(context, parentLayout, contactName);
                return true;
            }
        });

        return row;
    }

    private static void showRemoveContactDialog(final Context context, final ViewGroup parentLayout, final String contactName) {
        String displayContactName = isLikelyPhoneNumber(contactName) ? sanitizePhoneNumber(contactName) : contactName;
        AlertDialog dialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(context.getString(R.string.vm_remove_contact_title))
                .setMessage(context.getString(R.string.vm_remove_contact_message, displayContactName))
                .setNegativeButton(context.getString(R.string.vm_dialog_cancel), null)
                .setPositiveButton(context.getString(R.string.vm_dialog_remove), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removeContactFromList(context, parentLayout, contactName);
                    }
                })
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(createRoundedDrawable(
                            context.getResources().getColor(R.color.oneUiSurface),
                            context.getResources().getColor(R.color.oneUiStroke),
                            24
                    ));
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.oneUiSection));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
            }
        });
        dialog.show();
    }

    private static void removeContactFromList(Context context, ViewGroup parentLayout, String contactName) {
        DroidPreferences.RemoveString(context, contactName);
        RemovePendingNotificationsForContact(contactName);
        ShowLayout(context, parentLayout);
        NotifyContactsChanged(context);
    }

    private static void setContactReadPreference(String contactName, boolean shouldRead) {
        DroidPreferences.SetString(contextCommon, contactName, shouldRead ? "S" : "N");
        if (!shouldRead) {
            RemovePendingNotificationsForContact(contactName);
        }
    }

    private static View createDivider(Context context) {
        View divider = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 1)
        );
        params.topMargin = dp(context, 2);
        params.bottomMargin = dp(context, 2);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(context.getResources().getColor(R.color.oneUiStroke));
        return divider;
    }

    private static TextView createCircularIcon(Context context, String text, int colorRes) {
        TextView icon = new TextView(context);
        icon.setText(text);
        icon.setGravity(Gravity.CENTER);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        icon.setBackground(createRoundedDrawable(
                context.getResources().getColor(colorRes),
                Color.TRANSPARENT,
                24
        ));
        return icon;
    }

    private static LinearLayout createCardContainer(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(params);
        card.setBackground(createRoundedDrawable(
                context.getResources().getColor(R.color.oneUiSurface),
                context.getResources().getColor(R.color.oneUiStroke),
                26
        ));
        return card;
    }

    private static View createSpacer(Context context, int dpHeight) {
        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, dpHeight)
        ));
        return space;
    }

    private static GradientDrawable createRoundedDrawable(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radiusDp * 3f);
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(2, strokeColor);
        }
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }

    private static boolean isNotificationListenerEnabled(Context context) {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );

        if (TextUtils.isEmpty(enabledListeners)) {
            return false;
        }

        ComponentName componentName = new ComponentName(context, com.droid.ray.droidvoicemessage.notification.DroidNotification.class);
        String flatName = componentName.flattenToString();
        return enabledListeners.contains(flatName);
    }


    static View.OnClickListener getOnClickCheckBox(final CheckBox checkBox) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String valor = "N";

                if (checkBox.isChecked()) {
                    valor = "S";
                }

                String contactName = checkBox.getText().toString();
                DroidPreferences.SetString(contextCommon, contactName, valor);
                if (valor.equals("N")) {
                    RemovePendingNotificationsForContact(contactName);
                }

            }
        };
    }

    public static void getAllContact(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            HashSet<String> contactNames = new HashSet<>();
            contactPhoneByDuplicateKey.clear();

            int phoneContacts = collectPhoneContactNames(context, cr, contactNames);

            Log.d(TAG, " Contact sources phone=" + phoneContacts
                    + " total=" + contactNames.size());
        } catch (Exception ex) {
            Log.d(TAG, " erro getContact:  " + ex.getMessage());
        }
    }

    private static int collectPhoneContactNames(Context context, ContentResolver cr, HashSet<String> contactNames) {
        Cursor cursor = null;
        int contactSize = 0;

        try {
            cursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    name = sanitizeContactName(name);

                    if (!TextUtils.isEmpty(name)) {
                        rememberContactPhone(name, number);
                        migratePhoneNumberPreference(context, number, name);
                        if (saveContactIfMissing(context, contactNames, name)) {
                            contactSize++;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.d(TAG, " erro collectPhoneContactNames: " + ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contactSize;
    }

    private static int collectVisibleContactNames(Context context, ContentResolver cr, HashSet<String> contactNames) {
        Cursor cursor = null;
        int contactSize = 0;

        try {
            cursor = cr.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                    null,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                    if (saveContactIfMissing(context, contactNames, name)) {
                        contactSize++;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.d(TAG, " erro collectVisibleContactNames: " + ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contactSize;
    }

    private static int collectWhatsappDataContactNames(Context context, ContentResolver cr, HashSet<String> contactNames) {
        Cursor cursor = null;
        int contactSize = 0;

        try {
            cursor = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Data.DISPLAY_NAME_PRIMARY},
                    ContactsContract.Data.MIMETYPE + " LIKE ?",
                    new String[]{"%whatsapp%"},
                    ContactsContract.Data.DISPLAY_NAME_PRIMARY + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY));
                    if (saveContactIfMissing(context, contactNames, name)) {
                        contactSize++;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.d(TAG, " erro collectWhatsappDataContactNames: " + ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contactSize;
    }

    private static int collectWhatsappRawContactNames(Context context, ContentResolver cr, HashSet<String> contactNames) {
        Cursor cursor = null;
        int contactSize = 0;

        try {
            cursor = cr.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY},
                    ContactsContract.RawContacts.ACCOUNT_TYPE + " LIKE ?",
                    new String[]{"%whatsapp%"},
                    ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
                    if (saveContactIfMissing(context, contactNames, name)) {
                        contactSize++;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.d(TAG, " erro collectWhatsappRawContactNames: " + ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contactSize;
    }

    private static boolean saveContactIfMissing(Context context, HashSet<String> contactNames, String contactName) {
        contactName = sanitizeContactName(contactName);
        if (TextUtils.isEmpty(contactName)) {
            return false;
        }

        boolean isNewInCurrentLoad = contactNames.add(getContactDuplicateKey(contactName));
        if (!isNewInCurrentLoad) {
            return false;
        }

        if (DroidPreferences.GetString(context, contactName).equals("")) {
            DroidPreferences.SetString(context, contactName, "N");
        }
        return true;
    }

    private static void rememberContactPhone(String contactName, String phoneNumber) {
        String duplicateKey = getContactDuplicateKey(contactName);
        phoneNumber = sanitizePhoneNumber(phoneNumber);
        if (TextUtils.isEmpty(duplicateKey) || TextUtils.isEmpty(phoneNumber)) {
            return;
        }

        if (!contactPhoneByDuplicateKey.containsKey(duplicateKey)) {
            contactPhoneByDuplicateKey.put(duplicateKey, phoneNumber);
        }
    }

    private static String getContactPhoneNumber(String contactName) {
        String duplicateKey = getContactDuplicateKey(contactName);
        if (TextUtils.isEmpty(duplicateKey)) {
            return "";
        }

        String phoneNumber = contactPhoneByDuplicateKey.get(duplicateKey);
        if (!TextUtils.isEmpty(phoneNumber)) {
            return phoneNumber;
        }

        return "";
    }

    private static void migratePhoneNumberPreference(Context context, String number, String contactName) {
        if (context == null || TextUtils.isEmpty(number) || TextUtils.isEmpty(contactName)) {
            return;
        }

        HashSet<String> possibleKeys = new HashSet<>();
        possibleKeys.add(number.trim());

        String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
        if (!TextUtils.isEmpty(normalizedNumber)) {
            possibleKeys.add(normalizedNumber);
        }

        for (String key : possibleKeys) {
            if (TextUtils.isEmpty(key) || key.equals(contactName)) {
                continue;
            }

            String oldValue = DroidPreferences.GetString(context, key);
            if (!oldValue.equals("")) {
                String currentValue = DroidPreferences.GetString(context, contactName);
                String newValue = "S".equals(oldValue) || "S".equals(currentValue) ? "S" : "N";
                DroidPreferences.SetString(context, contactName, newValue);
                DroidPreferences.RemoveString(context, key);
                NotifyContactsChanged(context);
            }
        }

        Map<String, String> savedValues = DroidPreferences.GetAllString(context);
        Set set = savedValues.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String savedKey = sanitizeContactName(entry.getKey().toString());
            if (TextUtils.isEmpty(savedKey) || savedKey.equals(contactName) || !isLikelyPhoneNumber(savedKey)) {
                continue;
            }

            String savedNormalizedNumber = PhoneNumberUtils.normalizeNumber(savedKey);
            if (phoneNumbersMatch(savedNormalizedNumber, normalizedNumber)) {
                String oldValue = entry.getValue().toString();
                String currentValue = DroidPreferences.GetString(context, contactName);
                String newValue = "S".equals(oldValue) || "S".equals(currentValue) ? "S" : "N";
                DroidPreferences.SetString(context, contactName, newValue);
                DroidPreferences.RemoveString(context, savedKey);
                RemovePendingNotificationsForContact(savedKey);
                NotifyContactsChanged(context);
            }
        }
    }

    private static boolean phoneNumbersMatch(String firstNumber, String secondNumber) {
        if (TextUtils.isEmpty(firstNumber) || TextUtils.isEmpty(secondNumber)) {
            return false;
        }

        if (firstNumber.equals(secondNumber)) {
            return true;
        }

        int minMatchLength = Math.min(8, Math.min(firstNumber.length(), secondNumber.length()));
        return minMatchLength >= 7
                && firstNumber.substring(firstNumber.length() - minMatchLength)
                .equals(secondNumber.substring(secondNumber.length() - minMatchLength));
    }

    private static String resolveContactName(Context context, String title) {
        if (context == null || TextUtils.isEmpty(title) || !isLikelyPhoneNumber(title)) {
            return title;
        }

        Cursor cursor = null;

        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(title));
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String name = sanitizeContactName(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)));
                if (!TextUtils.isEmpty(name)) {
                    migratePhoneNumberPreference(context, title, name);
                    saveContactIfMissing(context, new HashSet<String>(), name);
                    return name;
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, " erro resolveContactName: " + ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return title;
    }

    private static String getNotificationContactTitle(String title) {
        title = sanitizeContactName(title);
        int indexGroup = title.indexOf("@");
        if (indexGroup > 0) {
            return title.substring(0, indexGroup).trim();
        }
        return title;
    }

    private static String replaceNotificationContactTitle(String title, String oldContactTitle, String newContactTitle) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(oldContactTitle) || TextUtils.isEmpty(newContactTitle)) {
            return title;
        }

        int indexGroup = title.indexOf("@");
        if (indexGroup > 0) {
            return newContactTitle + title.substring(indexGroup);
        }

        return newContactTitle;
    }

    private static boolean isLikelyPhoneNumber(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }

        String phoneText = value.trim();
        if (!phoneText.matches("^[+()\\-\\.\\s0-9]+$")) {
            return false;
        }

        String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneText);
        return !TextUtils.isEmpty(normalizedNumber) && normalizedNumber.length() >= 7;
    }

    private static String sanitizeContactName(String contactName) {
        if (contactName == null) {
            return "";
        }

        return contactName.replace("\n", " ").replace("\r", " ").trim();
    }

    private static String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String cleanPhoneNumber = phoneNumber.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " ").trim();
        if (TextUtils.isEmpty(cleanPhoneNumber)) {
            return "";
        }

        try {
            String formattedPhoneNumber = PhoneNumberUtils.formatNumber(cleanPhoneNumber, Locale.getDefault().getCountry());
            if (!TextUtils.isEmpty(formattedPhoneNumber)) {
                return formattedPhoneNumber;
            }
        } catch (Exception ex) {
            Log.d(TAG, " erro sanitizePhoneNumber: " + ex.getMessage());
        }

        return cleanPhoneNumber;
    }

    private static String getContactDuplicateKey(String contactName) {
        contactName = sanitizeContactName(contactName);
        if (TextUtils.isEmpty(contactName)) {
            return "";
        }

        return Normalizer.normalize(contactName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public static boolean AskPermissionGrand(Activity activity, Context appContext) {
        boolean retorno = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : PERMISSIONS) {
                if (appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
                    retorno = false;
                }
            }
        }
        return retorno;
    }
}
