package com.example.yspdemo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.rtp.AudioCodec;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MaoCodec {
    private int audioTrack = -1;
    private Context context;
    private boolean notFinish = true;

    public MaoCodec(Context context) {
        this.context = context;
    }

    private MediaExtractor getAudioPcm() {
        MediaExtractor extractor = new MediaExtractor();
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.yp);
        try {
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioTrack = i;
                    extractor.selectTrack(i);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return extractor;
    }

    public void decodeMp3() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaExtractor extractor = getAudioPcm();
                    File parent = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecord");
                    File child = new File(parent, "audio.pcm");
                    FileOutputStream fos = new FileOutputStream(child);
                    MediaFormat format = extractor.getTrackFormat(audioTrack);
                    MediaCodec.BufferInfo inputInfo = new MediaCodec.BufferInfo();
                    MediaCodec audioCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
                    audioCodec.configure(format, null, null, 0);
                    audioCodec.start();
                    boolean temp = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        while (notFinish) {
                            int inputIndex = audioCodec.dequeueInputBuffer(0);
                            if (inputIndex >= 0) {
                                temp = false;
                                ByteBuffer inputBuffer = audioCodec.getInputBuffer(inputIndex);
                                inputBuffer.clear();
                                int sampleSize = extractor.readSampleData(inputBuffer, 0);
                                if (sampleSize < 0) {
                                    audioCodec.queueInputBuffer(inputIndex,
                                            0,
                                            0,
                                            0L,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    );
                                    notFinish = false;
                                } else {
                                    inputInfo.offset = 0;
                                    inputInfo.size = sampleSize;
                                    inputInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                                    inputInfo.presentationTimeUs = extractor.getSampleTime();
                                    Log.e("maff", "往解码器写入数据，当前时间戳：" + inputInfo.presentationTimeUs);
                                    //通知MediaCodec解码刚刚传入的数据
                                    audioCodec.queueInputBuffer(inputIndex, inputInfo.offset, sampleSize, inputInfo.presentationTimeUs, 0);
                                    extractor.advance();
                                }
                            } else {
                                if (!temp) {
                                    break;
                                }
                            }
                        }

                        byte[] pcmBytes;
                        boolean isFinishOut = false;
                        ByteBuffer outputBuffer = null;
                        while (!isFinishOut) {
                            int outputIndex = audioCodec.dequeueOutputBuffer(inputInfo, 0);
                            if (outputIndex < 0) {
                                continue;
                            } else {
                                outputBuffer = audioCodec.getOutputBuffer(outputIndex);
                            }
                            pcmBytes = new byte[inputInfo.size];
                            outputBuffer.get(pcmBytes);
                            outputBuffer.clear();
                            fos.write(pcmBytes);
                            fos.flush();
                            Log.e("maff", "释放输出流缓冲区：" + outputIndex);
                            audioCodec.releaseOutputBuffer(outputIndex, false);
                            Log.e("maff", "编解码结束");
                            extractor.release();
                            audioCodec.stop();
                            audioCodec.release();
                            notFinish = false;
                            isFinishOut = true;
                        }
                        fos.close();
                    } else {
                        return;
                    }
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void enCodeAAC() {
        File parent = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecord");
        File audioPcm = new File(parent, "audioPcm.pcm");
        byte[] buffer = new byte[8*1024];
        byte[] allAudioBytes;

        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;

        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);
        MediaCodec mediaEncode = null;
        try {
            FileInputStream fis = new FileInputStream(audioPcm);
            mediaEncode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaEncode.start();
            ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
            ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
            MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();
            File audioPath = new File(parent, "audioPcm.aac");
            //初始化文件写入流
            FileOutputStream fos = new FileOutputStream(audioPath);
            BufferedOutputStream bos = new BufferedOutputStream(fos,500 * 1024);
            boolean isReadEnd = false;
            while (!isReadEnd){
                for (int i = 0;i < encodeInputBuffers.length - 1;i++){//减掉1很重要，不要忘记
                    if (fis.read(buffer) != -1){
                        allAudioBytes = Arrays.copyOf(buffer,buffer.length);
                    } else {
                        Log.e("maff","文件读取完成");
                        isReadEnd = true;
                        break;
                    }
                    Log.e("maff","读取文件并写入编码器" + allAudioBytes.length);
                    inputIndex = mediaEncode.dequeueInputBuffer(-1);
                    inputBuffer = encodeInputBuffers[inputIndex];
                    inputBuffer.clear();
                    inputBuffer.limit(allAudioBytes.length);
                    inputBuffer.put(allAudioBytes);//将pcm数据填充给inputBuffer
                    mediaEncode.queueInputBuffer(inputIndex,0,allAudioBytes.length,0,0);//开始编码
                }
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo,10000);
                while (outputIndex >= 0){
                    //从解码器中取出数据
                    outBitSize = encodeBufferInfo.size;
                    outPacketSize = outBitSize + 7;//7为adts头部大小
                    outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出的buffer
                    outputBuffer.position(encodeBufferInfo.offset);
                    outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                    chunkAudio = new byte[outPacketSize];
//                    AudioCodec.addADTStoPacket(chunkAudio,outPacketSize);//添加ADTS
                    outputBuffer.get(chunkAudio,7,outBitSize);//将编码得到的AAC数据取出到byte[]中，偏移量为7
                    outputBuffer.position(encodeBufferInfo.offset);
                    Log.e("maff","编码成功并写入文件" + chunkAudio.length);
                    bos.write(chunkAudio,0,chunkAudio.length);//将文件保存在sdcard中
                    bos.flush();

                    mediaEncode.releaseOutputBuffer(outputIndex,false);
                    outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo,10000);
                }
            }
            mediaEncode.stop();
            mediaEncode.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
