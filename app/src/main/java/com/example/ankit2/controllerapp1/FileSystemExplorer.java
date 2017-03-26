package com.example.ankit2.controllerapp1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;


import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


import javax.microedition.khronos.egl.EGLConfig;


public class FileSystemExplorer extends GvrActivity implements GvrView.StereoRenderer, IVRGestureListener {


    protected Object3D canvas;
    protected Object3D pane1;
    protected Object3D pane2;

    protected Object3D objList[];
    File fileList[];
    protected Bitmap fileNameBitmaps[];
    protected Bitmap highlightedFileNameBitmaps[];
    protected int textureDataHandles[];
    protected int textureDataHandlesHighlighted[];
    protected byte currentPane = 1;



    int oldX = -1;
    int oldY = -1;


    private static final String TAG = "FileSystemExplorer";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    public static FileSystemExplorer FileSystemExplorerInstance;
    private static boolean activityRunning;

    private static final float CAMERA_Z = 0.01f;

    private static final float YAW_LIMIT = 0.38f;
    private static final float PITCH_LIMIT = 0.12f;

    //This is the logical screen density of the controller.
    //It is needed to scale the movement data
    public static float density;

    private static final int COORDS_PER_VERTEX = 3;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[]{0.0f, 0.0f, 3.0f, 1.0f};

    // Convenience vector for extracting the position from a matrix via multiplication.
    private static final float[] POS_MATRIX_MULTIPLY_VEC = {0, 0, 0, 1.0f};


    private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
    private static final String SUCCESS_SOUND_FILE = "success.wav";

    private final float[] lightPosInEyeSpace = new float[4];


    private int textureProgram;
    private Vibrator vibrator;

    /**
     * This will be used to pass in the texture.
     */
    private int mTextureUniformHandle;

    /**
     * This will be used to pass in model texture coordinate information.
     */
    private int mTextureCoordinateHandle;

    /**
     * Size of the texture coordinate data in elements.
     */
    private final int mTextureCoordinateDataSize = 2;

    private FloatBuffer mCubeTextureCoordinates;



    private int textureMVPMatrixHandle;
    private int textureMVMatrixHandle;
    private int textureLightPosHandle;
    private int texturePositionHandle;
    private int textureColorHandle;
    private int textureNormalHandle;
    private int textureModelParam;

    final float[] textureCoordinateData = {

            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;

    private float[] tempPosition;
    private float[] headRotation;

    private Bitmap bmp = null;

    private int drawingColours[] = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.BLACK};
    private int currentColour = 0;


    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;
    private volatile int successSourceId = GvrAudioEngine.INVALID_ID;


    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type  The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    public int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }


    public int runtimeLoadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            // bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }




    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    private static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeGvrView();

        activityRunning = true;

        canvas = new Object3D(6, 3);
        pane1 = new Object3D(6, 3);
        pane2 = new Object3D(6, 3);



        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        tempPosition = new float[4];
        headRotation = new float[4];
        headView = new float[16];
        FileSystemExplorerInstance = this;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Initialize 3D audio engine.
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    }

    public void initializeGvrView() {
        setContentView(R.layout.common_ui);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(false);

        // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
        // Daydream controller input for basic interactions using the existing Cardboard trigger API.
        gvrView.enableCardboardTriggerEmulation();

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        setGvrView(gvrView);
    }

    Bitmap createTextBitmap(final String text, int backgroundColor) {
        final Paint textPaint = new Paint() {
            {
                setColor(Color.WHITE);
                setTextAlign(Paint.Align.CENTER);
                setTextSize(75f);
                setAntiAlias(true);
            }
        };
        final Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        final Bitmap bmp = Bitmap.createBitmap(bounds.width()*2, bounds.height()*2, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(backgroundColor);
        final Canvas canvas = new Canvas(bmp);
        canvas.drawText(text, bounds.width(), bounds.height()*1.25f, textPaint);
        return bmp;
    }

    /*This function creates a new bitmap from the original one and applies a
    border to it.
     */

    Bitmap applyBorder(Bitmap bmp, int color)
    {
        //create a copy of the bitmap. The dimensions are different so that
        //a new bitmap is returned instead of the original one.
        Bitmap tempBmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth()+1, bmp.getHeight()+1, false);
        for(int i=0;i<tempBmp.getWidth();i++)
        {
            tempBmp.setPixel(i,0,color);
            tempBmp.setPixel(i,1,color);
            tempBmp.setPixel(i,tempBmp.getHeight()-1,color);
            tempBmp.setPixel(i,tempBmp.getHeight()-2,color);
        }

        for(int i=0;i<tempBmp.getHeight();i++)
        {
            tempBmp.setPixel(0,i,color);
            tempBmp.setPixel(1,i,color);
            tempBmp.setPixel(tempBmp.getWidth()-1,i,color);
            tempBmp.setPixel(tempBmp.getWidth()-2,i,color);
        }

        return tempBmp;
    }

    @Override
    public void onPause() {
        gvrAudioEngine.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        gvrAudioEngine.resume();
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }


    /**
     * Creates the buffers we use to store information about the 3D world.
     * <p>
     * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.


        mCubeTextureCoordinates = ByteBuffer.allocateDirect(textureCoordinateData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(textureCoordinateData).position(0);

        int textureVertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.per_pixel_vertex_shader);
        int textureFragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.per_pixel_fragment_shader);

        density = VRModeFragment.controllerDPI;

        textureProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(textureProgram, textureVertexShader);
        GLES20.glAttachShader(textureProgram, textureFragmentShader);
        GLES20.glLinkProgram(textureProgram);
        GLES20.glUseProgram(textureProgram);

        checkGLError("Texture program");

        File testDir = new File(Environment.getExternalStorageDirectory(), "vrtest");
        fileList = testDir.listFiles();


        if (fileList != null) {
            int numFiles = fileList.length;
            objList = new Object3D[numFiles];
            fileNameBitmaps = new Bitmap[numFiles];
            highlightedFileNameBitmaps = new Bitmap[numFiles];
            textureDataHandles = new int[numFiles];
            textureDataHandlesHighlighted = new int[numFiles];
            for (int i = 0; i < numFiles; i++)
            {
                objList[i] = new Object3D(6,3);
                objList[i].setVertices(WorldLayoutData.FILE_OBJ_COORDS)
                        .setColours(WorldLayoutData.SPRITE_COLORS, WorldLayoutData.SPRITE_COLORS)
                        .setNormals(WorldLayoutData.SPRITE_NORMALS)
                        .setPosition(0.0f, (numFiles/2) - i - 0.2f*i, -4.5f);

                textureDataHandles[i] = runtimeLoadTexture(fileNameBitmaps[i] = createTextBitmap(fileList[i].getName(), 0xff551a8b));
                textureDataHandlesHighlighted[i] = runtimeLoadTexture(highlightedFileNameBitmaps[i] = applyBorder(fileNameBitmaps[i], Color.YELLOW));
                objList[i].textureData = textureDataHandles[i];
            }
        }
        pane1.setVertices(WorldLayoutData.SPRITE_COORDS)
                .setColours(WorldLayoutData.SPRITE_COLORS, WorldLayoutData.SPRITE_COLORS)
                .setNormals(WorldLayoutData.SPRITE_NORMALS)
                .setPosition(1.8f, 0.0f, -1.2f);
        pane2.setVertices(WorldLayoutData.SPRITE_COORDS)
                .setColours(WorldLayoutData.SPRITE_COLORS, WorldLayoutData.SPRITE_COLORS)
                .setNormals(WorldLayoutData.SPRITE_NORMALS)
                .setPosition(2.4f, 0.0f, 0.8f);

        pane1.textureData = runtimeLoadTexture(createTextBitmap("***", Color.WHITE));
        pane2.textureData = runtimeLoadTexture(createTextBitmap("***", Color.WHITE));

        texturePositionHandle = GLES20.glGetAttribLocation(textureProgram, "a_Position");
        textureColorHandle = GLES20.glGetAttribLocation(textureProgram, "a_Color");
        textureNormalHandle = GLES20.glGetAttribLocation(textureProgram, "a_Normal");
        textureModelParam = GLES20.glGetUniformLocation(textureProgram, "u_Model");
        textureMVPMatrixHandle = GLES20.glGetUniformLocation(textureProgram, "u_MVP");
        textureMVMatrixHandle = GLES20.glGetUniformLocation(textureProgram, "u_MVMatrix");
        textureLightPosHandle = GLES20.glGetUniformLocation(textureProgram, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(textureProgram, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(textureProgram, "a_TexCoordinate");
        checkGLError("Texture program params");


        // Avoid any delays during start-up due to decoding of sound files.
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                        // returned sourceId handle is stored and allows for repositioning the sound object
                        // whenever the cube position changes.
                        gvrAudioEngine.preloadSoundFile(OBJECT_SOUND_FILE);
                        sourceId = gvrAudioEngine.createSoundObject(OBJECT_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(
                                sourceId, canvas.position[0], canvas.position[0], canvas.position[0]);
                        gvrAudioEngine.playSound(sourceId, true /* looped playback */);
                        // Preload an unspatialized sound to be played on a successful trigger on the cube.
                        gvrAudioEngine.preloadSoundFile(SUCCESS_SOUND_FILE);
                    }
                })
                .start();

        updateModelPosition();

        checkGLError("onSurfaceCreated");
    }

    /**
     * Sets the initial positions of all the objects
     */
    protected void updateModelPosition() {


       for(Object3D obj:objList)
       {
           Matrix.setIdentityM(obj.model, 0);
           Matrix.translateM(obj.model, 0, obj.position[0], obj.position[1], obj.position[2]);
       }

        Matrix.setIdentityM(pane1.model, 0);
        Matrix.translateM(pane1.model, 0, pane1.position[0], pane1.position[1], pane1.position[2]);
        Matrix.rotateM(pane1.model, 0, -55, 0,1,0);
        Matrix.scaleM(pane1.model, 0, 1, 1.3f,1);

        Matrix.setIdentityM(pane2.model, 0);
        Matrix.translateM(pane2.model, 0, pane2.position[0], pane2.position[1], pane2.position[2]);
        Matrix.rotateM(pane2.model, 0, -90, 0,1,0);
        Matrix.scaleM(pane2.model, 0, 1, 1.3f,1);



        // Update the sound location to match it with the new cube position.
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, canvas.position[0], canvas.position[0], canvas.position[0]);
        }
        checkGLError("updateCubePosition");
    }

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    public String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {

        for(int i=0;i<objList.length;i++)
        {
            if(isLookingAtObject(objList[i]))
                objList[i].textureData = textureDataHandlesHighlighted[i];

            else
                objList[i].textureData = textureDataHandles[i];

        }

        if(bmp!=null)
        {
            if(currentPane == 1)
            {
                pane1.textureData = runtimeLoadTexture(bmp);
                currentPane = 2;
            }

            else if(currentPane == 2)
            {
                pane2.textureData = runtimeLoadTexture(bmp);
                currentPane = 1;
            }

            bmp = null;
        }

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(headView, 0);

        // Update the 3d audio engine with the most recent head rotation.
        headTransform.getQuaternion(headRotation, 0);
        gvrAudioEngine.setHeadRotation(
                headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
        // Regular update call to GVR audio engine.
        gvrAudioEngine.update();

        checkGLError("onReadyToDraw");
    }


    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("colorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);


        Matrix.multiplyMM(modelView, 0, view, 0, pane1.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawPane1();

        Matrix.multiplyMM(modelView, 0, view, 0, pane2.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawPane2();
        drawFileObjects(perspective);
    }

    private void drawFileObjects(float[] perspective)
    {
        for(int i=0;i<objList.length;i++)
        {
            Matrix.multiplyMM(modelView, 0, view, 0, objList[i].model, 0);
            Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

            GLES20.glUseProgram(textureProgram);
            // Set the active texture unit to texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // Bind the texture to this unit.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, objList[i].textureData);

            // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            GLES20.glUniform3fv(textureLightPosHandle, 1, lightPosInEyeSpace, 0);

            // Set the Model in the shader, used to calculate lighting
            GLES20.glUniformMatrix4fv(textureModelParam, 1, false, objList[i].model, 0);

            // Set the ModelView in the shader, used to calculate lighting
            GLES20.glUniformMatrix4fv(textureMVMatrixHandle, 1, false, modelView, 0);

            // Set the position of the cube
            GLES20.glVertexAttribPointer(
                    texturePositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, objList[i].verticesBuffer);

            // Set the ModelViewProjection matrix in the shader.
            GLES20.glUniformMatrix4fv(textureMVPMatrixHandle, 1, false, modelViewProjection, 0);

            // Set the normal positions of the cube, again for shading
            GLES20.glVertexAttribPointer(textureNormalHandle, 3, GLES20.GL_FLOAT, false, 0, objList[i].normalsBuffer);
            GLES20.glVertexAttribPointer(textureColorHandle, 4, GLES20.GL_FLOAT, false, 0, objList[i].coloursBuffer);


            // Pass in the texture coordinate information
            mCubeTextureCoordinates.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates);

            // Enable vertex arrays
            GLES20.glEnableVertexAttribArray(texturePositionHandle);
            GLES20.glEnableVertexAttribArray(textureNormalHandle);
            GLES20.glEnableVertexAttribArray(textureColorHandle);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, objList[i].numberOfVertices);

            // Disable vertex arrays
            GLES20.glDisableVertexAttribArray(texturePositionHandle);
            GLES20.glDisableVertexAttribArray(textureNormalHandle);
            GLES20.glDisableVertexAttribArray(textureColorHandle);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        }
    }

    private void drawPane1() {
        GLES20.glUseProgram(textureProgram);
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pane1.textureData);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        GLES20.glUniform3fv(textureLightPosHandle, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(textureModelParam, 1, false, pane1.model, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(textureMVMatrixHandle, 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(
                texturePositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, pane1.verticesBuffer);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(textureMVPMatrixHandle, 1, false, modelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(textureNormalHandle, 3, GLES20.GL_FLOAT, false, 0, pane1.normalsBuffer);
        GLES20.glVertexAttribPointer(textureColorHandle, 4, GLES20.GL_FLOAT, false, 0,pane1.coloursBuffer);


        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(texturePositionHandle);
        GLES20.glEnableVertexAttribArray(textureNormalHandle);
        GLES20.glEnableVertexAttribArray(textureColorHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, pane1.numberOfVertices);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(texturePositionHandle);
        GLES20.glDisableVertexAttribArray(textureNormalHandle);
        GLES20.glDisableVertexAttribArray(textureColorHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
    }
    private void drawPane2() {
        GLES20.glUseProgram(textureProgram);
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pane2.textureData);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        GLES20.glUniform3fv(textureLightPosHandle, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(textureModelParam, 1, false, pane2.model, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(textureMVMatrixHandle, 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(
                texturePositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, pane2.verticesBuffer);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(textureMVPMatrixHandle, 1, false, modelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(textureNormalHandle, 3, GLES20.GL_FLOAT, false, 0, pane2.normalsBuffer);
        GLES20.glVertexAttribPointer(textureColorHandle, 4, GLES20.GL_FLOAT, false, 0,pane2.coloursBuffer);


        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(texturePositionHandle);
        GLES20.glEnableVertexAttribArray(textureNormalHandle);
        GLES20.glEnableVertexAttribArray(textureColorHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, pane2.numberOfVertices);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(texturePositionHandle);
        GLES20.glDisableVertexAttribArray(textureNormalHandle);
        GLES20.glDisableVertexAttribArray(textureColorHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
    }



    @Override
    public void onFinishFrame(Viewport viewport) {
    }


    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
    }


    @Override
    public void onFlick() {
    }

    @Override
    public void onTap(float x, float y) {
        for(int i=0;i<objList.length;i++)
        {
            if(isLookingAtObject(objList[i]))
            {
                String fileName = fileList[i].getName();
                System.out.println("Tapped on "+ fileName);
                if(fileName.charAt(fileName.length()-1) == 'g')
                {
                    //It's a jpg or jpeg
                    bmp = BitmapFactory.decodeFile(fileList[i].getAbsolutePath());
                    if(bmp == null)
                    System.out.println("*********bmp is null");
                }

                else if(fileName.charAt(fileName.length()-1) == 't')
                {
                    //It's a txt
                    File file = new File(fileList[i].getAbsolutePath());
                    String str = null;
                    String lines[] = null;
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(file);
                        byte[] data = new byte[(int) file.length()];
                        fis.read(data);
                        fis.close();
                        str = new String(data, "UTF-8");
                        lines = str.split("\\r?\\n");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    final Paint textPaint = new Paint() {
                        {
                            setColor(Color.BLACK);
                            setTextAlign(Paint.Align.LEFT);
                            setTextSize(20f);
                            setAntiAlias(true);
                        }
                    };

                    int lineNumber = 1;
                    bmp = Bitmap.createBitmap(250, 310, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    final Canvas canvas = new Canvas(bmp);
                    for(String w: lines) {
                        canvas.drawText(w, 15, 20*lineNumber++, textPaint);
                        //System.out.println(w);
                    }

                }

            }
        }

        vibrator.vibrate(50);
    }

    @Override
    public void onSwipe(float velocityX, float velocityY) {

    }

    @Override
    public void onDrag(float newX, float newY) {

    }

    @Override
    public void onPinch(float newX, float newY) {
    }

    @Override
    public void onPinchStart(float newX, float newY) {
    }

    public static synchronized boolean isAlive() {
        return activityRunning;
    }

    public static FileSystemExplorer getInstance() {
        return FileSystemExplorerInstance;
    }


    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     *
     * @return true if the user is looking at the object.
     */
    private boolean isLookingAtObject(Object3D obj) {
        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(modelView, 0, headView, 0, obj.model, 0);
        Matrix.multiplyMV(tempPosition, 0, modelView, 0, POS_MATRIX_MULTIPLY_VEC, 0);

        float pitch = (float) Math.atan2(tempPosition[1], -tempPosition[2]);
        float yaw = (float) Math.atan2(tempPosition[0], -tempPosition[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }

    class Object3D {
        float vertices[];
        float normals[];
        float colours[];
        float highlightColours[];
        float position[];
        float model[];
        int numberOfVertices;
        int textureData;
        float[] accumulatedRotation;

        FloatBuffer verticesBuffer;
        FloatBuffer normalsBuffer;
        FloatBuffer coloursBuffer;
        FloatBuffer highlightColoursBuffer;


        public Object3D(int numberOfVertices, int numberOfDimensions) {
            vertices = new float[numberOfVertices * numberOfDimensions];
            normals = new float[numberOfVertices * numberOfDimensions];
            colours = new float[numberOfVertices * 4];
            accumulatedRotation = new float[16];
            highlightColours = new float[numberOfVertices * 4];
            position = new float[3];
            model = new float[16];
            this.numberOfVertices = numberOfVertices;
        }

        public Object3D setVertices(float[] vertices) {
            System.arraycopy(vertices, 0, this.vertices, 0, vertices.length);
            ByteBuffer bb = ByteBuffer.allocateDirect(this.vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            verticesBuffer = bb.asFloatBuffer();
            verticesBuffer.put(this.vertices);
            verticesBuffer.position(0);
            return this;
        }

        public Object3D setNormals(float[] normals) {
            System.arraycopy(normals, 0, this.normals, 0, normals.length);
            ByteBuffer bb = ByteBuffer.allocateDirect(this.normals.length * 4);
            bb.order(ByteOrder.nativeOrder());
            normalsBuffer = bb.asFloatBuffer();
            normalsBuffer.put(this.normals);
            normalsBuffer.position(0);
            return this;
        }

        public Object3D setColours(float[] colours, float[] highlightColours) {
            System.arraycopy(colours, 0, this.colours, 0, colours.length);
            System.arraycopy(highlightColours, 0, this.highlightColours, 0, colours.length);
            ByteBuffer bb = ByteBuffer.allocateDirect(this.colours.length * 4);
            bb.order(ByteOrder.nativeOrder());
            coloursBuffer = bb.asFloatBuffer();
            coloursBuffer.put(this.colours);
            coloursBuffer.position(0);
            ByteBuffer bb1 = ByteBuffer.allocateDirect(this.highlightColours.length * 4);
            bb1.order(ByteOrder.nativeOrder());
            highlightColoursBuffer = bb1.asFloatBuffer();
            highlightColoursBuffer.put(this.highlightColours);
            highlightColoursBuffer.position(0);
            return this;
        }

        public Object3D setPosition(float x, float y, float z) {
            position[0] = x;
            position[1] = y;
            position[2] = z;
            return this;
        }

    }
}

