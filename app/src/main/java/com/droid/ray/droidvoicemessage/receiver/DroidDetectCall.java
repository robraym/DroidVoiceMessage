package com.droid.ray.droidvoicemessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.droid.ray.droidvoicemessage.common.DroidCommon;

import java.util.Date;

import static com.droid.ray.droidvoicemessage.common.DroidCommon.TAG;

public class DroidDetectCall extends BroadcastReceiver {

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceiver - Call");

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.

            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }

            onCallStateChanged(context, state, number);

    }


    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {

        if (lastState == state) {
            //No change, debounce extras
            Log.d(TAG, "onReceiver - lastState = state");
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                Log.d(TAG, "onIncomingCallStarted");
                DroidCommon.inCall = true;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    Log.d(TAG, "onOutgoingCallStarted");
                    DroidCommon.inCall = true;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    Log.d(TAG, "onMissedCall");
                    DroidCommon.inCall = false;

                } else if (isIncoming) {
                    Log.d(TAG, "onIncomingCallEnded");
                    DroidCommon.inCall = false;
                } else {
                    Log.d(TAG, "onOutgoingCallEnded");
                    DroidCommon.inCall = false;
                }
                break;
        }
        lastState = state;
        Log.d(TAG, "onReceiver - onCallStateChanged");
        if (!DroidCommon.inCall) {
            DroidCommon.EnviaMsg(context);
        }
    }
}
