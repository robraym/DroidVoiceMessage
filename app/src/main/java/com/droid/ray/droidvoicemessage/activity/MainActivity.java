package com.droid.ray.droidvoicemessage.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.droid.ray.droidvoicemessage.R;
import com.droid.ray.droidvoicemessage.common.DroidCommon;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DroidVoiceMessage";
    private Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = this;
            DroidCommon.forceBreak = true;
            DroidCommon.forceBreak = false;
            setContentView(R.layout.activity_main);
            //DroidCommon.StartPhoneService(context);

            if (DroidCommon.AskPermissionGrand(this, getApplicationContext())) {
                DroidCommon.getAllContact(context);
                DroidCommon.ShowLayout(context, (ViewGroup) findViewById(R.id.layout_id));
            }
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
            if (DroidCommon.AskPermissionGrand(this, getApplicationContext())) {
                DroidCommon.getAllContact(context);
                DroidCommon.ShowLayout(context, (ViewGroup) findViewById(R.id.layout_id));
            }
        } catch (Exception ex) {
            Log.d(TAG, "onResume: " + ex.getMessage());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        switch (requestCode) {
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Do your work.
                    DroidCommon.getAllContact(context);
                    DroidCommon.ShowLayout(context, (ViewGroup) findViewById(R.id.layout_id));
                }
                return;
            }
        }

    }
}

