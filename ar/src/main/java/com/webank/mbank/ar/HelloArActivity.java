/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.mbank.ar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.webank.mbank.ar.rendering.BackgroundRenderer;
import com.webank.mbank.ar.rendering.ObjectRenderer;
import com.webank.mbank.ar.rendering.PlaneRenderer;
import com.webank.mbank.ar.rendering.PointCloudRenderer;
import com.webank.mbank.ar.utils.CameraPermissionHelper;
import com.webank.mbank.ar.utils.DisplayRotationHelper;
import com.webank.mbank.ar.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.translateM;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = "HelloArActivity";

    private GLSurfaceView surfaceView;
    private Session session;
    private TextView scoreTx;
    private SoundPool soundPool;
    private Anchor hitAnchor;
    private Context mContext;
    private Snackbar messageSnackbar;
    private DisplayRotationHelper displayRotationHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer robot = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloud = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private float[] virtualObjectModelMatrix1 = new float[16];
    private float[] virtualObjectModelMatrix2 = new float[16];
    private float[] robotModelMatrixTemp = new float[16];
    private float[] virtualObjectModelMatrixTemp = new float[16];
    private float[] robotAnimationModelMatrixTemp = new float[16];
    private float[] robotRotateMatrix = new float[16];
    private float[] temp1 = new float[16];


    // Tap handling and UI.
    private final ArrayBlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<float[]> virtualModelMatrixList = new ArrayList<>();
    private final ArrayList<float[]> robotModelMatrixList = new ArrayList<>();
    private final float[] robotModelMatrix = new float[16];

    private boolean isGameOver = true;
    private boolean direction = true;
    private boolean installRequested;
    private boolean xTranslateStrideComplete = false;
    private boolean isFirstDrawComplete = false;
    private boolean isGameStart = false;

    private float yTranslateStride = 0f;
    private float downTime;
    private float upTime;
    //    private float virtualObjectScaleFactor = 0.0012f;
    private float virtualObjectScaleFactor = 0.0009f;
    private float robotScaleFactor = 15f;
    private float translateStride = 0f;
    private float zRotateStride = 0f;
    private float zRotateAngle = 0f;
    private float randomTranslateValue;
    private float halfWidthOftheBox = 0.09f;
    //    private float robotleftX = 0.0676f;
    //    private float rightX = 0.41f;
    //    private float robotRightX = 0.06200808f;
    //    private float UpZ = 0.32f;
    //    private float robotUpZ = 0.08f;
    //    private float DownZ = 0.44f;
    //    private float robotDownZ = 0.143f;
    //    private float robotY = 0.065f;
    //    private float leftX = 0.5f;
    private float pressTime;
    private float robotY = 0.14f;
    private float jumpDistance;
    private float robotX = 0;
    private float robotZ = 0;

    private int n = 1;
    private int soundId;
    private int jumpSuccessNum = 1;
    private int jumpfailNum = 1;
    private int randomOrientation;
    private int gameScore = 0;
    private int maxGameScore = 0;
    private int robotRotateAngle = -90;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_ar);
        mContext = this;
        surfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
        scoreTx = (TextView) findViewById(R.id.game_score);

        surfaceView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            downTime = event.getDownTime();
                            if (isFirstDrawComplete) {
                                playVoice(R.raw.long_press);


                            }
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (isFirstDrawComplete) {
                                releaseVoiceUnloop();
                            }
                            upTime = event.getEventTime();
                            pressTime = upTime - downTime;
                            if (isGameOver) {
                                onSingleTap(event);
                            }
                        }
                        return true;
                    }
                });

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        //配置输出的平面的颜色以及透明度，16位的深度缓存(depth buffer)，不开启遮罩缓存(stencil buffer)。
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        //设置渲染模式
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        installRequested = false;

        //通过设置过大的loglevel，关闭log日志的打印
        Logger.setLogLevel(10);


    }


    private void onSingleTap(MotionEvent e) {

        // Queue tap if there is space. Tap is lost if queue is full.
        queuedSingleTaps.offer(e);

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "亲，您需要安装ARCore哦";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "亲，您的ARCore需要升级哦";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "亲，请升级该APP";
                exception = e;
            } catch (Exception e) {
                message = "亲，抱歉，该手机不支持AR！";
                exception = e;
            }

            if (message != null) {
                showSnackbarMessage(message, true);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(session);
            if (!session.isSupported(config)) {
                showSnackbarMessage("This device does not support AR", true);
            }
            //会话最初有一个默认配置。如果需要配置不同于默认值的配置，则应该调用它。
            // 在调用此方法之前，应该使用检查配置 isSupported (Config config)
            session.configure(config);
        }

        showLoadingMessage();
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        surfaceView.onResume();
        //注册显示监听器
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
        releaseVoiceUnloop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        backgroundRenderer.createOnGlThread(/*context=*/ this);

        // Prepare the other rendering objects.
        try {
            virtualObject.createOnGlThread(/*context=*/ this, "webox.obj", "webox.png");
            robot.createOnGlThread(/*context=*/ this, "webanktest.obj", "webanktest.png");

            //这里采用默认的BlendModeo：BlendMode.Opaque
            virtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
            robot.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            planeRenderer.createOnGlThread(/*context=*/ this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        pointCloud.createOnGlThread(/*context=*/ this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        if (isFirstDrawComplete && pressTime > 300) {
            longPressDraw();
            return;
        }

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.通知ARCore会话视图大小已更改，以便可以适当调整透视矩阵和视频背景
        //因为在onSurfaceChanged中调用了 displayRotationHelper.onSurfaceChanged(width, height)，所以需要通知ARCore回话视图大小已经改变
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            //设置纹理
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            //从ARSession获取当前帧。 当配置设置为UpdateMode.BLOCKING（默认情况下）时，
            // 这会将渲染限制为相机帧速率。
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            if (!isFirstDrawComplete) {
                MotionEvent tap = queuedSingleTaps.poll();
                if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                    for (HitResult hit : frame.hitTest(tap)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon
                        //检查是否有任何平面被检测到，以及是否在平面多边形内部检测
                        Trackable trackable = hit.getTrackable();
                        // Creates an anchor if a plane or an oriented point was hit.
                        if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))) {

                            hitAnchor = hit.createAnchor();
                            isGameOver = false;
                            Log.e(TAG, "产生新的tap，第" + n + "次游戏开始了！");
                            n++;
                            break;
                        }
                    }
                }
            }

            // 1、Draw background.绘制背景
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            //根据图像的平均强度计算照明
            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            //            // Visualize tracked points.
            //            PointCloud pointCloud = frame.acquirePointCloud();
            //            this.pointCloud.update(pointCloud);
            //            //2、绘制检测到的特征点
            //            this.pointCloud.draw(viewmtx, projmtx);
            //
            //            // Application is responsible for releasing the point cloud resources after
            //            // using it.
            //            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (messageSnackbar != null) {
                for (Plane plane : session.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                        if (isGameOver) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    gameStartTip();
                                }
                            });
                        }
                        break;
                    }
                }
            }
            if (!isGameStart) {
                return;
            }

            // 3、Visualize planes.绘制检测到的平面
            planeRenderer.drawPlanes(
                    session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            if (virtualModelMatrixList.size() >= 20) {
                virtualModelMatrixList.remove(0);
            }
            if (hitAnchor == null) {
                return;
            }
            if (hitAnchor.getTrackingState() != TrackingState.TRACKING) {
                return;
            }
            if (virtualModelMatrixList.size() == 0) {
                playVoice(R.raw.game_start);
                hitAnchor.getPose().toMatrix(virtualObjectModelMatrix1, 0);
                virtualObject.updateModelMatrix(virtualObjectModelMatrix1, virtualObjectScaleFactor);
                virtualObject.draw(viewmtx, projmtx, lightIntensity);
                virtualModelMatrixList.add(virtualObjectModelMatrix1);

                hitAnchor.getPose().toMatrix(robotModelMatrix, 0);
                translateM(robotModelMatrix, 0, 0, robotY, 0);
                robot.updateModelMatrix(rotateRobot(robotModelMatrix), robotScaleFactor);
                robot.draw(viewmtx, projmtx, lightIntensity);
                robotModelMatrixList.add(robotModelMatrix);


                hitAnchor.getPose().toMatrix(virtualObjectModelMatrix2, 0);
                translateM(virtualObjectModelMatrix2, 0, 0.45f, 0f, 0f);
                virtualObject.updateModelMatrix(virtualObjectModelMatrix2, virtualObjectScaleFactor);
                virtualObject.draw(viewmtx, projmtx, lightIntensity);
                virtualModelMatrixList.add(virtualObjectModelMatrix2);
                isFirstDrawComplete = true;

            } else {
                for (int i = 0; i < virtualModelMatrixList.size(); i++) {
                    virtualObject.updateModelMatrix(virtualModelMatrixList.get(i), virtualObjectScaleFactor);
                    virtualObject.draw(viewmtx, projmtx, lightIntensity);
                }
                float[] robotModelMatrixNew = robotModelMatrixList.get(0);
                robot.updateModelMatrix(rotateRobot(robotModelMatrixNew), robotScaleFactor);
                robot.draw(viewmtx, projmtx, lightIntensity);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }

    }

    private float[] rotateRobot(float[] robotModelMatrix) {
        System.arraycopy(robotModelMatrix, 0, robotRotateMatrix, 0, robotModelMatrix.length);
        if (direction) {
            robotRotateAngle = -90;
            rotateM(robotRotateMatrix, 0, robotRotateAngle, 0f, 1f, 0f);
            return robotRotateMatrix;
        } else {
            robotRotateAngle = -180;
            rotateM(robotRotateMatrix, 0, robotRotateAngle, 0f, 1f, 0f);
            return robotRotateMatrix;
        }
    }

    private void gameStartTip() {

        AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(mContext);
        normalDialog.setTitle("点击屏幕任意位置开始游戏！");
        normalDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isGameStart = true;
                    }
                });
        normalDialog.show();


    }


    private void longPressDraw() {
        Logger.d(TAG, "longPressDraw执行");
        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            Frame frame = session.update();
            Camera camera = frame.getCamera();

            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtxNew = new float[16];
            camera.getProjectionMatrix(projmtxNew, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtxNew = new float[16];
            camera.getViewMatrix(viewmtxNew, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensityNew = frame.getLightEstimate().getPixelIntensity();

            //            PointCloud pointCloud = frame.acquirePointCloud();
            //            this.pointCloud.update(pointCloud);
            //            this.pointCloud.draw(viewmtxNew, projmtxNew);
            //
            //            // Application is responsible for releasing the point cloud resources after
            //            // using it.
            //            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (messageSnackbar != null) {
                for (Plane plane : session.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

                        planeRenderer.drawPlanes(
                                session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtxNew);

            drawOriginVirtual(viewmtxNew, projmtxNew, lightIntensityNew);
            drawRobotAnimation(viewmtxNew, projmtxNew, lightIntensityNew);

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }


   /* private void drawRobotAnimation(float[] viewmtxNew, float[] projmtxNew, float lightIntensityNew) {

        float[] lastVirtualModelMatrix = virtualModelMatrixList.get(virtualModelMatrixList.size() - 1);
        float[] robotModelMatrix = robotModelMatrixList.get(0);
        System.arraycopy(robotModelMatrix, 0, robotModelMatrixTemp, 0, robotModelMatrix.length);
        System.arraycopy(lastVirtualModelMatrix, 0, virtualObjectModelMatrixTemp, 0, lastVirtualModelMatrix.length);

        float X1 = robotModelMatrix[12];
        float Z1 = robotModelMatrix[14];

        float X2 = lastVirtualModelMatrix[12];
        float Z2 = lastVirtualModelMatrix[14];

        Logger.d("X1=" + X1);
        Logger.d("X2=" + X2);
        Logger.d("Z1=" + Z1);
        Logger.d("Z2=" + Z2);


        if (direction) {
            Logger.d("X轴方向");
            if (translateStride <= jumpDistance) {
//                Logger.d("平移之前");
//
//                Logger.d("robotModelMatrixTemp[12]=" + robotModelMatrixTemp[12]);
//                Logger.d("robotModelMatrixTemp[14]=" + robotModelMatrixTemp[14]);
//                for (int i = 0; i < robotModelMatrixTemp.length; i++) {
//                    Logger.d("robotModelMatrixTemp[" + i + "]=" + robotModelMatrixTemp[i]);
//
//                }

                translateM(robotModelMatrixTemp, 0, translateStride, yTranslateStride, 0);
//                Logger.d("平移之后");
//                Logger.d("robotModelMatrixTemp[12]=" + robotModelMatrixTemp[12]);
//                Logger.d("robotModelMatrixTemp[14]=" + robotModelMatrixTemp[14]);
//                for (int i = 0; i < robotModelMatrixTemp.length; i++) {
//                    Logger.d("robotModelMatrixTemp[" + i + "]=" + robotModelMatrixTemp[i]);
//
//                }

                rotateM(robotModelMatrixTemp, 0, zRotateStride, 0f, 1f, 0f);
//                Logger.d("旋转之后");
//                Logger.d("robotModelMatrixTemp[12]=" + robotModelMatrixTemp[12]);
//                Logger.d("robotModelMatrixTemp[14]=" + robotModelMatrixTemp[14]);
//                for (int i = 0; i < robotModelMatrixTemp.length; i++) {
//                    Logger.d("robotModelMatrixTemp[" + i + "]=" + robotModelMatrixTemp[i]);
//
//                }

                Logger.d("translateM中的xTranslateStride=" + translateStride);
                Logger.d("translateM中的yTranslateStride=" + yTranslateStride);
                Logger.d("rotateM中的zRotateStride=" + zRotateStride);
                robotX = robotModelMatrixTemp[12];
                robotZ = robotModelMatrixTemp[14];
                Logger.d("robotX = robotModelMatrixTemp[12])=" + robotX);
                Logger.d("robotZ = robotModelMatrixTemp[14])=" + robotZ);

                robot.updateModelMatrix(rotateRobot(robotModelMatrixTemp), robotScaleFactor);
                System.arraycopy(robotModelMatrixTemp, 0, robotAnimationModelMatrixTemp, 0, robotModelMatrixTemp.length);
                robot.draw(viewmtxNew, projmtxNew, lightIntensityNew);
                changeStride();

            } else {
                if ((X2 - halfWidthOftheBox) <= robotX && robotX <= (X2 + halfWidthOftheBox)
                        && (Z2 - halfWidthOftheBox) <= robotZ && robotZ <= (Z2 + halfWidthOftheBox)) {
                    Logger.d("小机器人跳起后的位置符合要求，第" + jumpSuccessNum + "次落在桌子范围内");
                    jumpSuccessNum++;
                    Logger.d("X2 - halfWidthOftheBox=" + (X2 - halfWidthOftheBox));
                    Logger.d("robotX=" + robotX);
                    Logger.d("X2 + halfWidthOftheBox=" + (X2 + halfWidthOftheBox));
                    Log.d(TAG, ".............");
                    Logger.d("Z2 - halfWidthOftheBox=" + (Z2 - halfWidthOftheBox));
                    Logger.d("robotZ=" + robotZ);
                    Logger.d("Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));
                    drawNewVirturalObject(virtualObjectModelMatrixTemp, viewmtxNew, projmtxNew, lightIntensityNew);
                    updateRobotModelMatrix(robotAnimationModelMatrixTemp, robotModelMatrix);
                    playVoice(R.raw.jump_success);
                } else {
                    Logger.d(TAG, "小机器人跳起后的位置不符合要求，第" + jumpfailNum + "次落在桌子范围之外");
                    jumpfailNum++;
                    Logger.d(TAG, "X2 - halfWidthOftheBox=" + (X2 - halfWidthOftheBox));
                    Logger.d(TAG, "robotX=" + robotX);
                    Logger.d(TAG, "X2 + halfWidthOftheBox=" + (X2 + halfWidthOftheBox));
                    Log.d(TAG, ".............");
                    Logger.d("Z2 - halfWidthOftheBox=" + (Z2 - halfWidthOftheBox));
                    Logger.d("robotZ=" + robotZ);
                    Logger.d("Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));

                    isFirstDrawComplete = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gameOver(mContext);
                        }
                    });
                }
            }

        } else {
            Logger.d("Z轴方向");
            if (translateStride <= jumpDistance) {
//                Logger.d("平移之前");
//                Logger.d("robotModelMatrixTemp[12]=" + robotModelMatrixTemp[12]);
//                Logger.d("robotModelMatrixTemp[14]=" + robotModelMatrixTemp[14]);

                translateM(robotModelMatrixTemp, 0, 0, yTranslateStride, translateStride);
//                Logger.d("平移之后");
//                Logger.d("robotModelMatrixTemp[12]=" + robotModelMatrixTemp[12]);
//                Logger.d("robotModelMatrixTemp[14]=" + robotModelMatrixTemp[14]);

                rotateM(robotModelMatrixTemp, 0, zRotateStride, 0f, 1f, 0f);
//                Logger.d("旋转之后");
//                Logger.d("robotModelMatrixTemp[12]=" + robotModelMatrixTemp[12]);
//                Logger.d("robotModelMatrixTemp[14]=" + robotModelMatrixTemp[14]);

                Logger.d(TAG, "translateM中的xTranslateStride=" + translateStride);
                Logger.d(TAG, "translateM中的yTranslateStride=" + yTranslateStride);
                Logger.d(TAG, "rotateM中的zRotateStride=" + zRotateStride);
                robotX = robotModelMatrixTemp[12];
                robotZ = robotModelMatrixTemp[14];
//                Logger.d("robotZ = robotModelMatrixTemp[14])=" + robotZ);
                Logger.d("robotX = robotModelMatrixTemp[12])=" + robotX);
                Logger.d("robotZ = robotModelMatrixTemp[14])=" + robotZ);

                robot.updateModelMatrix(rotateRobot(robotModelMatrixTemp), robotScaleFactor);
                System.arraycopy(robotModelMatrixTemp, 0, robotAnimationModelMatrixTemp, 0, robotModelMatrixTemp.length);
                robot.draw(viewmtxNew, projmtxNew, lightIntensityNew);
                changeStride();

            } else {
                if ((X2 - halfWidthOftheBox) <= robotX && robotX <= (X2 + halfWidthOftheBox)
                        && (Z2 - halfWidthOftheBox) <= robotZ && robotZ <= (Z2 + halfWidthOftheBox)) {
                    Logger.d(TAG, "小机器人跳起后的位置符合要求，第" + jumpSuccessNum + "次落在桌子范围内");
                    jumpSuccessNum++;
                    Logger.d(TAG, "Z2 =" + (Z2));
                    Logger.d(TAG, "robotZ=" + robotZ);
                    Logger.d(TAG, "Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));

                    drawNewVirturalObject(virtualObjectModelMatrixTemp, viewmtxNew, projmtxNew, lightIntensityNew);
                    updateRobotModelMatrix(robotAnimationModelMatrixTemp, robotModelMatrix);
                    playVoice(R.raw.jump_success);
                } else {
                    Logger.d(TAG, "小机器人跳起后的位置不符合要求，第" + jumpfailNum + "次落在桌子范围之外");
                    jumpfailNum++;
                    Logger.d(TAG, "Z2 =" + (Z2));
                    Logger.d(TAG, "robotZ=" + robotZ);
                    Logger.d(TAG, "Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));

                    isFirstDrawComplete = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gameOver(mContext);
                        }
                    });

                }
            }
        }
    }
*/

    private void drawRobotAnimation(float[] viewmtxNew, float[] projmtxNew, float lightIntensityNew) {

        float[] lastVirtualModelMatrix = virtualModelMatrixList.get(virtualModelMatrixList.size() - 1);
        float[] robotModelMatrix = robotModelMatrixList.get(0);
        System.arraycopy(robotModelMatrix, 0, robotModelMatrixTemp, 0, robotModelMatrix.length);
        System.arraycopy(lastVirtualModelMatrix, 0, virtualObjectModelMatrixTemp, 0, lastVirtualModelMatrix.length);

        float X1 = robotModelMatrix[12];
        float Z1 = robotModelMatrix[14];

        float X2 = lastVirtualModelMatrix[12];
        float Z2 = lastVirtualModelMatrix[14];

        Logger.d("X1=" + X1);
        Logger.d("X2=" + X2);
        Logger.d("Z1=" + Z1);
        Logger.d("Z2=" + Z2);


        if (direction) {
            Logger.d("X轴方向");
            if (translateStride <= jumpDistance) {
                translateM(robotModelMatrixTemp, 0, translateStride, yTranslateStride, 0);
                rotateM(robotModelMatrixTemp, 0, zRotateStride, 0f, 1f, 0f);
                Logger.d("translateM中的xTranslateStride=" + translateStride);
                Logger.d("translateM中的yTranslateStride=" + yTranslateStride);
                Logger.d("rotateM中的zRotateStride=" + zRotateStride);
                robotX = robotModelMatrixTemp[12];
                robotZ = robotModelMatrixTemp[14];
                Logger.d("robotX = robotModelMatrixTemp[12])=" + robotX);
                Logger.d("robotZ = robotModelMatrixTemp[14])=" + robotZ);

                robot.updateModelMatrix(rotateRobot(robotModelMatrixTemp), robotScaleFactor);
                System.arraycopy(robotModelMatrixTemp, 0, robotAnimationModelMatrixTemp, 0, robotModelMatrixTemp.length);
                robot.draw(viewmtxNew, projmtxNew, lightIntensityNew);
                changeStride();

            } else {
                if ((X2 - halfWidthOftheBox) <= robotX && robotX <= (X2 + halfWidthOftheBox)
                        && (Z2 - halfWidthOftheBox) <= robotZ && robotZ <= (Z2 + halfWidthOftheBox)) {
                    Logger.d("小机器人跳起后的位置符合要求，第" + jumpSuccessNum + "次落在桌子范围内");
                    jumpSuccessNum++;
                    Logger.d("X2 - halfWidthOftheBox=" + (X2 - halfWidthOftheBox));
                    Logger.d("robotX=" + robotX);
                    Logger.d("X2 + halfWidthOftheBox=" + (X2 + halfWidthOftheBox));
                    Logger.d(".............");
                    Logger.d("Z2 - halfWidthOftheBox=" + (Z2 - halfWidthOftheBox));
                    Logger.d("robotZ=" + robotZ);
                    Logger.d("Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));
                    drawNewVirturalObject(virtualObjectModelMatrixTemp, viewmtxNew, projmtxNew, lightIntensityNew);
                    updateRobotModelMatrix(robotAnimationModelMatrixTemp, robotModelMatrix);
                    playVoice(R.raw.jump_success);
                } else {
                    Logger.d(TAG, "小机器人跳起后的位置不符合要求，第" + jumpfailNum + "次落在桌子范围之外");
                    jumpfailNum++;
                    Logger.d(TAG, "X2 - halfWidthOftheBox=" + (X2 - halfWidthOftheBox));
                    Logger.d(TAG, "robotX=" + robotX);
                    Logger.d(TAG, "X2 + halfWidthOftheBox=" + (X2 + halfWidthOftheBox));
                    Logger.d(".............");
                    Logger.d("Z2 - halfWidthOftheBox=" + (Z2 - halfWidthOftheBox));
                    Logger.d("robotZ=" + robotZ);
                    Logger.d("Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));

                    isFirstDrawComplete = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gameOver(mContext);
                        }
                    });
                }
            }

        } else {
            Logger.d("Z轴方向");
            if (translateStride <= jumpDistance) {
                translateM(robotModelMatrixTemp, 0, 0, yTranslateStride, translateStride);
                rotateM(robotModelMatrixTemp, 0, zRotateStride, 0f, 1f, 0f);
                Logger.d(TAG, "translateM中的xTranslateStride=" + translateStride);
                Logger.d(TAG, "translateM中的yTranslateStride=" + yTranslateStride);
                Logger.d(TAG, "rotateM中的zRotateStride=" + zRotateStride);
                robotX = robotModelMatrixTemp[12];
                robotZ = robotModelMatrixTemp[14];
                Logger.d("robotX = robotModelMatrixTemp[12])=" + robotX);
                Logger.d("robotZ = robotModelMatrixTemp[14])=" + robotZ);

                robot.updateModelMatrix(rotateRobot(robotModelMatrixTemp), robotScaleFactor);
                System.arraycopy(robotModelMatrixTemp, 0, robotAnimationModelMatrixTemp, 0, robotModelMatrixTemp.length);
                robot.draw(viewmtxNew, projmtxNew, lightIntensityNew);
                changeStride();

            } else {
                if ((X2 - halfWidthOftheBox) <= robotX && robotX <= (X2 + halfWidthOftheBox)
                        && (Z2 - halfWidthOftheBox) <= robotZ && robotZ <= (Z2 + halfWidthOftheBox)) {
                    Logger.d(TAG, "小机器人跳起后的位置符合要求，第" + jumpSuccessNum + "次落在桌子范围内");
                    jumpSuccessNum++;
                    Logger.d(TAG, "X2 - halfWidthOftheBox=" + (X2 - halfWidthOftheBox));
                    Logger.d(TAG, "robotX=" + robotX);
                    Logger.d(TAG, "X2 + halfWidthOftheBox=" + (X2 + halfWidthOftheBox));
                    Logger.d(".............");
                    Logger.d("Z2 - halfWidthOftheBox=" + (Z2 - halfWidthOftheBox));
                    Logger.d("robotZ=" + robotZ);
                    Logger.d("Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));


                    drawNewVirturalObject(virtualObjectModelMatrixTemp, viewmtxNew, projmtxNew, lightIntensityNew);
                    updateRobotModelMatrix(robotAnimationModelMatrixTemp, robotModelMatrix);
                    playVoice(R.raw.jump_success);
                } else {
                    Logger.d(TAG, "小机器人跳起后的位置不符合要求，第" + jumpfailNum + "次落在桌子范围之外");
                    jumpfailNum++;
                    Logger.d(TAG, "X2 - halfWidthOftheBox=" + (X2 - halfWidthOftheBox));
                    Logger.d(TAG, "robotX=" + robotX);
                    Logger.d(TAG, "X2 + halfWidthOftheBox=" + (X2 + halfWidthOftheBox));
                    Logger.d(".............");
                    Logger.d("Z2 - halfWidthOftheBox=" + (Z2 - halfWidthOftheBox));
                    Logger.d("robotZ=" + robotZ);
                    Logger.d("Z2 + halfWidthOftheBox=" + (Z2 + halfWidthOftheBox));


                    isFirstDrawComplete = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gameOver(mContext);
                        }
                    });

                }
            }
        }
    }

    private void changeStride() {
        jumpDistance = pressTime * 0.39589f / 1000;
        Logger.d("jumpDistance=" + jumpDistance);
        zRotateAngle = (float) (360 * 0.02 / jumpDistance);
        float a = jumpDistance - translateStride;
        if (a >= 0.02) {
            translateStride = (float) (translateStride + 0.02);
            Logger.d(TAG, "a >= 0.02，translateStride = (float) (translateStride + 0.02)=" + translateStride);
        } else {
            if (!xTranslateStrideComplete) {
                Logger.d(TAG, "a >= 0.02条件不满足时，translateStride=" + translateStride);
                Logger.d(TAG, "jumpDistance-translateStride=" + a);
                translateStride = translateStride + a;
                xTranslateStrideComplete = true;
            } else {
                translateStride = (float) (translateStride + 0.02);
                Logger.d(TAG, "前面两个条件都不满足，translateStride = (float) (translateStride + 0.02)执行到");
            }
        }

        if (zRotateStride < 360) {
            float angle = 360 - zRotateStride;
            if (angle >= zRotateAngle) {
                zRotateStride = zRotateStride + zRotateAngle;
            } else {
                Logger.d(TAG, "angle >= zRotateAngle不满足时，zRotateStride=" + zRotateStride);
                Logger.d(TAG, "360 - zRotateStride=" + (360 - zRotateStride));
                zRotateStride = zRotateStride + angle;

            }
        }

        yTranslateStride = (float) (-0.8 * translateStride * translateStride / jumpDistance / jumpDistance + 0.8 * translateStride / jumpDistance + 0.1);


    }

    private void updateRobotModelMatrix(float[] robotAnimationModelMatrixNew, float[] originRobotModelMatrix) {

        float[] tempMatrix = new float[16];
        float destY = robotAnimationModelMatrixNew[13];
        float origY = originRobotModelMatrix[13];
        translateM(robotAnimationModelMatrixNew, 0, 0f, -(destY - origY), 0);
        System.arraycopy(robotAnimationModelMatrixNew, 0, tempMatrix, 0, robotAnimationModelMatrixNew.length);
        robotModelMatrixList.set(0, tempMatrix);


    }

    private void gameOver(Context context) {
        if (maxGameScore < gameScore) {
            maxGameScore = gameScore;
        }
        playVoice(R.raw.game_over);
        AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(context);
        normalDialog.setTitle("游戏失败");
        normalDialog.setMessage("你的得分为：" + gameScore + "\n" + "最高得分为：" + maxGameScore + "\n" + "点击屏幕任意位置重新开始游戏");
        normalDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isGameOver = true;
                    }
                });
        normalDialog.show();
        reset();


    }

    private void reset() {
        scoreTx.setText("0");
        translateStride = 0;
        yTranslateStride = 0;
        zRotateStride = 0;
        zRotateAngle = 0;
        pressTime = 0;
        gameScore = 0;
        direction = true;
        xTranslateStrideComplete = false;


        virtualModelMatrixList.clear();
        robotModelMatrixList.clear();
        hitAnchor = null;

        jumpfailNum = 1;
        jumpSuccessNum = 1;
    }

    private void drawNewVirturalObject(float[] virtualObjectModelMatrix, float[] viewmtxNew,
                                       float[] projmtxNew, float lightIntensityNew) {
        randomOrientation = (int) (2 + Math.random() * 10);
        double n = Math.random();
        if (n <= 0.1) {
            randomTranslateValue = (float) (0.30 + n);
        }
        if (n > 0.1 && n <= 0.2) {
            randomTranslateValue = (float) (0.20 + n);
        }
        if (n > 0.2 && n < 0.5) {
            randomTranslateValue = (float) (0.1 + n);
        }
        if (n >= 0.5 && n < 0.7) {
            randomTranslateValue = (float) (n - 0.2);
        }
        if (n >= 0.7) {
            randomTranslateValue = (float) (n - 0.4);
        }

        gameScore++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scoreTx.setText(gameScore + "");
            }
        });

        float[] virtualObjectModelMatrixNew = new float[16];
        System.arraycopy(virtualObjectModelMatrix, 0, virtualObjectModelMatrixNew, 0, virtualObjectModelMatrix.length);
        //随机数可以被2整除，就往X轴方向跳；否则往Z轴跳。
        if (randomOrientation % 2 == 0) {
            Logger.d(TAG, "新桌子在X轴方向");
            translateM(virtualObjectModelMatrixNew, 0, 1 * randomTranslateValue, 0f, 0f);
            virtualObject.updateModelMatrix(virtualObjectModelMatrixNew, virtualObjectScaleFactor);
            virtualObject.draw(viewmtxNew, projmtxNew, lightIntensityNew);
            virtualModelMatrixList.add(virtualObjectModelMatrixNew);
            direction = true;

        } else {
            Logger.d(TAG, "新桌子在Z轴方向");
            translateM(virtualObjectModelMatrixNew, 0, 0f, 0f, 1 * randomTranslateValue);
            virtualObject.updateModelMatrix(virtualObjectModelMatrixNew, virtualObjectScaleFactor);
            virtualObject.draw(viewmtxNew, projmtxNew, lightIntensityNew);
            virtualModelMatrixList.add(virtualObjectModelMatrixNew);
            direction = false;
        }

        resetStride();


    }

    private void resetStride() {
        translateStride = 0;
        yTranslateStride = 0;
        zRotateStride = 0;
        zRotateAngle = 0;
        pressTime = 0;
        xTranslateStrideComplete = false;
    }


    private void drawOriginVirtual(float[] viewmtxNew, float[] projmtxNew, float lightIntensityNew) {
        for (int i = 0; i < virtualModelMatrixList.size(); i++) {
            virtualObject.updateModelMatrix(virtualModelMatrixList.get(i), virtualObjectScaleFactor);
            virtualObject.draw(viewmtxNew, projmtxNew, lightIntensityNew);
        }

    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        messageSnackbar =
                Snackbar.make(
                        HelloArActivity.this.findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_INDEFINITE);
        messageSnackbar.getView().setBackgroundColor(0xbf323232);

        if (finishOnDismiss) {
            messageSnackbar.setAction(
                    "确定",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            messageSnackbar.dismiss();
                        }
                    });
            messageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            finish();
                        }
                    });
        }
        messageSnackbar.show();
    }

    private void showLoadingMessage() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showSnackbarMessage("手机正在识别平面当中，请左右平移手机！", false);
                    }
                });
    }

    private void hideLoadingMessage() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (messageSnackbar != null) {
                            messageSnackbar.dismiss();
                        }
                        messageSnackbar = null;
                    }
                });
    }

    private void playVoice(int resId) {
        soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 1);
        soundId = soundPool.load(mContext, resId, 1);
        soundPool.setOnLoadCompleteListener(new LoadListener(soundId));
    }


    private static final class LoadListener implements SoundPool.OnLoadCompleteListener {
        private int soundId;

        public LoadListener(int soundId) {
            this.soundId = soundId;
        }

        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            soundPool.play(soundId, 1, 1, 1, 0, 1);

        }

    }

    private void releaseVoiceUnloop() {
        if (soundPool != null && soundId > 0) {
            soundPool.stop(soundId);
            soundPool.release();
            soundPool.setOnLoadCompleteListener(null);
            soundPool = null;
        }
    }
}
