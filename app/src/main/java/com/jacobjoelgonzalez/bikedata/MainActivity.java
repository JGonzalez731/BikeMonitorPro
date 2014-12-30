package com.jacobjoelgonzalez.bikedata;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends FragmentActivity implements ActionBar.TabListener, btDeviceDialog.onDeviceSelectedListener {

    /**
     * Used to send sensor readings to desired rpm, speed, and acceleration fragments
     */
    public interface onReadingReceivedListener{
        public void onReadingReceived(double msPerRev);
    }

    //Listener used to send readings to fragments
    private onReadingReceivedListener readingListener;

    //Intent request code
    private final int REQUEST_ENABLE_BT = 1;

    //Layout views
    private ActionBar actionBar;
    private ViewPager pager;

    private Activity activity = this;

    /**
     * Local bluetooth adapter
     */
    private BluetoothAdapter adapter = null;

    /**
     * Member object for bluetooth services
     */
    private BluetoothSensorService sensorService = null;

    /**
     * String to store incoming sensor readings
     */
    private double reading;

    /**
     * String to hold sensor readings for File Save option
     */
    public static StringBuilder sensorReadings = new StringBuilder(0);

    //Object to act a lock for synchronized access to sensorReadings
    public static Object lock = new Object();

    /**
     * Handler for communicating with BluetoothSensorService
     */
    private Handler serviceHandler;

    //Codes to track onActivityResult for onPostResume
    private int request_code;
    private int result_code;

    /**
     * Radius of bike tire in inches to be accessed by SpeedFragment and AccelerationFragment
     * to calculate speed in mph and acceleration in ft/s^2 respectively
     */
    public static double RADIUS_IN = 14.5;
    /**
     * Radius of bike tire in centimeters to be accessed by SpeedFragment and AccelerationFragment
     * to calculate speed in kph and acceleration in m/s^2 respectively
     */
    public static double RADIUS_CM = 36.83;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create a viewPager for sliding action bar tabs
        pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(new pagerAdapter(getSupportFragmentManager()));
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                //Set selected tab to current fragment
                actionBar.setSelectedNavigationItem(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        //Generate action bar tabs
        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab rpmTab = actionBar.newTab().setText("RPM").setTabListener(this);
        actionBar.addTab(rpmTab);
        ActionBar.Tab speedTab = actionBar.newTab().setText("SPEED").setTabListener(this);
        actionBar.addTab(speedTab);
        ActionBar.Tab accelerationTab = actionBar.newTab().setText("ACCEL").setTabListener(this);
        actionBar.addTab(accelerationTab);

        //Initialize handler for communication with non UI threads
        serviceHandler = new Handler(){

            @Override
            public void handleMessage(Message msg){
                switch(msg.what){
                    case Constants.MESSAGE_DEVICE_NAME:
                        //Display connected toast
                        String deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                        Toast.makeText(activity, "Connected to "+deviceName, Toast.LENGTH_LONG).show();

                        //Prompt user to set radius of bike tire
                        setRadiusDialog radiusDialog = new setRadiusDialog();
                        radiusDialog.show(getSupportFragmentManager(), "radius_dialog");
                        break;

                    case Constants.MESSAGE_TIRE_READING:
                        //Get sensor reading
                        reading = msg.getData().getDouble(Constants.TIRE_READING);

                        //Send reading to selected fragment
                        switch(pager.getCurrentItem()){
                            case 1:
                                //Send to speed fragment
                                readingListener = (SpeedFragment)getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.pager+":1");
                                readingListener.onReadingReceived(reading);
                                break;
                            case 2:
                                //Send to acceleration fragment
                                readingListener = (AccelerationFragment)getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.pager+":2");
                                readingListener.onReadingReceived(reading);
                                break;
                        }

                        break;

                    case Constants.MESSAGE_PEDAL_READING:
                        //Get sensor reading
                        reading = msg.getData().getDouble(Constants.PEDAL_READING);

                        //When RPM fragment is selected
                        if(pager.getCurrentItem() == 0) {
                            //Send to RPM fragment
                            readingListener = (RPMFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":0");
                            readingListener.onReadingReceived(reading);
                        }

                        break;

                    case Constants.MESSAGE_TOAST:
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };

        //Get local bluetooth adapter
        adapter = BluetoothAdapter.getDefaultAdapter();

        //Check if bluetooth is supported
        if(adapter == null){
            //Display warning
            Toast.makeText(this, "BikeData requires BT capabilities.", Toast.LENGTH_LONG).show();
            //Exit application
            finish();
        }

        //Check if bluetooth is off
        if(!adapter.isEnabled()){
            //Request bluetooth be enabled if it is off.
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        }
        //If bluetooth is on, setup app session
        else{
            setup();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.save_data:
                saveDataDialog saveDialog = new saveDataDialog();
                saveDialog.show(getSupportFragmentManager(), "save_dialog");
                return true;

            case R.id.set_radius:
                setRadiusDialog radiusDialog = new setRadiusDialog();
                radiusDialog.show(getSupportFragmentManager(), "radius_dialog");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    //Listener for action bar tabs
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        //Set fragment in view pager based on selected tab
        int pos = tab.getPosition();
        pager.setCurrentItem(pos);
    }
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {}
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {}

    /**
     * Setup background operations for communicating with bluetooth sensor
     */
    public void setup(){

        //Initialize the BluetoothSensorService to perform bluetooth connections
        sensorService = new BluetoothSensorService(serviceHandler);

        //Launch btDeviceDialog to see remote devices and scan
        FragmentManager manager = getSupportFragmentManager();
        DialogFragment dialog = new btDeviceDialog();
        dialog.show(manager, "bt_device");
    }

    @Override
    public void onDeviceSelected(String address) {
        //Create bluetooth device object from MAC address
        BluetoothDevice device = adapter.getRemoteDevice(address);

        //Attempt to connect to remote device through Bluetooth sensor service
        sensorService.connect(device);
    }

    /**
     * This class attaches UI fragments to the ViewPager
     */
    private class pagerAdapter extends FragmentPagerAdapter{
        public pagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch(i){
                case 0:
                    return new RPMFragment();
                case 1:
                    return new SpeedFragment();
                default:
                    return new AccelerationFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    //Called after bluetooth enable activity exits
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //Save result and request codes for onPostResume()
        //Which is called after every call to onActivityResult()
        request_code = requestCode;
        result_code = resultCode;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if(request_code == REQUEST_ENABLE_BT){
            //If bluetooth was enabled, setup app session
            if(result_code == RESULT_OK) {
                setup();
            }
            //Otherwise display warning to user and close app
            else{
                Toast.makeText(this, "Please enable BT and reopen BikeData.", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        //Reset codes
        request_code = 0;
        result_code = 0;
    }

}
