package com.jacobjoelgonzalez.bikedata;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by Jacob on 12/8/2014.
 */
public class AccelerationFragment extends Fragment implements AdapterView.OnItemSelectedListener, MainActivity.onReadingReceivedListener {

    //Layout views
    TextView accelerationBox;
    TextView accelerationTenthsValue;

    private double RADIUS_IN = 14.5; //Radius of the bike tire in inches
    private double RADIUS_CM = 28.0; //Radius of the bike tire in centimeters

    private final int FPS2 = 4; //feet/second^2
    private final int MPS2 = 5; //meters/second^2

    private int units = FPS2; //Units for calculation function

    //Stores current milliseconds per revolution of output tire
    private double currentMsPerRev = 0;

    //Stores previous milliseconds per revolution of output tire
    private double prevMsPerRev = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.acceleration_fragment, container, false);

        //Set listener for spinner
        Spinner unitSpinner = (Spinner)fragView.findViewById(R.id.accelerationUnits);
        unitSpinner.setOnItemSelectedListener(this);

        //Get objects to add to array adapter for spinner
        String[] accelerationUnits = {
                new String("ft/s"+(char)178),
                new String("m/s"+(char)178)
        };

        //Set array adapter for spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(), R.layout.unit_spinner_item, accelerationUnits);
        unitSpinner.setAdapter(adapter);

        //Set view variables
        accelerationBox = (TextView)fragView.findViewById(R.id.accelerationValue);
        accelerationTenthsValue = (TextView)fragView.findViewById(R.id.accelerationTenthsValue);

        return fragView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position == 0){
            units = FPS2;
        }
        else{
            units = MPS2;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /*Calculation functions*/

    /**
     * Calculates the rpm of the bicycle based on the bikes current milliseconds/rev
     * @param mpr - milliseconds per revolution of bike, retrieved from bluetooth sensor.
     * @return - the calculated rpm of the bike.
     */
    public double getCadence(double mpr){return 60000.0/mpr;}

    /**
     * Calculates the acceleration of the bicycle.
     * @param currMPR - the current milliseconds per revolution of bike, retrieved from bluetooth sensor.
     * @param prevMPR - the previous mpr sensor reading.
     * @param UNITS - the units that the acceleration calculation should be in.
     *              4 = feet/second^2
     *              5 = meters/second^2
     * @return - the calculated acceleration of the bicycle in the specified units.
     */
    public String[] getAcceleration(double currMPR, double prevMPR, int UNITS){
        double rpmDiff = getCadence(currMPR)-getCadence(prevMPR);
        double timeDiff = currMPR/1000; //Seconds per revolution

        if(UNITS == FPS2) {

            //Get int val
            int intVal = (int)((Math.PI * RADIUS_IN * rpmDiff) / (360 * timeDiff));

            //Get tenths place
            double remainder = (Math.PI * RADIUS_IN * rpmDiff) % (360 * timeDiff);
            int tenthsVal = (int)Math.abs(Math.round((remainder/(360*timeDiff))*10.0));
            if(tenthsVal == 10){
                intVal++;
                tenthsVal = 0;
            }

            //Check for negative values between -1 and 0
            String intValStr;
            if(intVal==0 && rpmDiff<0 && tenthsVal!=0){
                intValStr = "-0";
            }
            else{
                intValStr = String.valueOf(intVal);
            }

            String[] accelerationValues = {intValStr, String.valueOf(tenthsVal)};

            return accelerationValues;
        }
        else {

            //Get int val
            int intVal = (int)((Math.PI * RADIUS_CM * rpmDiff) / (3000 * timeDiff));

            //Get tenths place
            double remainder = (Math.PI * RADIUS_CM * rpmDiff) % (3000 * timeDiff);
            int tenthsVal = (int)Math.abs(Math.round((remainder/(3000*timeDiff))*10.0));
            if(tenthsVal == 10){
                intVal++;
                tenthsVal = 0;
            }

            //Check for negative values between -1 and 0
            String intValStr;
            if(intVal==0 && rpmDiff<0 && tenthsVal!=0){
                intValStr = "-0";
            }
            else{
                intValStr = String.valueOf(intVal);
            }

            String[] accelerationValues = {intValStr, String.valueOf(tenthsVal)};

            return accelerationValues;
        }
    }

    @Override
    public void onReadingReceived(double msPerRev) {
        currentMsPerRev = msPerRev;

        //Calculate acceleration to the nearest integer
        String[] accelerationValues = getAcceleration(currentMsPerRev, prevMsPerRev, units);

        //Set previous ms/rev value to current ms/rev value
        prevMsPerRev = currentMsPerRev;

        //Display acceleration to UI
        accelerationBox.setText(accelerationValues[0]);
        accelerationTenthsValue.setText(accelerationValues[1]);
    }
}
