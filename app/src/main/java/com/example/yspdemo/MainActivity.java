package com.example.yspdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isRecording;
    private File file;
    private int audioSize;
    private MaoAudio maoAudio;
    private CameraDevice cameraDevice;
    private String cameraId;
    private final static String TAG = "Yspdemo_MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExtractorTest();
    }


    private void ExtractorTest() {
        setContentView(R.layout.activity_extractor);
        MaoExtractor maoExtractor = new MaoExtractor();
        maoExtractor.extracMedia(this);
        SurfaceView surfaceView = findViewById(R.id.surface);
        final SurfaceHolder surfaceHolder = surfaceView.getHolder();
        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Muxer" + "/muxer.mp4");
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                MediaPlayer mediaPlayer;
                if (file.exists()) {
//                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.testmv);
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.fromFile(file));
                    mediaPlayer.setDisplay(surfaceHolder);
                    mediaPlayer.start();
                    Log.d(TAG, "surfaceCreated: ");

                }


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @SuppressLint("MissingPermission")
    private void CameraNv21() {
        setContentView(R.layout.activity_camera);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.CAMERA,
                    },
                    0);
        }
        CameraManager cameraManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIdList = new String[0];//手机的相机列表
            try {
                cameraIdList = cameraManager.getCameraIdList();
                for (String s : cameraIdList) {//找到后置摄像头
                    CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(s);
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = s;
                        break;
                    }
                }


                final TextureView textureView = findViewById(R.id.surface);
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                            @Override
                            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                                Surface surface1 = new Surface(surface);
                                try {
                                    final CaptureRequest.Builder captureRequest;
                                    captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    captureRequest.addTarget(surface1);
                                    cameraDevice.createCaptureSession(Arrays.asList(surface1), new CameraCaptureSession.StateCallback() {
                                        @Override
                                        public void onConfigured(@NonNull CameraCaptureSession session) {
                                            CameraCaptureSession session1 = session;
                                            try {
                                                session1.setRepeatingRequest(captureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                                                    @Override
                                                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                                        super.onCaptureCompleted(session, request, result);
                                                    }
                                                }, null);
                                            } catch (CameraAccessException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                                        }
                                    }, null);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                            }

                            @Override
                            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                                return false;
                            }

                            @Override
                            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                            }
                        });
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                }, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return;
        }

    }

    private void playAudio() {
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    0);
        }
        maoAudio = MaoAudio.getSingleAudio();
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maoAudio.mediaPlay(getApplicationContext());
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maoAudio.stop();
            }
        });
        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maoAudio.record();
            }
        });
    }

}


