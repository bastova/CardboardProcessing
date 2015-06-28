//Scott Little 2015, GPLv3
//pBoard is Processing for Cardboard
 
import android.os.Bundle; //for preventing sleep
import android.view.WindowManager;
 
int backColor = 100;
float ax = 0;
float ay = 0;
float az = 0;
float mx = 0;
float my = 0;
float mz = 0; //sensor variables
float ax1 = 0;
float ay1 = 0;
float az1 = 0;
float mx1 = 0;
float my1 = 0;
float mz1 = 0; //sensor variables
float eyex = 50; //camera variables
float eyey = 50;
float eyez = 0;
float panx = 0;
float pany = 0;
float l1 = 0;
float r1 = 0;
float b1 = 0;
float t1 = 0;
float l2 = 0;
float r2 = 0;
float b2 = 0;
float t2 = 0;
PGraphics lv; //left viewport
PGraphics rv; //right viewport
PShape s; //the object to be displayed

PMatrix3D eyeTransL;
PMatrix3D eyeTransR;
 
//********************************************************************
// The following code is required to prevent sleep.
//********************************************************************
void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
// fix so screen doesn't go to sleep when app is active
getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
}
//********************************************************************
 
int mWidth = 0;
int mHeight = 0;
float zNear = 0.1f;
float zFar = 100.0f;
float cameraZ = 0.01f;
float fov = 0;
 
void setup() {
 
fov = 2*atan((mHeight/2.0) / cameraZ); // need to know Fy
mWidth = window.innerWidth;
mHeight = window.innerHeight;
size(mWidth, mHeight); //used to set P3D renderer
//orientation(LANDSCAPE); //causes crashing if not started in this orientation
 
lv = createGraphics(mWidth/2,mHeight,P3D); //size of left viewport
rv = createGraphics(mWidth/2,mHeight,P3D);
 
//s = createShape();
TexturedCube( s, 50, 50);

eyeTransL = new PMatrix3D (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1);
eyeTransR = new PMatrix3D (0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1);
}
 
void draw(){
//draw something fancy on every viewports
//panx = mx;//panx;//-mx*10;
//pany = my;
//panz = mz;
//eyex = ax;
//eyey = ay;//-20;//*az;
//eyez = az;
 
ViewPort(lv, ax, ay, az, mx, my, mz, eyeTransL, b1, l1, r1, t1);
ViewPort(rv, ax1, ay1, az1, mx1, my1, mz1, eyeTransR, b2, l2, r2, t2);
 
//add the two viewports to your main panel
image(lv, 0, 0);
image(rv, mWidth/2, 0);
}
 
void changeBackColor(int c) {
	backColor = c;
}

void setEyeViewTransform(boolean left, float rx, float ry, float rz, float tx, float ty, float tz, float l, float r, float b, float t) {
	if (left) {
		ax = tx;
		ay = ty;
		az = tz;
		mx = rx;
		my = ry;
		mz = rz;
		l1 = l;
		r1 = r;
		b1 = b;
		t1 = t;
	} else {
		ax1 = tx;
		ay1 = ty;
		az1 = tz;
		mx1 = rx;
		my1 = ry;
		mz1 = rz;
		l2 = l;
		r2 = r;
		b2 = b;
		t2 = t;
	}
}

void setEyeViewParams(boolean left, float r11, float r12, float r13, 
						float r21, float r22, float r23,
						float r31, float r32, float r33, 
						float t1, float t2, float t3) {
	if (left) {
		eyeTransL = 
			new PMatrix3D(r11, r12, r13, 0, r21, r22, r23, 0, r31, r32, r33, 0, t1, t2, t3, 1);
	} else {
		eyeTransR = 
			new PMatrix3D(r11, r12, r13, 0, r21, r22, r23, 0, r31, r32, r33, 0, t1, t2, t3, 1);
	}
}

void onAccelerometerEvent(float x, float y, float z){
ax = x;
ay = y;
az = z;
}
 
void onGyroscopeEvent(float x, float y, float z){
mx = x;
my = y;
mz = z;
}
 
void ViewPort(PGraphics v, float x, float y, float z, float px, float py, float pz, PMatrix3D m, float b, float l, float r, float t){
v.beginDraw();
v.background(backColor);
//v.lights();
//v.pushMatrix();
//v.camera(x+eyeoff, y, 300, px, py, 0, 0.0, 1.0, 0.0);
/*v.applyMatrix(m.m00, m.m10, m.m20, m.m30,
			  m.m01, m.m11, m.m21, m.m31,
			  m.m02, m.m12, m.m22, m.m32,
			  m.m03, m.m13, m.m23, m.m33);*/
//v.perspective(PI/2f, mWidth/2/mHeight, zNear, zFar);
v.frustum(-tan(l*PI/180f)*0.1,tan(r*PI/180f)*0.1,-tan(b*PI/180f)*0.1,tan(t*PI/180f)*0.1,zNear,zFar);
v.camera( 0.0f, 0.0f, cameraZ, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
//v.beginCamera();
//v.camera();
//v.camera( 0.0f, 0.0f, 0.01f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
v.translate(x,y,z);
v.rotateX(-px);
v.rotateY(py);
v.rotateZ(-pz);
/*v.applyMatrix(m.m00, m.m10, m.m20, m.m30,
			  m.m01, m.m11, m.m21, m.m31,
			  m.m02, m.m12, m.m22, m.m32,
			  m.m03, m.m13, m.m23, m.m33);*/
//v.endCamera();
v.pushMatrix();
v.translate(0,0,-1);
v.box(0.5f);
//v.shape(s);
v.popMatrix();
v.pushMatrix();
v.translate(0,0,-10);
v.box(3);
v.popMatrix();
//v.camera();
v.endDraw();
}
 
void TexturedCube( PShape s, int a, int b) {
beginShape(QUADS);
//texture(tex);
 
// +Z "front" face
vertex(-a, -a, a, 0, b);
vertex( a, -a, a, b, b);
vertex( a, a, a, b, 0);
vertex(-a, a, a, 0, 0);
 
// -Z "back" face
vertex( a, -a, -a, 0, 0);
vertex(-a, -a, -a, b, 0);
vertex(-a, a, -a, b, b);
vertex( a, a, -a, 0, b);
 
// +Y "bottom" face
vertex(-a, a, a, 0, 0);
vertex( a, a, a, b, 0);
vertex( a, a, -a, b, b);
vertex(-a, a, -a, 0, b);
 
// -Y "top" face
vertex(-a, -a, -a, 0, 0);
vertex( a, -a, -a, b, 0);
vertex( a, -a, a, b, b);
vertex(-a, -a, a, 0, b);
 
// +X "right" face
vertex( a, -a, a, 0, 0);
vertex( a, -a, -a, b, 0);
vertex( a, a, -a, b, b);
vertex( a, a, a, 0, b);
 
// -X "left" face
vertex(-a, -a, -a, 0, 0);
vertex(-a, -a, a, b, 0);
vertex(-a, a, a, b, b);
vertex(-a, a, -a, 0, b);
 
endShape();
}