package com.jacobjoelgonzalez.bikedata;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Created by Jacob on 12/15/2014.
 */
public class setRadiusDialog extends DialogFragment {

    private final int CM = 1;
    private final int INCH = 2;

    private int units = INCH;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //Set title
        builder.setTitle(R.string.set_radius_dialog_title);

        //Set content
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.set_radius_dialog, null);
        builder.setView(view);

        //Supply the spinner with an array and add listener
        Spinner unitSpinner = (Spinner)view.findViewById(R.id.radiusUnits);
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Change units used by positive action button to set radius
                if(position == 0){
                    units = INCH;
                }
                else{
                    units = CM;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.radius_units, R.layout.radius_unit_spinner_item);
        unitSpinner.setAdapter(adapter);

        //Set action buttons
        builder.setPositiveButton("set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Get the radius value
                EditText radiusValue = (EditText)view.findViewById(R.id.radiusValue);
                Double radius = Double.parseDouble(radiusValue.getText().toString());

                //Set the radius value for speed and acceleration calculations
                if(units == INCH){
                    setRADIUS_IN(radius);
                }
                else{
                    setRADIUS_CM(radius);
                }
            }
        })
        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        return builder.create();
    }

    /**
     * Sets the inch and cm radius values based on a given radius in inches.
     * @param radius - the radius of the bike tire in inches.
     */
    public void setRADIUS_IN(double radius){
        MainActivity.RADIUS_IN = radius;
        MainActivity.RADIUS_CM = radius*2.54;
    }

    /**
     * Sets the cm and inch radius values based on the given radius in centimeters
     * @param radius - the radius of the bike tire in centimeters.
     */
    public void setRADIUS_CM(double radius){
        MainActivity.RADIUS_CM = radius;
        MainActivity.RADIUS_IN = radius/2.54;
    }
}
