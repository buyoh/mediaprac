package com.example.mediaprac;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private String mVideoUrl = "https://ia600603.us.archive.org/30/items/Tears-of-Steel/tears_of_steel_1080p.mp4";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent it = getIntent();
        if (it != null) {
            if (it.getData() != null && it.getData().getPath() != null) {
                mVideoUrl = it.getData().getPath();
            }
        }

        setContentView(R.layout.activity_main);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof MainFragment) {
            Bundle args = new Bundle();
            args.putString("video_url", mVideoUrl);
            fragment.setArguments(args);
        }
    }
}
