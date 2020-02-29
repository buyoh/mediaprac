package com.example.mediaprac;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.example.mediaprac", appContext.getPackageName());
    }

    @Test
    public void codecExists() {
        boolean video = false, audio = false;
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo mci : mcl.getCodecInfos()) {
            for (String type : mci.getSupportedTypes()) {
                video |= type.contains("video");
                audio |= type.contains("audio");
            }
        }
        assertTrue(video);
        assertTrue(audio);
    }
}
