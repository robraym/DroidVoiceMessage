package com.droid.ray.droidvoicemessage.activity;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.droid.ray.droidvoicemessage.R;
import com.droid.ray.droidvoicemessage.common.DroidCommon;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DroidVoiceMessage";
    private Context context;
    private boolean contactsReceiverRegistered = false;
    private final BroadcastReceiver contactsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receivedContext, Intent intent) {
            refreshContactsLayout();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = this;
            DroidCommon.forceBreak = true;
            DroidCommon.forceBreak = false;
            setContentView(R.layout.activity_main);
            //DroidCommon.StartPhoneService(context);

            refreshContactsLayout();
        } catch (Exception ex) {
            Log.d(TAG, "onCreate: " + ex.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (context == null) {
                context = this;
            }
            registerContactsReceiver();
            refreshContactsLayout();
        } catch (Exception ex) {
            Log.d(TAG, "onResume: " + ex.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterContactsReceiver();
    }

    private void refreshContactsLayout() {
        if (DroidCommon.AskPermissionGrand(this, getApplicationContext())) {
            DroidCommon.getAllContact(context);
            DroidCommon.ShowLayout(context, (ViewGroup) findViewById(R.id.layout_id));
        }
    }

    private void registerContactsReceiver() {
        if (contactsReceiverRegistered) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter(DroidCommon.ACTION_CONTACTS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(contactsChangedReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(contactsChangedReceiver, intentFilter);
        }
        contactsReceiverRegistered = true;
    }

    private void unregisterContactsReceiver() {
        if (!contactsReceiverRegistered) {
            return;
        }

        try {
            unregisterReceiver(contactsChangedReceiver);
        } catch (Exception ex) {
            Log.d(TAG, "unregisterContactsReceiver: " + ex.getMessage());
        } finally {
            contactsReceiverRegistered = false;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        switch (requestCode) {
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Do your work.
                    refreshContactsLayout();
                }
                return;
            }
        }

    }
}

