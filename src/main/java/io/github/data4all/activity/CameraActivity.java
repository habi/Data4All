/*
 * Copyright (c) 2014, 2015 Data4All
 * 
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.data4all.activity;

import io.github.data4all.R;
import io.github.data4all.handler.CapturePictureHandler;
import io.github.data4all.listener.ButtonRotationListener;
import io.github.data4all.logger.Log;
import io.github.data4all.service.OrientationListener;
import io.github.data4all.service.OrientationListener.HorizonListener;
import io.github.data4all.service.OrientationListener.LocalBinder;
import io.github.data4all.util.HorizonCalculationUtil;
import io.github.data4all.util.HorizonCalculationUtil.ReturnValues;
import io.github.data4all.view.AutoFocusCrossHair;
import io.github.data4all.view.CameraPreview;
import io.github.data4all.view.TouchView;
import io.github.data4all.view.CaptureAssistView;

import java.util.Arrays;
import java.util.Timer;

import android.R.color;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Activity for constructing the camera.
 * 
 * This activity is used to to create and handle the lifecycle. It produces the
 * layout. checks for the existence of a camera and generates the animations.
 * This activity stands in connection with the classes {@link CameraPreview
 * } and
 * {@link AutoFocusCrossHair}.
 * 
 * @author Andre Koch
 * @CreationDate 09.02.2015
 * @LastUpdate 12.02.2015
 * @version 1.2
 * 
 */

public class CameraActivity extends AbstractActivity {

    // Logger Tag
    private static final String TAG = CameraActivity.class.getSimpleName();

    OrientationListener orientationListener;
    boolean orientationBound;

    public static final String FINISH_TO_CAMERA = "io.github.data4all.activity.CameraActivity:FINISH_TO_CAMERA";

    private Camera mCamera;

    private CameraPreview cameraPreview;
    private ImageButton btnCapture;
    private AutoFocusCrossHair mAutoFocusCrossHair;

    private OrientationEventListener listener;
    private ShutterCallback shutterCallback;

    private CaptureAssistView cameraAssistView;

    private Button btnCStatus;
    // runs without a timer by reposting this handler at the end of the runnable
    private boolean setUpComplete = false;
    long startTime = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            if (setUpComplete) {
                updateCalibrationStatus();
            }

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate is called");
        super.onCreate(savedInstanceState);

        // remove title and status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        shutterCallback = new ShutterCallback() {
            public void onShutter() {
                final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(200);
            }
        };
    }

    /**
     * Setup the layout and find the views.
     */
    private void setLayout() {
        setContentView(R.layout.activity_camera);

        // Set the capturing button
        btnCapture = (ImageButton) findViewById(R.id.btnCapture);
        btnCStatus = (Button) findViewById(R.id.calibrationStatus);
        listener = new ButtonRotationListener(this,
                Arrays.asList((View) btnCapture));

        cameraAssistView = (CaptureAssistView) findViewById(R.id.cameraAssistView);

        // Set the Focus animation
        mAutoFocusCrossHair = (AutoFocusCrossHair) findViewById(R.id.af_crosshair);
        AbstractActivity.addNavBarMargin(getResources(), btnCapture);
        AbstractActivity.addNavBarMargin(getResources(), btnCStatus);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setLayout();
        if (isDeviceSupportCamera()) {
            try {
                cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);

                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                cameraPreview.setCamera(mCamera);
                mCamera.startPreview();
                this.setListener(btnCapture);
            } catch (RuntimeException ex) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.noCamSupported), Toast.LENGTH_LONG)
                        .show();
                Log.e(TAG, "device supports no camera", ex);
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.noCamSupported), Toast.LENGTH_LONG)
                    .show();
            finish();
            Log.d(TAG, "device supports no camera");
            return;
        }
        listener.enable();
        Intent intent = new Intent(this, OrientationListener.class);
        bindService(intent, orientationListenerConnection,
                Context.BIND_AUTO_CREATE);
        this.startService(intent);
        setUpComplete = true;
        this.timerRunnable.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationListener.setHorizonListener(null);
        if (orientationBound) {
            unbindService(orientationListenerConnection);
            orientationBound = false;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            cameraPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
        listener.disable();

        stopService(new Intent(this, OrientationListener.class));
        timerHandler.removeCallbacks(timerRunnable);
        setUpComplete = false;
    }

    private void updateCalibrationStatus() {
        switch (OrientationListener.CALIBRATION_STATUS) {
        case OrientationListener.CALIBRATION_OK:
            this.btnCStatus.setBackgroundResource(R.color.sensorOk);
            this.btnCStatus.setClickable(false);
            break;
        case OrientationListener.CALIBRATION_BROKEN_ALL:
            this.btnCStatus.setBackgroundResource(R.color.sensorBrokenAll);
            this.btnCStatus.setClickable(true);
            break;
        case OrientationListener.CALIBRATION_BROKEN_ACCELEROMETER:
            this.btnCStatus.setBackgroundResource(R.color.sensorBroken);
            this.btnCStatus.setClickable(true);
            break;
        case OrientationListener.CALIBRATION_BROKEN_MAGNETOMETER:
            this.btnCStatus.setBackgroundResource(R.color.sensorBroken);
            this.btnCStatus.setClickable(true);
            break;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * io.github.data4all.activity.AbstractActivity#onWorkflowFinished(android
     * .content.Intent)
     */
    @Override
    protected void onWorkflowFinished(Intent data) {
        if (data == null || !data.getBooleanExtra(FINISH_TO_CAMERA, false)) {
            finishWorkflow(data);
        }
    }

    /* ********************************************************** *
     * ********************************************************** *
     * **********************************************************
     */

    /**
     * Set the camera-action listener to the given image-button.
     * 
     * @author tbrose
     * 
     * @param button
     *            The image-button to use.
     */
    private void setListener(ImageButton button) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OrientationListener.CALIBRATION_STATUS == OrientationListener.CALIBRATION_OK) {
                    // After photo is taken, disable button for clicking twice
                    btnCapture.setEnabled(false);
                    mCamera.takePicture(shutterCallback, null,
                            new CapturePictureHandler(CameraActivity.this,
                                    cameraPreview));
                } else {
                    showCalibrationDialog();
                }
            }
        });

        button.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (OrientationListener.CALIBRATION_STATUS == OrientationListener.CALIBRATION_OK) {
                    // After photo is taken, disable button for clicking twice
                    btnCapture.setEnabled(false);

                    mAutoFocusCrossHair.showStart();
                    mAutoFocusCrossHair.doAnimation();
                    mCamera.autoFocus(new AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                mAutoFocusCrossHair.success();
                                if (cameraAssistView != null
                                        && !cameraAssistView.isSkylook()) {
                                    mCamera.takePicture(shutterCallback, null,
                                            new CapturePictureHandler(
                                                    CameraActivity.this,
                                                    cameraPreview));

                                } else {
                                    mAutoFocusCrossHair.fail();
                                    btnCapture.setEnabled(true);
                                }
                            } else {
                                mAutoFocusCrossHair.fail();
                                btnCapture.setEnabled(true);
                            }
                        }
                    });
                } else {
                    showCalibrationDialog();
                }
                return true;

            }
        });
    }

    /**
     * This method looks whether the device has a camera and then returns a
     * boolean.
     * 
     * @return boolean true if device has a camera, false otherwise
     */
    private boolean isDeviceSupportCamera() {
        Log.d(TAG, "look if device has camera");
        return getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }

    /**
     * Define method to check calibration status.
     * 
     * @param view
     *            current view used this method
     */
    public void onClickCalibrationStatus(View view) {
        showCalibrationDialog();
    }

    /**
     * This method shows an AlertDialog if the phone need recalibration.
     */
    private void showCalibrationDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        switch (OrientationListener.CALIBRATION_STATUS) {
        case OrientationListener.CALIBRATION_OK:
            alertDialogBuilder.setTitle(R.string.goodSensorCalibrationTitle);
            break;
        case OrientationListener.CALIBRATION_BROKEN_ALL:
            alertDialogBuilder.setTitle(R.string.badSensorCalibrationTitle);
            break;
        case OrientationListener.CALIBRATION_BROKEN_ACCELEROMETER:
            alertDialogBuilder
                    .setTitle(R.string.badAcceleometerCalibrationTitle);
            break;
        case OrientationListener.CALIBRATION_BROKEN_MAGNETOMETER:
            alertDialogBuilder
                    .setTitle(R.string.badMagnetometerCalibrationTitle);
            break;
        }

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.badSensorCalibration)
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    /**
     * This method update the Camera View Assist for drawing the horizontal line
     * on sensor change.
     */
    public void updateCameraAssistView() {

        if (orientationListener != null) {
            final Camera.Parameters params = mCamera.getParameters();
            final float horizontalViewAngle = (float) Math.toRadians(params
                    .getVerticalViewAngle());
            final float verticalViewAngle = (float) Math.toRadians(params
                    .getHorizontalViewAngle());
            cameraAssistView.setInformations(horizontalViewAngle,
                    verticalViewAngle,
                    orientationListener.getDeviceOrientation());
            cameraAssistView.invalidate();
            // disable the camerabutton when the camera looks to the sky
            if (!cameraAssistView.isSkylook()) {
                btnCapture.setVisibility(View.VISIBLE);
            } else {
                btnCapture.setVisibility(View.GONE);
            }
        }

    }

    /** Defines callbacks for the orientation service, passed to bindService() */
    private ServiceConnection orientationListenerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            orientationListener = binder.getService();
            orientationBound = true;
            HorizonListener horizonListener = new OrientationListener.HorizonListener() {

                @Override
                public void makeHorizon(boolean state) {
                    updateCameraAssistView();
                }

            };
            orientationListener.setHorizonListener(horizonListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            orientationBound = false;
        }
    };

}
