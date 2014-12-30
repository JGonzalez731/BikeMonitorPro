package com.jacobjoelgonzalez.bikedata;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by Jacob on 12/9/2014.
 */
public class btDeviceDialog extends DialogFragment {
    /**
     * Tag for log
     */
    private final String TAG = "btDeviceDialog: ";

    /**
     * Set for paired devices
     */
    Set<BluetoothDevice> btDevices;

    //List view for displaying paired and found bluetooth devices
    private ListView listView;
    private ListView foundListView;
    //ArrayAdapter that holds the string representations of the paired and found bluetooth devices
    private ArrayAdapter<CharSequence> listAdapter;
    private ArrayAdapter<String> foundListAdapter;

    //Local Bluetooth adapter
    private BluetoothAdapter adapter;

    /**
     * Listener object to send data to the UI activity
     */
    private onDeviceSelectedListener callback;

    /**
     * Interface for sending the address of the selected device to the UI activity
     */
    public interface onDeviceSelectedListener{
        public void onDeviceSelected(String address);
    }

    @Override
    public void onStart() {
        super.onStart();

        //Register for broadcasts when bluetooth discovery is started
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(btReceiver, filter);
    }

    //Create a broadcast receiver for ACTION_FOUND, ACTION_DISCOVERY_STARTED, and ACTION_DISCOVERY_FINISHED
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //When discovery scan begins
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                //Alert the user that the scan has started
                Toast.makeText(getActivity(), "Scanning for devices...", Toast.LENGTH_SHORT).show();
            }

            //When discovery finds a device
            else if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Get bt device object from intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //Skip device if it is bonded because it has already been listed
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //Add device to list view
                    foundListAdapter.add(device.getName() + "   " + device.getAddress());
                    //Refresh list view
                    foundListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (onDeviceSelectedListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Create view with layout for dialog
        final View view = inflater.inflate(R.layout.bt_device_dialogfragment, container, false);

        //Initialize variables
        final Activity activity = getActivity();
        adapter = BluetoothAdapter.getDefaultAdapter();
        listView = (ListView)view.findViewById(R.id.btDeviceListView);
        foundListView = (ListView)view.findViewById(R.id.foundDeviceListView);
        listAdapter = new ArrayAdapter<CharSequence>(activity, android.R.layout.simple_spinner_dropdown_item);
        foundListAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item);


        //Add header to dialog
        getDialog().setTitle(R.string.bt_device_dialog_title);
        //Add a click listener to the scan button
        Button scanButton = (Button)view.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //If adapter is already scanning, stop it
                if(adapter.isDiscovering()){
                    adapter.cancelDiscovery();
                }

                //Change visibility of found devices title
                view.findViewById(R.id.foundDeviceTitle).setVisibility(View.VISIBLE);

                //Clear list view for found devices
                foundListAdapter.clear();
                foundListAdapter.notifyDataSetChanged();

                //Scan for available bt devices
                adapter.startDiscovery();
            }
        });

        //Query paired bt devices
        btDevices = adapter.getBondedDevices();
        //Add them to items for the Dialog
        if(btDevices.size() > 0){
            for(BluetoothDevice device : btDevices){
                listAdapter.add(device.getName()+"   "+device.getAddress());
            }
        }

        //Add on click listener to list view items
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Stop device discovery
                adapter.cancelDiscovery();

                //Get MAC address of the selected bt device
                TextView item = (TextView)view;
                String[] contents = item.getText().toString().split("   ");
                String macAddress = contents[1];

                //Send address to main activity and close dialog
                callback.onDeviceSelected(macAddress);
                dismiss();
            }
        });

        //Set adapter for list views
        listView.setAdapter(listAdapter);
        foundListView.setAdapter(foundListAdapter);

        return view;
    }

    public void onPause(){
        super.onPause();

        //Unregister receiver to avoid IntentReceiver leak
        getActivity().unregisterReceiver(btReceiver);
    }
}