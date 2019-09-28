package com.example.yspdemo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MaoExtractor {
    int videoTrackIndex = -1;
    private static final String TAG = "MaoExtractor";
    public void extracMedia(Context context) {
        MediaExtractor mediaExtractor = null;
        MediaMuxer mMediaMuxer = null;
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Muxer" );
        if (!file.exists()){
            file.mkdirs();
        }
        File extractorFile = new File(file,"muxer.mp4");
        if(extractorFile.exists()){
            extractorFile.delete();
            try {
                extractorFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            mMediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Muxer" + "/muxer.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaExtractor = new MediaExtractor();
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.testmv);
            mediaExtractor.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            Log.d(TAG, "extracMedia: ");
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    mediaExtractor.selectTrack(i);
                    videoTrackIndex = i;
                    mMediaMuxer.addTrack(format);
                    break;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "extracMedia: "+e);
        }
        mMediaMuxer.start();
        ByteBuffer byteBuffer = ByteBuffer.allocate(100 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        while (true) {
            int size = mediaExtractor.readSampleData(byteBuffer, 0);
            if (size < 0) {
                break;
            }
            info.offset = 0;
            info.size = size;
            info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            info.presentationTimeUs = mediaExtractor.getSampleTime();
            mMediaMuxer.writeSampleData(videoTrackIndex,byteBuffer,info);
            mediaExtractor.advance();
        }
        mediaExtractor.release();
        mMediaMuxer.stop();
        mMediaMuxer.release();
    }
}
