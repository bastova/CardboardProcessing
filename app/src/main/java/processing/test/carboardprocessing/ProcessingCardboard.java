package processing.test.carboardprocessing;

import android.content.Context;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.FieldOfView;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Created by Anton on 5/12/2015.
 */
public class ProcessingCardboard extends CardboardActivity implements CardboardView.StereoRenderer {


    private static final float CAMERA_Z = 0.01f;

    private float[] camera;
    private float[] view;
    private float[] eyeMatrix;
    float left = 0;
    float right = 0;
    float bottom = 0;
    float top = 0;

    private boolean isLeftEye = true;

    private WebView myWebView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        camera = new float[16];
        view = new float[16];
        eyeMatrix = new float[16];

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        //myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        myWebView.loadUrl("file:///android_asset/anything.html");

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        //headTransform.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        myWebView.post(new Runnable() {
            @Override
            public void run() {
                myWebView.loadUrl("javascript:testEcho('Hello World!')");
            }
        });
    }

    private float[] angles;
    @Override
    public void onDrawEye(Eye eye) {
        // Apply the eye transformation to the camera.
        eyeMatrix = eye.getEyeView();

        FieldOfView fov = eye.getFov();
        left = fov.getLeft();
        right = fov.getRight();
        bottom = fov.getBottom();
        top = fov.getTop();

        if (eye.getType() == Eye.Type.LEFT) isLeftEye = true;
        else if (eye.getType() == Eye.Type.RIGHT) isLeftEye = false;

        angles = new float[3];
        getRotation(angles, eyeMatrix);
        myWebView.post(new Runnable() {
            @Override
            public void run() {
                myWebView.loadUrl("javascript:setEyeViewTransform(" +
                        isLeftEye + ","
                        + angles[0] + ","
                        + angles[1] + ","
                        + angles[2] + ","
                        + eyeMatrix[12] + ","
                        + eyeMatrix[13] + ","
                        + eyeMatrix[14] + ","
                        + left + ","
                        + right + ","
                        + bottom + ","
                        + top + ")");
            }
        });
        /*myWebView.post(new Runnable() {
            @Override
            public void run() {
                myWebView.loadUrl("javascript:setEyeViewParams(" +
                        isLeftEye + ","
                        + eyeMatrix[0] + ","
                        + eyeMatrix[1] + ","
                        + eyeMatrix[2] + ","
                        + eyeMatrix[3] + ","
                        + eyeMatrix[4] + ","
                        + eyeMatrix[5] + ","
                        + eyeMatrix[6] + ","
                        + eyeMatrix[7] + ","
                        + eyeMatrix[8] + ","
                        + eyeMatrix[9] + ","
                        + eyeMatrix[10] + ","
                        + eyeMatrix[11] + ","
                        + eyeMatrix[12] + ","
                        + eyeMatrix[13] + ","
                        + eyeMatrix[14] + ","
                        + eyeMatrix[15] +")");
            }
        });*/
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);
        //Log.w("view", view.toString());
    }

    void getRotation(float[] angles, float[] m) {
        if (m[2] != 1 && m[2] != -1) {
            angles[1] = (float) -Math.asin((double)m[2]);
            angles[0] =
                    (float) Math.atan2((double)m[6]/Math.cos((double)angles[1]),
                            (double)m[10]/Math.cos((double)angles[1]));
            angles[2] =
                    (float) Math.atan2((double)m[1]/Math.cos((double)angles[1]),
                            (double)m[0]/Math.cos((double)angles[1]));
        } else {
            angles[2] = 0f;
            if (m[2] == -1) {
                angles[1] = 3.1415926f/2f;
                angles[0] = angles[2] +
                        (float) Math.atan2((double)m[4],(double)m[8]);
            } else {
                angles[1] = -3.1415926f/2f;
                angles[0] = -angles[2] +
                        (float) Math.atan2(-(double)m[4],-(double)m[8]);
            }
        }
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
}
