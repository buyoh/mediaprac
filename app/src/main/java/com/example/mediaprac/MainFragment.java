package com.example.mediaprac;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.mediaprac.mymedia.MyMedia;
import com.example.mediaprac.mymedia.MyMediaFactory;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";


    private String mVideoUrl = null;

    SurfaceView mVideoSurfaceView = null;

    Handler mUIHandler = null;
    MyMedia mMedia = null;
    SurfaceHolder.Callback mSurfaceHolderCallback = null;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance() {
        return new MainFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mVideoSurfaceView = view.findViewById(R.id.surfaceView1);
        attachHandler(view);

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
        mVideoSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mUIHandler = new Handler();

        if (getArguments() != null) {
            mVideoUrl = getArguments().getString("video_url");
            Log.d(TAG, "given url: " + mVideoUrl);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mMedia != null && mMedia.isPlaying()) {
            mMedia.pause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    //

    public void asyncInitializeButton_onClick(final View view) {
        if (mVideoUrl == null) {
            toast("invalid video url");
            return;
        }

        final Context context = getActivity();//getApplicationContext();

        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
                if (mMedia != null && mMedia.isInitialized()) {
                    toast("media is not released");
                    return;
                }
                Surface s = mVideoSurfaceView.getHolder().getSurface();

                toast("initializing...");
                mMedia = MyMediaFactory.create(MyMediaFactory.TYPE_ASYNC, s, context);

                try {
                    if (mMedia.initialize(mVideoUrl)) {
                        toast("initialize success");
                    } else {
                        toast("initialize failed");
                    }
                } catch (IOException e) {
                    toast("initialize failed: IOException");
                    e.printStackTrace();
                }
            }

            @Override
            public void finalizeTask() {
                enableView(view, true);
            }
        }).start();

    }

    public void asyncTunnelInitializeButton_onClick(final View view) {
        if (mVideoUrl == null) {
            toast("invalid video url");
            return;
        }

        final Context context = getActivity().getApplicationContext();

        view.setEnabled(false);
        new Thread(new TryRunnable() {
            @Override
            public void runTask() {
                if (mMedia != null && mMedia.isInitialized()) {
                    toast("media is not released");
                    return;
                }
                Surface s = mVideoSurfaceView.getHolder().getSurface();

                toast("initializing...");
                mMedia = MyMediaFactory.create(MyMediaFactory.TYPE_TUNNELED_ASYNC, s, context);

                try {
                    if (mMedia.initialize(mVideoUrl)) {
                        toast("initialize success");
                    } else {
                        toast("initialize failed");
                    }
                } catch (IOException e) {
                    toast("initialize failed: IOException");
                    e.printStackTrace();
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

    private void attachHandler(View view) {
        view.findViewById(R.id.asyncInitializeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                asyncInitializeButton_onClick(view);
            }
        });
        view.findViewById(R.id.asyncTunnelInitializeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                asyncTunnelInitializeButton_onClick(view);
            }
        });
        view.findViewById(R.id.runButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runButton_onClick(view);
            }
        });
        view.findViewById(R.id.releaseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                releaseButton_onClick(view);
            }
        });
        view.findViewById(R.id.resumeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumeButton_onClick(view);
            }
        });
        view.findViewById(R.id.pauseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseButton_onClick(view);
            }
        });
        view.findViewById(R.id.restartButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartButton_onClick(view);
            }
        });

    }

    //

    private void runOnUIThread(Runnable r) {
        if (Thread.currentThread().equals(mUIHandler.getLooper().getThread())) {
            r.run();
        } else {
            mUIHandler.post(r);
        }
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
        SurfaceView sv = mVideoSurfaceView;
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
        final Context that = getActivity();
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
            try {
                runTask();
            } catch (Exception e) {
                e.printStackTrace();
                onErrorTask();
            } finally {
                finalizeTask();
            }
        }

        public abstract void runTask();

        public void onErrorTask() {
        }

        public void finalizeTask() {
        }
    }
}