package com.jacobjoelgonzalez.bikedata;

/**
 * Created by Jacob on 12/11/2014.
 */

/**
 * Defines several constants used between BluetoothSensorService and the UI
 */
public class Constants {

    //Message types sent from the BluetoothSensorService to the UI handler
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_TIRE_READING = 3;
    public static final int MESSAGE_PEDAL_READING = 4;

    //Key names received from the BluetoothSensorService
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String TIRE_READING = "tire_reading";
    public static final String PEDAL_READING = "pedal_reading";

}
