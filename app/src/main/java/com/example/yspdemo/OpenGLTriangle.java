package com.example.yspdemo;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLTriangle implements GLSurfaceView.Renderer {
    private float[] trianglePoint = new float[]{//opengl是三维的，分别表示xyz，所以设置z轴为0
            //Order of coordinates: X, Y, Z, R,G,B,
            -0.5f, 0.5f, 0.0f, //  0.top left RED
            -0.5f, -0.5f, 0.0f,  //  1.bottom right Blue
            0.5f, 0.5f, 0.0f,   //  3.top right WHITE
            0.5f, -0.5f, 0.0f  //  2.bottom left GREEN
    };
    private int programId;
    private int uMatrix;
    private float[] mProjectionMatrix = new float[16];
    private String vertextShader =
            "#version 300 es\n" +
                    "uniform mat4 u_Matrix;\n"+
                    "in vec4 vPosition;\n" +//vec4 四整数向量
                    "void main() {\n" +
                    "     gl_Position  = vPosition*u_Matrix;\n" +
                    "}\n";
    private String fragmentShader =
            "#version 300 es\n" +
                    "precision mediump float;\n" +
                    "out vec4 fragColor;\n" +
                    "void main() {\n" +
                    "     fragColor = vec4(1.0,1.0,1.0,1.0);\n" +
                    "}\n";
    private FloatBuffer buffer;
    public OpenGLTriangle(){
        //一个浮点数占4个字节
        buffer = ByteBuffer.allocateDirect(trianglePoint.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(trianglePoint);
        buffer.position(0);
    }

    private static int compileShader(int type, String shaderCode){
        //创建着色器
        final int shaderId = GLES30.glCreateShader(type);
        if(shaderId != 0){
            //加载到着色器
            GLES30.glShaderSource(shaderId,shaderCode);
            //编译着色器
            GLES30.glCompileShader(shaderId);
            //检测状态
            final int[] compileStatus = new int[1];
            GLES30.glGetShaderiv(shaderId,GLES30.GL_COMPILE_STATUS,compileStatus,0);
            if(compileStatus[0] == 0){
                String logInfo = GLES30.glGetShaderInfoLog(shaderId);
                System.err.println(logInfo);
                GLES30.glDeleteShader(shaderId);
                return 0;
            }
            return shaderId;
        }else {
            return 0;
        }
    }

    public  int linkProgram(int vertexShaderId, int fragmentShaderId) {
        programId = GLES30.glCreateProgram();
        if (programId != 0) {
            //将顶点着色器加入到程序
            GLES30.glAttachShader(programId, vertexShaderId);
            //将片元着色器加入到程序中
            GLES30.glAttachShader(programId, fragmentShaderId);
            //链接着色器程序
            GLES30.glLinkProgram(programId);
            final int[] linkStatus = new int[1];
            //验证OpenGL程序是否可用
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                String logInfo = GLES30.glGetProgramInfoLog(programId);
                System.err.println(logInfo);
                GLES30.glDeleteProgram(programId);
                return 0;
            }
            return programId;
        } else {
            //创建失败
            return 0;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //设置背景颜色
        GLES30.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

        // 编译顶点着色器
        final int vertexShaderId = compileShader(GLES30.GL_VERTEX_SHADER, vertextShader);
        // 编译片段着色器
        final int fragmentShaderId = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);
        //在OpenGLES环境中使用程序
        GLES30.glUseProgram(linkProgram(vertexShaderId, fragmentShaderId));
        uMatrix = GLES30.glGetUniformLocation(programId,"u_Matrix" );

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视图窗口
        GLES30.glViewport(0, 0, width, height);
        //主要还是长宽进行比例缩放
        float aspectRatio = width > height ?
                (float) width / (float) height :
                (float) height / (float) width;

        if (width > height) {
            //横屏。需要设置的就是左右。
            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1f, -1.f, 1f);
        } else {
            //竖屏。需要设置的就是上下
            Matrix.orthoM(mProjectionMatrix, 0, -1, 1f, -aspectRatio, aspectRatio, -1.f, 1f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //把颜色缓冲区设置为我们预设的颜色
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        //准备坐标数据
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, buffer);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(0);

        //绘制三个点
//        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 3);

        //绘制直线
//        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, 2);
//        GLES30.glLineWidth(10);
        GLES30.glUniformMatrix4fv(uMatrix,1,false,mProjectionMatrix,0);
        //绘制三角形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(0);
    }
}
