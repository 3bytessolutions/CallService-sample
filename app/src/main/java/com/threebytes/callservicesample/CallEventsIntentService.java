package com.threebytes.callservicesample;

import android.app.IntentService;
import android.content.Intent;

import com.threebytes.callapi.CallService;

public class CallEventsIntentService extends IntentService {
    private static final String EXTRA_REMOTE_USER_ID = "remoteId";
    private static final String EXTRA_EVENT_TYPE = "type";
    private static final String EVENT_INCOMING_CALL = "INCOMING_CALL";
    private static final String EVENT_CALL_RESPONSE = "CALL_RESPONSE";

    private static final String ACTION_STRING_FINISH = "ACTION_STRING_FINISH";

    public CallEventsIntentService() {
        super("CallEventsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
        if(EVENT_INCOMING_CALL.equals(eventType)) {
            handleIncomingCall(intent);
        } else if(EVENT_CALL_RESPONSE.equals(eventType)) {
            handleCallResponse(intent);
        }
        //CallEventsBroadcastReceiver.completeWakefulIntent(intent);
    }
    private void handleIncomingCall(Intent intent) {
        Intent incomingCallIntent = new Intent(this,
                IncomingCallActivity.class);
        incomingCallIntent.putExtra(EXTRA_REMOTE_USER_ID, intent.getStringExtra(EXTRA_REMOTE_USER_ID));

        incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(incomingCallIntent);
    }
    private void handleCallResponse(Intent intent) {
        boolean answered = intent.getBooleanExtra("answered",false);

        if(answered) {
            CallService.getDefaultInstance().start(intent.getStringExtra(EXTRA_REMOTE_USER_ID), this, new CallService.Callback() {
                @Override
                public void onSuccess() {
                    Intent new_intent = new Intent();
                    new_intent.setAction(ACTION_STRING_FINISH);
                    sendBroadcast(new_intent);
                }

                @Override
                public void onError(Exception error) {
                    Intent new_intent = new Intent();
                    new_intent.setAction(ACTION_STRING_FINISH);
                    sendBroadcast(new_intent);
                }
            });
        } else {
            Intent new_intent = new Intent();
            new_intent.setAction(ACTION_STRING_FINISH);
            sendBroadcast(new_intent);
        }
    }
}
