package edu.umich.engin.dpm.angel_grabber.data;

import edu.umich.engin.dpm.angel_grabber.sensor.SensorQueue;
import edu.umich.engin.dpm.angel_grabber.stream.Streamable;

/**
 * Created by Richmond on 1/24/2016.
 */
public interface SensorDataHandler extends Streamable {
    public void receiveData(SensorQueue data);
}
