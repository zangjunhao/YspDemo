package com.example.yspdemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MaoAudio {

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isRecording;
    File file;
    private int audioSize;

    private static MaoAudio singleAudio = new MaoAudio();

    private MaoAudio() {
        audioSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioSize);
        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecord");
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static MaoAudio getSingleAudio() {
        return singleAudio;
    }

    public void record() {
        getAudio();
    }

    public void stop() {
        isRecording = false;
    }

    public void play() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File child = new File(file, "audio.pcm");
                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(child)));
                    int trackSize = AudioTrack.getMinBufferSize(44100,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    // 音频流的类型
                    // STREAM_ALARM：警告声
                    // STREAM_MUSIC：音乐声
                    // STREAM_RING：铃声
                    // STREAM_SYSTEM：系统声音，例如低电提示音，锁屏音等
                    // STREAM_VOCIE_CALL：通话声
                    int streamType = AudioManager.STREAM_MUSIC;

                    // 采样率 Hz
                    int sampleRate = 44100;
                    // 单声道
                    int channelConfig = AudioFormat.CHANNEL_OUT_MONO;

                    // 音频数据表示的格式
                    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

                    // MODE_STREAM：在这种模式下，通过write一次次把音频数据写到AudioTrack中。这和平时通过
                    // write系统调用往文件中写数据类似，但这种工作方式每次都需要把数据从用户提供的Buffer中拷贝到
                    // AudioTrack内部的Buffer中，这在一定程度上会使引入延时。为解决这一问题，AudioTrack就引入
                    // 了第二种模式。

                    // MODE_STATIC：这种模式下，在play之前只需要把所有数据通过一次write调用传递到AudioTrack
                    // 中的内部缓冲区，后续就不必再传递数据了。这种模式适用于像铃声这种内存占用量较小，延时要求较
                    // 高的文件。但它也有一个缺点，就是一次write的数据不能太多，否则系统无法分配足够的内存来存储
                    // 全部数据。
                    int mode = AudioTrack.MODE_STREAM;

                    int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                    audioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, 2048), mode);
                    byte[] datas = new byte[trackSize];
                    audioTrack.play();
                    while (true) {
                        int i = 0;
                        try {
                            while (dataInputStream.available() > 0 && i < datas.length) {
                                datas[i] = dataInputStream.readByte();
                                i++;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        audioTrack.write(datas, 0, datas.length);
                        //表示读取完了
                        if (i != trackSize) {
                            audioTrack.stop();
                            audioTrack.release();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void getAudio() {
        isRecording = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                File child = new File(file, "audio.pcm");
                if (child.exists()) {
                    child.delete();
                }

                try {
                    child.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(child)));
                    byte[] bytes = new byte[audioSize];
                    audioRecord.startRecording();
                    int r = 0;
                    while (isRecording) {
                        int readResult = audioRecord.read(bytes, 0, audioSize);
                        for (int i = 0; i < readResult; i++) {
                            outputStream.write(bytes[i]);
                            Log.d("maff", "run: " + bytes[i]);
                        }
                        r++;
                    }
                    audioRecord.stop();
                    audioRecord.release();
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
