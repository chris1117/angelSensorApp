package edu.umich.engin.dpm.angel_grabber.data;

import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private volatile ArrayList<SensorQueue> mProducerQueue = new ArrayList<SensorQueue>();
    private volatile ArrayList<SensorQueue> mConsumerQueue = new ArrayList<SensorQueue>();

    private volatile boolean mIsRunning = false;
    private ConsumerThread mThread;

    private String mFileNamePrefix = "";
    private Date mSessionStartTime;


    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted");

        mSessionStartTime = Calendar.getInstance().getTime();
        mFileNamePrefix = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(mSessionStartTime);

        mIsRunning = true;
        mThread = new ConsumerThread();
        mThread.start();
    }

    @Override
    public void onStreamStopped() {
        Log.d(TAG, "onStreamStopped");

        mIsRunning = false;

        for (FileHandler handler : mFileHandlers.values()) {
            handler.close();
        }
        mFileHandlers.clear();
    }

    @Override
    public void receiveData(SensorQueue queue) {
        synchronized (mProducerQueue) {
            mProducerQueue.add(queue);
        }
        synchronized (mThread) {
            mThread.notify();
        }
    }

    public void writeData(SensorQueue queue) {
        Log.d(TAG, "Queueing");

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


    private class ConsumerThread extends Thread {
        @Override
        public void run() {
            while (mIsRunning) {
                synchronized (this) {
                    while (mProducerQueue.isEmpty()) {
                        try {
                            wait();
                        } catch (InterruptedException ex) { }
                    }
                }

                synchronized (mProducerQueue) {
                    ArrayList<SensorQueue> temp = mProducerQueue;
                    mProducerQueue = mConsumerQueue;
                    mConsumerQueue = temp;
                }

                for (SensorQueue q : mConsumerQueue) {
                    writeData(q);
                }
                mConsumerQueue.clear();
            }
        }
    }
}
