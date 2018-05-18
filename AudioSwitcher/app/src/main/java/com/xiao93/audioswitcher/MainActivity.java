package com.xiao93.audioswitcher;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    Button startMusicButton;
    Button startPodcastButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startMusicButton = findViewById(R.id.start_music_button);
        startPodcastButton = findViewById(R.id.start_podcast_button);
        startMusicButton.setOnClickListener(this);
        startPodcastButton.setOnClickListener(this);

        Intent someIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.music");
        if (someIntent != null) {
            startActivity(someIntent);//null pointer check in case package name was not found
        }

        // Really shouldn't do this on UI thread, but seeing as this is a one (or two :P) time thing, and its to ensure that the activity has started, I'm not too fussed about this
        SystemClock.sleep(1000);

        someIntent = getPackageManager().getLaunchIntentForPackage("com.bambuna.podcastaddict");
        if (someIntent != null) {
            startActivity(someIntent);//null pointer check in case package name was not found
        }

        SystemClock.sleep(1000);

        someIntent = new Intent(getApplicationContext(), MainActivity.class);
        someIntent.setAction(Intent.ACTION_MAIN);
        someIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        someIntent.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(someIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_music_button:
                Intent startMusicIntent = new Intent(MainActivity.this, ForegroundService.class);
                startMusicIntent.setAction(Constants.ACTION.START_MUSIC);
                startService(startMusicIntent);
                break;
            case R.id.start_podcast_button:
                Intent startPodcastIntent = new Intent(MainActivity.this, ForegroundService.class);
                startPodcastIntent.setAction(Constants.ACTION.START_PODCAST);
                startService(startPodcastIntent);
                break;
            default:
                break;
        }

    }
}
