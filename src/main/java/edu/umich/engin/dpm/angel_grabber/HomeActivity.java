/*
 * Copyright (c) 2015, Seraphim Sense Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 *    and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.umich.engin.dpm.angel_grabber;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.angel.sdk.BleCharacteristic;
import com.angel.sdk.BleDevice;
import com.angel.sdk.ChAccelerationEnergyMagnitude;
import com.angel.sdk.ChAccelerationWaveform;
import com.angel.sdk.ChBatteryLevel;
import com.angel.sdk.ChHeartRateMeasurement;
import com.angel.sdk.ChOpticalWaveform;
import com.angel.sdk.ChStepCount;
import com.angel.sdk.ChTemperatureMeasurement;
import com.angel.sdk.SrvActivityMonitoring;
import com.angel.sdk.SrvBattery;
import com.angel.sdk.SrvHealthThermometer;
import com.angel.sdk.SrvHeartRate;
import com.angel.sdk.SrvWaveformSignal;

import junit.framework.Assert;

import edu.umich.engin.dpm.angel_grabber.data.SensorDataHandler;
import edu.umich.engin.dpm.angel_grabber.data.SensorDataHandlerToFile;
import edu.umich.engin.dpm.angel_grabber.sensor.SensorBuffer;
import edu.umich.engin.dpm.angel_grabber.sensor.SensorType;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);
        orientation = getResources().getConfiguration().orientation;

        mHandler = new Handler(this.getMainLooper());

        mPeriodicReader = new Runnable() {
            @Override
            public void run() {
                mBleDevice.readRemoteRssi();
                if (mChAccelerationEnergyMagnitude != null) {
                    mChAccelerationEnergyMagnitude.readValue(mAccelerationEnergyMagnitudeListener);
                }

                mHandler.postDelayed(mPeriodicReader, RSSI_UPDATE_INTERVAL);
            }
        };

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mHRWaveformView = (GraphView) findViewById(R.id.graph_green);
            mHRWaveformView.setStrokeColor(0xffffffff);
            mRelHRWaveformView = (GraphView) findViewById(R.id.graph_blue);
            mRelHRWaveformView.setStrokeColor(0xffffffff);
            mAccelerationWaveformView = (GraphView) findViewById(R.id.graph_acceleration);
            mAccelerationWaveformView.setStrokeColor(0xfff7a300);

            mSensorDataHandler = new SensorDataHandlerToFile();
            try {
                mAccelerometerBuffer = new SensorBuffer(SensorType.ACCELEROMETER, 16, 256);
                mHeartRateBuffer = new SensorBuffer(SensorType.HEARTRATE, 8, 8);
                mHRWaveformBlueBuffer = new SensorBuffer(SensorType.HR_WAVEFORM_BLUE, 16, 256);
                mHRWaveformGreenBuffer = new SensorBuffer(SensorType.HR_WAVEFORM_GREEN, 16, 256);
            }
            catch (InstantiationException ex) {
                Toast.makeText(this, "Couldn't make buffers: instantiation exception", Toast.LENGTH_LONG);
            }
            catch (IllegalAccessException ex) {
                Toast.makeText(this, "Couldn't make buffers: illegal access exception", Toast.LENGTH_LONG);
            }

            mSensorDataHandler.onStreamStarted();
        }
    }

    protected void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        assert(extras != null);
        mBleDeviceAddress = extras.getString("ble_device_address");

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            connectGraphs(mBleDeviceAddress);
        } else {
            connect(mBleDeviceAddress);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            displaySignalStrength(0);
        }
        unscheduleUpdaters();
        if (mHRWaveformView != null) {
            mHRWaveformView.stopFillAtInterval();
        }
        mBleDevice.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSensorDataHandler != null) {
            mSensorDataHandler.onStreamStopped();
        }
    }

    private void connectGraphs(String deviceAddress) {

        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }
        mBleDevice = new BleDevice(this, mDeviceGraphLifecycleCallback, mHandler);

        try {
            mBleDevice.registerServiceClass(SrvWaveformSignal.class);
            mBleDevice.registerServiceClass(SrvHeartRate.class);

        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InstantiationException e) {
            throw new AssertionError();
        }

        mBleDevice.connect(deviceAddress);

    }

    private void connect(String deviceAddress) {
        // A device has been chosen from the list. Create an instance of BleDevice,
        // populate it with interesting services and then connect

        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }
        mBleDevice = new BleDevice(this, mDeviceLifecycleCallback, mHandler);

        try {
            mBleDevice.registerServiceClass(SrvHeartRate.class);
            mBleDevice.registerServiceClass(SrvHealthThermometer.class);
            mBleDevice.registerServiceClass(SrvBattery.class);
            mBleDevice.registerServiceClass(SrvActivityMonitoring.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InstantiationException e) {
            throw new AssertionError();
        }

        mBleDevice.connect(deviceAddress);

        scheduleUpdaters();
        displayOnDisconnect();
    }

    private final BleDevice.LifecycleCallback mDeviceGraphLifecycleCallback = new BleDevice.LifecycleCallback() {
        @Override
        public void onBluetoothServicesDiscovered(BleDevice bleDevice) {
            try {
                bleDevice.getService(SrvWaveformSignal.class).getAccelerationWaveform().enableNotifications(mAccelerationWaveformListener);
            } catch (AssertionError ex) { }
            //bleDevice.getService(SrvWaveformSignal.class).getOpticalWaveform().enableNotifications(mOpticalWaveformListener);
            try {
                bleDevice.getService(SrvHeartRate.class).getHeartRateMeasurement().enableNotifications(mHeartRateListener);
            } catch (AssertionError ex) { }
        }

        @Override
        public void onBluetoothDeviceDisconnected() {
            unscheduleUpdaters();
            connectGraphs(mBleDeviceAddress);
        }

        @Override
        public void onReadRemoteRssi(int i) {

        }
    };


    /**
     * Upon Heart Rate Service discovery starts listening to incoming heart rate
     * notifications. {@code onBluetoothServicesDiscovered} is triggered after
     * {@link BleDevice#connect(String)} is called.
     */
    private final BleDevice.LifecycleCallback mDeviceLifecycleCallback = new BleDevice.LifecycleCallback() {
        @Override
        public void onBluetoothServicesDiscovered(BleDevice device) {
            try {
                device.getService(SrvHeartRate.class).getHeartRateMeasurement().enableNotifications(mHeartRateListener);
            } catch (AssertionError ex) {}
            try {
                device.getService(SrvHealthThermometer.class).getTemperatureMeasurement().enableNotifications(mTemperatureListener);
            } catch (AssertionError ex) {}
            try {
                device.getService(SrvBattery.class).getBatteryLevel().enableNotifications(mBatteryLevelListener);
            } catch (AssertionError ex) {}
            try {
                device.getService(SrvActivityMonitoring.class).getStepCount().enableNotifications(mStepCountListener);
            } catch (AssertionError ex) {}
            mChAccelerationEnergyMagnitude = device.getService(SrvActivityMonitoring.class).getChAccelerationEnergyMagnitude();
            Assert.assertNotNull(mChAccelerationEnergyMagnitude);
        }


        @Override
        public void onBluetoothDeviceDisconnected() {
            displayOnDisconnect();
            unscheduleUpdaters();

            // Re-connect immediately
            connect(mBleDeviceAddress);
        }

        @Override
        public void onReadRemoteRssi(final int rssi) {
            displaySignalStrength(rssi);
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue> mAccelerationWaveformListener = new BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue>() {
        @Override
        public void onValueReady(ChAccelerationWaveform.AccelerationWaveformValue accelerationWaveformValue) {
            if (accelerationWaveformValue != null && accelerationWaveformValue.wave != null && mAccelerationWaveformView != null)
                for (Integer item : accelerationWaveformValue.wave) {
                    mAccelerationWaveformView.addValue(item);
                    if (mAccelerometerBuffer != null) {
                        mAccelerometerBuffer.recordReading(item, mSensorDataHandler);
                    }
                }

        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue> mOpticalWaveformListener = new BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue>() {
        @Override
        public void onValueReady(ChOpticalWaveform.OpticalWaveformValue opticalWaveformValue) {
            if (opticalWaveformValue != null && opticalWaveformValue.wave != null)
                for (ChOpticalWaveform.OpticalSample item : opticalWaveformValue.wave) {
                    if (mHRWaveformBlueBuffer != null && mHRWaveformGreenBuffer != null) {
                        mHRWaveformGreenBuffer.recordReading(item.green, mSensorDataHandler);
                        mHRWaveformBlueBuffer.recordReading(item.blue, mSensorDataHandler);
                    }
                }
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChHeartRateMeasurement.HeartRateMeasurementValue> mHeartRateListener = new BleCharacteristic.ValueReadyCallback<ChHeartRateMeasurement.HeartRateMeasurementValue>() {
        @Override
        public void onValueReady(final ChHeartRateMeasurement.HeartRateMeasurementValue hrMeasurement) {
            int hr = hrMeasurement.getHeartRateMeasurement();
            Log.d("HomeActivity", "Heart rate: " + hr + " bpm");
            displayHeartRate(hr);
            if (mHRWaveformView != null && mRelHRWaveformView != null) {
                mHRWaveformView.fillAtInterval(200);
                mRelHRWaveformView.fillAtInterval(200);
                mHRWaveformView.addValue((float)(hr));
                mRelHRWaveformView.addValue(relativeHeartrate(hr));
            }
            if (mHeartRateBuffer != null) {
                mHeartRateBuffer.recordReading(hr, mSensorDataHandler);
            }
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChBatteryLevel.BatteryLevelValue> mBatteryLevelListener =
        new BleCharacteristic.ValueReadyCallback<ChBatteryLevel.BatteryLevelValue>() {
        @Override
        public void onValueReady(final ChBatteryLevel.BatteryLevelValue batteryLevel) {
            displayBatteryLevel(batteryLevel.value);
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChTemperatureMeasurement.TemperatureMeasurementValue> mTemperatureListener =
        new BleCharacteristic.ValueReadyCallback<ChTemperatureMeasurement.TemperatureMeasurementValue>() {
            @Override
            public void onValueReady(final ChTemperatureMeasurement.TemperatureMeasurementValue temperature) {
                displayTemperature(temperature.getTemperatureMeasurement());
            }
        };

    private final BleCharacteristic.ValueReadyCallback<ChStepCount.StepCountValue> mStepCountListener =
        new BleCharacteristic.ValueReadyCallback<ChStepCount.StepCountValue>() {
            @Override
            public void onValueReady(final ChStepCount.StepCountValue stepCountValue) {
                displayStepCount(stepCountValue.value);
            }
        };

    private final BleCharacteristic.ValueReadyCallback<ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue> mAccelerationEnergyMagnitudeListener =
        new BleCharacteristic.ValueReadyCallback<ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue>() {
            @Override
            public void onValueReady(final ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue accelerationEnergyMagnitudeValue) {
                displayAccelerationEnergyMagnitude(accelerationEnergyMagnitudeValue.value);
            }
        };

    private void displayHeartRate(final int bpm) {
        TextView textView = (TextView)findViewById(R.id.textview_heart_rate);
        if (textView != null) {
            textView.setText(bpm + " bpm");

            ScaleAnimation effect = new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            effect.setDuration(ANIMATION_DURATION);
            effect.setRepeatMode(Animation.REVERSE);
            effect.setRepeatCount(1);

            View heartView = findViewById(R.id.imageview_heart);
            heartView.startAnimation(effect);
        }
    }

    private void displaySignalStrength(int db) {
        int iconId;
        if (db > -70) {
            iconId = R.drawable.ic_signal_4;
        } else if (db > - 80) {
            iconId = R.drawable.ic_signal_3;
        } else if (db > - 85) {
            iconId = R.drawable.ic_signal_2;
        } else if (db > - 87) {
            iconId = R.drawable.ic_signal_1;
        } else {
            iconId = R.drawable.ic_signal_0;
        }
        ImageView imageView = (ImageView)findViewById(R.id.imageview_signal);
        imageView.setImageResource(iconId);
        TextView textView = (TextView)findViewById(R.id.textview_signal);
        textView.setText(db + "db");
    }

    private void displayBatteryLevel(int percents) {
        int iconId;
        if (percents < 20) {
            iconId = R.drawable.ic_battery_0;
        } else if (percents < 40) {
            iconId = R.drawable.ic_battery_1;
        } else if (percents < 60) {
            iconId = R.drawable.ic_battery_2;
        } else if (percents < 80) {
            iconId = R.drawable.ic_battery_3;
        } else {
            iconId = R.drawable.ic_battery_4;
        }

        ImageView imageView = (ImageView)findViewById(R.id.imageview_battery);
        imageView.setImageResource(iconId);
        TextView textView = (TextView)findViewById(R.id.textview_battery);
        textView.setText(percents + "%");
    }

    private void displayTemperature(final float degreesCelsius) {
        TextView textView = (TextView)findViewById(R.id.textview_temperature);
        textView.setText(degreesCelsius + "\u00b0C");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);
        View thermometerTop = findViewById(R.id.imageview_thermometer_top);
        thermometerTop.startAnimation(effect);
    }

    private void displayStepCount(final int stepCount) {
        TextView textView = (TextView)findViewById(R.id.textview_step_count);
        Assert.assertNotNull(textView);
        textView.setText(stepCount + " steps");

        TranslateAnimation moveDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_PARENT, 0.25f);
        moveDown.setDuration(ANIMATION_DURATION);
        moveDown.setRepeatMode(Animation.REVERSE);
        moveDown.setRepeatCount(1);
        View stepLeft = findViewById(R.id.imageview_step_left);
        stepLeft.startAnimation(moveDown);

        TranslateAnimation moveUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_PARENT, -0.25f);
        moveUp.setDuration(ANIMATION_DURATION);
        moveUp.setRepeatMode(Animation.REVERSE);
        moveUp.setRepeatCount(1);
        View stepRight = findViewById(R.id.imageview_step_right);
        stepRight.startAnimation(moveUp);
    }

    private void displayAccelerationEnergyMagnitude(final int accelerationEnergyMagnitude) {
        TextView textView = (TextView) findViewById(R.id.textview_acceleration);
        Assert.assertNotNull(textView);
        textView.setText(accelerationEnergyMagnitude + "g");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);

        View imageView = findViewById(R.id.imageview_acceleration);
        imageView.startAnimation(effect);
    }

    private void displayOnDisconnect() {
        displaySignalStrength(-99);
        displayBatteryLevel(0);
    }

    private float relativeHeartrate(int hrWorking) {
        if (mMinHR == 0 || hrWorking < mMinHR) {
            mMinHR = hrWorking;
        }
        return 100f * (hrWorking - mMinHR) / (220f - mUserAge - mMinHR);
    }

    private void scheduleUpdaters() {
        mHandler.post(mPeriodicReader);
    }

    private void unscheduleUpdaters() {
        mHandler.removeCallbacks(mPeriodicReader);
    }

    private static final int RSSI_UPDATE_INTERVAL = 1000; // Milliseconds
    private static final int ANIMATION_DURATION = 500; // Milliseconds

    private int orientation;

    private GraphView mAccelerationWaveformView, mRelHRWaveformView, mHRWaveformView;

    private BleDevice mBleDevice;
    private String mBleDeviceAddress;

    private Handler mHandler;
    private Runnable mPeriodicReader;
    private ChAccelerationEnergyMagnitude mChAccelerationEnergyMagnitude = null;

    private SensorDataHandler mSensorDataHandler;

    private SensorBuffer mAccelerometerBuffer;
    private SensorBuffer mHeartRateBuffer;
    private SensorBuffer mHRWaveformBlueBuffer;
    private SensorBuffer mHRWaveformGreenBuffer;

    private int mUserAge = 30;
    private int mMinHR = 0;
}
