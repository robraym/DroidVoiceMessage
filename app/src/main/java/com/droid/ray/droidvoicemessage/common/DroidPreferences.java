package com.droid.ray.droidvoicemessage.common;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Robson on 31/07/2017.
 */

public class DroidPreferences {

    public static final String PREF_ID = "DroidVoiceMessage";

    public static void SetInteger(Context context, String chave, int valor) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_ID, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(chave, valor);
        editor.commit();
    }

    public static int GetInteger(Context context, String chave) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_ID, 0);
        int i = sharedPreferences.getInt(chave, 0);
        return i;

    }

    public static void SetString(Context context, String chave, String valor) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_ID, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(chave, valor);
        editor.commit();
    }

    public static void RemoveString(Context context, String chave) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_ID, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(chave);
        editor.commit();
    }

    public static String GetString(Context context, String chave) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_ID, 0);
        String i = sharedPreferences.getString(chave, "");
        return i;

    }

    public static HashMap<String, String> GetAllString(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_ID, 0);
        HashMap<String, String> values = new HashMap<>();
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                values.put(entry.getKey(), (String) value);
            }
        }
        return values;
    }
}
