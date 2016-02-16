package edu.umich.engin.dpm.angel_grabber.sensor;

import android.util.Log;

import edu.umich.engin.dpm.angel_grabber.data.SensorDataHandler;

/**
 * Created by Richmond on 1/24/2016.
 */
public final class SensorBuffer {
    private static final String TAG = "SensorBuffer";

    private final SensorQueue[] mQueues;

    private int mActiveQueue = 0;
    private int mPreviousQueue = -1;


    public SensorBuffer(SensorType sensorType, int numQueues, int queueSize)
            throws IllegalAccessException, InstantiationException {
        Log.d(TAG, "Constructor");

        mQueues = new SensorQueue[numQueues];
        for (int i = 0; i < numQueues; ++i) {
            mQueues[i] = new SensorQueue(sensorType, queueSize);
        }
    }

    public boolean recordReading(int value, SensorDataHandler connector) {
        Log.v(TAG, "recordReading");

        if (mActiveQueue == mPreviousQueue) {
            Log.e(TAG, "No available queues");
            return false;
        }

        SensorQueue.Status status = mQueues[mActiveQueue].recordReading(value);
        switch (status) {
            case ReadyToSend:
                Log.d(TAG, "Ready to send queue #" + String.valueOf(mActiveQueue));
                nextQueue();
                connector.receiveData(mQueues[mPreviousQueue]);
                return true;
            case Locked:
                Log.d(TAG, "Locked");
                nextQueue();
                return recordReading(value, connector);
        }

        return true;
    }

    private void nextQueue() {
        mPreviousQueue = mActiveQueue;
        if (++mActiveQueue == mQueues.length) {
            mActiveQueue = 0;
        }
    }
}
