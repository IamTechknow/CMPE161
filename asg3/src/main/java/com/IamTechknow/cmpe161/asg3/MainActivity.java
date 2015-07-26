package com.IamTechknow.cmpe161.asg3;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.view.TextureView;
import android.util.Size;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Button;
import android.widget.SeekBar;

import java.util.Collections;

/**
 * Android Implementation of CMPE 161 Assignment 3
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
                    .add(R.id.container, new RollonFragment())
                    .commit();
        }
    }

    /**
     * The program flow of control is: <br />
     * 1) Add the texture view to the top-level view and attach the callback <br />
     * 2) In the callback, when the surface is created, do the following: <br />
     *      Setup the CameraManager
     *      Obtain the CameraCharacteristics for the back facing camera
     *      Get supported sizes for the camera, and find the biggest size
     *      Set the size for the surfaceHolder
     *      open the CameraDevice, and set the CaptureSessionListener callback to start the capture session
     *
     *    If the texture view changed (due to rotation or on app resume), set the biggest size again
     *
     *    If the texture view is destroyed, end the capture session <br />
     *
     *    If the texture view is updated (30 times a second) redraw on shapes textureview
     *
     * 3) In the CameraDevice callback:
     *      Put the texture surface into a list
     *      Setup a camera preview request and make it repeat
     *
     * 4) In the CaptureSession state callback:
     *      Create a request for a camera preview
     *      Add the texture surface for the request
     *      Build the request, and set it to be repeating
     */
    public static class RollonFragment extends Fragment {
        //Camera fields
        public static final String TAG = "VideoOverlay";
        private CameraDevice mCamera;
        private CameraManager mCameraManager;
        private CameraCharacteristics mCameraCharacteristics;
        private String mCameraId;
        private CameraCaptureSession mCaptureSession;
        private Size mPreviewSize;
        private CaptureRequest.Builder mPreviewBuilder;

        //Sensor and graphics fields
        public final float RADIUS = 20.0f, WIDTH = 8.0f;
        private SensorManager mSensorManager;
        private Paint mPaint;

        //UI fields
        private TextureView mTextureView;
        private SurfaceView mSurfaceView;
        private SurfaceHolder mSurfaceHolder;
        private Switch mRawSwitch, mFusedSwitch;
        private Button mReset;
        private SeekBar mSeekBar;

        //Thread fields
        private Handler mPreviewHandler;
        private HandlerThread mPreviewThread;

        //Callback fields
        private SensorEventListener mShakeResponse = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) { //Check for shake
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = event.values[0], y = event.values[1], z = event.values[2];
                    float accelSqrt = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);


                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                //When the camera is opened start the preview
                Log.i(TAG, "Successfully opened camera");
                mCamera = camera;
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                if (texture == null) {
                    Log.e(TAG, "texture is null");
                    return;
                }

                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(texture);

                try {
                    mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                } catch (CameraAccessException e){
                    e.printStackTrace();
                }

                mPreviewBuilder.addTarget(surface);
                try { //Create a list containing the surface before creating session
                    mCamera.createCaptureSession(Collections.singletonList(surface), mPreviewStateCallback, null);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Failed to create a capture session", e);
                    e.printStackTrace();
                }
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

        private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                //Try to draw the shapes here by obtaining a surface from the surface view and its canvas
                Surface s = mSurfaceHolder.getSurface();
                Canvas c = s.lockCanvas(null); //redraw the whole screen. define rect won't work

                c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //always reset canvas

                s.unlockCanvasAndPost(c);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "onSurfaceTextureSizeChanged()");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.i(TAG, "onSurfaceTextureDestroyed()");
                return true;
            }

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "onSurfaceTextureAvailable()");

                mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
                try{ //Get information about the camera to create the CameraDevice
                    mCameraId = mCameraManager.getCameraIdList()[0];
                    mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

                    //Get supported file formats and sizes and get the largest preview size
                    StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = getBestSize(map.getOutputSizes(SurfaceTexture.class));

                    mCameraManager.openCamera(mCameraId, mStateCallback, null);
                }
                catch(CameraAccessException e) {
                    Log.e(TAG, "Unable to list cameras", e);
                    e.printStackTrace();
                }
            }
        };

        private CameraCaptureSession.StateCallback mPreviewStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                //Request for preview footage and set a repeating request
                mCaptureSession = session;
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                setupHandler();

                try {
                    mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mPreviewHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosed(CameraCaptureSession session) {
                mCaptureSession = null;
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e(TAG, "Configuration error on device '" + mCamera.getId());
            }
        };

        public RollonFragment() {
        }

        //Setup the fragment. Ensure it survives rotation and setup camera, paint, and sensor manager
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mPaint = new Paint();

            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(WIDTH);
            mPaint.setColor(Color.BLUE);
            mPaint.setStyle(Paint.Style.FILL);

            setupHandler();
            mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
            mSensorManager.registerListener(mShakeResponse, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onResume() {
            //Re-register background handler and listeners
            super.onResume();
            setupHandler();
            mSensorManager.registerListener(mShakeResponse, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

            if(mCameraId != null) //Resume camera session
                try {
                    if(mTextureView.isAvailable())
                        mCameraManager.openCamera(mCameraId, mStateCallback, mPreviewHandler);
                    else
                        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
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

            mPreviewThread.quitSafely();
            try {
                mPreviewThread.join();
            } catch (InterruptedException ex) {
                Log.e(TAG, "Background worker thread was interrupted while joined", ex);
            }

            //Free accelerometer
            mSensorManager.unregisterListener(mShakeResponse);
            super.onPause();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);

            //Obtain the texture view to implement callbacks for displaying raw pixel data
            mTextureView = (TextureView) v.findViewById(R.id.camera_view);
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

            //Set the SurfaceView to respond to touch events to create shapes
            //Here we set the view to be transparent to see camera preview
            mSurfaceView = (SurfaceView) v.findViewById(R.id.shape_view);
            mSurfaceView.setZOrderOnTop(true);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

            //Configure the switch to set whether to draw lines or circles
            mRawSwitch = (Switch) v.findViewById(R.id.raw_switch);
            mRawSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(mFusedSwitch.isChecked())
                        mFusedSwitch.setClickable(false);
                    else
                        mFusedSwitch.setClickable(true);
                }
            });

            mFusedSwitch = (Switch) v.findViewById(R.id.fused_switch);
            mFusedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(mRawSwitch.isChecked())
                        mRawSwitch.setClickable(false);
                    else
                        mRawSwitch.setClickable(true);
                }
            });

            mReset = (Button) v.findViewById(R.id.reset);
            mReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRawSwitch.setClickable(true); mFusedSwitch.setClickable(true);
                    mRawSwitch.setChecked(false); mFusedSwitch.setChecked(false);
                }
            });

            mSeekBar = (SeekBar) v.findViewById(R.id.seekBar);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            return v;
        }

        //Choose the largest size for the preview
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

        private void setupHandler() {
            // Start a background thread to manage camera requests
            mPreviewThread = new HandlerThread("background");
            mPreviewThread.start();
            mPreviewHandler = new Handler(mPreviewThread.getLooper());
        }
    }
}
