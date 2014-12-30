package com.jacobjoelgonzalez.bikedata;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Jacob on 12/8/2014.
 */
public class RPMFragment extends Fragment implements MainActivity.onReadingReceivedListener {

    //View of the fragment, used to update UI from onReadingReceived
    private TextView cadenceValue = null;
    private TextView cadenceDecimalPoint = null;
    private TextView cadenceTenthsValue = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rpm_fragment, container, false);

        //Layout view
        cadenceValue = (TextView)view.findViewById(R.id.cadenceValue);
        cadenceDecimalPoint = (TextView)view.findViewById(R.id.cadenceDecimalPoint);
        cadenceTenthsValue = (TextView)view.findViewById(R.id.cadenceTenthsValue);

        return view;
    }

    /**
     * Calculates the rpm of the bicycle based on the bikes current milliseconds/rev to the tenths place
     * @param mpr - milliseconds per revolution of bike, retrieved from bluetooth sensor.
     * @return - A long array whose first index is the mathematical integer value of the cadence calculation.
     *          The second index is the tenths place of the cadence calculation
     */
    public int[] getCadence(double mpr){

        //Get int value
        int intVal = (int)(60000/mpr);

        //Get the tenths place value
        double remainder = 60000%mpr;
        int tenthsVal = (int)Math.round((remainder/mpr)*10.0);
        if(tenthsVal == 10){
            intVal++;
            tenthsVal = 0;
        }

        int[] values = {intVal, tenthsVal};

        return values;
    }

    @Override
    public void onReadingReceived(double msPerRev) {

        //Calculate the rpm to the nearest tenth
        int[] rpmVals = getCadence(msPerRev);

        //Change text color based on rpm
        if(rpmVals[0] >= 96) changeTextColor(Color.RED);
        else if(86 <= rpmVals[0] && rpmVals[0] <96) changeTextColor(Color.GREEN);
        else if(80 <= rpmVals[0] && rpmVals[0] < 86) changeTextColor(Color.YELLOW);
        else changeTextColor(Color.BLACK);

        //Display current rpm value
        cadenceValue.setText(String.valueOf(rpmVals[0]));
        cadenceTenthsValue.setText(String.valueOf(rpmVals[1]));
    }

    /**
     * Changes the text color of the displayed rpm value.
     * @param color - an integer representation of a color.
     */
    public void changeTextColor(int color){
        cadenceValue.setTextColor(color);
        cadenceDecimalPoint.setTextColor(color);
        cadenceTenthsValue.setTextColor(color);
    }
}
