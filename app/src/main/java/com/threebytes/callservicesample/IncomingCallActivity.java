package com.threebytes.callservicesample;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.threebytes.callapi.CallService;

import java.util.Timer;
import java.util.TimerTask;

public class IncomingCallActivity extends AppCompatActivity {

    public static final int NOTIFICATION_ID = 1234;
    private static final String EXTRA_REMOTE_USER_ID = "remoteId";

    private String remoteId = null;
    private String remoteName = null;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private int ringerMode;

    private boolean setNotification = true;

    private SettingsContentObserver settingsContentObserver;
    public static Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        getSupportActionBar().hide();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle bundle = getIntent().getExtras();
        remoteId = bundle.getString(EXTRA_REMOTE_USER_ID);
        remoteName = remoteId;//TODO put display name here for remote user


        TextView label = (TextView) findViewById(R.id.textLabel);
        label.setText("Video call from " + remoteName);

        settingsContentObserver = new SettingsContentObserver(this,
                new Handler());
        getApplicationContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                settingsContentObserver);

        try {
            Uri alert = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alert);
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            ringerMode = audioManager.getRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = { 0, 2000, 1000, 2000, 1000 };
                vibrator.vibrate(pattern, 1);
            }
        } catch (Exception e) {
        }

        cancelTimer();

        timer = new Timer();
        CallAnswerTimerTask timerTask = new CallAnswerTimerTask();
        timerTask.callerName = remoteName;
        timer.schedule(timerTask, 20000);
    }

    public void acceptCall(View view) {

        view.setEnabled(false);
        setNotification = false;
        stopRinger();
        cancelTimer();

        TextView label = (TextView) findViewById(R.id.textLabel);
        label.setText("Connecting to " + remoteName);

        view.setVisibility(View.INVISIBLE);

        ImageView imageBtnIgnore = (ImageView) findViewById(R.id.imageBtnIgnore);
        imageBtnIgnore.setVisibility(View.INVISIBLE);

        CallService.getDefaultInstance().callResponse(remoteId, true, this, new CallService.Callback() {
            @Override
            public void onError(Exception error) {
                error.printStackTrace();

                // Release the wake lock provided by the WakefulBroadcastReceiver.
                CallEventsBroadcastReceiver.completeWakefulIntent(CallEventsBroadcastReceiver.intent);
                finish();
            }

            @Override
            public void onSuccess() {
                CallService.getDefaultInstance().start(remoteId, IncomingCallActivity.this, new CallService.Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception error) {

                    }
                });

                // Release the wake lock provided by the WakefulBroadcastReceiver.
                CallEventsBroadcastReceiver.completeWakefulIntent(CallEventsBroadcastReceiver.intent);

                finish();
            }
        });
    }

    public void ignoreCall(View view) {
        view.setEnabled(false);
        stopRinger();
        setNotification = false;
        cancelTimer();

        setOnGoingCallStatus("N");

        CallService.getDefaultInstance().callResponse(remoteId, false, this, new CallService.Callback() {
            @Override
            public void onError(Exception error) {
                error.printStackTrace();
            }

            @Override
            public void onSuccess() {
            }
        });

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        CallEventsBroadcastReceiver.completeWakefulIntent(CallEventsBroadcastReceiver.intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        if (!setNotification || !powerManager.isScreenOn()) {
            return;
        }

        stopRinger();

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        CallEventsBroadcastReceiver.completeWakefulIntent(CallEventsBroadcastReceiver.intent);

        finish();
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void stopRinger() {

        getApplicationContext().getContentResolver().unregisterContentObserver(
                settingsContentObserver);

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL
                && mediaPlayer != null) {
            mediaPlayer.stop();
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE
                && vibrator != null) {
            vibrator.cancel();
        }
    }

    public class SettingsContentObserver extends ContentObserver {
        int previousVolume;
        int previousRingerMode;
        Context context;

        public SettingsContentObserver(Context c, Handler handler) {
            super(handler);
            context = c;

            AudioManager audio = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_RING);
            previousRingerMode = audio.getRingerMode();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            AudioManager audio = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING);
            int currentRingerMode = audio.getRingerMode();

            if (currentRingerMode != previousRingerMode
                    || currentVolume != previousVolume) {
                stopRinger();
            }
        }
    }

    class CallAnswerTimerTask extends TimerTask {
        public String callerName;

        @Override
        public void run() {
            missedCallNotification(callerName);
            setNotification = false;
            stopRinger();
            finish();
        }
    }

    private void missedCallNotification(String callerName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this)
                //.setSmallIcon(R.drawable.ic_stat_gcm)
                .setContentTitle("Missed call...")
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText("Missed call from " + callerName))
                .setContentText("Missed call from " + callerName);

        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancelTimer() {
        if (timer != null)
            timer.cancel();
    }

    private void setOnGoingCallStatus(String status){
        SharedPreferences.Editor prefY = PreferenceManager
                .getDefaultSharedPreferences(this).edit();
        prefY.putString("PROPERTY_ONGOING_CALL", status);
        prefY.commit();
    }
}


