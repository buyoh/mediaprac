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

    private static final String TAG = "MainActivity";

    private static final String myNiceVideoContent = "https://ia600603.us.archive.org/30/items/Tears-of-Steel/tears_of_steel_1080p.mp4";

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
        if (mMedia != null && mMedia.isPlaying()) {
            mMedia.pause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    //

    public void asyncInitializeButton_onClick(final View view) {

        final Context context = this;//getApplicationContext();

        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
                if (mMedia != null && mMedia.isInitialized()) {
                    toast("media is not released");
                    return;
                }
                SurfaceView sv = findViewById(R.id.surfaceView1);
                Surface s = sv.getHolder().getSurface();

                toast("initializing...");
                mMedia = MyMediaFactory.create(MyMediaFactory.TYPE_ASYNC, s, context);
                if (mMedia.initialize(myNiceVideoContent)) {
                    toast("initialize success");
                } else {
                    toast("initialize failed");
                }
            }
            @Override
            public void finalizeTask() {
                enableView(view, true);
            }
        }).start();

    }

    public void asyncTunnelInitializeButton_onClick(final View view) {

        final Context context = getApplicationContext();

        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
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
            @Override
            public void finalizeTask() {
                enableView(view, true);
            }
        }).start();

    }

    public void runButton_onClick(final View view) {
        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
                if (mMedia == null || !mMedia.isInitialized()) {
                    toast("media is not initialized");
                    return;
                }
                toast("calling run...");
                mMedia.run();
                toast("run");
            }
            @Override
            public void finalizeTask() {
                enableView(view, true);
            }
        }).start();
    }

    public void releaseButton_onClick(final View view) {
        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
                if (mMedia == null || !mMedia.isInitialized()) {
                    toast("media is not initialized");
                    return;
                }
                mMedia.release();
                toast("release");
                mMedia = null;
            }
            @Override
            public void finalizeTask() {
                enableView(view, true);
            }
        }).start();
    }

    public void resumeButton_onClick(View view) {
        Log.d(TAG, "clicked resume button");
        if (mMedia == null || !mMedia.isRunning()) {
            toast("media is not running");
            return;
        }
        mMedia.resume();
        toast("resume");
    }

    public void pauseButton_onClick(View view) {
        Log.d(TAG, "clicked pause button");
        if (mMedia == null || !mMedia.isRunning()) {
            toast("media is not running");
            return;
        }
        mMedia.pause();
        toast("pause");
    }

    public void restartButton_onClick(final View view) {
        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
                if (mMedia == null || !mMedia.isRunning()) {
                    toast("media is not running");
                    return;
                }
                mMedia.seekTo(0);
                toast("seek to 0");
            }
            @Override
            public void finalizeTask() {
                enableView(view, true);
            }
        }).start();
    }

    //

    private void enableView(int id, boolean enable) {
        View v = findViewById(id);
        enableView(v, enable);
    }
    private void enableView(final View view, final boolean enable) {
        if (!Thread.currentThread().equals(mUIHandler.getLooper().getThread())) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    enableView(view, enable);
                }
            });
        }
        else {
            view.setEnabled(enable);
        }

    }

    private void toast(final String message) {
        if (!Thread.currentThread().equals(mUIHandler.getLooper().getThread())) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    toast(message);
                }
            });
        }
        else {
            Log.d("TOAST", message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

    }

    static abstract class TryRunnable implements Runnable {
        @Override
        public final void run() {
            try{
                runTask();
            }
            catch (Exception e) {
                e.printStackTrace();
                onErrorTask();
            }
            finally {
                finalizeTask();
            }
        }
        public abstract void runTask();
        public void onErrorTask() {}
        public void finalizeTask() {}
    }
}
