package com.example.mediaprac;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    static final String myNiceVideoContent = "https://ia600603.us.archive.org/30/items/Tears-of-Steel/tears_of_steel_1080p.mp4";
//    static final String myNiceVideoContent = "https://scontent-nrt1-1.cdninstagram.com/v/t50.2886-16/41638619_239560346735367_5701419805668761311_n.mp4?_nc_ht=scontent-nrt1-1.cdninstagram.com&_nc_cat=103&_nc_ohc=NN0ddOIY3fUAX81uvbP&oe=5E5992B3&oh=f3f3097a6daa345e82fc5c0f12c7cb24";

    Handler mUIHandler = null;
    MyMedia mMedia = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUIHandler = new Handler();
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mMedia != null && mMedia.isRunning()) {
            mMedia.pause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    //

    public void initializeButton_onClick(View view) {

        final Context context = getApplicationContext();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mMedia != null && mMedia.isInitialized()) {
                    toast("media is not released");
                    return;
                }
                SurfaceView sv = findViewById(R.id.surfaceView1);
                Surface s = sv.getHolder().getSurface();

                toast("initializing...");
                mMedia = MyMediaFactory.create(MyMediaFactory.TYPE_TUNNELED_ASYNC, s, context);
                if (mMedia.initialize(myNiceVideoContent)) {
                    toast("initialize success");
                } else {
                    toast("initialize failed");
                }
            }
        }).start();

    }

    public void runButton_onClick(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mMedia == null || !mMedia.isInitialized()) {
                    toast("media is not initialized");
                    return;
                }
                toast("calling run...");
                mMedia.run();
                toast("run");
            }
        }).start();
    }

    public void releaseButton_onClick(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mMedia == null || !mMedia.isInitialized()) {
                    toast("media is not initialized");
                    return;
                }
                mMedia.release();
                toast("release");
                mMedia = null;
            }
        }).start();
    }

    public void resumeButton_onClick(View view) {
        if (mMedia == null || !mMedia.isRunning()) {
            toast("media is not running");
            return;
        }
        mMedia.resume();
    }

    public void pauseButton_onClick(View view) {
        if (mMedia == null || !mMedia.isRunning()) {
            toast("media is not running");
            return;
        }
        mMedia.pause();
    }

    public void restartButton_onClick(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mMedia == null || !mMedia.isRunning()) {
                    toast("media is not running");
                    return;
                }
                mMedia.seekTo(0);
                toast("seek to 0");
            }
        }).start();
    }

    //

    private void enableView(int id, boolean enable) {
        View v = findViewById(id);
        v.setEnabled(enable);
    }

    private void toast(final String message) {
        Log.d("TOAST", message);
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
