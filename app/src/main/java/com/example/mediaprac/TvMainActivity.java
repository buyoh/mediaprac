package com.example.mediaprac;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class TvMainActivity extends AppCompatActivity {

    private String mVideoUrl = "https://ia600603.us.archive.org/30/items/Tears-of-Steel/tears_of_steel_1080p.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_main);
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