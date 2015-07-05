package com.IamTechknow.asg2.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.util.Size;

/**
 * Android Implementation of CMPE 161 Assignment 2. Uses the new Camera2 API found in API level 21 and above.
 * Uses code from the following examples:
 * https://android.googlesource.com/platform/frameworks/base/+/fd887436bd111e4d2c7307578a51b5070025b7f2/tests/Camera2Tests/CameraToo/src/com/example/android/camera2/cameratoo/CameraTooActivity.java
 * https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
 * http://www.willowtreeapps.com/blog/camera2-and-you-leveraging-android-lollipops-new-camera/
 * http://pierrchen.blogspot.com/2015/01/android-camera2-api-explained.html
 * http://jylee-world.blogspot.com/2014/12/a-tutorial-of-androidhardwarecamera2.html
 */

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hide the status bar and window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        //Camera fields
        public static final String TAG = "VideoOverlay";
        private CameraDevice mCamera;
        private CameraManager mCameraManager;
        private CameraCharacteristics mCameraCharacteristics;
        private String mCameraId;
        private CameraCaptureSession mCaptureSession;
        private Size mPreviewSize;

        //Surface fields
        private SurfaceView mSurfaceView;
        private SurfaceHolder mSurfaceHolder;

        //Thread fields
        private Handler mPreviewHandler;
        private HandlerThread mPreviewThread;

        //Callback fields
        private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Log.i(TAG, "Successfully opened camera");

            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Log.e(TAG, "Camera was disconnected");
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.e(TAG, "State error on device '" + camera.getId() + "': code " + error);
            }
        };

        private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { //Setup the camera here
                Log.i(TAG, "Surface created");
                mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
                try { //Get information about the camera to create the CameraDevice
                    mCameraId = mCameraManager.getCameraIdList()[0];
                    mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

                    //Get supported file formats and sizes and get the largest preview size
                    StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = getBestSize(map.getOutputSizes(ImageFormat.JPEG));
                    mSurfaceHolder.setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                    mCameraManager.openCamera(mCameraId, mStateCallback, null); //open camera. null = use main thread looper
                    //FIXME: Connect the surface to the camera
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Unable to list cameras", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //surface Changed, set new preview size
                StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = getBestSize(map.getOutputSizes(ImageFormat.JPEG));
                mSurfaceHolder.setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "Surface destroyed");
                //Close the camera session and camera
                if(mCaptureSession != null) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }

                if (mCamera != null) {
                    mCamera.close();
                    mCamera = null;
                }

            }
        };

        private CameraCaptureSession.StateCallback mPreviewStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCaptureSession = session;
            }

            @Override
            public void onClosed(CameraCaptureSession session) {
                mCaptureSession = null;
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        };

        public PlaceholderFragment() {
        }

        //Setup the fragment. Ensure it survives rotation and setup camera
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

        }

        @Override
        public void onResume() {
            super.onResume();
            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

            //Resume camera session
            try {
                mCameraManager.openCamera(mCameraId, mStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPause() {
            //Close the camera session and camera
            if(mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }

            if (mCamera != null) {
                mCamera.close();
                mCamera = null;
            }
            super.onPause();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);

            //Obtain the surface holder to implement callbacks for displaying raw pixel data
            mSurfaceView = (SurfaceView) v.findViewById(R.id.camera_view);
            mSurfaceHolder = mSurfaceView.getHolder();

            mSurfaceHolder.addCallback(mSurfaceCallback);

            return v;
        }

        private Size getBestSize(Size[] sizes) {
            Size bestSize = sizes[0];
            int largestArea = bestSize.getWidth() * bestSize.getHeight();
            for (Size s : sizes) {
                int area = s.getWidth() * s.getHeight();
                if (area > largestArea) {
                    bestSize = s;
                    largestArea = area;
                }
            }
            return bestSize;
        }
    }
}
