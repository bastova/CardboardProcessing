package processing.test.carboardprocessing;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
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

import javax.microedition.khronos.egl.EGLConfig;

public class CardboardProcessing extends MyPApplet implements CardboardView.StereoRenderer,
        MagnetSensor.OnCardboardTriggerListener, NfcSensor.OnCardboardNfcListener,
        SensorConnection.SensorListener{

    private final SensorConnection sensorConnection = new SensorConnection(this);
    private boolean convertTapIntoTriggerEnabled = true;

    private static final int NAVIGATION_BAR_TIMEOUT_MS = 2000;
    private CardboardView mCardboardView;
    private MagnetSensor mMagnetSensor;
    private NfcSensor mNfcSensor;
    private int mVolumeKeysMode;

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
        SurfaceView sf = (SurfaceView) content.getChildAt(0);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);

        // **** Transparency **** // Needs more testing to show Cardboard's overlay
        /*cardboardView.setZOrderOnTop(true);
        cardboardView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        cardboardView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);*/
        //cardboardView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // *** End Transparent *** //

        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        sf.setZOrderMediaOverlay(true); // Put Processing on top of cardboard surfaceview

        FrameLayout fl = (FrameLayout) findViewById(android.R.id.content);
        RelativeLayout ll = (RelativeLayout) fl.getChildAt(0);
        ll.addView(sf);
        ll.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

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

    float camRadius = 320;

    PGraphics lv; //left viewport
    PGraphics rv; //right viewport

    float panx, pany, panz;
    float camx, camy, camz;

    PGraphics big;
    PGraphics big2;

    PGraphics feedback;  // Image drawn onto by & re-fed to the shader every loop
    public void setup() {

        size(displayWidth, displayHeight, OPENGL);

        noStroke();
        frameRate(60);

        feedback = createGraphics(width/2, height/2, OPENGL);

        feedback.beginDraw();
        feedback.background(0);
        feedback.endDraw();
    }
    public void draw() {


        feedback.beginDraw();

        if (mousePressed) {
            feedback.fill( 255 );
            feedback.noStroke();
            feedback.ellipse( mouseX, mouseY, 100, 100 );
        }

        feedback.endDraw();

        image( feedback, 0, 0 );

        text(frameRate, 10, 10);
    }
    /*
public void setup()
  {

      size(displayWidth,displayHeight,P3D); //used to set P3D renderer
      orientation(LANDSCAPE); //causes crashing if not started in this orientation

      big = createGraphics(displayWidth/2, displayHeight, P3D);
      big.beginDraw();
      big.endDraw();

      big2 = createGraphics(displayWidth/2, displayHeight, P3D);

         big.beginDraw();
         big.background(128);
         big.line(20, 1800, 1800, 900);
         // etc..
         big.endDraw();

      image(big, 0,0);

      big2.beginDraw();
      big2.background(222);
      big2.line(20, 1800, 1800, 900);
      // etc..
      big2.endDraw();

      Log.w("processing", String.valueOf(displayHeight));
      Log.w("processing", String.valueOf(displayWidth));
      //lv = createGraphics(displayWidth/2,displayHeight,P3D); //size of left viewport
      //rv = createGraphics(displayWidth/2,displayHeight,P3D);
   // smooth();
  }

public void draw()
  {
   // background(0,0,100);
   // translate(400,300,0);
    //background(255);
   // fill(255,50,50);

      //panx = panx-mx*10;
      panx = 0;
      pany = 0;
      panz = 0;
      camx = 0;
      //eyey = -20*az;
      camy = -500f;
      camz = 320;

     // ViewPort(lv, camx, camy, camz, panx, pany, panz, -15);
     // ViewPort(rv, camx, camy, camz, panx, pany, panz, 15);

//add the two viewports to your main panel
    //  image(lv, 0, 0);
    //  image(rv, displayWidth/4+10, 0);

    /*float camX = 0 + sin(0) * camRadius;
    float camY = 0 - 50;
    float camZ = 0 + cos(0) * camRadius;

    camera(camx, camy, camz, 0, 0, 0, 0,1,0);

    pushMatrix();
    translate(0,100,-200);
    box(200);
    popMatrix();*/
 // }

    void ViewPort(PGraphics v, float x, float y, float z, float px, float py, float pz, int eyeoff){
        v.beginDraw();
        v.background(102);
        v.lights();
        v.pushMatrix();
        v.camera(x+eyeoff, y, z, px, py, pz, 0.0f, 1.0f, 0.0f);
        v.noStroke();
        v.box(20);
        v.popMatrix();
        v.endDraw();
    }


  public int sketchWidth() { return displayWidth; }
  public int sketchHeight() { return displayHeight; }
  public String sketchRenderer() { return P3D; }

    // ********************************** //
    // CardboardActivity methods:
    // ********************************** //

    public void setCardboardView(CardboardView cardboardView)
    {
       /*mCardboardView = cardboardView;

        if (cardboardView != null) {
            CardboardDeviceParams cardboardDeviceParams =
                    CardboardDeviceParams.createFromNfcContents(mNfcSensor.getTagContents());
            if (cardboardDeviceParams == null) {
                Log.w("cardboardDeviceParams", "device params null");
                cardboardDeviceParams = new CardboardDeviceParams();
            }

            cardboardView.updateCardboardDeviceParams(cardboardDeviceParams);
        }*/
        mCardboardView = cardboardView;
        if (cardboardView == null) {
            return;
        }
        //cardboardView.setOnClickListener(new CardboardActivity.1(this));

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

        float[] headView = new float[16];
        headTransform.getHeadView(headView, 0);
       /* Log.w("head", String.valueOf(headView[0]) + " " +
                String.valueOf(headView[1]) + " " +
                String.valueOf(headView[2]) + " " +
                String.valueOf(headView[3]) + " " +
                String.valueOf(headView[4]) + " " +
                String.valueOf(headView[5]));*/
    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i2) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }

    @Override
    public void onRendererShutdown() {

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

    // ********************************** //

}
