package kharico.granularsynthesizer.gl;

/**
 * Created by fxpa72 on 6/13/2016.
 */
import android.content.Context;
import android.graphics.*;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class VideoTextureRenderer extends TextureSurfaceRenderer implements SurfaceTexture.OnFrameAvailableListener
{

    public static volatile float shaderVal = 0.f;

    private static final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec4 vTexCoordinate;" +
                    "uniform mat4 textureTransform;" +
                    "varying vec2 v_TexCoordinate;" +
                    "void main() {" +
                    "   v_TexCoordinate = (textureTransform * vTexCoordinate).xy;" +
                    "   gl_Position = vPosition;" +
                    "}";

    private static final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "float rand(vec4 co){" +
                    "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                    "}" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    gl_FragColor = color;" +
                    //"    gl_FragColor = vec4(rand(color));" +
                    "}";

    private static final String sepiaFragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    //"precision mediump float;" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "float rand(vec4 co){" +
                    "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                    "}" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    lowp vec4 outputColor;" +
                    "    outputColor.r = (color.r * 0.393) + (color.g * 0.769) + (color.b * 0.189);" +
                    "    outputColor.g = (color.r * 0.349) + (color.g * 0.686) + (color.b * 0.168);" +
                    "    outputColor.b = (color.r * 0.272) + (color.g * 0.534) + (color.b * 0.131);" +
                    "    outputColor.a = 1.0;" +
                    "    gl_FragColor = outputColor;" +
                    //"    gl_FragColor = vec4(rand(outputColor))*outputColor;" +
                    "}";


    private static final String sepiaFragmentShaderCode_1 =
            "#extension GL_OES_EGL_image_external : require\n" +
                    //"precision mediump float;" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "float rand(vec4 co){" +
                    "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                    "}" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    lowp vec4 tempColor = vec4(0.8*color + 0.2*rand(color));" +
                    "    lowp vec4 outputColor;" +
                    "    outputColor.r = (tempColor.r * 0.393) + (tempColor.g * 0.769) + (tempColor.b * 0.189);" +
                    "    outputColor.g = (tempColor.r * 0.349) + (tempColor.g * 0.686) + (tempColor.b * 0.168);" +
                    "    outputColor.b = (tempColor.r * 0.272) + (tempColor.g * 0.534) + (tempColor.b * 0.131);" +
                    "    outputColor.a = 1.0;" +
                    "    gl_FragColor = outputColor;" +
                    "}";

    private static final String sepiaFragmentShaderCode_2 =
        "#extension GL_OES_EGL_image_external : require\n" +
                "uniform samplerExternalOES texture;" +
                "varying vec2 v_TexCoordinate;" +
                "float rand(vec4 co){" +
                "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                "}" +
                "void main () {" +
                "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                "    lowp vec4 tempColor = vec4(0.6*color + 0.4*rand(color));" +
                "    lowp vec4 outputColor;" +
                "    outputColor.r = (tempColor.r * 0.393) + (tempColor.g * 0.769) + (tempColor.b * 0.189);" +
                "    outputColor.g = (tempColor.r * 0.349) + (tempColor.g * 0.686) + (tempColor.b * 0.168);" +
                "    outputColor.b = (tempColor.r * 0.272) + (tempColor.g * 0.534) + (tempColor.b * 0.131);" +
                "    outputColor.a = 1.0;" +
                "    gl_FragColor = outputColor;" +
                "}";

    private static final String sepiaFragmentShaderCode_3 =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "float rand(vec4 co){" +
                    "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                    "}" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    lowp vec4 tempColor = vec4(0.4*color + 0.6*rand(color));" +
                    "    lowp vec4 outputColor;" +
                    "    outputColor.r = (tempColor.r * 0.393) + (tempColor.g * 0.769) + (tempColor.b * 0.189);" +
                    "    outputColor.g = (tempColor.r * 0.349) + (tempColor.g * 0.686) + (tempColor.b * 0.168);" +
                    "    outputColor.b = (tempColor.r * 0.272) + (tempColor.g * 0.534) + (tempColor.b * 0.131);" +
                    "    outputColor.a = 1.0;" +
                    "    gl_FragColor = outputColor;" +
                    "}";

    private static final String sepiaFragmentShaderCode_4 =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "float rand(vec4 co){" +
                    "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                    "}" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    lowp vec4 tempColor = vec4(0.2*color + 0.8*rand(color));" +
                    "    lowp vec4 outputColor;" +
                    "    outputColor.r = (tempColor.r * 0.393) + (tempColor.g * 0.769) + (tempColor.b * 0.189);" +
                    "    outputColor.g = (tempColor.r * 0.349) + (tempColor.g * 0.686) + (tempColor.b * 0.168);" +
                    "    outputColor.b = (tempColor.r * 0.272) + (tempColor.g * 0.534) + (tempColor.b * 0.131);" +
                    "    outputColor.a = 1.0;" +
                    "    gl_FragColor = outputColor;" +
                    "}";

    private static final String sepiaFragmentShaderCode_5 =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "float rand(vec4 co){" +
                    "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);" +
                    "}" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    lowp vec4 tempColor = vec4(rand(color));" +
                    "    lowp vec4 outputColor;" +
                    "    outputColor.r = (tempColor.r * 0.393) + (tempColor.g * 0.769) + (tempColor.b * 0.189);" +
                    "    outputColor.g = (tempColor.r * 0.349) + (tempColor.g * 0.686) + (tempColor.b * 0.168);" +
                    "    outputColor.b = (tempColor.r * 0.272) + (tempColor.g * 0.534) + (tempColor.b * 0.131);" +
                    "    outputColor.a = 1.0;" +
                    "    gl_FragColor = outputColor;" +
                    "}";

    private static float squareSize = 1.0f;
    private static float squareCoords[] = { -squareSize,  squareSize, 0.0f,   // top left
            -squareSize, -squareSize, 0.0f,   // bottom left
            squareSize, -squareSize, 0.0f,   // bottom right
            squareSize,  squareSize, 0.0f }; // top right

    private static short drawOrder[] = { 0, 1, 2, 0, 2, 3};

    private Context ctx;

    // Texture to be shown in background
    private FloatBuffer textureBuffer;
    private float textureCoords[] = { 0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f };
    private int[] textures = new int[1];

    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    private int FilterFragmentShaderHandle;
    private int FilterFragmentShaderHandle_1;
    private int FilterFragmentShaderHandle_2;
    private int FilterFragmentShaderHandle_3;
    private int FilterFragmentShaderHandle_4;
    private int FilterFragmentShaderHandle_5;
    private int shaderProgram;
    private int filterShaderProgram;
    private int filterShaderProgram_1;
    private int filterShaderProgram_2;
    private int filterShaderProgram_3;
    private int filterShaderProgram_4;
    private int filterShaderProgram_5;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private SurfaceTexture videoTexture;
    private float[] videoTextureTransform;
    private boolean frameAvailable = false;

    private int videoWidth;
    private int videoHeight;
    private boolean adjustViewport = false;

    public boolean sepiaFilterOn;

    public VideoTextureRenderer(Context context, SurfaceTexture texture, int width, int height)
    {
        super(texture, width, height);
        this.ctx = context;
        videoTextureTransform = new float[16];
        sepiaFilterOn = false;
    }

    private void loadShaders()
    {

        //default shader
        vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShaderHandle, vertexShaderCode);
        GLES20.glCompileShader(vertexShaderHandle);
        checkGlError("Vertex shader compile");

        fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
        Log.d("loadShaders", "normal");

        GLES20.glCompileShader(fragmentShaderHandle);
        checkGlError("Pixel shader compile");

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShaderHandle);
        GLES20.glAttachShader(shaderProgram, fragmentShaderHandle);
        GLES20.glLinkProgram(shaderProgram);
        checkGlError("Shader program compile");

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(shaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

        //sepia shader
        FilterFragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(FilterFragmentShaderHandle, sepiaFragmentShaderCode);
        Log.d("loadSepiaShaders", "normal");

        GLES20.glCompileShader(FilterFragmentShaderHandle);
        checkGlError("Pixel shader compile");

        filterShaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(filterShaderProgram, vertexShaderHandle);
        GLES20.glAttachShader(filterShaderProgram, FilterFragmentShaderHandle);
        GLES20.glLinkProgram(filterShaderProgram);
        checkGlError("Shader program compile");

        GLES20.glGetProgramiv(filterShaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(filterShaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

        //sepia shader 1
        FilterFragmentShaderHandle_1 = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(FilterFragmentShaderHandle_1, sepiaFragmentShaderCode_1);
        Log.d("loadSepiaShaders", "normal");

        GLES20.glCompileShader(FilterFragmentShaderHandle_1);
        checkGlError("Pixel shader compile");

        filterShaderProgram_1 = GLES20.glCreateProgram();
        GLES20.glAttachShader(filterShaderProgram_1, vertexShaderHandle);
        GLES20.glAttachShader(filterShaderProgram_1, FilterFragmentShaderHandle_1);
        GLES20.glLinkProgram(filterShaderProgram_1);
        checkGlError("Shader program compile");

        GLES20.glGetProgramiv(filterShaderProgram_1, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(filterShaderProgram_1);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

        //sepia shader 2
        FilterFragmentShaderHandle_2 = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(FilterFragmentShaderHandle_2, sepiaFragmentShaderCode_2);
        Log.d("loadSepiaShaders", "normal");

        GLES20.glCompileShader(FilterFragmentShaderHandle_2);
        checkGlError("Pixel shader compile");

        filterShaderProgram_2 = GLES20.glCreateProgram();
        GLES20.glAttachShader(filterShaderProgram_2, vertexShaderHandle);
        GLES20.glAttachShader(filterShaderProgram_2, FilterFragmentShaderHandle_2);
        GLES20.glLinkProgram(filterShaderProgram_2);
        checkGlError("Shader program compile");

        GLES20.glGetProgramiv(filterShaderProgram_2, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(filterShaderProgram_2);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

        //sepia shader 3
        FilterFragmentShaderHandle_3 = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(FilterFragmentShaderHandle_3, sepiaFragmentShaderCode_3);
        Log.d("loadSepiaShaders", "normal");

        GLES20.glCompileShader(FilterFragmentShaderHandle_3);
        checkGlError("Pixel shader compile");

        filterShaderProgram_3 = GLES20.glCreateProgram();
        GLES20.glAttachShader(filterShaderProgram_3, vertexShaderHandle);
        GLES20.glAttachShader(filterShaderProgram_3, FilterFragmentShaderHandle_3);
        GLES20.glLinkProgram(filterShaderProgram_3);
        checkGlError("Shader program compile");

        GLES20.glGetProgramiv(filterShaderProgram_3, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(filterShaderProgram_3);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

        //sepia shader 4
        FilterFragmentShaderHandle_4 = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(FilterFragmentShaderHandle_4, sepiaFragmentShaderCode_4);
        Log.d("loadSepiaShaders", "normal");

        GLES20.glCompileShader(FilterFragmentShaderHandle_4);
        checkGlError("Pixel shader compile");

        filterShaderProgram_4 = GLES20.glCreateProgram();
        GLES20.glAttachShader(filterShaderProgram_4, vertexShaderHandle);
        GLES20.glAttachShader(filterShaderProgram_4, FilterFragmentShaderHandle_4);
        GLES20.glLinkProgram(filterShaderProgram_4);
        checkGlError("Shader program compile");

        GLES20.glGetProgramiv(filterShaderProgram_4, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(filterShaderProgram_4);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

        //sepia shader 5
        FilterFragmentShaderHandle_5 = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(FilterFragmentShaderHandle_5, sepiaFragmentShaderCode_5);
        Log.d("loadSepiaShaders", "normal");

        GLES20.glCompileShader(FilterFragmentShaderHandle_5);
        checkGlError("Pixel shader compile");

        filterShaderProgram_5 = GLES20.glCreateProgram();
        GLES20.glAttachShader(filterShaderProgram_5, vertexShaderHandle);
        GLES20.glAttachShader(filterShaderProgram_5, FilterFragmentShaderHandle_5);
        GLES20.glLinkProgram(filterShaderProgram_5);
        checkGlError("Shader program compile");

        GLES20.glGetProgramiv(filterShaderProgram_5, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(filterShaderProgram_5);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

    }

    private void setupVertexBuffer()
    {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder. length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }


    private void setupTexture(Context context)
    {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");

        videoTexture = new SurfaceTexture(textures[0]);
        videoTexture.setOnFrameAvailableListener(this);
    }

    @Override
    protected boolean draw()
    {
        synchronized (this)
        {
            if (frameAvailable)
            {
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            }
            else
            {
                return false;
            }

        }

        if (adjustViewport)
            adjustViewport();

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw texture
        GLES20.glUseProgram(shaderProgram);
        int textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "texture");
        int textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");

        if (sepiaFilterOn) {

            if (shaderVal == 0.f) {
                GLES20.glUseProgram(filterShaderProgram);
                textureParamHandle = GLES20.glGetUniformLocation(filterShaderProgram, "texture");
                textureCoordinateHandle = GLES20.glGetAttribLocation(filterShaderProgram, "vTexCoordinate");
                positionHandle = GLES20.glGetAttribLocation(filterShaderProgram, "vPosition");
                textureTranformHandle = GLES20.glGetUniformLocation(filterShaderProgram, "textureTransform");
            }
            else if (shaderVal >= 0.f && shaderVal < 0.2f) {
                GLES20.glUseProgram(filterShaderProgram_1);
                textureParamHandle = GLES20.glGetUniformLocation(filterShaderProgram_1, "texture");
                textureCoordinateHandle = GLES20.glGetAttribLocation(filterShaderProgram_1, "vTexCoordinate");
                positionHandle = GLES20.glGetAttribLocation(filterShaderProgram_1, "vPosition");
                textureTranformHandle = GLES20.glGetUniformLocation(filterShaderProgram_1, "textureTransform");
            }
            else if (shaderVal >= 0.2f && shaderVal < 0.4f) {
                GLES20.glUseProgram(filterShaderProgram_2);
                textureParamHandle = GLES20.glGetUniformLocation(filterShaderProgram_2, "texture");
                textureCoordinateHandle = GLES20.glGetAttribLocation(filterShaderProgram_2, "vTexCoordinate");
                positionHandle = GLES20.glGetAttribLocation(filterShaderProgram_2, "vPosition");
                textureTranformHandle = GLES20.glGetUniformLocation(filterShaderProgram_2, "textureTransform");
            }
            else if (shaderVal >= 0.4f && shaderVal < 0.6f) {
                GLES20.glUseProgram(filterShaderProgram_3);
                textureParamHandle = GLES20.glGetUniformLocation(filterShaderProgram_3, "texture");
                textureCoordinateHandle = GLES20.glGetAttribLocation(filterShaderProgram_3, "vTexCoordinate");
                positionHandle = GLES20.glGetAttribLocation(filterShaderProgram_3, "vPosition");
                textureTranformHandle = GLES20.glGetUniformLocation(filterShaderProgram_3, "textureTransform");
            }
            else if (shaderVal >= 0.6f && shaderVal < 0.8f) {
                GLES20.glUseProgram(filterShaderProgram_4);
                textureParamHandle = GLES20.glGetUniformLocation(filterShaderProgram_4, "texture");
                textureCoordinateHandle = GLES20.glGetAttribLocation(filterShaderProgram_4, "vTexCoordinate");
                positionHandle = GLES20.glGetAttribLocation(filterShaderProgram_4, "vPosition");
                textureTranformHandle = GLES20.glGetUniformLocation(filterShaderProgram_4, "textureTransform");
            }
            else if (shaderVal >= 0.8f) {
                GLES20.glUseProgram(filterShaderProgram_5);
                textureParamHandle = GLES20.glGetUniformLocation(filterShaderProgram_5, "texture");
                textureCoordinateHandle = GLES20.glGetAttribLocation(filterShaderProgram_5, "vTexCoordinate");
                positionHandle = GLES20.glGetAttribLocation(filterShaderProgram_5, "vPosition");
                textureTranformHandle = GLES20.glGetUniformLocation(filterShaderProgram_5, "textureTransform");
            }
        }


        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);

        return true;
    }

    private void adjustViewport()
    {
        float surfaceAspect = height / (float)width;
        float videoAspect = videoHeight / (float)videoWidth;

        if (surfaceAspect > videoAspect)
        {
            float heightRatio = height / (float)videoHeight;
            int newWidth = (int)(width * heightRatio);
            int xOffset = (newWidth - width) / 2;
            GLES20.glViewport(-xOffset, 0, newWidth, height);
        }
        else
        {
            float widthRatio = width / (float)videoWidth;
            int newHeight = (int)(height * widthRatio);
            int yOffset = (newHeight - height) / 2;
            GLES20.glViewport(0, -yOffset, width, newHeight);
        }

        adjustViewport = false;
    }

    @Override
    protected void initGLComponents()
    {
        setupVertexBuffer();
        setupTexture(ctx);
        loadShaders();
    }

    @Override
    protected void deinitGLComponents()
    {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        videoTexture.release();
        videoTexture.setOnFrameAvailableListener(null);
    }

    public void setVideoSize(int width, int height)
    {
        this.videoWidth = width;
        this.videoHeight = height;
        adjustViewport = true;
    }

    public void checkGlError(String op)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    public SurfaceTexture getVideoTexture()
    {
        return videoTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        synchronized (this)
        {
            frameAvailable = true;
        }
    }

    public void updateShader(double sliderVal) {
        shaderVal = (float) sliderVal;
        //Log.d("updateShader", String.valueOf(sliderVal));
    }
}
