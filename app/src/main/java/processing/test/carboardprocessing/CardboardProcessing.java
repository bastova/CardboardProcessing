package processing.test.carboardprocessing;

import android.app.Activity;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import com.google.vrtoolkit.cardboard.sensors.MagnetSensor;
import com.google.vrtoolkit.cardboard.sensors.NfcSensor;
import com.google.vrtoolkit.cardboard.sensors.SensorConnection;

import processing.core.*;
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;

public class CardboardProcessing extends PApplet implements CardboardView.StereoRenderer,
        MagnetSensor.OnCardboardTriggerListener, NfcSensor.OnCardboardNfcListener,
        SensorConnection.SensorListener{

    private final SensorConnection sensorConnection = new SensorConnection(this);
    //private final VolumeKeyState volumeKeyState = new VolumeKeyState(this);
   // private final FullscreenMode fullscreenMode = new FullscreenMode(this);
    private boolean convertTapIntoTriggerEnabled = true;

    private static final int NAVIGATION_BAR_TIMEOUT_MS = 2000;
    private CardboardView mCardboardView;
    private MagnetSensor mMagnetSensor;
    private NfcSensor mNfcSensor;
    private int mVolumeKeysMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CarboardActivity onCreate: //
        //requestWindowFeature(1);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(128);

       // mMagnetSensor = new MagnetSensor(this);
       // mMagnetSensor.setOnCardboardTriggerListener(this);

       // mNfcSensor = NfcSensor.getInstance(this);
       // mNfcSensor.addOnCardboardNfcListener(this);

       // onNfcIntent(getIntent());

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

       // logContentView(getWindow().getDecorView(), "");
        ViewGroup view = (ViewGroup)getWindow().getDecorView();
        LinearLayout overallContent = (LinearLayout)view.getChildAt(0);
        FrameLayout content = (FrameLayout)overallContent.getChildAt(1);
        SurfaceView sf = (SurfaceView) content.getChildAt(0);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        sf.setZOrderMediaOverlay(true);
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

    float camRadius = 320;

public void setup()
  {

    smooth();
  }

public void draw()
  {
    background(0,0,100);
    translate(400,300,0);
    background(255);
    fill(255,50,50);

    float camX = 0 + sin(0) * camRadius;
    float camY = 0 - 50;
    float camZ = 0 + cos(0) * camRadius;

    camera(camX, camY, camZ, 0, 0, 0, 0,1,0);

    pushMatrix();
    translate(0,100,-200);
    box(200);
    popMatrix();
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
        float[] headView = new float[16];
        headTransform.getHeadView(headView, 0);
        Log.w("head", String.valueOf(headView[0]) + " " +
                String.valueOf(headView[1]) + " " +
                String.valueOf(headView[2]) + " " +
                String.valueOf(headView[3]) + " " +
                String.valueOf(headView[4]) + " " +
                String.valueOf(headView[5]));
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
