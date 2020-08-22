package com.example.mediaprac;

import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.media.PlaybackParams;
import android.media.SyncParams;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class MyMediaAsync implements MyMedia {

    static final String TAG = "MyMedia";

    private String mContentUri = "https://ia600603.us.archive.org/30/items/Tears-of-Steel/tears_of_steel_1080p.mp4";

    private boolean mRunning, mPlaying, mInitialized;
    private Surface mGivenSurface, mSurface;
    private AudioTrack mAudioTrack;
    private MediaSync mSync;
    private MediaExtractor mVideoExtractor, mAudioExtractor;
    private MediaCodec mVideoCodec, mAudioCodec;

    private int mWidth = -1, mHeight = -1;

    public MyMediaAsync(Surface surface) {
        assert surface != null;
        mRunning = false;
        mPlaying = false;
        mInitialized = false;
        mGivenSurface = surface;
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

        mSync = new MediaSync();
        mSync.setPlaybackParams(new PlaybackParams().setSpeed(0.f));
        mSync.setSurface(mGivenSurface);
        mSurface = mSync.createInputSurface();

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

        mVideoExtractor = MyMediaUtil.createExtractor(mContentUri);
        assert mVideoExtractor != null;
        mVideoExtractor.selectTrack(videoTrackIndex);
        mVideoCodec = MyMediaUtil.createVideoDecoder(videoMediaFormat, mSurface);

        mAudioExtractor = MyMediaUtil.createExtractor(mContentUri);
        assert mAudioExtractor != null;
        mAudioExtractor.selectTrack(audioTrackIndex);
        mAudioCodec = MyMediaUtil.createAudioDecoder(audioMediaFormat);

        mAudioTrack = MyMediaUtil.createAudioTrack(MyMediaUtil.createAudioFormatFromMediaFormat(audioMediaFormat));
        mSync.setAudioTrack(mAudioTrack);

        mSync.setSyncParams(new SyncParams()
                .setSyncSource(SyncParams.SYNC_SOURCE_DEFAULT));

        mWidth = videoMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = videoMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

        mVideoCodec.setCallback(new CodecCallback(mVideoExtractor, mSync, false));
        mAudioCodec.setCallback(new CodecCallback(mAudioExtractor, mSync, true));

        mSync.setCallback(new MediaSync.Callback() {
            @Override
            public void onAudioBufferConsumed(MediaSync sync, ByteBuffer audioBuffer, int bufferId) {
                Log.d(TAG, "onAudioBufferConsumed " + bufferId);
                mAudioCodec.releaseOutputBuffer(bufferId, true);
            }
        }, null);// This needs to be done since sync is paused on creation.


        mInitialized = true;

        Log.d(TAG, "[2]initialize");
        return true;
    }

    @Override
    public void setSurface(@NonNull Surface s) {
        mGivenSurface = s;
        if (mVideoCodec == null) return;
        assert mSync != null;
        mSync.setSurface(mGivenSurface);
    }

    @Override
    public void release() {
        Log.d(TAG, "[1]release");
        if (!mInitialized)
            return;
        pause();
        mRunning = false;

        mSync.release();
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
        mSync = null;
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
        mSync.setPlaybackParams(new PlaybackParams().setSpeed(1.f));
        mAudioTrack.play();
        mPlaying = true;
    }

    @Override
    public void pause() {
        mSync.setPlaybackParams(new PlaybackParams().setSpeed(0.f));
        mAudioTrack.pause();
//        mSync.flush();
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
        MediaSync mSync;
        boolean mIsAudio;

        CodecCallback(MediaExtractor extractor, MediaSync sync, boolean isAudio) {
            mExtractor = extractor;
            mSync = sync;
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
            long time = mExtractor.getSampleTime();
            Log.d(TAG, "sampleTime=" + time);
            mediaCodec.queueInputBuffer(
                    index, 0, size,
                    time, 0);

            mExtractor.advance();
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
            Log.d(TAG, "onOutputBufferAvailable " + getTag() + " i=" + index + " timeMs=" + bufferInfo.presentationTimeUs / 1000);
            if (mIsAudio) {
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                if (buffer == null) {
                    Log.w(TAG, "buffer is null");
                    return;
                }
                mSync.queueAudio(buffer, index, bufferInfo.presentationTimeUs);
            } else {

                mediaCodec.releaseOutputBuffer(index, bufferInfo.presentationTimeUs * 1000); // renderTimestampNs
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
