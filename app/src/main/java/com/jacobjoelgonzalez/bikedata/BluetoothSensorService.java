package com.jacobjoelgonzalez.bikedata;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by Jacob on 12/11/2014.
 */
public class BluetoothSensorService{

    /**
     * UUID used to connect to remote device
     */
    private static UUID uuid = null;

    //Member fields
    private BluetoothAdapter adapter;
    private Handler handler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    /**
     * Sets the BluetoothAdapter and handler for fields for the object
     * @param serviceHandler - a handler for the service to send messages to the UI activity
     */
    public BluetoothSensorService(Handler serviceHandler){

        //Initialize variable
        adapter = BluetoothAdapter.getDefaultAdapter();
        handler = serviceHandler;
    }

    /**
     * Tells the user that a connection attempt is being made with the remote device.
     */
    private void establishing(){
        //Send an establishing connection message back to the UI
        Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Establishing connection...");
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    /**
     * Alerts the user that the attempt to connect to the remote device failed.
     */
    private void connectionFailed(){
        //Send a connection failed message back to the UI
        Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Connection attempt failed.");
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    /**
     * Called from UI to initiate connection to remote device
     */
    public void connect(BluetoothDevice device){
        //Cancel any thread attempting to make a connection
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        //Cancel any thread currently running a connection
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        //Set uuid for connection based on a cached UUID supported by the remote device
        ParcelUuid[] uuids = device.getUuids();

        //If there are no cached UUIDs, inform user
        if(uuids == null){
            Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, "Unable to connect. Try pairing with device first.");
            msg.setData(bundle);
            handler.sendMessage(msg);

            return;
        }

        uuid = uuids[0].getUuid();

        //Start a connectThread to attempt to connect to the remote device
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread{

        private final BluetoothDevice device;
        private final BluetoothSocket socket;

        ConnectThread(BluetoothDevice client){

            //Use tmp, because socket is final
            BluetoothSocket tmp = null;
            device = client;

            //Get a bluetooth socket to connect with the given device
            try{
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            }catch(IOException e){}
            socket = tmp;
        }

        @Override
        public void run(){
            //UI feedback
            establishing();

            //Cancel the device discovery, to open up bandwidth
            adapter.cancelDiscovery();

            //Connect to device through socket
            try{
                socket.connect();
            }catch(IOException e){

                //Unable to connect. Close socket and exit
                connectionFailed();

                try{
                    socket.close();
                }catch(IOException E){}

                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSensorService.this) {
                connectThread = null;
            }

            //Start connected thread
            manageConnectedSocket(socket, device);
        }

        /**
         * Cancels listening socket, and finish thread
         */
        public void cancel(){
            try{
                socket.close();
            }catch(IOException e){}
        }
    }

    /* Variables to set a timeout for sensor readings.*/
    private long pedalStartTime = System.currentTimeMillis();
    private long pedalTimeOut = 2500;
    private int pedalTimeOutCount = 0;
    private double lastPedalReading = 1500000.0;

    private long tireStartTime = System.currentTimeMillis();
    private long tireTimeOut = 2500;
    private int tireTimeOutCount = 0;
    private double lastTireReading = 1500000.0;

    //Stores milliseconds since last the pedal sensor reading
    private long pedalTimeDifference;

    //Stores milliseconds sine the last tire sensor reading
    private long tireTimeDifference;

    //Artificial pedal reading to be sent to UI activity
    private double artificialPedalReading;

    //Artificial tire reading to be sent to UI activity
    private double artificialTireReading;

    /**
     * This thread handles the connection with a remote device.
     */
    private class ConnectedThread extends Thread{
        private final InputStream in;
        private final BluetoothSocket socket;
        private final timerThread timer = new timerThread();

        //Buffer for separating incoming sensor readings
        private StringBuilder sensorReadingBuffer = new StringBuilder(0);
        private String reading;

        public ConnectedThread(BluetoothSocket btSocket){

            InputStream tmpIn = null;
            socket = btSocket;

            //Get and set input stream
            try {
                tmpIn = socket.getInputStream();
            }catch(IOException e){}

            in = tmpIn;
        }

        @Override
        public void run() {
            //Start timerThread
            timer.start();

            //Listen for and receive data from the device
            byte[] buffer = new byte[1024]; //Byte array buffer to store incoming data chunks
            int bytes; //Int to hold the number of bytes read

            //Listen for incoming data until an exception is thrown
            while (true) {
                try {
                    //Read data from the input stream
                    bytes = in.read(buffer);

                    //Construct a char[] from the valid bytes in the byte[]
                    String readStr = new String(buffer, 0, bytes);
                    char[] charArr = readStr.toCharArray();

                    //Separate complete sensor readings
                    for(int i=0; i<charArr.length; i++){

                        //Add any characters that are not a newline to sensor buffer
                        if(charArr[i] != '\n'){
                            sensorReadingBuffer.append(charArr[i]);
                        }

                        //If reading is complete
                        else {
                            reading = sensorReadingBuffer.toString();

                            //Clear buffer
                            sensorReadingBuffer = new StringBuilder(0);

                            /*Separate pedal and tire sensor readings*/
                            //Check for empty string caused by malformed readings
                            if(reading.length() > 0){
                                //Add to sensor readings for file save option
                                synchronized (MainActivity.lock) {
                                    MainActivity.sensorReadings.append(reading + '\n');
                                }

                                //Tire sensor reading
                                if(reading.charAt(0) == '\t'){
                                    //Get milliseconds per revolution from reading
                                    String[] values = reading.split("\t");
                                    //Ignore malformed readings
                                    if(values.length == 4){
                                        //Save and display reading
                                        newTireReading(Double.parseDouble(values[3]));
                                    }
                                }

                                //Pedal sensor reading
                                else{
                                    //Get milliseconds per revolution
                                    String[] values = reading.split("\t");
                                    //Ignore malformed readings
                                    if(values.length == 2){
                                        //Save and display reading
                                        newPedalReading(Double.parseDouble(values[1]));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }

            //Exit timer thread, because this connectedThread is about to die
            timer.cancel();
        }

        //Call from main activity to close thread
        public void cancel(){
            try{
                socket.close();

                //Exit timer thread
                timer.cancel();
            }catch(IOException e){}
        }
    }

    //Sets new pedal readings and sends them to the UI activity. Synchronized to avoid concurrent variable access with timerThread.
    private synchronized void newPedalReading(double reading){
        //Set pedal reading
        lastPedalReading = reading;

        //Send reading to UI handler
        Message msg = handler.obtainMessage(Constants.MESSAGE_PEDAL_READING);
        Bundle bundle = new Bundle();
        bundle.putDouble(Constants.PEDAL_READING, lastPedalReading);
        msg.setData(bundle);
        handler.sendMessage(msg);

        //Reset timer and timeout value
        pedalStartTime = System.currentTimeMillis();
        pedalTimeOut = 2500;
        pedalTimeOutCount = 0;
    }

    //Sets new tire readings and sends them to the UI activity. Synchronized to avoid concurrent variable access with timerThread.
    private synchronized void newTireReading(double reading){
        //Set tire reading
        lastTireReading = reading;

        //Send reading to UI handler
        Message msg = handler.obtainMessage(Constants.MESSAGE_TIRE_READING);
        Bundle bundle = new Bundle();
        bundle.putDouble(Constants.TIRE_READING, lastTireReading);
        msg.setData(bundle);
        handler.sendMessage(msg);

        //Reset timer and timeout value
        tireStartTime = System.currentTimeMillis();
        tireTimeOut = 2500;
        tireTimeOutCount = 0;
    }

    /**
     * This thread acts as a timer for pedal and tire sensor readings.
     * If no sensor reading occurs after three seconds,
     * then the program will start inserting artificial sensors values
     * to smoothly bring the displayed rpm and speed/acceleration values to zero.
     * These artificial sensor values are not recorded in the data set for the file save option.
     * A reading of 1.5 million milliseconds sets the rpm to 0.04 which rounds to 0.0 when rpm is calculated.
     */
    private class timerThread extends Thread{
        //Flag used to exit thread
        private boolean waiting = true;

        @Override
        public void run() {

            while(waiting) {
                //Check pedal timer
                updatePedalSensorTimer();

                //Check tire timer
                updateTireSensorTimer();

                //Only check timer every 100 milliseconds to reduce CPU usage.
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
        }

         // Exits the timer thread
        public void cancel(){
            waiting = false;
        }
    }

    //Gradually brings displayed readings to 0 if tire sensor times out.
    //Synchronized to avoid concurrent variable access with connectedThread.
    private synchronized void updateTireSensorTimer(){
        //Update tire sensor timer
        tireTimeDifference = System.currentTimeMillis() - tireStartTime;
        //Bring calculated rpm down to 0.0 in 4 even increments based on the last received sensor reading
        if (tireTimeDifference > tireTimeOut) {
            //Increase the timeout count
            tireTimeOutCount++;

            //Set artificial reading
            switch(tireTimeOutCount){
                case 1:
                    //Three-fourths of last speed value
                    artificialTireReading = (4.0/3.0)*lastTireReading;
                    break;

                case 2:
                    //One-half of last speed value
                    artificialTireReading = 2.0*lastTireReading;
                    break;

                case 3:
                    //One-fourth of last speed value
                    artificialTireReading = 4.0*lastTireReading;
                    break;

                default:
                    //Set speed to 0
                    artificialTireReading = 1500000.0;
            }

            //Send artificial reading
            Message msg = handler.obtainMessage(Constants.MESSAGE_TIRE_READING);
            Bundle bundle = new Bundle();
            bundle.putDouble(Constants.TIRE_READING, artificialTireReading);
            msg.setData(bundle);
            handler.sendMessage(msg);

            //Reset tire timer
            tireStartTime = System.currentTimeMillis();

            //Lower timeout value
            tireTimeOut = 500;
        }
    }

    //Gradually brings displayed readings to 0 if pedal sensor times out.
    //Synchronized to avoid concurrent variable access with connectedThread.
    private synchronized void updatePedalSensorTimer(){
        //Update pedal sensor timer
        pedalTimeDifference = System.currentTimeMillis() - pedalStartTime;
        //Bring calculated rpm down to 0.0 in 4 even increments based on the last received sensor reading
        if (pedalTimeDifference > pedalTimeOut) {
            //Increase the timeout count
            pedalTimeOutCount++;

            //Set artificial reading based on how many times the sensor has timed out
            switch(pedalTimeOutCount){
                case 1:
                    //Three-fourths of the last calculated rpm
                    artificialPedalReading = (4.0/3.0)*lastPedalReading;
                    break;

                case 2:
                    //One-half of last rpm value
                    artificialPedalReading = 2.0*lastPedalReading;
                    break;

                case 3:
                    //One-fourth of last rpm value
                    artificialPedalReading = 4.0*lastPedalReading;
                    break;

                default:
                    //Set rpm to 0
                    artificialPedalReading = 1500000.0;
            }

            //Send artificial reading
            Message msg = handler.obtainMessage(Constants.MESSAGE_PEDAL_READING);
            Bundle bundle = new Bundle();
            bundle.putDouble(Constants.PEDAL_READING, artificialPedalReading);
            msg.setData(bundle);
            handler.sendMessage(msg);

            //Reset pedal timer
            pedalStartTime = System.currentTimeMillis();

            //Lower timeout value
            pedalTimeOut = 500;
        }
    }

    /**
     * Initiates a connected thread to handle communication with remote device
     * @param socket - connected bluetooth socket
     * @param device - remote device that was connected to.
     */
    private void manageConnectedSocket(BluetoothSocket socket, BluetoothDevice device){
        //Cancel the thread that completed the connection
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        //Cancel any thread currently running a connection
        if(connectedThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        //Start the thread to manage the bluetooth connection and transmit incoming sensor reading
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }
}
