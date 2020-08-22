package com.example.mediaprac;

import android.view.Surface;

public interface MyMedia {
    // do not call from ui thread.
    boolean initialize(String contentUri);

    // start extracting. media will not play.
    // do not call from ui thread.
    void run();

    void release();

    void resume();

    void pause();

//    void stop();

    void seekTo(long timeUs);

    int getWidth();

    int getHeight();

    void setSurface(Surface s);

    boolean isRunning();

    boolean isPlaying();

    boolean isInitialized();
}
