package com.IamTechknow.asg2.app;

import android.app.Fragment;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.FrameLayout;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Simple test to demonstrate testing workflow
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mActivity;
    private FrameLayout mLayout;
    private Fragment mFragment;

    private SensorManager mManager;
    private Sensor mAccelerometer;

    public ApplicationTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mLayout = (FrameLayout) mActivity.findViewById(R.id.container);
        mFragment = new MainActivity.PlaceholderFragment();

        //Try setting the fragment
        mActivity.getFragmentManager().beginTransaction()
                .add(R.id.container, mFragment)
                .commit();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    //test preconditions - verify test fixture is set up correctly (this method is automatically run)
    public void testPreconditions() {
        assertNotNull("MainActivity is null", mActivity);
        assertNotNull("FrameLayout is null", mLayout);
        assertNotNull("Fragment is null", mFragment);
    }

    //Test the accelerometer
    public void testSensors() {
        mManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        assertNotNull("Could not obtain accelerometer", mAccelerometer);

        assertTrue(mAccelerometer.getMinDelay() > 0); //test if sensor can return a value at any time
    }
}