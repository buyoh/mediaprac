package com.example.mediaprac;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mediaprac.mymedia.MyMedia;
import com.example.mediaprac.mymedia.MyMediaFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String myNiceVideoContent = "https://ia600603.us.archive.org/30/items/Tears-of-Steel/tears_of_steel_1080p.mp4";

    Handler mUIHandler = null;
    MyMedia mMedia = null;
    SurfaceHolder.Callback mSurfaceHolderCallback = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSurfaceHolderCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (mMedia == null || !mMedia.isInitialized())
                    return;
                mMedia.setSurface(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                if (mMedia == null || !mMedia.isInitialized())
                    return;
                mMedia.setSurface(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            }
        };

        super.onCreate(savedInstanceState);

        mUIHandler = new Handler();
        setContentView(R.layout.activity_main);

        SurfaceView sv = findViewById(R.id.surfaceView1);
        sv.getHolder().addCallback(mSurfaceHolderCallback);
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

                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        resizeVideoFrame();
                    }
                });

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

    private void runOnUIThread(Runnable r) {
        if (Thread.currentThread().equals(mUIHandler.getLooper().getThread())) {
            r.run();
        } else {
            mUIHandler.post(r);
        }
    }

    private void enableView(int id, boolean enable) {
        View v = findViewById(id);
        enableView(v, enable);
    }

    private void enableView(final View view, final boolean enable) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                view.setEnabled(enable);
            }
        });
    }

    private void resizeVideoFrame() {
        SurfaceView sv = findViewById(R.id.surfaceView1);
        View pv = (View) sv.getParent();
        int maxWidth = pv.getWidth();
        int maxHeight = pv.getHeight();
        int originalWidth = mMedia.getWidth();
        int originalHeight = mMedia.getHeight();
        double rw = (double) maxWidth / originalWidth;
        double rh = (double) maxHeight / originalHeight;
        double r = Math.min(rw, rh);
        ViewGroup.LayoutParams lp = sv.getLayoutParams();
        lp.width = (int) (r * originalWidth);
        lp.height = (int) (r * originalHeight);
        sv.setLayoutParams(lp);
    }

    private void toast(final String message) {
        final Context that = this;
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Log.d("TOAST", message);
                Toast.makeText(that, message, Toast.LENGTH_SHORT).show();
            }
        });
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
