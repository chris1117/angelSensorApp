package edu.umich.engin.dpm.angel_grabber.data;

import android.os.Build;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.umich.engin.dpm.angel_grabber.sensor.SensorQueue;
import edu.umich.engin.dpm.angel_grabber.sensor.SensorReading;

/**
 * Created by Richmond on 1/24/2016.
 */
public class FileHandler {
    private static final String TAG = "FileHandler";

    private static final String SUB_DIRECTORY = "AngelGrabber";
    private static final String EXTENSION = "csv";

    private static final String STORAGE_DIRECTORY =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                    Environment.DIRECTORY_DOCUMENTS : Environment.DIRECTORY_DCIM);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS a");

    private final BufferedWriter mWriter;

    private final Date mStartTime;
    private long mFirstSensorTimestamp = -1;

    private boolean mIsRunning = false;


    public FileHandler(String filename, Date startTime) throws IOException {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            throw new IOException("External storage is not mounted");
        }

        File root = new File(Environment.getExternalStoragePublicDirectory(STORAGE_DIRECTORY), SUB_DIRECTORY);
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new FileNotFoundException("Unable to create directory \"" + SUB_DIRECTORY + "\" in \"" + STORAGE_DIRECTORY + "\"");
            }
        }

        File file = new File(root, filename + "." + EXTENSION);
        mWriter = new BufferedWriter(new FileWriter(file, true));
        mStartTime = startTime;
    }

    public void writeQueue(SensorQueue queue) throws IOException {
        new WriteThread(queue).start();
    }

    private void writeLine(SensorReading reading) throws IOException {
        long timeFromStart = (reading.getTimestamp() - mFirstSensorTimestamp);

        mWriter.write(String.valueOf(timeFromStart / 1000.0));
        mWriter.write(44);
        mWriter.write(DATE_FORMAT.format(mStartTime.getTime() + timeFromStart));
        mWriter.write(44);
        mWriter.write(String.valueOf(reading.getValue()));
        mWriter.write(10);
    }

    public void close() {
        try {
            mWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class WriteThread extends Thread {
        private final SensorQueue mQueue;

        public WriteThread(SensorQueue queue) {
            mQueue = queue;
        }

        public void run() {
            while (mIsRunning) {
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ex) { }
            }

            mIsRunning = true;
            SensorReading[] readings = mQueue.getReadings();

            if (mFirstSensorTimestamp < 0 && readings.length > 0) {
                mFirstSensorTimestamp = readings[0].getTimestamp();
            }

            for (SensorReading r : readings) {
                try {
                    writeLine(r);
                }
                catch (IOException ex) { }
            }
            mQueue.unlock();
            mIsRunning = false;
        }
    }
}
