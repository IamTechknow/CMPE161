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
import android.view.*;
import android.util.Size;
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

    public static class RollonFragment extends Fragment {
        //Camera fields
        public static final String TAG = "RollOn";
        private CameraDevice mCamera;
        private CameraManager mCameraManager;
        private CameraCharacteristics mCameraCharacteristics;
        private String mCameraId;
        private CameraCaptureSession mCaptureSession;
        private Size mPreviewSize;
        private CaptureRequest.Builder mPreviewBuilder;

        //Sensor and graphics fields
        public final float RADIUS = 20.0f, ACCEL_FACTOR = 3.0f, GAMMA_MULTIPLYER = 0.0001f;
        private float x, y, a_y, a_x, v_x, v_x1, v_y, v_y1, mGamma, w, h, mInterval; //status of ball
        private boolean moveX, moveY;
        private SensorManager mSensorManager;
        private Paint mPaint;

        //UI fields
        private TextureView mTextureView;
        private SurfaceView mSurfaceView;
        private SurfaceHolder mSurfaceHolder;
        private Switch mRawSwitch, mGravity;
        private Button mReset;
        private SeekBar mSeekBar;

        //Thread fields
        private Handler mPreviewHandler;
        private HandlerThread mPreviewThread;

        //Callback fields

        //Use the push method to regularly obtain accelerometer values to move the ball
        private SensorEventListener mAccelListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //get delay of sensor in microseconds, convert to seconds
                mInterval = event.sensor.getMinDelay() / 1000000.0F;
                if(mInterval < 1/30f) //no faster than 30 FPS
                    mInterval = 1/30f;

                updateBall(event.values);
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
                c.drawCircle(x, y, RADIUS, mPaint);

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

                //Set initial position of ball
                x = width/2.0f; y = height/2.0f; w = width; h = height;

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
            moveX = moveY = false;
            a_x = a_y = v_x = v_x1 = v_y = v_y1 = 0;
            mGamma = GAMMA_MULTIPLYER * 20;

            mPaint = new Paint();

            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.BLUE);
            mPaint.setStyle(Paint.Style.FILL);

            setupHandler();
            mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        }

        @Override
        public void onResume() {
            //Re-register background handler and listeners
            super.onResume();
            setupHandler();
            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

            if(mRawSwitch.isChecked()) //unlike touch listeners, we do want to check for checked status here
                mSensorManager.registerListener(mAccelListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            else if(mGravity.isChecked())
                mSensorManager.registerListener(mAccelListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);

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
            mSensorManager.unregisterListener(mAccelListener);
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

            //Configure the switches to disable the opposite switch if needed, and set sensor
            mRawSwitch = (Switch) v.findViewById(R.id.raw_switch);
            mGravity = (Switch) v.findViewById(R.id.fused_switch);

            //Set the switches to respond to touches, not to changed states. Set the sensor manager
            mRawSwitch.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if(mGravity.isChecked()) {
                            mSensorManager.unregisterListener(mAccelListener);
                            mGravity.setChecked(false);
                        }

                        //This is happening before the checked state is toggled, so check for unchecked
                        if(!mRawSwitch.isChecked()) //Set listener
                            mSensorManager.registerListener(mAccelListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                        else {
                            mSensorManager.unregisterListener(mAccelListener);
                            moveX = moveY = false;
                        }
                    }
                    return false; //allow the next listener to change visual state of toggle
                }
            });
            //FIXME: Gravity sensor does not behave as expected
            mGravity.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if(mRawSwitch.isChecked()) {
                            mRawSwitch.setChecked(false);
                            mSensorManager.unregisterListener(mAccelListener);
                        }

                        if(!mGravity.isChecked())
                            mSensorManager.registerListener(mAccelListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
                        else {
                            mSensorManager.unregisterListener(mAccelListener);
                            moveX = moveY = false;
                        }
                    }
                    return false;
                }
            });

            mReset = (Button) v.findViewById(R.id.reset);
            mReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reset();
                }
            });

            mSeekBar = (SeekBar) v.findViewById(R.id.seekBar);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mGamma = GAMMA_MULTIPLYER * progress; //Slider value changed, set gamma value accordingly
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

        private void reset() { //reset the ball
            x = mTextureView.getWidth()/2; y = mTextureView.getHeight()/2;
            moveX = moveY = false;
            a_x = a_y = v_x = v_x1 = v_y = v_y1 = 0; mGamma = GAMMA_MULTIPLYER * 20;
            mSeekBar.setProgress(20);
            mRawSwitch.setClickable(true); mGravity.setClickable(true);
            mRawSwitch.setChecked(false); mGravity.setChecked(false);
        }

        private void updateBall(float[] values) {
            //Check if ball is at boundary. Set ball position limits
            int w_min = (int) RADIUS, h_min = (int) RADIUS;
            int w_max = (int) (w - RADIUS), h_max = (int) (h - RADIUS);
            a_x = -ACCEL_FACTOR * values[0]; a_y = ACCEL_FACTOR * values[1]; //CCW rotation = positive X values

            //Don't update ball if the view is not yet setup
            if(w_max < 0 || h_max < 0) return;

            if(x <= w_min || x >= w_max) {
                moveX = false;
                x = x <= w_min ? w_min : w_max; //Place at left or right boundary
                v_x = 0;
            } else
                moveX = true;

            if(y <= h_min || y >= h_max) {
                moveY = false;
                y = y <= h_min ? h_min: h_max; //Place at top or bottom edge
                v_y = 0;
            } else
                moveY = true;


            //But if the ball is moving away from a boundary, let it
            if((x == w_max && a_x < 0) || (x == w_min && a_x > 0))
                moveX = true;

            if((y == h_max && a_y < 0) || (y == h_min && a_y > 0))
                moveY = true;

            if(moveX) {
                v_x1 = v_x; //set curr value to old
                v_x = v_x1 + (a_x * mInterval) - (mGamma * v_x1);
                x = x + (v_x * mInterval);
            }

            if(moveY) {
                v_y1 = v_y; //set curr value to old
                v_y = v_y1 + (a_y * mInterval) - (mGamma * v_y1);
                y = y + (v_y * mInterval);
            }
        }
    }
}
