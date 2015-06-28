package processing.test.carboardprocessing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.nfc.NdefMessage;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import com.google.vrtoolkit.cardboard.sensors.MagnetSensor;
import com.google.vrtoolkit.cardboard.sensors.NfcSensor;
import com.google.vrtoolkit.cardboard.sensors.SensorConnection;

import processing.core.*;
import processing.core.PApplet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class CardboardP extends MyPApplet implements CardboardView.StereoRenderer,
        MagnetSensor.OnCardboardTriggerListener, NfcSensor.OnCardboardNfcListener,
        SensorConnection.SensorListener{

    private SurfaceView sf;

    /** Store our model data in a float buffer. */
    private FloatBuffer mPlaneVertices;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** How many elements per vertex. */
    private final int mStrideBytes = 7 * mBytesPerFloat;

    /** Offset of the position data. */
    private final int mPositionOffset = 0;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /** Offset of the color data. */
    private final int mColorOffset = 3;

    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;

    // This triangle is red, green, and blue.
    final float[] planeVerticesData = {
            // X, Y, Z,
            // R, G, B, A
            -1.0f, 1.0f, -1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            -1.0f, -1.0f, -1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            1.0f, 1.0f, -1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            -1.0f, -1.0f, -1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            1.0f, -1.0f, -1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            1.0f, 1.0f, -1.0f,
            1.0f, 0.0f, 0.0f, 1.0f
    };

    /** Store our model data in a float buffer. */
    private FloatBuffer mCubeTextureCoordinates;

    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;

    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;

    /** This is a handle to our texture data. */
    private int mTextureDataHandle;

    // S, T (or X, Y)
    // Texture coordinate data.
    // Because images have a Y axis pointing downward (values increase as you move down the image) while
    // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
    // What's more is that the texture coordinates are the same for every face.
    final float[] cubeTextureCoordinateData =
            {
                    // Front face
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f
            };

    private float[] modelCube;
    private float[] camera;
    private float[] viewView;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model color information. */
    private int mColorHandle;

    final String vertexShader =
            "uniform mat4 u_MVPMatrix;        \n"     // A constant representing the combined model/view/projection matrix.
                    + "attribute vec2 a_UVCoordinate; \n"     // Per-vertex texture coordinate information we will pass in."
                    + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
                    + "attribute vec4 a_Color;        \n"     // Per-vertex color information we will pass in.

                    + "varying vec4 v_Color;          \n"     // This will be passed into the fragment shader.
                    + "varying vec2 v_UVCoordinate;"
                    + "void main()                    \n"     // The entry point for our vertex shader.
                    + "{                              \n"
                    + "   v_Color = a_Color;          \n"     // Pass the color through to the fragment shader.
                                                              // It will be interpolated across the triangle.
                    + "   v_UVCoordinate = a_UVCoordinate; \n"
                    + "   gl_Position = u_MVPMatrix   \n"     // gl_Position is a special variable used to store the final position.
                    + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                    + "}                              \n";    // normalized screen coordinates.


    final String fragmentShader =
            "#extension GL_OES_EGL_image_external : require                       \n"
                    // ^- Important, this says to the GPU's shader compiler that we are using that external texture extension
                    // Your precision, I use lowp, it's up to you obviously
                    + "precision mediump float;                                                \n"
                    // Texture UV sent from vertex shader
                    + "varying vec2 v_UVCoordinate;                                          \n"
                    // Our WebView's texture! Note that it is NOT (And can't be) sampler2D
                    + "uniform samplerExternalOES uniform_texture0;                         \n"
                    + "void main() {                                                        \n"
                    + "    gl_FragColor = texture2D( uniform_texture0, v_UVCoordinate );     \n"
                    + "}                                                                    \n";

    private final SensorConnection sensorConnection = new SensorConnection(this);
    private CardboardView mCardboardView;
    private MagnetSensor mMagnetSensor;
    private NfcSensor mNfcSensor;
    private int mVolumeKeysMode;

    CustomImageView myImgView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(128);

        this.sensorConnection.onCreate(this);

        setVolumeKeysMode(2);

        if (Build.VERSION.SDK_INT < 19) {
            final Handler handler = new Handler();
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
            {
                public void onSystemUiVisibilityChange(int visibility)
                {
                    if ((visibility & 0x2) == 0)
                        handler.postDelayed(new Runnable()
                        {
                            public void run() {
                                setFullscreenMode();
                            }
                        }
                                , 2000L);
                }
            });
        }
        // CardboardActivity onCreate end //

        ViewGroup view = (ViewGroup)getWindow().getDecorView();
        LinearLayout overallContent = (LinearLayout)view.getChildAt(0);
        FrameLayout content = (FrameLayout)overallContent.getChildAt(1);
        sf = (SurfaceView) content.getChildAt(0);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);

        cardboardView.setEGLContextClientVersion(2);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        //sf.setZOrderMediaOverlay(true); // Put Processing on top of cardboard surfaceview

        FrameLayout fl = (FrameLayout) findViewById(android.R.id.content);
        RelativeLayout ll = (RelativeLayout) fl.getChildAt(0);
        //ll.addView(sf);
        addContentView(sf,new ViewGroup.LayoutParams( TEXTURE_WIDTH, TEXTURE_HEIGHT ));

        ll.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        modelCube = new float[16];
        camera = new float[16];
        viewView = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        headView = new float[16];

    }

    protected void onResume()
    {
        super.onResume();

        if (mCardboardView != null) {
            mCardboardView.onResume();
        }

        this.sensorConnection.onResume(this);
    }

    protected void onPause()
    {
        super.onPause();
        if (mCardboardView != null) {
            mCardboardView.onPause();
        }

        this.sensorConnection.onPause(this);
    }

    void logContentView(View parent, String indent) {
        Log.w("test", indent + parent.getClass().getName());
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)parent;
            for (int i = 0; i < group.getChildCount(); i++)
                logContentView(group.getChildAt(i), indent + " ");
        }
    }

    // ************************************ //
    // ********** Processing Code ********* //
    // ************************************ //

    PGraphics feedback;  // Image drawn onto by & re-fed to the shader every loop

    float y = 0.1f;
    float x = 0.1f;
    float z = 0.1f;

    public void setup() {

        size(TEXTURE_WIDTH, TEXTURE_HEIGHT, P3D);

       /* feedback = createGraphics(640/2, 480, P3D);

        feedback.beginDraw();
        feedback.background(100);
        feedback.endDraw();*/

        frameRate(60);

    }

    public void draw() {

        translate(TEXTURE_WIDTH/2,TEXTURE_HEIGHT/2,0);
        rotateX(x);
        rotateY(y);
        rotateZ(z);
        background(100, 200, 255);
        fill(255,228,225);
        box(200);
        x += random(.1f);
        y += random(.1f);
        z += random(.1f);

       /* feedback.beginDraw();

        feedback.fill( 255 );
        feedback.noStroke();
        feedback.translate(640/4, 480/2);
        feedback.box( 100 );

        feedback.endDraw();

        image( feedback, 0, 0 );*/
       // loadPixels();


        loadPixels();

        // ***test
       if ( surface != null ) {
            // Requires a try/catch for .lockCanvas( null )
            try {
                final Canvas surfaceCanvas = surface.lockCanvas(null); // Android canvas from surface

                ByteBuffer buf = ByteBuffer.allocateDirect(TEXTURE_WIDTH * TEXTURE_HEIGHT * 4);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                GLES20.glReadPixels(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
                buf.rewind();
                Bitmap bmp = Bitmap.createBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf);
                surfaceCanvas.drawBitmap(bmp, 0, 0, null);
           //     storeImage(bmp);
                bmp.recycle();
                surface.unlockCanvasAndPost( surfaceCanvas ); // We're done with the canvas!

              //  GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            } catch ( Surface.OutOfResourcesException excp ) {
                excp.printStackTrace();
            }
        }
        //*** test end
    }

    public int sketchWidth() { return displayWidth; }
    public int sketchHeight() { return displayHeight; }
    public String sketchRenderer() { return P3D; }

    public Bitmap screenShot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d("I/O",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("I/O", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("I/O", "Error accessing file: " + e.getMessage());
        }
    }

    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp + ".png";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
    // ********************************** //
    // CardboardActivity methods:
    // ********************************** //

    public void setCardboardView(CardboardView cardboardView)
    {
        mCardboardView = cardboardView;
        if (cardboardView == null) {
            return;
        }

        NdefMessage tagContents = this.sensorConnection.getNfcSensor().getTagContents();
        if (tagContents != null) {
            Log.w("cdp", "tag contents not null");
            updateCardboardDeviceParams(CardboardDeviceParams.createFromNfcContents(tagContents));
        }
    }

    void updateCardboardDeviceParams(CardboardDeviceParams newParams)
    {
        if (mCardboardView != null) {
            Log.w("cdp", "updating head");
            mCardboardView.updateCardboardDeviceParams(newParams);
        }
    }

    private void setFullscreenMode()
    {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    public void setVolumeKeysMode(int mode)
    {
        mVolumeKeysMode = mode;
    }

    protected void onNfcIntent(Intent intent)
    {
        this.mNfcSensor.onNfcIntent(intent);
    }

    // ********************************** //
    // CardboardView.StereoRenderer methods:
    // ********************************** //

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        GLES20.glClearColor(0, 0, 1, 0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        synchronized ( this ) {
            surfaceTexture.updateTexImage(); // Update texture
        }

        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(headView, 0);

        Matrix.setIdentityM(modelCube, 0);
        Matrix.rotateM(modelCube, 0, 0.0f, 0.0f, 0.0f, 1.0f);

        float[] modelCubeCopy = new float[16];
        for (int i = 0; i < modelCube.length; i++) {
            modelCubeCopy[i] = modelCube[i];
        }

        Matrix.multiplyMM(modelCube, 0, headView, 0, modelCubeCopy, 0);
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the active texture unit to texture unit 0.
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        // GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        // GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(viewView, 0, eye.getEyeView(), 0, camera, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelView, 0, viewView, 0, modelCube, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawPlane(mPlaneVertices);
    }

    private void drawPlane(final FloatBuffer aPlaneBuffer) {
        // Pass in the position information
        aPlaneBuffer.position(mPositionOffset);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aPlaneBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        aPlaneBuffer.position(mColorOffset);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aPlaneBuffer);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, modelViewProjection, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    // ************** Util Methods ************** //

    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private int Engine_CreateSurfaceTexture( int width, int height ) {

        final int[] glTexture = new int[1];

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.position(0);

        GLES20.glGenTextures( 1, glTexture, 0 );
        if ( glTexture[0] > 0 ) {
            GLES20.glBindTexture( GL_TEXTURE_EXTERNAL_OES, glTexture[0] );

            // Notice the use of GL_TEXTURE_2D for texture creation
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, byteBuffer);

            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        }
        return glTexture[0];
    }


    @Override
    public void onSurfaceChanged(int i, int i2) {

        surface = null;
        surfaceTexture = null;

        int glSurfaceTex = Engine_CreateSurfaceTexture( TEXTURE_WIDTH, TEXTURE_HEIGHT );
        if ( glSurfaceTex > 0 ) {
            surfaceTexture = new SurfaceTexture( glSurfaceTex );
            surfaceTexture.setDefaultBufferSize( TEXTURE_WIDTH, TEXTURE_HEIGHT );
            surface = new Surface( surfaceTexture );
        }

        mTextureDataHandle = glSurfaceTex;
    }

    public static Surface surface;
    private SurfaceTexture surfaceTexture;
    private final int TEXTURE_WIDTH     = ( 960 );
    private final int TEXTURE_HEIGHT    = ( 1080 );

    //FrameBuffer stuff
    private IntBuffer frameBuffer;

    private int[] fb, renderTex, db;

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        Log.d("GLES20Ext", extensions);
        // GL startup code

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f);

        fb = new int[1];
        GLES20.glGenFramebuffers(1, fb, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);

        /*
        renderTex = new int[1];
        GLES20.glGenTextures(1, renderTex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 1920, 1080, 0,
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        db = new int[1];
        GLES20.glGenRenderbuffers(1, db, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, db[0]);

        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                1920, 1080);

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, db[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, renderTex[0], 0);

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }
        //end FBO
        */


        // Initialize the buffers.
        mPlaneVertices = ByteBuffer.allocateDirect(planeVerticesData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPlaneVertices.put(planeVerticesData).position(0);

        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);

        // Load in the vertex shader.
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }

        if (vertexShaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the vertex shader.
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0)
        {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(programHandle, "uniform_texture0");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(programHandle, "a_UVCoordinate");

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle);

    }

    @Override
    public void onRendererShutdown() {
        GLES20.glDeleteFramebuffers(1, fb, 0);
    }

    // *********************************** //
    // Sensor Interface methods:
    // *********************************** //

    @Override
    public void onInsertedIntoCardboard(CardboardDeviceParams cardboardDeviceParams) {
        updateCardboardDeviceParams(cardboardDeviceParams);
    }

    @Override
    public void onRemovedFromCardboard() {

    }

    @Override
    public void onCardboardTrigger() {

    }

    //..................................................

    class CustomImageView extends ImageView {
        public CustomImageView( Context context ) {
            super( context ); // Call WebView's constructor

            setLayoutParams( new ViewGroup.LayoutParams( TEXTURE_WIDTH, TEXTURE_HEIGHT ) );
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw( Canvas canvas ) {
            /*if ( surface != null ) {
                // Requires a try/catch for .lockCanvas( null )
                try {
                    final Canvas surfaceCanvas = surface.lockCanvas(null); // Android canvas from surface
                    myImgView.setDrawingCacheEnabled(true);
                    Bitmap bmap = myImgView.getDrawingCache();
                    surfaceCanvas.drawBitmap(bmap, 0, 0, new Paint());
                    super.onDraw( surfaceCanvas ); // Call the WebView onDraw targeting the canvas
                    surface.unlockCanvasAndPost( surfaceCanvas ); // We're done with the canvas!
                } catch ( Surface.OutOfResourcesException excp ) {
                    excp.printStackTrace();
                }
            }*/
             super.onDraw( canvas ); // <- Uncomment this if you want to show the original view
        }
    }


}
