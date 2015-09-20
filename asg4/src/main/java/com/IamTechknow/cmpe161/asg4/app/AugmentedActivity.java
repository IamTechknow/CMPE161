package com.IamTechknow.cmpe161.asg4.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.*;
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
import android.widget.Button;

import java.util.Collections;

public class AugmentedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hide the status bar and window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_augmented);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    /**
     * <p>The app works as follows: </p>
     * <p>1) When the app starts, the initial rotation matrix needs to be obtained. There are two ways:
     * a) from the accelerometer and magnetometer
     *    based on the current rotation of the device. This may be done by SensorManager.getRotationMatrix()
     * b) Start the current orientation with the identity matrix, which represents the initial orientation.
     *    Multiply the initial matrix with the delta matrix from gyro measurements in the integration method. </p>
     *
     * <p>2) When the gyro sensor changes, calculate the new rotation matrix and orientation </p>
     *
     * <p>3) Update the translation of the vectors representing the columns and rotate them </p>
     */

    public static class PlaceholderFragment extends Fragment {
        //Camera fields
        public static final String TAG = "AugmentedApp";
        private CameraDevice mCamera;
        private CameraManager mCameraManager;
        private CameraCharacteristics mCameraCharacteristics;
        private String mCameraId;
        private CameraCaptureSession mCaptureSession;
        private Size mPreviewSize;
        private CaptureRequest.Builder mPreviewBuilder;

        //Sensor and graphics fields
        public static final float NS2S = 1.0f / 1000000000.0f, EPSILON = 0.00000001f, FOV = (90.0f * (float) Math.PI) / 180.0f;
        public static final int N_COLUMNS = 3;
        private SensorManager mSensorManager;
        private Paint mTextPaint, mPaint;
        private float[] mColumnEndPoints, mRotationVector, mOrientation, mPrevOrientation, mOrientDelta, mInitAccel, mInitMag;
        private float[] mDeltaRotationMatrix, mCurRotationMatrix = {1, 0, 0, 0, 1, 0, 0, 0, 1}; //endpoints of lines representing columns, rotation vector is quaternion
        private long mTimeStamp;
        private boolean mHasInitialOrientation;

        //UI fields
        private TextureView mTextureView;
        private SurfaceView mSurfaceView;
        private SurfaceHolder mSurfaceHolder;
        private Button mReset;

        //Thread fields
        private Handler mPreviewHandler;
        private HandlerThread mPreviewThread;

        //Callback fields

        //Set this listener for accelerometer and magnetometer to get initial orientation
        private SensorEventListener mInitialOrientationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    System.arraycopy(event.values, 0, mInitAccel, 0, mInitAccel.length);

                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
					System.arraycopy(event.values, 0, mInitMag, 0, mInitMag.length);

				calculateInitialOrientation();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        //Use the push method to regularly obtain gyro values to update user orientation
        private SensorEventListener mGyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //Compute the timestep's delta rotation after sampling it from gyro data
                if(mTimeStamp != 0 && mHasInitialOrientation) {
                    final float dT = (event.timestamp - mTimeStamp) * NS2S;
                    // Axis of the rotation sample, not normalized yet.
                    float axisX = event.values[0], axisY = event.values[1], axisZ = event.values[2];

                    // Calculate the angular speed of the sample
                    float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (that is, EPSILON should represent your maximum allowable margin of error)
                    if (omegaMagnitude > EPSILON) {
                        axisX /= omegaMagnitude;
                        axisY /= omegaMagnitude;
                        axisZ /= omegaMagnitude;
                    }

                    // Integrate around this axis with the angular speed by the timestep
                    // in order to get a delta rotation from this sample over the timestep
                    // We will convert this axis-angle representation of the delta rotation
                    // into a quaternion before turning it into the rotation matrix.
                    float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                    float sinThetaOverTwo = (float) Math.sin(thetaOverTwo), cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                    mRotationVector[0] = sinThetaOverTwo * axisX;
                    mRotationVector[1] = sinThetaOverTwo * axisY;
                    mRotationVector[2] = sinThetaOverTwo * axisZ;
                    mRotationVector[3] = cosThetaOverTwo;
                }

                mTimeStamp = event.timestamp; //nanoseconds start boot

                //convert rotation matrix from rotation vector
                SensorManager.getRotationMatrixFromVector(mDeltaRotationMatrix, mRotationVector);

                //concatenate the delta rotation we computed with the current rotation
                // in order to get the updated rotation.
                mCurRotationMatrix = mulMatrices(mCurRotationMatrix, mDeltaRotationMatrix);

                //Now get updated rotation orientation vector
				System.arraycopy(mOrientation, 0, mPrevOrientation, 0, mOrientation.length);
                SensorManager.getOrientation(mCurRotationMatrix, mOrientation);

				//Let's get the difference between the two Orientations
				//TODO: Account for overflow
				mOrientDelta = new float[] {mOrientation[0] - mPrevOrientation[0], mOrientation[1] - mPrevOrientation[1], mOrientation[2] - mPrevOrientation[2]};

				updateColumns();
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
                c.drawLines(mColumnEndPoints, 0, N_COLUMNS * 4, mPaint); //stop drawing after values used
				c.drawText("Z: " + Float.toString(mOrientation[0]) + " Y: " + Float.toString(mOrientation[1]) + " X: " + Float.toString(mOrientation[2]), 640.0f, 500.0f, mTextPaint); //debugging only, check orientation

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

        public PlaceholderFragment() {
        }

        //Setup the fragment. Ensure it survives rotation and setup camera, paint, and sensor manager
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            mColumnEndPoints = new float[N_COLUMNS * 4];
            mRotationVector = new float[4]; mDeltaRotationMatrix = new float[9];
            mOrientation = new float[3]; mOrientDelta = new float[3]; mPrevOrientation = new float[3]; mInitAccel = new float[3]; mInitMag = new float[3];
            mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.BLUE);
            mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(20.0f);

            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(Color.BLUE);
            mTextPaint.setTextSize(50.0f);
            mTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setLinearText(true);

			setColVals();

            setupHandler();
            mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
            mSensorManager.registerListener(mGyroListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mInitialOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mInitialOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onResume() {
            //Re-register background handler and listeners
            super.onResume();
            setupHandler();
            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            mSensorManager.registerListener(mGyroListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
            if(!mHasInitialOrientation) {
                mSensorManager.registerListener(mInitialOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mInitialOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            }

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

            //Free gyro. App needs to update initial orientation
            mSensorManager.unregisterListener(mGyroListener);
            mSensorManager.unregisterListener(mInitialOrientationListener);
			mHasInitialOrientation = false;
            super.onPause();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_augmented, container, false);

            //Obtain the texture view to implement callbacks for displaying raw pixel data
            mTextureView = (TextureView) v.findViewById(R.id.camera_view);
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

            //Set the SurfaceView to respond to touch events to create shapes
            //Here we set the view to be transparent to see camera preview
            mSurfaceView = (SurfaceView) v.findViewById(R.id.shape_view);
            mSurfaceView.setZOrderOnTop(true);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

            mReset = (Button) v.findViewById(R.id.reset);
            mReset.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					reset();
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

        private void reset() { //reset the app
			setColVals();
            mRotationVector = new float[4];
            mDeltaRotationMatrix = new float[9];
            mOrientation = new float[3];
            mCurRotationMatrix = new float[] {1, 0, 0, 0, 1, 0, 0, 0, 1};

            mHasInitialOrientation = false;
            mSensorManager.registerListener(mInitialOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mInitialOrientationListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }

		private void setColVals() { //TODO: Init all columns
			mColumnEndPoints = new float[] {300.0f, 300.0f, 300.0f, 1000.0f, 600.0f, 300.0f, 600.0f, 1000.0f, 900.0f, 300.0f, 900.0f, 1000.0f};
		}

		/*
			TODO:Primary method to update the fields representing the columns
		*/
		private void updateColumns() {
			float deltaX, deltaY; //Translate columns by this much

			//With a FOV of 90 degrees, calculate the percentage of difference of rotation
			//for each axis divided by FOV, then multiply that with half the amount of the screen width/height
			deltaX = mOrientDelta[2] > 0 ? (mOrientDelta[0] / FOV) * (mPreviewSize.getWidth() /4) : -(mOrientDelta[2] / FOV) * (mPreviewSize.getWidth() /4);
			deltaY = mOrientDelta[1] > 0 ? (mOrientDelta[2] / FOV) * (mPreviewSize.getHeight() /4) : -(mOrientDelta[1] / FOV) * (mPreviewSize.getHeight() /4);

			//Update X coordinates for columns which have even indices
			for(int i = 0; i < mColumnEndPoints.length; i += 2)
				mColumnEndPoints[i] += deltaX;

			//Update Y coordinates for columns which have odd indices
			for(int i = 1; i < mColumnEndPoints.length; i += 2)
				mColumnEndPoints[i] += deltaY;
		}

        //Calculate initial rotation using accelerator and magentic field data
        private void calculateInitialOrientation() {
            if(SensorManager.getRotationMatrix(mCurRotationMatrix, null, mInitAccel, mInitMag)) {
				SensorManager.getOrientation(mCurRotationMatrix, mOrientation);
				mHasInitialOrientation = true;

				Log.d(TAG, "Obtained initial orientation");
				//disable both sensors
				mSensorManager.unregisterListener(mInitialOrientationListener);
			}
        }

        private float[] mulMatrices(float[] a, float[] b) {
            float[] result = new float[9];
            result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
            result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
            result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];
            result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
            result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
            result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];
            result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
            result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
            result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

            return result;
        }
    }
}
