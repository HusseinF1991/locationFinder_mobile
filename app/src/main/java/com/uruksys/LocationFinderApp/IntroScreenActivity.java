package com.uruksys.LocationFinderApp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

public class IntroScreenActivity extends AppCompatActivity {

    int frameNumber = 1;
    ImageView introActivityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_screen);

        final Activity thisActivity =  this;
        VideoView fullscreenVideoView = findViewById(R.id.fullscreenVideoView);
        final View placeholder = (View) findViewById(R.id.placeholder);

       String path = "android.resource://" + getPackageName() + "/" + R.raw.introvideo;
        fullscreenVideoView.setVideoURI(Uri.parse(path));
        fullscreenVideoView.setMediaController(null);
        fullscreenVideoView.start();
        fullscreenVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                thisActivity.finish();
            }
        });


        fullscreenVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            // video started; hide the placeholder.
                            placeholder.setVisibility(View.GONE);
                            return true;
                        }
                        return false;
                    }
                });
            }
        });
    }
}

