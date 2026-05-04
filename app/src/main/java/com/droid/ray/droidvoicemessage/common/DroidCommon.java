package com.droid.ray.droidvoicemessage.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Robson on 04/08/2017.
 */

public class DroidCommon {
    private static Context contextCommon;
    public static final String TAG = "VoiceMessage";
    public static ArrayList<String> Notification = new ArrayList<>();
    public static ArrayList<String> AllNotification = new ArrayList<>();
    public static Boolean inCall = false;
    public static Boolean InThread = false;
    public static Boolean LoopingNotification = false;

    public static final int PERMISSION_ALL = 2;
    public static final String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE};
    public static boolean forceBreak = false;

    public static void EnviaMsg(Context context) {
        try {
            if (DroidCommon.Notification.size() > 0) {
                Log.d(TAG, "EnviaMsg - startService");
                StopService();
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
                tit = tit.replace("@", " grupo ");
                notif = tit + " " + notif;
                String PrefNotif = DroidPreferences.GetString(contextCommon, contato);
                if (PrefNotif.equals("") && !notif.trim().isEmpty()) {
                    if (!contato.contains("(")) {
                        DroidPreferences.SetString(contextCommon, contato, "N");
                    }
                }
                if (PrefNotif.equals("") || PrefNotif.equals("N")) {
                    notif = "";
                }

            } catch (Exception ex) {
                Log.d(TAG, "DroidPreferences: " + ex.getMessage());
            }
        } else notif = "";

        String notifLower = notif.toLowerCase();
        if (DroidCommon.AllNotification.contains(notif) || notifLower.equals("procurando novas mensagens") || notifLower.equals("mensagens está em execução")) {
            notif = "";
        }
        return notif;
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

    public static void TimeSleep(Integer seg) {
        try {
            Thread.sleep(seg);
        } catch (Exception ex) {
        }
    }

    public static void AddNotification(String notif) {
        try {
            if (!notif.toString().isEmpty() && !DroidCommon.Notification.contains(notif)) {
                Notification.add(notif);
                Log.d(TAG, "AddNotification: " + notif);
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error AddNotification: " + ex.getMessage());
        }
    }

    public static void AddAllNotification(String notif) {
        try {
            if (!notif.toString().isEmpty() && !DroidCommon.AllNotification.contains(notif)) {
                AllNotification.add(notif);
                Log.d(TAG, "AddAllNotification: " + notif);
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error AddAllNotification: " + ex.getMessage());
        }
    }

    public static void RemoveNotification(String msg) {
        try {
            int pos = Notification.indexOf(msg);
            Notification.remove(pos);
            Log.d(TAG, "RemoveNotification: " + msg);
        } catch (Exception ex) {
            Log.d(TAG, "Error RemoveNotification: " + ex.getMessage());
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
        ll.setPadding(dp(context, 24), dp(context, 28), dp(context, 24), dp(context, 32));
        sv.addView(ll);

        ll.addView(createHeader(context));
        ll.addView(createSpacer(context, 20));

        ll.addView(createNotificationCard(context, isNotificationListenerEnabled(context)));

        ll.addView(createSpacer(context, 16));
        ll.addView(createSectionHeader(
                context,
                context.getString(R.string.vm_contacts_title),
                context.getString(R.string.vm_contacts_summary)
        ));
        ll.addView(createSpacer(context, 12));

        LinearLayout contactsCard = createCardContainer(context);
        boolean hasContacts = false;

        Map<String, String> map = new TreeMap<String, String>(DroidPreferences.GetAllString(context));
        Set set2 = map.entrySet();
        Iterator iterator2 = set2.iterator();
        while (iterator2.hasNext()) {
            Map.Entry key = (Map.Entry) iterator2.next();
            hasContacts = true;
            CheckBox ch = new CheckBox(context);
            if (Build.VERSION.SDK_INT < 21) {
                CompoundButtonCompat.setButtonTintList(ch, ColorStateList.valueOf(context.getResources().getColor(R.color.oneUiIconGreen)));
            } else {
                ch.setButtonTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.oneUiIconGreen)));
            }
            ch.setTextColor(context.getResources().getColor(R.color.oneUiTextPrimary));
            contextCommon = context;
            ch.setText(key.getKey().toString());
            ch.setOnClickListener(getOnClickCheckBox(ch));
            ch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            ch.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            ch.setPadding(0, dp(context, 12), 0, dp(context, 12));

            if (key.getValue().equals("S")) {
                ch.setChecked(true);
            }

            contactsCard.addView(createContactRow(context, ch));
        }

        if (!hasContacts) {
            TextView empty = new TextView(context);
            empty.setText(context.getString(R.string.vm_contacts_empty));
            empty.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            empty.setPadding(0, dp(context, 8), 0, dp(context, 8));
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
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(title);

        TextView summary = new TextView(context);
        summary.setText(context.getString(R.string.vm_header_summary));
        summary.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        summary.setPadding(0, dp(context, 8), 0, 0);
        header.addView(summary);

        return header;
    }

    private static LinearLayout createNotificationCard(Context context, boolean isEnabled) {
        LinearLayout card = createCardContainer(context);
        card.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
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
        row.setMinimumHeight(dp(context, 44));
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
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        section.addView(title);

        TextView summary = new TextView(context);
        summary.setText(summaryText);
        summary.setTextColor(context.getResources().getColor(R.color.oneUiTextSecondary));
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        summary.setPadding(0, dp(context, 4), 0, 0);
        section.addView(summary);

        return section;
    }

    private static LinearLayout createContactRow(Context context, CheckBox checkBox) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = createCircularIcon(context, "\uD83D\uDC64", R.color.oneUiIconGreen);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(context, 40), dp(context, 40));
        iconParams.rightMargin = dp(context, 14);
        row.addView(icon, iconParams);

        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(checkBox, checkParams);

        return row;
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
        card.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(params);
        card.setBackground(createRoundedDrawable(
                context.getResources().getColor(R.color.oneUiSurface),
                context.getResources().getColor(R.color.oneUiStroke),
                24
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

                DroidPreferences.SetString(contextCommon, checkBox.getText().toString(), valor);

            }
        };
    }

    public static void getAllContact(Context context) {

        try {
            //This class provides applications access to the content model.
            ContentResolver cr = context.getContentResolver();

//RowContacts for filter Account Types
            Cursor contactCursor = cr.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts._ID,
                            ContactsContract.RawContacts.CONTACT_ID},
                    ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                    new String[]{"com.whatsapp"},
                    null);

//ArrayList for Store Whatsapp Contact
            ArrayList<String> myWhatsappContacts = new ArrayList<>();

            if (contactCursor != null) {
                if (contactCursor.getCount() > 0) {
                    if (contactCursor.moveToFirst()) {
                        do {
                            //whatsappContactId for get Number,Name,Id ect... from  ContactsContract.CommonDataKinds.Phone
                            String whatsappContactId = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID));

                            if (whatsappContactId != null) {
                                //Get Data from ContactsContract.CommonDataKinds.Phone of Specific CONTACT_ID
                                Cursor whatsAppContactCursor = cr.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                        new String[]{whatsappContactId}, null);

                                if (whatsAppContactCursor != null) {
                                    whatsAppContactCursor.moveToFirst();
                                    String name = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                    String number = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                                    whatsAppContactCursor.close();

                                    //Add Number to ArrayList
                                    myWhatsappContacts.add(number);

                                    if (DroidPreferences.GetString(context, name).equals("")) {
                                        DroidPreferences.SetString(context, name, "N");
                                    }

                                    Log.d(TAG, "WhatsApp contact name " + name);
                                    Log.d(TAG, " WhatsApp contact number :  " + number);
                                }
                            }
                        } while (contactCursor.moveToNext());
                        contactCursor.close();
                    }
                }
            }

            Log.d(TAG, " WhatsApp contact size :  " + myWhatsappContacts.size());

        } catch (Exception ex) {
            Log.d(TAG, " erro getContact:  " + ex.getMessage());
        }
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
