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
public class SpeedFragment extends Fragment implements AdapterView.OnItemSelectedListener, MainActivity.onReadingReceivedListener {

    //Layout views
    TextView speedBox = null;
    TextView speedTenthsValue = null;

    private final int MPH = 2;  //miles per hour
    private final int KPH = 3;  //kilometers per hour

    private int units = MPH; //Units for calculation function

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.speed_fragment, container, false);

        //Supply the spinner with an array and add listener
        Spinner unitSpinner = (Spinner)fragView.findViewById(R.id.speedUnits);
        unitSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.speed_units, R.layout.unit_spinner_item);
        //Apply the adapter to the spinner
        unitSpinner.setAdapter(adapter);

        //Set view variables
        speedBox = (TextView)fragView.findViewById(R.id.speedValue);
        speedTenthsValue = (TextView)fragView.findViewById(R.id.speedTenthsValue);

        return fragView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position == 0){
            //Set units for calculation function
            units = MPH;
        }
        else{
            units = KPH;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Calculates the speed of the bike using the specified UNITS
     * @param mpr - milliseconds per revolution of bike, retrieved from bluetooth sensor.
     * @param UNITS - the units that the speed calculation should be in.
     *              2 = miles/hour
     *              3 = kilometers/hour
     * @return - the calculated speed of the bicycle, in the specified units.
     */
    public int[] getSpeed(double mpr, int UNITS){
        if(UNITS == MPH) {

            //Get int value
            int intVal = (int) ((1250.0 * Math.PI * MainActivity.RADIUS_IN) / (11 * mpr));

            //Get tenths place value
            double remainder = (1250.0 * Math.PI * MainActivity.RADIUS_IN) % (11 * mpr);
            int tenthsVal = (int)Math.round((remainder / (11 * mpr)) * 10.0);
            if(tenthsVal == 10){
                intVal++;
                tenthsVal = 0;
            }

            int[] mphValues = {intVal, tenthsVal};

            return mphValues;
        }
        else{

            //Get int value
            int intVal = (int)((72*Math.PI*MainActivity.RADIUS_CM)/mpr);

            //Get tenths place value
            double remainder = (72*Math.PI*MainActivity.RADIUS_CM)%mpr;
            int tenthsVal = (int)Math.round((remainder/mpr)*10.0);
            if(tenthsVal == 10){
                intVal++;
                tenthsVal = 0;
            }

            int[] kphValues = {intVal, tenthsVal};

            return kphValues;
        }
    }

    @Override
    public void onReadingReceived(double msPerRev) {

        //Calculate speed to the nearest integer
        int[] speedValues = getSpeed(msPerRev, units);

        //Display speed to UI
        speedBox.setText(String.valueOf(speedValues[0]));
        speedTenthsValue.setText(String.valueOf(speedValues[1]));
    }
}
