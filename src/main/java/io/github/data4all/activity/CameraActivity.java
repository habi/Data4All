package io.github.data4all.activity;

import io.github.data4all.R;
import io.github.data4all.handler.CapturePictureHandler;
import io.github.data4all.logger.Log;
import io.github.data4all.view.AutoFocusCrossHair;
import io.github.data4all.view.CameraPreview;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * This class serves as Acitivity for the camera . This class creates the Camera
 * with all associated views and handles the control of the camera in
 * cooperation with the @CameraPreview Class
 * 
 * @author: Andre Koch
 */

public class CameraActivity extends Activity {

    // Logger Tag
    private static final String TAG = CameraActivity.class.getSimpleName();

    private Camera mCamera;
    private CameraPreview cameraPreview;
    private ImageButton btnCapture;
    private AutoFocusCrossHair mAutofocusCrosshair;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate is called");
        super.onCreate(savedInstanceState);

        // remove title and status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        // Checking camera availability
        Log.d(TAG, "check if device support Camera");
        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.noCamSupported), Toast.LENGTH_LONG)
                    .show();
            finish();
            Log.d(TAG, "device supports no camera");
            return;
        }

        btnCapture = (ImageButton) findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(btnCaptureOnClickListener);

    }

    private OnClickListener btnCaptureOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            
          
            
            mCamera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if(success){
                        mCamera.takePicture(shutterCallback, null,
                                new CapturePictureHandler(getApplicationContext()));
                    }
                }
            });
            
            btnCapture.setOnClickListener(null);
            btnCapture.setClickable(false);

        }
    };

    
 
        
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume is called");

        super.onResume();

        if (mCamera == null) {
            Log.d(TAG, "camera is null, so we have to recreate");
            this.createCamera();
        }
        
        btnCapture.setOnClickListener(btnCaptureOnClickListener);
        btnCapture.setClickable(true);

    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause is called");
        super.onPause();

        if (mCamera != null) {
            Log.d(TAG, "camera is not null on pause, so release it");
            cameraPreview.setCamera(null);
            this.releaseCamera();
        }

        btnCapture.setOnClickListener(null);
        btnCapture.setClickable(false);

    }

    /**
     * This method setup the camera.It calls a method from @CameraPreview and
     * sets all camera parameters.
     */
    private void createCamera() {
        Log.i(TAG, "createCamera is called");

        if (this.isDeviceSupportCamera()) {
            mCamera = this.getCameraInstance();
            cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
            cameraPreview.setCamera(mCamera);
        } else {
            Log.e(TAG, "Device not support camera");
        }
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
     * This method release the camera for other applications and set camera to
     * null.
     */
    private void releaseCamera() {
        Log.d(TAG, "release Camera is called");
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.releaseCamera();
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Log.d(TAG, "try to get an instance of camera");
        Camera camera = null;
        try {
            this.releaseCamera();
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        } catch (RuntimeException ex) {
            Log.e(TAG, "failed to get an instance of camera", ex);
        }

        return camera;
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {

        }
    };

}
