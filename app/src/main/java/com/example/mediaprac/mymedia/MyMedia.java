package com.example.mediaprac.mymedia;

import android.view.Surface;

import java.io.IOException;

public interface MyMedia {
    // do not call from ui thread.
    boolean initialize(String contentUri) throws IOException;

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
