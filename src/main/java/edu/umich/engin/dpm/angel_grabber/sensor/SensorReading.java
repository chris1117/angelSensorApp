package edu.umich.engin.dpm.angel_grabber.sensor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by Richmond on 1/24/2016.
 */
public class SensorReading implements Externalizable {
    private long mTimestamp = 0;
    private int mValue = 0;


    public SensorReading() { }

    public void set(int v) {
        mTimestamp = System.currentTimeMillis();
        mValue = v;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public int getValue() {
        return mValue;
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        mTimestamp = objectInput.readLong();
        mValue = objectInput.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeLong(mTimestamp);
        objectOutput.writeInt(mValue);
    }
}
