package com.example.mediaprac;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

        //https://developer.android.com/training/permissions/requesting?hl=ja
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            Log.d(TAG, "READ_EXTERNAL_STORAGE granted");
        } else {
            Log.d(TAG, "shouldShowRequestPermissionRationale");
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }
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
