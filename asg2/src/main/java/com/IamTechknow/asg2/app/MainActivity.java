package com.IamTechknow.asg2.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Bundle;
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
        private CameraDevice mCamera;
        private CameraManager mCameraManager;
        private String mCameraId;
        private CameraCaptureSession mCaptureSession;
        private Size mPreviewSize;
        private SurfaceView mSurfaceView;

        public PlaceholderFragment() {
        }

        //Setup the fragment. Ensure it survives rotation and setup camera
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            try { //FIXME: Finish this
                String[] cameraList = mCameraManager.getCameraIdList();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            //Resume camera session
            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

            try { //FIXME: Finish this
                //mCameraManager.openCamera(mCameraId, , );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPause() {
            super.onPause();

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);

            //FIXME: Do sth with this
            mSurfaceView = (SurfaceView) v.findViewById(R.id.camera_view);

            return v;
        }
    }
}
