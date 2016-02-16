package edu.umich.engin.dpm.angel_grabber.data;

import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import edu.umich.engin.dpm.angel_grabber.sensor.SensorQueue;
import edu.umich.engin.dpm.angel_grabber.sensor.SensorType;

/**
 * Created by Richmond on 1/24/2016.
 */
public class SensorDataHandlerToFile implements SensorDataHandler {
    private static final String TAG = "SensorDataHandlerToFile";

    private final Hashtable<SensorType, FileHandler> mFileHandlers = new Hashtable<SensorType, FileHandler>(5);

    private String mFileNamePrefix = "";
    private Date mSessionStartTime;


    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted");

        mSessionStartTime = Calendar.getInstance().getTime();
        mFileNamePrefix = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(mSessionStartTime);
    }

    @Override
    public void onStreamStopped() {
        Log.d(TAG, "onStreamStopped");

        for (FileHandler handler : mFileHandlers.values()) {
            handler.close();
        }
        mFileHandlers.clear();
    }

    @Override
    public void receiveData(SensorQueue queue) {
        Log.d(TAG, "Received queue");

        Log.d(TAG, "Writing queue w/ starting timestamp = " + String.valueOf(queue.getReadings()[0].getTimestamp()));
        SensorType sensor = queue.getSensorType();
        FileHandler handler = mFileHandlers.get(sensor);
        try {
            if (handler == null) {
                String filename = makeFileName(sensor);
                Log.d(TAG, "Making new file handler: " + filename);
                handler = new FileHandler(filename, mSessionStartTime);
                mFileHandlers.put(sensor, handler);
            }
            handler.writeQueue(queue);
        }
        catch (IOException e) {
            Log.e(TAG, "Unable to decode data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String sensorStringId(SensorType sensor) {
        switch (sensor) {
            case ACCELEROMETER:      return "Accelerometer";
            case GYROSCOPE:          return "Gyroscope";
            case HEARTRATE:          return "HeartRate";
            case HR_WAVEFORM_BLUE:   return "HeartRateWaveformBlue";
            case HR_WAVEFORM_GREEN:  return "HeartRateWaveformGreen";

            default:
                return "Unknown";
        }
    }

    private String makeFileName(SensorType sensor) {
        return mFileNamePrefix + "_" + sensorStringId(sensor);
    }
}
