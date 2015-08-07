package com.threebytes.callservicesample;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.threebytes.callapi.CallService;


public class OutgoingCallActivity extends AppCompatActivity {
    private static final String EXTRA_REMOTE_USER_ID = "remoteId";

    private String remoteId = null;
    private String remoteName = null;

    private boolean hasCallAborted = false;

    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private int mMusicStreamVol;

    public static Boolean[] hasCallAnswered = new Boolean[] { false };

    private boolean onStopCalled = false;

    private boolean activityReceiverRegister = false;
    private static final String ACTION_STRING_FINISH = "ACTION_STRING_FINISH";
    private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(activityReceiver);

            onStopCalled = true;

            resetWindowFlags();

            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
            }

            // Release the wake lock provided by the WakefulBroadcastReceiver.
            CallEventsBroadcastReceiver.completeWakefulIntent(CallEventsBroadcastReceiver.intent);

            finish();
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        getSupportActionBar().hide();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            getSupportActionBar().setIcon(android.R.color.transparent);

        Bundle bundle = getIntent().getExtras();
        remoteId = bundle.getString(EXTRA_REMOTE_USER_ID);
        remoteName = remoteId;//TODO put display name here for remote user

        TextView label = (TextView) findViewById(R.id.textLabel);
        label.setText("Calling to " + remoteName);

        CallService.getDefaultInstance().initiateCall(remoteId, this, new CallService.Callback() {
            @Override
            public void onError(Exception error) {
                error.printStackTrace();

                getSupportActionBar().setTitle(remoteName);
                getSupportActionBar().setSubtitle("");

                resetWindowFlags();

                findViewById(R.id.imageBtnIgnore).setVisibility(View.GONE);

                TextView errView = (TextView) findViewById(R.id.textViewErr);
                errView.setText("Could not reach");
                errView.setVisibility(View.VISIBLE);

                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                findViewById(R.id.imageAvatar).setVisibility(View.GONE);
                findViewById(R.id.textLabel).setVisibility(View.GONE);
                findViewById(R.id.textBackground).setVisibility(View.GONE);
            }

            @Override
            public void onSuccess() {
                if (hasCallAborted) {
                    return;
                }

                if (activityReceiver != null) {
                    IntentFilter intentFilter = new IntentFilter(ACTION_STRING_FINISH);
                    registerReceiver(activityReceiver, intentFilter);
                    activityReceiverRegister = true;
                }

                final TextView errView = (TextView) findViewById(R.id.textViewErr);

                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                    mMediaPlayer = MediaPlayer.create(
                            getApplicationContext(),
                            R.raw.outgoing_ringtone);

                    mMusicStreamVol = mAudioManager
                            .getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxMusicStreamVol = mAudioManager
                            .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            maxMusicStreamVol / 2 + maxMusicStreamVol
                                    / 4,
                            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    mMediaPlayer.setLooping(true);
                    mAudioManager.setSpeakerphoneOn(true);
                    mMediaPlayer.start();
                }

                final MediaPlayer mediaPlayerFinal = mMediaPlayer;
                final AudioManager audioManagerFinal = mAudioManager;

                final Runnable callTimerRunnable = new Runnable() {
                    public void run() {

                        if(onStopCalled)
                            return;

                        resetWindowFlags();

                        findViewById(R.id.imageBtnIgnore)
                                .setVisibility(View.GONE);


                        getSupportActionBar().setTitle(remoteName);
                        getSupportActionBar().setSubtitle("");


                        if (mediaPlayerFinal != null) {
                            mediaPlayerFinal.stop();
                            audioManagerFinal.setSpeakerphoneOn(false);
                            mAudioManager
                                    .setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            mMusicStreamVol,
                                            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        }

                        try {
                            TextView frndNameView = (TextView) findViewById(R.id.textViewFrndName);
                            frndNameView
                                    .setText(remoteName);
                            frndNameView.setVisibility(View.INVISIBLE);

                            errView.setText(remoteName + " is not available right now.\nPlease try again later.");
                            errView.setVisibility(View.VISIBLE);
                            findViewById(R.id.imageAvatar)
                                    .setVisibility(View.GONE);
                            findViewById(R.id.textLabel).setVisibility(
                                    View.GONE);
                            findViewById(R.id.textBackground)
                                    .setVisibility(View.GONE);

                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                        } catch (Exception e) {
                        }
                    }
                };

                errView.postDelayed(callTimerRunnable, 25000);

                synchronized (hasCallAnswered[0]) {
                    hasCallAnswered[0] = false;
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        if(onStopCalled)
            return;

        onStopCalled = true;

        resetWindowFlags();

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mAudioManager.setSpeakerphoneOn(false);
            mAudioManager
                    .setStreamVolume(AudioManager.STREAM_MUSIC,
                            mMusicStreamVol,
                            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }

    }

    private void resetWindowFlags() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void ignoreCall(View view) {
        hasCallAborted = true;

        if(activityReceiverRegister)
            unregisterReceiver(activityReceiver);

        finish();
    }

    @Override
    public void onBackPressed() {
        ignoreCall(null);
    }
}