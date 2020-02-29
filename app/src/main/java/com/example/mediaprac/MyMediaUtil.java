package com.example.mediaprac;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class MyMediaUtil {

    static final String TAG = "MyMediaUtil";

    static public MediaExtractor createExtractor(String uri) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(uri);
        } catch (IOException e) {
            e.printStackTrace();
            extractor.release();
            return null;
        }

        return extractor;
    }


    static public int[] findTrackAVIndex(MediaExtractor extractor) {
        int[] ii = new int[2];
        ii[0] = -1; // video
        ii[1] = -1; // audio

        for (int i = 0, n = extractor.getTrackCount(); i < n; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "track " + i + " : key_mime = " + mime);
            if (mime == null) continue;
            if (mime.contains("video"))
                ii[0] = i;
            else if (mime.contains("audio"))
                ii[1] = i;
        }
        return ii;
    }


    static public MediaCodec createMediaCodec(MediaFormat format) {
        assert format != null;
        String codecName = new MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format);
        MediaCodec codec = null;
        try {
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "try to create a codec mime=" + mime + " codecName=" + codecName);
            try {
                if (codecName != null)
                    codec = MediaCodec.createByCodecName(codecName);
                else if (mime != null)
                    codec = MediaCodec.createDecoderByType(mime);  // may be throw IllegalArgumentException
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "createMediaCodec IllegalArgumentException (" + mime + " codec does not exist)");
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return codec;
    }


    static public MediaCodec createVideoDecoder(MediaFormat format, Surface surface) {
        MediaCodec codec = createMediaCodec(format);
        if (codec == null)
            return null;
        codec.configure(format, surface, null, 0);
        return codec;
    }


    static public MediaCodec createAudioDecoder(MediaFormat format) {
        MediaCodec codec = createMediaCodec(format);
        if (codec == null)
            return null;
        codec.configure(format, null, null, 0);
        return codec;
    }


    static public AudioFormat createAudioFormatFromMediaFormat(MediaFormat mediaFormat) {
        AudioFormat.Builder b = new AudioFormat.Builder();
        if (mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            b.setSampleRate(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        } else {
            Log.w(TAG, "createAudioFormatFormMediaFormat: not found KEY_SAMPLE_RATE");
            b.setSampleRate(44100);
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            b.setEncoding(mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING));
        } else {
            Log.w(TAG, "createAudioFormatFormMediaFormat: not found KEY_PCM_ENCODING");
            b.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_MASK)) {
            b.setChannelMask(mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK));
        } else {
            Log.w(TAG, "createAudioFormatFormMediaFormat: not found KEY_CHANNEL_MASK");
            b.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO);
        }

        return b.build();
    }


    static public AudioTrack createAudioTrack(AudioFormat audioFormat) {
        int size = AudioTrack.getMinBufferSize(audioFormat.getSampleRate(),
                audioFormat.getChannelMask(), audioFormat.getEncoding());
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build())
                .setAudioFormat(audioFormat)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(size)
                .build();
    }


    static public void dumpMediaCodecList() {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo mci : mcl.getCodecInfos()) {
            for (String type : mci.getSupportedTypes()) {
                Log.d(TAG, mci.getName() + " supports " + type);
            }
        }
    }
}
