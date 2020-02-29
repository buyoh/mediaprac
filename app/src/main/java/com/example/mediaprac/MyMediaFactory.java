package com.example.mediaprac;

import android.view.Surface;

public class MyMediaFactory {

    public static MyMedia Create(int type, Surface surface) {
        return new MyMediaAsync(surface);
    }
}
