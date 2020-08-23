package com.example.mediaprac.mymedia;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class MyMediaTunnelAsync implements MyMedia {
    // ref
    // http://blog.ironhead.ninja/2016/01/14/android-tunneled-playback.html

    static final String TAG = "MyMediaTunnelAsync";

    private String mContentUri = null;

    private boolean mRunning, mPlaying, mInitialized;
    private Surface mGivenSurface, mSurface;
    private AudioTrack mAudioTrack;
    //    private MediaSync mSync;
    private MediaExtractor mVideoExtractor, mAudioExtractor;
    private MediaCodec mVideoCodec, mAudioCodec;
    private Context mContext;

    private int mWidth = -1, mHeight = -1;

    public MyMediaTunnelAsync(Surface surface, Context context) {
        assert surface != null;
        mRunning = false;
        mPlaying = false;
        mInitialized = false;
        mGivenSurface = surface;
        mContext = context;
    }

    static private AudioTrack createAudioTrackAVSync(AudioFormat audioFormat, int audioSessionId) {
        int size = AudioTrack.getMinBufferSize(audioFormat.getSampleRate(),
                audioFormat.getChannelMask(), audioFormat.getEncoding());
        //
        AudioAttributes aa = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .setFlags(AudioAttributes.FLAG_HW_AV_SYNC)  // <<<<<
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        return new AudioTrack(aa, audioFormat, size * 3, AudioTrack.MODE_STREAM, audioSessionId);
    }

    @Override
    public boolean initialize(String contentUri) {
        Log.d(TAG, "[1]initialize");
        if (mRunning) {
            Log.w(TAG, "initialize failed. media is running.");
            return false;
        }
        mRunning = false;
        mContentUri = contentUri;

        MediaExtractor extractor = MyMediaUtil.createExtractor(mContentUri);
        if (extractor == null) {
            Log.w(TAG, "createExtractor failed.");
            return false;
        }

        mSurface = mGivenSurface;

        // get an audio session id for tunneling
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        int audioSessionId = audioManager.generateAudioSessionId();

        int videoTrackIndex, audioTrackIndex;
        {
            int[] ii = MyMediaUtil.findTrackAVIndex(extractor);
            videoTrackIndex = ii[0];
            audioTrackIndex = ii[1];
            if (videoTrackIndex < 0) {
                Log.w(TAG, "videoTrack not found");
                return false;
            }
            if (audioTrackIndex < 0) {
                Log.w(TAG, "audioTrack not found");
                return false;
            }
            Log.d(TAG, "videoTrackIndex=" + videoTrackIndex + " audioTrackIndex=" + audioTrackIndex);
        }
        MediaFormat audioMediaFormat = extractor.getTrackFormat(audioTrackIndex);
        MediaFormat videoMediaFormat = extractor.getTrackFormat(videoTrackIndex);

        // enable tunneled playback (tunneling)
        videoMediaFormat.setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback, true);
        // set audio session ID
        videoMediaFormat.setInteger(MediaFormat.KEY_AUDIO_SESSION_ID, audioSessionId);

        mAudioTrack = createAudioTrackAVSync(
                MyMediaUtil.createAudioFormatFromMediaFormat(audioMediaFormat),
                audioSessionId
        );
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.w(TAG, "AudioTrack is uninitialized");
        }

        mVideoExtractor = MyMediaUtil.createExtractor(mContentUri);
        assert mVideoExtractor != null;
        mVideoExtractor.selectTrack(videoTrackIndex);
        mVideoCodec = MyMediaUtil.createVideoDecoder(videoMediaFormat, mSurface);

        // check supported
        if (mVideoCodec.getCodecInfo()
                .getCapabilitiesForType(videoMediaFormat.getString(MediaFormat.KEY_MIME))
                .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback)) {
            Log.w(TAG, "VideoCodec is not supported TunneledPlayBack!!");
        }

        mAudioExtractor = MyMediaUtil.createExtractor(mContentUri);
        assert mAudioExtractor != null;
        mAudioExtractor.selectTrack(audioTrackIndex);
        mAudioCodec = MyMediaUtil.createAudioDecoder(extractor.getTrackFormat(audioTrackIndex));

        mWidth = videoMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = videoMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

        mVideoCodec.setCallback(new CodecCallback(mVideoExtractor, mAudioTrack, false));
        mAudioCodec.setCallback(new CodecCallback(mAudioExtractor, mAudioTrack, true));

        mInitialized = true;

        Log.d(TAG, "[2]initialize");
        return true;
    }

    @Override
    public void setSurface(@NonNull Surface s) {
        mGivenSurface = s;
        if (mVideoCodec == null) return;
        mSurface = mGivenSurface;
        mVideoCodec.setOutputSurface(mSurface);
    }


    @Override
    public void release() {
        Log.d(TAG, "[1]release");
        if (!mInitialized)
            return;
        pause();
        mRunning = false;

//        mSync.release();
        mVideoCodec.stop();
        mVideoCodec.release();
        mAudioCodec.stop();
        mAudioCodec.release();
        mVideoExtractor.release();
        mAudioExtractor.release();
        mAudioTrack.release();
        mVideoExtractor = null;
        mVideoCodec = null;
        mAudioExtractor = null;
        mAudioCodec = null;
        mAudioTrack = null;
//        mSync = null;
        mWidth = mHeight = -1;
        mInitialized = false;
        Log.d(TAG, "[2]release");
    }


    @Override
    public void run() {
        Log.d(TAG, "[1]run");
        if (mRunning) {
            Log.d(TAG, "already running");
            return;
        }
        mRunning = true;

        mVideoCodec.start();
        mAudioCodec.start();

        Log.d(TAG, "[2]run");
    }

    @Override
    public void resume() {
//        mSync.setPlaybackParams(new PlaybackParams().setSpeed(1.f));
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.w(TAG, "AudioTrack is not initialized. do nothing.");
            return;
        }
        mAudioTrack.play();
        mPlaying = true;
    }

    @Override
    public void pause() {
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.w(TAG, "AudioTrack is not initialized. do nothing.");
            return;
        }
        mAudioTrack.pause();
        mPlaying = false;
    }

//    @Override
//    public void stop() {
//        Log.d(TAG, "stop [1]");
//        mSync.setPlaybackParams(new PlaybackParams().setSpeed(0.f));
//        mSync.flush();
//        mVideoCodec.stop();  // bring to uninitialized state...
//        mAudioCodec.stop();
//        mAudioTrack.pause();
//        mAudioTrack.flush();
//        mAudioExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//        mVideoExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//        mRunning = false;
//        Log.d(TAG, "stop [2]");
//    }

    @Override
    public void seekTo(long timeUs) {
        mAudioExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        mVideoExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public boolean isPlaying() {
        return mPlaying;
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }


    static class CodecCallback extends MediaCodec.Callback {
        MediaExtractor mExtractor;
        AudioTrack mAudioTrack;
        boolean mIsAudio;

        CodecCallback(MediaExtractor extractor, AudioTrack audioTrack, boolean isAudio) {
            mExtractor = extractor;
            mAudioTrack = audioTrack;
            mIsAudio = isAudio;
        }

        private String getTag() {
            return mIsAudio ? "Audio" : "Video";
        }

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int index) {
            Log.d(TAG, "onInputBufferAvailable " + getTag() + " i=" + index);
            ByteBuffer buffer = mediaCodec.getInputBuffer(index);
            if (buffer == null) {
                Log.w(TAG, "codec buffer is null");
                return;
            }
            int size = mExtractor.readSampleData(buffer, 0);
            if (size < 0) {
                Log.w(TAG, "track empty");
                return;
            }
            long timeMs = mExtractor.getSampleTime(); // microsecond
            mediaCodec.queueInputBuffer(
                    index, 0, size,
                    timeMs, 0);

            mExtractor.advance();
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
            Log.d(TAG, "onOutputBufferAvailable " + getTag() + " i=" + index + " timeMs=" + bufferInfo.presentationTimeUs / 1000);
            if (mIsAudio) {
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                if (buffer == null) {
                    Log.w(TAG, "onOutputBufferAvailable: buffer is null");
                    return;
                }
                // streaming mode on a HW_AV_SYNC track
                // WARNING: The timestamp, in nanoseconds!!!!
                mAudioTrack.write(buffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING, bufferInfo.presentationTimeUs * 1000);
                mediaCodec.releaseOutputBuffer(index, false);
//                mediaCodec.releaseOutputBuffer(index, bufferInfo.presentationTimeUs * 1000); // renderTimestampNs

            } else {
//                mediaCodec.releaseOutputBuffer(index, false);
//                mediaCodec.releaseOutputBuffer(index, bufferInfo.presentationTimeUs * 1000); // renderTimestampNs
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.e(TAG, "codec: onError " + getTag());
            e.printStackTrace();
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.w(TAG, "codec: onOutputFormatChanged " + getTag());
        }
    }

}
