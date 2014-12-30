package com.jacobjoelgonzalez.bikedata;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Created by Jacob on 12/15/2014.
 */
public class saveDataDialog extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.save_data_dialog, container, false);

        //Add title
        getDialog().setTitle(R.string.save_data_dialog_title);

        //Save action button
        Button saveButton = (Button)view.findViewById(R.id.saveButton);
        final EditText fileNameInput = (EditText)view.findViewById(R.id.fileName);
        final TextView errorBox = (TextView)view.findViewById(R.id.saveErrorView);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check for external storage
                if(!isExternalStorageWritable()){
                    errorBox.setVisibility(View.VISIBLE);
                    errorBox.setText(R.string.external_storage_error);
                    return;
                }

                /*Create a file*/
                File parentDir = Environment.getExternalStoragePublicDirectory("BIKE DATA");
                //Make sure parent directory exists
                parentDir.mkdirs();
                String fileName = fileNameInput.getText().toString()+".txt";
                File file = new File(parentDir, fileName);

                //Check if file already exists
                if(file.exists()){
                    errorBox.setVisibility(View.VISIBLE);
                    errorBox.setText(R.string.file_exists_error);
                    return;
                }

                try {
                    //Write sensor readings to file
                    PrintWriter out = new PrintWriter(file);

                    //Avoid concurrent access to sensor reading buffer with connectedThread
                    synchronized (MainActivity.lock) {
                        out.write(MainActivity.sensorReadings.toString());

                        //Check if write was successful
                        if (out.checkError()) {
                            errorBox.setVisibility(View.VISIBLE);
                            errorBox.setText(R.string.write_error);
                            return;
                        }

                        //Clear sensor readings
                        MainActivity.sensorReadings = new StringBuilder(0);
                    }

                    //Notify user of successful file save
                    Toast.makeText(getActivity(), fileName+" has been saved.", Toast.LENGTH_SHORT).show();

                    //Exit dialog
                    dismiss();

                }catch(FileNotFoundException e){
                    errorBox.setVisibility(View.VISIBLE);
                    errorBox.setText(R.string.file_not_found_error);
                }
            }
        });

        //Cancel action button
        Button cancelButton = (Button)view.findViewById(R.id.cancelSaveButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }

    /**
     * Checks if external storage is available for read and write
     * @return - True if the phones external storage is available for read and write, and false otherwise.
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
