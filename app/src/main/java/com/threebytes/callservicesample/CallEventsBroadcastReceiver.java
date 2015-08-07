package com.threebytes.callservicesample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class CallEventsBroadcastReceiver extends WakefulBroadcastReceiver {
    public static Intent intent;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.intent = intent;

        ComponentName comp = new ComponentName(context.getPackageName(),
                CallEventsIntentService.class.getName());

        startWakefulService(context, (intent.setComponent(comp)));
    }
}
