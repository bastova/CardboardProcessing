package processing.test.carboardprocessing;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.BufferedOutputStream;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PGraphicsAndroid2D;
import processing.opengl.PGLES;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;

public class MyPApplet extends PApplet {

    Handler handler;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//    println("PApplet.onCreate()");

        if (DEBUG) println("onCreate() happening here: " + Thread.currentThread().getName());

        Window window = getWindow();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        displayWidth = dm.widthPixels;
        displayHeight = dm.heightPixels;
//    println("density is " + dm.density);
//    println("densityDpi is " + dm.densityDpi);
        if (DEBUG) println("display metrics: " + dm);

        //println("screen size is " + screenWidth + "x" + screenHeight);

//    LinearLayout layout = new LinearLayout(this);
//    layout.setOrientation(LinearLayout.VERTICAL | LinearLayout.HORIZONTAL);
//    viewGroup = new ViewGroup();
//    surfaceView.setLayoutParams();
//    viewGroup.setLayoutParams(LayoutParams.)
//    RelativeLayout layout = new RelativeLayout(this);
//    RelativeLayout overallLayout = new RelativeLayout(this);
//    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.FILL_PARENT);
//lp.addRule(RelativeLayout.RIGHT_OF, tv1.getId());
//    layout.setGravity(RelativeLayout.CENTER_IN_PARENT);

        int sw = sketchWidth();
        int sh = sketchHeight();

        // Get renderer name and class
        String rendererName = sketchRenderer();
        Class<?> rendererClass = null;
        try {
            rendererClass = Class.forName(rendererName);
        } catch (ClassNotFoundException exception) {
            String message = String.format(
                    "Error: Could not resolve renderer class name: %s", rendererName);
            throw new RuntimeException(message, exception);
        }

        if (rendererName.equals(JAVA2D)) {
            // JAVA2D renderer
            surfaceView = new SketchSurfaceView(this, sw, sh,
                    (Class<? extends PGraphicsAndroid2D>) rendererClass);
        } else if (PGraphicsOpenGL.class.isAssignableFrom(rendererClass)) {
            // P2D, P3D, and any other PGraphicsOpenGL-based renderer
            surfaceView = new MySketchSurfaceViewGL(this, sw, sh,
                    (Class<? extends PGraphicsOpenGL>) rendererClass);
        } else {
            // Anything else
            String message = String.format(
                    "Error: Unsupported renderer class: %s", rendererName);
            throw new RuntimeException(message);
        }

//    g = ((SketchSurfaceView) surfaceView).getGraphics();

//    surfaceView.setLayoutParams(new LayoutParams(sketchWidth(), sketchHeight()));

//    layout.addView(surfaceView);
//    surfaceView.setVisibility(1);
//    println("visibility " + surfaceView.getVisibility() + " " + SurfaceView.VISIBLE);
//    layout.addView(surfaceView);
//    AttributeSet as = new AttributeSet();
//    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(layout, as);

//    lp.addRule(android.R.styleable.ViewGroup_Layout_layout_height,
//    layout.add
        //lp.addRule(, arg1)
        //layout.addView(surfaceView, sketchWidth(), sketchHeight());

//      new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
//        RelativeLayout.LayoutParams.FILL_PARENT);

        if (sw == displayWidth && sh == displayHeight) {
            // If using the full screen, don't embed inside other layouts
            window.setContentView(surfaceView);
        } else {
            // If not using full screen, setup awkward view-inside-a-view so that
            // the sketch can be centered on screen. (If anyone has a more efficient
            // way to do this, please file an issue on Google Code, otherwise you
            // can keep your "talentless hack" comments to yourself. Ahem.)
            RelativeLayout overallLayout = new RelativeLayout(this);
            RelativeLayout.LayoutParams lp =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);

            LinearLayout layout = new LinearLayout(this);
            layout.addView(surfaceView, sketchWidth(), sketchHeight());
            overallLayout.addView(layout, lp);
            window.setContentView(overallLayout);
        }

        finished = false; // just for clarity

        // this will be cleared by draw() if it is not overridden
        looping = true;
        redraw = true;  // draw this guy once

        Context context = getApplicationContext();
        sketchPath = context.getFilesDir().getAbsolutePath();

        handler = new Handler();

        start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void invalidate() {
        if (surfaceView != null) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    surfaceView.invalidate();

                }
            });
        }

    }

    // .............................................................

    public class SketchSurfaceView extends SurfaceView implements
            SurfaceHolder.Callback {

        PGraphicsAndroid2D g2;
        SurfaceHolder surfaceHolder;


        public SketchSurfaceView(Context context, int wide, int high,
                                 Class<? extends PGraphicsAndroid2D> clazz) {
            super(context);

//      println("surface holder");
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed
            surfaceHolder = getHolder();
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

//      println("creating graphics");
            if (clazz.equals(PGraphicsAndroid2D.class)) {
                g2 = new PGraphicsAndroid2D();
            } else {
                try {
                    Constructor<? extends PGraphicsAndroid2D> constructor =
                            clazz.getConstructor();
                    g2 = constructor.newInstance();
                } catch (Exception exception) {
                    throw new RuntimeException(
                            "Error: Failed to initialize custom Android2D renderer",
                            exception);
                }
            }

            // Set semi-arbitrary size; will be set properly when surfaceChanged() called
            g2.setSize(wide, high);
//      newGraphics.setSize(getWidth(), getHeight());
            g2.setParent(MyPApplet.this);
            g2.setPrimary(true);
            // Set the value for 'g' once everything is ready (otherwise rendering
            // may attempt before setSize(), setParent() etc)
//      g = newGraphics;
            g = g2;  // assign the g object for the PApplet

//      println("setting focusable, requesting focus");
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
//      println("done making surface view");
        }


//    public PGraphics getGraphics() {
//      return g2;
//    }


        // part of SurfaceHolder.Callback
        public void surfaceCreated(SurfaceHolder holder) {
        }


        // part of SurfaceHolder.Callback
        public void surfaceDestroyed(SurfaceHolder holder) {
            //g2.dispose();
        }


        // part of SurfaceHolder.Callback
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (DEBUG) {
                System.out.println("SketchSurfaceView2D.surfaceChanged() " + w + " " + h);
            }
            surfaceChanged = true;

//      width = w;
//      height = h;
//
//      g.setSize(w, h);
        }


        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            surfaceWindowFocusChanged(hasFocus);
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return surfaceTouchEvent(event);
        }


        @Override
        public boolean onKeyDown(int code, android.view.KeyEvent event) {
            return surfaceKeyDown(code, event);
        }


        @Override
        public boolean onKeyUp(int code, android.view.KeyEvent event) {
            return surfaceKeyUp(code, event);
        }


        // don't think i want to call stop() from here, since it might be swapping renderers
//    @Override
//    protected void onDetachedFromWindow() {
//      super.onDetachedFromWindow();
//      stop();
//    }
    }

    // ...........................................................

    public class MySketchSurfaceViewGL extends SketchSurfaceViewGL {
        public MySketchSurfaceViewGL(Context context, int wide, int high,
                                   Class<? extends PGraphicsOpenGL> clazz) {
            super(context, wide, high, clazz);
           // setWillNotDraw(false);
        }

        @Override
        protected void onDraw( Canvas canvas ) {

           /* if ( CardboardP.surface != null ) {
                // Requires a try/catch for .lockCanvas( null )
                try {
                    final Canvas surfaceCanvas = CardboardP.surface.lockCanvas( null ); // Android canvas from surface
                  //  super.onDraw( surfaceCanvas ); // Call the WebView onDraw targeting the canvas
                    ByteBuffer buf = ByteBuffer.allocateDirect(1920 * 1080 * 4);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    GLES20.glReadPixels(0, 0, 1920, 1080,
                            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
                    buf.rewind();

                    Bitmap bmp = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(buf);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    paint.setDither(true);

                    surfaceCanvas.drawBitmap(bmp, 0, 0, paint);

                    bmp.recycle();
                    CardboardP.surface.unlockCanvasAndPost( surfaceCanvas ); // We're done with the canvas!
                } catch ( Surface.OutOfResourcesException excp ) {
                    excp.printStackTrace();
                }
            }*/
             super.onDraw( canvas ); // <- Uncomment this if you want to show the original view*
        }

    }

    // ...........................................................

}