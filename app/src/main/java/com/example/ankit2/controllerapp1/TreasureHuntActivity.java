package com.example.ankit2.controllerapp1;

/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


import javax.microedition.khronos.egl.EGLConfig;

/**
 * A Google VR sample application.
 * <p>
 * <p>The TreasureHunt scene consists of a planar ground grid and a floating
 * "treasure" cube. When the user looks at the cube, the cube will turn gold.
 * While gold, the user can activate the Cardboard trigger, either directly
 * using the touch trigger on their Cardboard viewer, or using the Daydream
 * controller-based trigger emulation. Activating the trigger will in turn
 * randomly reposition the cube.
 */
public class TreasureHuntActivity extends GvrActivity implements GvrView.StereoRenderer, IVRGestureListener {


    protected Object3D cube;
    protected Object3D tet;
    protected Object3D oct;
    protected Object3D floor;
    protected Object3D sprite;



    private static final String TAG = "TreasureHuntActivity";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    public static TreasureHuntActivity treasureHuntInstance;
    private static boolean activityRunning;

    private static final float CAMERA_Z = 0.01f;
    private static float TIME_DELTA = 0.3f;

    private static float X_ROTATION = 0.5f;
    private static float Y_ROTATION = 0.5f;
    private static float xScale = 0.0f;
    private static float yScale = 0.0f;


    private static final float YAW_LIMIT = 0.28f;
    private static final float PITCH_LIMIT = 0.28f;

    private float previousX;
    private float previousY;

    private int currImage = 0;

    //This is the logical screen density of the controller.
    //It is needed to scale the movement data
    public static float density;

    private static final int COORDS_PER_VERTEX = 3;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[]{0.0f, 2.0f, 0.0f, 1.0f};

    // Convenience vector for extracting the position from a matrix via multiplication.
    private static final float[] POS_MATRIX_MULTIPLY_VEC = {0, 0, 0, 1.0f};

    private static final float MIN_MODEL_DISTANCE = 3.0f;
    private static final float MAX_MODEL_DISTANCE = 7.0f;

    private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
    private static final String SUCCESS_SOUND_FILE = "success.wav";

    private final float[] lightPosInEyeSpace = new float[4];


    private int cubeProgram;
    private int floorProgram;
    private int textureProgram;

    private int cubePositionParam;
    private int cubeNormalParam;
    private int cubeColorParam;
    private int cubeModelParam;
    private int cubeModelViewParam;
    private int cubeModelViewProjectionParam;
    private int cubeLightPosParam;

    private int floorPositionParam;
    private int floorNormalParam;
    private int floorColorParam;
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;
    private long lastUpdate;

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

    /**
     * This is a handle to our texture data.
     */
    private int mTextureDataHandle;

    private int textureMVPMatrixHandle;
    private int textureMVMatrixHandle;
    private int textureLightPosHandle;
    private int texturePositionHandle;
    private int textureColorHandle;
    private int textureNormalHandle;
    private int textureModelParam;

    final float[] textureCoordinateData ={

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

    /**
     * Store the accumulated rotation.
     */


    /**
     * Store the current rotation.
     */
    private final float[] mCurrentRotation = new float[16];

    /**
     * A temporary matrix.
     */
    private float[] mTemporaryMatrix = new float[16];

    private float[] tempPosition;
    private float[] headRotation;

    private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;
    private float floorDepth = 20f;

    private Vibrator vibrator;
    private Bitmap bmp = null;


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

    public int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;	// No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public int runtimeLoadTexture(Bitmap bitmap)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
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

        cube = new Object3D(36, 3);
        tet = new Object3D(12, 3);
        oct = new Object3D(24, 3);
        floor = new Object3D(24, 3);
        sprite = new Object3D(6, 3);

        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        tempPosition = new float[4];
        headRotation = new float[4];
        headView = new float[16];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        treasureHuntInstance = this;
        // Initialize 3D audio engine.
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    }

    public void initializeGvrView() {
        setContentView(R.layout.common_ui);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

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


        cube.setVertices(WorldLayoutData.CUBE_COORDS)
                .setColours(WorldLayoutData.CUBE_COLORS, WorldLayoutData.CUBE_FOUND_COLORS)
                .setNormals(WorldLayoutData.CUBE_NORMALS)
                .setPosition(0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f);

        tet.setVertices(WorldLayoutData.TETRAHEDRON_COORDS)
                .setColours(WorldLayoutData.TETRAHEDRON_COLORS, WorldLayoutData.TETRAHEDRON_FOUND_COLORS)
                .setNormals(WorldLayoutData.TETRAHEDRON_NORMALS)
                .setPosition(3.0f, 0.0f, 1.0f);

        oct.setVertices(WorldLayoutData.OCTAHEDRON_COORDS)
                .setColours(WorldLayoutData.OCTAHEDRON_COLORS, WorldLayoutData.OCTAHEDRON_FOUND_COLORS)
                .setNormals(WorldLayoutData.OCTAHEDRON_NORMALS)
                .setPosition(-3.0f, 0.0f, 1.0f);

        floor.setVertices(WorldLayoutData.FLOOR_COORDS)
                .setColours(WorldLayoutData.FLOOR_COLORS, WorldLayoutData.FLOOR_COLORS)
                .setNormals(WorldLayoutData.FLOOR_NORMALS);

        sprite.setVertices(WorldLayoutData.SPRITE_COORDS)
                .setColours(WorldLayoutData.SPRITE_COLORS, WorldLayoutData.SPRITE_COLORS)
                .setNormals(WorldLayoutData.SPRITE_NORMALS)
                .setPosition(0.0f, 0.0f, 3.0f);

        mCubeTextureCoordinates = ByteBuffer.allocateDirect(textureCoordinateData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(textureCoordinateData).position(0);


        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);
        int textureVertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.per_pixel_vertex_shader);
        int textureFragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.per_pixel_fragment_shader);

        // Load the texture
        mTextureDataHandle = loadTexture(this, R.drawable.flower);

        density = VRModeFragment.controllerDPI;

        cubeProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cubeProgram, vertexShader);
        GLES20.glAttachShader(cubeProgram, passthroughShader);
        GLES20.glLinkProgram(cubeProgram);
        GLES20.glUseProgram(cubeProgram);

        checkGLError("Cube program");

        textureProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(textureProgram, textureVertexShader);
        GLES20.glAttachShader(textureProgram, textureFragmentShader);
        GLES20.glLinkProgram(textureProgram);
        GLES20.glUseProgram(textureProgram);

        checkGLError("Texture program");


        cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");
        cubeNormalParam = GLES20.glGetAttribLocation(cubeProgram, "a_Normal");
        cubeColorParam = GLES20.glGetAttribLocation(cubeProgram, "a_Color");

        cubeModelParam = GLES20.glGetUniformLocation(cubeProgram, "u_Model");
        cubeModelViewParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVMatrix");
        cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
        cubeLightPosParam = GLES20.glGetUniformLocation(cubeProgram, "u_LightPos");

        checkGLError("Cube program params");


        texturePositionHandle = GLES20.glGetAttribLocation(textureProgram, "a_Position");
        textureColorHandle = GLES20.glGetAttribLocation(textureProgram, "a_Color");
        textureNormalHandle = GLES20.glGetAttribLocation(textureProgram, "a_Normal");
        // Set program handles for cube drawing.
        textureModelParam = GLES20.glGetUniformLocation(textureProgram, "u_Model");
        textureMVPMatrixHandle = GLES20.glGetUniformLocation(textureProgram, "u_MVP");
        textureMVMatrixHandle = GLES20.glGetUniformLocation(textureProgram, "u_MVMatrix");
        textureLightPosHandle = GLES20.glGetUniformLocation(textureProgram, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(textureProgram, "u_Texture");

        mTextureCoordinateHandle = GLES20.glGetAttribLocation(textureProgram, "a_TexCoordinate");
        checkGLError("Texture program params");


        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, vertexShader);
        GLES20.glAttachShader(floorProgram, gridShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        checkGLError("Floor program");

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
        floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");

        checkGLError("Floor program params");

        Matrix.setIdentityM(floor.model, 0);
        Matrix.translateM(floor.model, 0, 0, -floorDepth, 0); // Floor appears below user.
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
                                sourceId, cube.position[0], cube.position[0], cube.position[0]);
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

        Matrix.setIdentityM(cube.model, 0);
        Matrix.translateM(cube.model, 0, cube.position[0], cube.position[1], cube.position[2]);

        Matrix.setIdentityM(tet.model, 0);
        Matrix.translateM(tet.model, 0, tet.position[0], tet.position[1], tet.position[2]);

        Matrix.setIdentityM(oct.model, 0);
        Matrix.translateM(oct.model, 0, oct.position[0], oct.position[1], oct.position[2]);

        Matrix.setIdentityM(sprite.model, 0);
        Matrix.translateM(sprite.model, 0, sprite.position[0], sprite.position[1], sprite.position[2]);

        // Update the sound location to match it with the new cube position.
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, cube.position[0], cube.position[0], cube.position[0]);
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

        if(bmp!=null)
        {
            mTextureDataHandle = runtimeLoadTexture(bmp);
            currImage =( currImage +1 ) % 4;
            bmp=null;
        }
        setObjRotation();
        setObjScaling();

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

    protected void setObjScaling()
    {
        Matrix.scaleM(sprite.model, 0, sprite.accumulatedScaling, sprite.accumulatedScaling, 1);
    }

    protected void setObjRotation() {

        Object3D modelInFocus = null;
        if (isLookingAtObject(cube))
            modelInFocus = cube;

        else if (isLookingAtObject(tet))
            modelInFocus = tet;

        else if (isLookingAtObject(oct))
            modelInFocus = oct;


        if (null != modelInFocus) {
            Matrix.setIdentityM(modelInFocus.model, 0);
            Matrix.translateM(modelInFocus.model, 0, modelInFocus.position[0], modelInFocus.position[1], modelInFocus.position[2]);

            // Set a matrix that contains the current rotation.

            Matrix.setIdentityM(mCurrentRotation, 0);
            Matrix.rotateM(mCurrentRotation, 0, X_ROTATION, 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(mCurrentRotation, 0, Y_ROTATION, 1.0f, 0.0f, 0.0f);
            X_ROTATION = 0.0f;
            Y_ROTATION = 0.0f;

            // Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
            Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, modelInFocus.accumulatedRotation, 0);
            System.arraycopy(mTemporaryMatrix, 0, modelInFocus.accumulatedRotation, 0, 16);

            // Rotate the cube taking the overall rotation into account.
            Matrix.multiplyMM(mTemporaryMatrix, 0, modelInFocus.model, 0, modelInFocus.accumulatedRotation, 0);
            System.arraycopy(mTemporaryMatrix, 0, modelInFocus.model, 0, 16);

        }
        else {
            Matrix.setIdentityM(cube.model, 0);
            Matrix.translateM(cube.model, 0, cube.position[0], cube.position[1], cube.position[2]);
            Matrix.multiplyMM(mTemporaryMatrix, 0, cube.model, 0, cube.accumulatedRotation, 0);
            System.arraycopy(mTemporaryMatrix, 0, cube.model, 0, 16);
            Matrix.setIdentityM(tet.model, 0);
            Matrix.translateM(tet.model, 0, tet.position[0], tet.position[1], tet.position[2]);
            Matrix.multiplyMM(mTemporaryMatrix, 0, tet.model, 0, tet.accumulatedRotation, 0);
            System.arraycopy(mTemporaryMatrix, 0, tet.model, 0, 16);
            Matrix.setIdentityM(oct.model, 0);
            Matrix.translateM(oct.model, 0, oct.position[0], oct.position[1], oct.position[2]);
            Matrix.multiplyMM(mTemporaryMatrix, 0, oct.model, 0, oct.accumulatedRotation, 0);
            System.arraycopy(mTemporaryMatrix, 0, oct.model, 0, 16);
        }
            Matrix.setIdentityM(sprite.model, 0);
            Matrix.translateM(sprite.model, 0, sprite.position[0], sprite.position[1], sprite.position[2]);

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
        Matrix.multiplyMM(modelView, 0, view, 0, cube.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        cube.draw();

        // Build the ModelView and ModelViewProjection matrices
        // for calculating tetrahedron position and light.
        Matrix.multiplyMM(modelView, 0, view, 0, tet.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        tet.draw();

        // Build the ModelView and ModelViewProjection matrices
        // for calculating octahedron position and light.
        Matrix.multiplyMM(modelView, 0, view, 0, oct.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        oct.draw();

        // Build the ModelView and ModelViewProjection matrices
        // for calculating sprite position and light.
        Matrix.multiplyMM(modelView, 0, view, 0, sprite.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawSprite();

        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, floor.model, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawFloor();
    }

    private void drawSprite()
    {
        GLES20.glUseProgram(textureProgram);
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        GLES20.glUniform3fv(textureLightPosHandle, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(textureModelParam, 1, false, sprite.model, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(textureMVMatrixHandle, 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(
                texturePositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, sprite.verticesBuffer);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(textureMVPMatrixHandle, 1, false, modelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(textureNormalHandle, 3, GLES20.GL_FLOAT, false, 0, sprite.normalsBuffer);
        GLES20.glVertexAttribPointer(textureColorHandle, 4, GLES20.GL_FLOAT, false, 0,
                isLookingAtObject(sprite) ? sprite.highlightColoursBuffer : sprite.coloursBuffer);


        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(texturePositionHandle);
        GLES20.glEnableVertexAttribArray(textureNormalHandle);
        GLES20.glEnableVertexAttribArray(textureColorHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, sprite.numberOfVertices);

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
     * Draw the floor.
     * <p>
     * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor() {
        GLES20.glUseProgram(floorProgram);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(floorModelParam, 1, false, floor.model, 0);
        GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false, modelViewProjection, 0);
        GLES20.glVertexAttribPointer(
                floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, floor.verticesBuffer);
        GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0, floor.normalsBuffer);
        GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floor.coloursBuffer);

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);
        GLES20.glEnableVertexAttribArray(floorColorParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 24);

        GLES20.glDisableVertexAttribArray(floorPositionParam);
        GLES20.glDisableVertexAttribArray(floorNormalParam);
        GLES20.glDisableVertexAttribArray(floorColorParam);

        checkGLError("drawing floor");
    }

    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
    }


    @Override
    public void onFlick() {
        vibrator.vibrate(50);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;	// No pre-scaling

        switch(currImage)
        {
            case 0:

                // Read in the resource
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.flower1, options);

                break;

            case 1:

                // Read in the resource
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.flower2, options);

                break;

            case 2:

                // Read in the resource
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.flower3, options);

                break;

            case 3:

                // Read in the resource
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.flower, options);

                break;
        }
    }

    @Override
    public void onTap(float x, float y) {
        if (isLookingAtObject(cube)) {
            successSourceId = gvrAudioEngine.createStereoSound(SUCCESS_SOUND_FILE);
            gvrAudioEngine.playSound(successSourceId, false /* looping disabled */);
            TIME_DELTA = 0.2f;
            //hideObject();
        }

        // Always give user feedback.
        vibrator.vibrate(50);
    }

    @Override
    public void onSwipe(float velocityX, float velocityY) {

    }

    @Override
    public void onDrag(float newX, float newY) {
        if (isLookingAtObject(cube) || isLookingAtObject(tet) || isLookingAtObject(oct)) {

            float deltaX = (newX - previousX) / density / 2f;
            float deltaY = (newY - previousY) / density / 2f;

            X_ROTATION += deltaX;
            Y_ROTATION += deltaY;

            previousX = newX;
            previousY = newY;
        }
    }

    @Override
    public void onPinch(float newX, float newY) {

        if(isLookingAtObject(sprite)) {
            float oldDist = (float) Math.sqrt(xScale * xScale + yScale * yScale);
            float newDist = (float) Math.sqrt(newX * newX + newY * newY);
            float scaleFactor = newDist / oldDist;

            sprite.accumulatedScaling=scaleFactor;
        }
    }

    @Override
    public void onPinchStart(float newX, float newY) {

        xScale = newX;
        yScale = newY;

    }

    public static synchronized boolean isAlive() {
        return activityRunning;
    }

    public static TreasureHuntActivity getInstance() {
        return treasureHuntInstance;
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
        float[] accumulatedRotation;
        float accumulatedScaling = 1;

        FloatBuffer verticesBuffer;
        FloatBuffer normalsBuffer;
        FloatBuffer coloursBuffer;
        FloatBuffer highlightColoursBuffer;


        public Object3D(int numberOfVertices, int numberOfDimensions) {
            vertices = new float[numberOfVertices * numberOfDimensions];
            normals = new float[numberOfVertices * numberOfDimensions];
            colours = new float[numberOfVertices * 4];
            accumulatedRotation = new float[16];
            Matrix.setIdentityM(accumulatedRotation, 0);
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

        public void draw() {
            GLES20.glUseProgram(cubeProgram);

            GLES20.glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

            // Set the Model in the shader, used to calculate lighting
            GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, model, 0);

            // Set the ModelView in the shader, used to calculate lighting
            GLES20.glUniformMatrix4fv(cubeModelViewParam, 1, false, modelView, 0);

            // Set the position of the cube
            GLES20.glVertexAttribPointer(
                    cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, verticesBuffer);

            // Set the ModelViewProjection matrix in the shader.
            GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);

            // Set the normal positions of the cube, again for shading
            GLES20.glVertexAttribPointer(cubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, normalsBuffer);
            GLES20.glVertexAttribPointer(cubeColorParam, 4, GLES20.GL_FLOAT, false, 0,
                    isLookingAtObject(this) ? highlightColoursBuffer : coloursBuffer);

            // Enable vertex arrays
            GLES20.glEnableVertexAttribArray(cubePositionParam);
            GLES20.glEnableVertexAttribArray(cubeNormalParam);
            GLES20.glEnableVertexAttribArray(cubeColorParam);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numberOfVertices);

            // Disable vertex arrays
            GLES20.glDisableVertexAttribArray(cubePositionParam);
            GLES20.glDisableVertexAttribArray(cubeNormalParam);
            GLES20.glDisableVertexAttribArray(cubeColorParam);

        }

    }
}


