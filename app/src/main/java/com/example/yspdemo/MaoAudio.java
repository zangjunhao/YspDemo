package com.example.yspdemo;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
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
    private static final String TAG = "maff";

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

    public void mediaPlay(Context context){
        File wavFile = new File(file, "audio.wav");
        if(wavFile.exists()){
            Log.d(TAG, "mediaPlay: ");
            MediaPlayer mediaPlayer = MediaPlayer.create(context, Uri.fromFile(wavFile));
            mediaPlayer.start();
        }
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
                    createWavFile(child);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void createWavFile(File pcmFile) {
        try {
            File wavFile = new File(file, "audio.wav");
            if(wavFile.exists()){
                wavFile.delete();
            }
            wavFile.createNewFile();
            FileInputStream in = new FileInputStream(pcmFile);
            FileOutputStream out = new FileOutputStream(wavFile);
            long audioLength = in.getChannel().size();//ChunkSize数据大小
            long audioDataLength = audioLength + 36;//加上wav头文件的大小
            //写入头文件
            WriteWaveFileHeader(out, audioLength, audioDataLength, 44100, AudioFormat.CHANNEL_OUT_MONO, 16 * 44100 * AudioFormat.CHANNEL_OUT_MONO / 8);
            byte[] bytes = new byte[audioSize];
            while (in.read(bytes) != -1) {
                out.write(bytes);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        try {
            out.write(header, 0, 44);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
