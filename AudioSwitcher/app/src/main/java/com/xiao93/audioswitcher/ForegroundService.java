package com.xiao93.audioswitcher;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.media.AudioManager.STREAM_MUSIC;

public class ForegroundService extends Service {

    private static final String TAG = "ForegroundService";

    private final int deltaVolumeLevel = 3;

    boolean mShouldBeEnabled = true;

    MediaSession mMediaSession;
    AudioManager mAudioManager;
    ComponentName mComponentName;

    int currentState = 0;
    int lastVolumeChangeDirection = 0;

    List<Boolean> playMusicStates = Arrays.asList(false, true, false, false);
    List<Boolean> podcastAddictStates = Arrays.asList(false, false, false, true);

    MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mComponentName = new ComponentName(this, NotificationListener.class);

        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        if (intent.getAction().equals(Constants.ACTION.START_MUSIC)) {
            currentState = 1;
            handleCurrentState();
            Log.i(TAG, "Received Start Foreground Intent ");
            startService();
        } else if (intent.getAction().equals(Constants.ACTION.START_PODCAST)) {
            currentState = 3;
            handleCurrentState();
            Log.i(TAG, "Received Start Foreground Intent ");
            startService();
        } else if (intent.getAction().equals(Constants.ACTION.PAUSE)) {
            if (currentState == 1 || currentState == 3) {
                currentState = (currentState + 1) % playMusicStates.size();
            }
            handleCurrentState();
            Log.i(TAG, "Received Pause Foreground Intent ");
            startService();
        } else if (intent.getAction().equals(Constants.ACTION.STOP)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void handleCurrentState() {
        Log.d(TAG, "Current State: " + Integer.toString(currentState));

        boolean playMusicState = playMusicStates.get(currentState);
        if (playMusicState) {
            maybeChangeVolume(-1);
            Intent intent = new Intent("com.android.music.musicservicecommand");
            intent.putExtra("command", "play");
            sendBroadcast(intent);
        } else {
            Intent intent = new Intent("com.android.music.musicservicecommand");
            intent.putExtra("command", "pause");
            sendBroadcast(intent);
        }

        boolean podcastAddictState = podcastAddictStates.get(currentState);
        if (podcastAddictState) {
            maybeChangeVolume(1);
            Intent intent = new Intent("com.bambuna.podcastaddict.service.player.play");
            sendBroadcast(intent);
        } else {
            Intent intent = new Intent("com.bambuna.podcastaddict.service.player.pause");
            sendBroadcast(intent);
        }

        Log.d(TAG, "Changing state. Music: " + Boolean.toString(playMusicState) + ", Podcast: " + Boolean.toString(podcastAddictState));
    }

    private void maybeChangeVolume(int direction) {
        if (mAudioManager == null) {
            return;
        }
        if (direction * lastVolumeChangeDirection > 0) {
            // Don't do any volume changes
            return;
        }

        lastVolumeChangeDirection = direction;

        int currentStreamVolume = mAudioManager.getStreamVolume(STREAM_MUSIC);
        Log.d(TAG, "Volume before change: " + Integer.toString(currentStreamVolume));
        int newVolume = currentStreamVolume + direction * deltaVolumeLevel;
        if (newVolume < 0) {
            newVolume = 0;
        }
        int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_MUSIC);
        if (newVolume > maxVolume) {
            newVolume = maxVolume;
        }
        mAudioManager.setStreamVolume(STREAM_MUSIC, newVolume, AudioManager.FLAG_PLAY_SOUND);
        Log.d(TAG, "Volume after change: " + Integer.toString(newVolume));
    }

    protected BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();

            if (Constants.ACTION.START_MUSIC.equals(action)) {
                currentState = 1;
                handleCurrentState();
                Log.i(TAG, "Received Start Foreground Intent ");
            } else if (Constants.ACTION.START_PODCAST.equals(action)) {
                currentState = 3;
                handleCurrentState();
                Log.i(TAG, "Received Start Foreground Intent ");
            } else if (Constants.ACTION.PAUSE.equals(action)) {
                if (currentState == 1 || currentState == 3) {
                    currentState = (currentState + 1) % playMusicStates.size();
                }
                handleCurrentState();
                Log.i(TAG, "Received Pause Foreground Intent ");
            } else if (Constants.ACTION.STOP.equals(action)) {
                unregisterReceiver(this);
                stopForeground(true);
                stopSelf();
            }
        }
    };


    private void startService() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION.START_MUSIC);
        intentFilter.addAction(Constants.ACTION.START_PODCAST);
        intentFilter.addAction(Constants.ACTION.STOP);
        intentFilter.addAction(Constants.ACTION.PAUSE);

        registerReceiver(broadcastReceiver, intentFilter);

        PendingIntent startMusicPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION.START_MUSIC), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent startPodcastPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION.START_PODCAST), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION.PAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION.STOP), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action startMusicAction = new Notification.Action.Builder(R.mipmap.icon_launcher, "Music", startMusicPendingIntent).build();
        Notification.Action startPodcastAction = new Notification.Action.Builder(R.mipmap.icon_launcher, "Podcast", startPodcastPendingIntent).build();
        Notification.Action pauseAction = new Notification.Action.Builder(R.mipmap.icon_launcher, "Pause", pausePendingIntent).build();

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Audio Switcher")
                .setTicker("Currently Running")
                .setContentText("Tap to stop service")
                .setSmallIcon(R.mipmap.icon_launcher)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .addAction(startMusicAction)
                .addAction(startPodcastAction)
                .addAction(pauseAction)
                .build();

        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        MediaSessionManager mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mMediaSessionManager == null) {
            Toast.makeText(getApplicationContext(), "Null media session manager!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (onActiveSessionsChangedListener == null) {
            onActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
                @Override
                public void onActiveSessionsChanged(@Nullable List<MediaController> list) {
                    boolean updateButtonReceiver = false;
                    if (list == null) {
                        list = Collections.emptyList();
                    }
                    // recreate MediaSession if another app handles media buttons
                    for (MediaController mediaController : list) {
                        if (!TextUtils.equals(getPackageName(), mediaController.getPackageName())) {
                            if ((mediaController.getFlags() & (MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)) != 0L) {
                                updateButtonReceiver = true;
                            }
                        }
                    }

                    if (updateButtonReceiver) {
                        // using a handler with a delay of about 2 seconds because this listener fires very often.
                        mAudioFocusHandler.removeCallbacksAndMessages(null);
                        mAudioFocusHandler.sendEmptyMessageDelayed(0, 2000);
                    }
                }
            };
            mMediaSessionManager.addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, mComponentName);
        }
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
    }

    private final Handler mAudioFocusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mShouldBeEnabled) {
                updateButtonReceiverEnabled(true);
            }
        }
    };

    private void updateButtonReceiverEnabled(boolean shouldBeEnabled) {
        // clear old session
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession.setFlags(0);
            mMediaSession.setCallback(null);
            mMediaSession.release();
            mMediaSession = null;
        }

        mMediaSession = new MediaSession(this, getApplicationContext().getPackageName() + "." + TAG);
        mMediaSession.setCallback(
                new MediaSession.Callback() {
                    public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                        Bundle extras = mediaButtonIntent.getExtras();
                        if (extras != null) {
                            for (String key : extras.keySet()) {
                                if (key.equals("android.intent.extra.KEY_EVENT")) {
                                    Object value = extras.get(key);
                                    if (value instanceof KeyEvent) {
                                        KeyEvent keyEvent = (KeyEvent) value;
                                        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                            Log.d(TAG, KeyEvent.keyCodeToString(keyEvent.getKeyCode()));
                                            currentState = (currentState + 1) % 4;
                                            handleCurrentState();
                                        }
                                    }
                                }
                            }
                        }
                        super.onMediaButtonEvent(mediaButtonIntent);
                        return false;
                    }

                    public void onPause() {
                        Log.d(TAG, "onPause called (media button pressed)");
                        super.onPause();
                    }

                    public void onPlay() {
                        Log.d(TAG, "onPlay called (media button pressed)");
                        super.onPlay();
                    }

                    public void onStop() {
                        Log.d(TAG, "onStop called (media button pressed)");
                        super.onStop();
                    }
                });
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setActive(true);
        mMediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                .setState(PlaybackState.STATE_CONNECTING, 0, 0f)
                .build());

        if (shouldBeEnabled != mShouldBeEnabled) {
            getPackageManager().setComponentEnabledSetting(mComponentName,
                    shouldBeEnabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }

        mShouldBeEnabled = shouldBeEnabled;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "In onDestroy");
        // Remove listener
        MediaSessionManager mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (onActiveSessionsChangedListener != null && mMediaSessionManager != null) {
            mMediaSessionManager.removeOnActiveSessionsChangedListener(onActiveSessionsChangedListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}
