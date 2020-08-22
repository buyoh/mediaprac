package com.example.mediaprac;

import android.content.Context;
import android.view.Surface;

public class MyMediaFactory {

    final public static int TYPE_ASYNC = 0;
    final public static int TYPE_TUNNELED_ASYNC = 1;

    public static MyMedia create(int type, Surface surface) {
        return create(type, surface, null);
    }

    public static MyMedia create(int type, Surface surface, Context context) {
        return
                type == TYPE_ASYNC ? new MyMediaAsync(surface) :
                        type == TYPE_TUNNELED_ASYNC ? new MyMediaTunnelAsync(surface, context) :
                                create(0, surface);
    }
}
