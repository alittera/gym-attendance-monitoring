package com.iot.connectedgym;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.Utils;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView notifications, myBeacon;
    private TextInputLayout user_name, user_email, user_age;
    private EditText full_name, email, age;
    private Button clear_notification, save_data;
    private RadioGroup gender_group;
    private BeaconManager beaconManager;
    private Region region;
    private int room_1 = 53723, room_2 = 44680, room_3 = 56571, currentRoom = 0;

    private SharedPreferences prefs;

    private final String DEBUG_TAG = "DEBUG";
    final private int REQUEST_ENABLE_BT = 125;
    private int request=0, max_request=99;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    myBeacon.setVisibility(View.VISIBLE);
                    notifications.setVisibility(View.GONE);
                    clear_notification.setVisibility(View.GONE);
                    user_name.setVisibility(View.GONE);
                    user_email.setVisibility(View.GONE);
                    user_age.setVisibility(View.GONE);
                    gender_group.setVisibility(View.GONE);
                    save_data.setVisibility(View.GONE);
                    return true;
                case R.id.navigation_notifications:
                    myBeacon.setVisibility(View.GONE);
                    notifications.setVisibility(View.VISIBLE);
                    clear_notification.setVisibility(View.VISIBLE);
                    user_name.setVisibility(View.GONE);
                    user_email.setVisibility(View.GONE);
                    user_age.setVisibility(View.GONE);
                    gender_group.setVisibility(View.GONE);
                    save_data.setVisibility(View.GONE);
                    return true;
                case R.id.navigation_account:
                    myBeacon.setVisibility(View.GONE);
                    notifications.setVisibility(View.GONE);
                    clear_notification.setVisibility(View.GONE);
                    user_name.setVisibility(View.VISIBLE);
                    user_email.setVisibility(View.VISIBLE);
                    user_age.setVisibility(View.VISIBLE);
                    gender_group.setVisibility(View.VISIBLE);
                    save_data.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_settings:
                    myBeacon.setVisibility(View.GONE);
                    notifications.setVisibility(View.GONE);
                    clear_notification.setVisibility(View.GONE);
                    user_name.setVisibility(View.GONE);
                    user_email.setVisibility(View.GONE);
                    user_age.setVisibility(View.GONE);
                    gender_group.setVisibility(View.GONE);
                    save_data.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        myBeacon = (TextView) findViewById(R.id.mybeacon);
        notifications = (TextView) findViewById(R.id.notifications);
        notifications.setMovementMethod(new ScrollingMovementMethod());
        clear_notification = (Button) findViewById(R.id.clear_notifications);
        user_name = (TextInputLayout) findViewById(R.id.text_input_layout_name);
        user_email = (TextInputLayout) findViewById(R.id.text_input_layout_email);
        user_age = (TextInputLayout) findViewById(R.id.text_input_layout_age);
        full_name = (EditText) findViewById(R.id.edit_text_name);
        email = (EditText) findViewById(R.id.edit_text_email);
        age = (EditText) findViewById(R.id.edit_text_age);
        gender_group = (RadioGroup) findViewById(R.id.user_gender);
        save_data = (Button) findViewById(R.id.save_data);

        // Add Beacon Manager
        beaconManager = new BeaconManager(getApplicationContext());

        // Create region
        region = new Region("Room", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        //
        beaconManager.setBackgroundScanPeriod(1000, 1000);

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    beaconManager.startRanging(region);
                }
            }

            @Override
            public void onExitedRegion(Region region) {
                myBeacon.setText("Your current room:\nNo room!");
                notifications.append("You left the room!\n");
                beaconManager.startRanging(region);
            }
        });

        beaconManager.setForegroundScanPeriod(1000, 1000);

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    Beacon nearestBeacon = list.get(0);
                    if (nearestBeacon.getMinor() == room_1) {
                        myBeacon.setText("Your current room:\nRoom 1");
                        if(currentRoom != 1) {
                            notifications.append("You joined Room 1\n");
                            currentRoom = 1;
                        }
                    } else if (nearestBeacon.getMinor() == room_2) {
                        myBeacon.setText("Your current room:\nRoom 2");
                        if(currentRoom != 2) {
                            notifications.append("You joined Room 2\n");
                            currentRoom = 2;
                        }
                    } else if (nearestBeacon.getMinor() == room_3) {
                        myBeacon.setText("Your current room:\nRoom 3");
                        if(currentRoom != 3) {
                            notifications.append("You joined Room 3\n");
                            currentRoom = 3;
                        }
                    }
                    //nearestBeacon.getRssi();
                    //nearestBeacon.getMeasuredPower();
                    Utils.Proximity pos = Utils.computeProximity(nearestBeacon);
                    Log.d(DEBUG_TAG, "  Utils.computeProximity(nearestBeacon): " + Utils.computeProximity(nearestBeacon));
                    String msg = "";
                    if (pos == Utils.Proximity.IMMEDIATE) {
                        msg = "IMMEDIATE";
                    } else if (pos == Utils.Proximity.NEAR) {
                        msg = "NEAR";
                    } else if (pos == Utils.Proximity.FAR) {
                        msg = "FAR";

                    } else if (pos == Utils.Proximity.UNKNOWN) {
                        msg = "UNKNOWN";
                    }
                    Log.d(DEBUG_TAG, "Distance from Beacon: "+msg);
                }
            }
        });

        // User's data
        prefs = getApplicationContext().getSharedPreferences("userData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        boolean firstTime = prefs.getBoolean("firstTime", false);

        if (!firstTime) {
            editor.putBoolean("firstTime", true);
            editor.putString("full_name", "");
            editor.putString("email", "");
            editor.putString("age", "");
            editor.putString("gender", "");
            editor.commit();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume()");
        // ENABLE BLUETooth
        enableBluetoothPermission();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                //beaconManager.startRanging(region);
                beaconManager.startMonitoring(region);
            }
        });

        prefs = getApplicationContext().getSharedPreferences("userData", MODE_PRIVATE);
        full_name.setText(prefs.getString("full_name", null));
        email.setText(prefs.getString("email", null));
        age.setText(prefs.getString("age", null));
        if(prefs.getString("gender", null).equals("M")) {
            gender_group.check(R.id.radio_male);
        }
        else if(prefs.getString("gender", null).equals("F")) {
            gender_group.check(R.id.radio_female);
        }

        save_data.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setData(prefs, full_name.getText().toString(), email.getText().toString(), age.getText().toString(), ((RadioButton)findViewById(gender_group.getCheckedRadioButtonId())).getText().toString());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(DEBUG_TAG, "onPause()");
    }

    @Override
    protected  void onStop(){
        super.onStop();
        Log.d(DEBUG_TAG, "onStop()");
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        Log.d(DEBUG_TAG, "onDestroy()");
        //beaconManager.stopRanging(region);
        beaconManager.stopMonitoring(region);
        beaconManager.disconnect();
    }

    private void enableBLT(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if(request<max_request) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            }
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                enableGPS();
            }
        }else{
            Log.d(DEBUG_TAG,"request more than 4");
        }

    }
    @Override
    //return from  startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT) in enableBLT()
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        Log.i(DEBUG_TAG, "onActivityResult: GPS Enabled by user");
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Log.i(DEBUG_TAG, "onActivityResult: User rejected GPS request");
                        break;
                    default:
                        break;
                }
                break;
            case REQUEST_ENABLE_BT:
                switch(resultCode) {
                    case RESULT_OK:
                        Log.d(DEBUG_TAG,"L utente ha dato il permesso");
                        break;
                    case RESULT_CANCELED:
                        Log.d(DEBUG_TAG,"L utente non ha dato il permesso");
                        break;
                }
        }
    }


    private void enableBluetoothPermission() {
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            //shouldShowRequestPermissionRationale() = If this function is called on pre-M, it will always return false.
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                showMessageOKCancel("You need to allow access for BLT scanning on Android 6.0 and above.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_BT);
                            }
                        });

                return;
            }
            //If this function is called on pre-M, OnRequestPermissionsResultCallback will be suddenly called with correct PERMISSION_GRANTED or PERMISSION_DENIED result.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_BT);
            return;
        }
        //once i have got the ACCESS_COARSE_LOCATION i can check if the blt is turns on.
        enableBLT();
    }

    //return from  enableBluetoothPermission()
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Log.d(DEBUG_TAG,"PERMESSO DATO!!!");
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "PERMISSION_GRANTED Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void enableGPS() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(MainActivity.this).checkLocationSettings(builder.build());



        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    public void clearAllNotifications(View v) {
        notifications.setText("");
        Toast.makeText(this, "Notifications deleted!", Toast.LENGTH_LONG).show();
    }
/*
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_male:
                if (checked)
                    RadioButton btn = (RadioButton) findViewById(R.id.radio_female);
                    // Pirates are the best
                    break;
            case R.id.radio_female:
                if (checked)
                    // Ninjas rule
                    break;
        }
    }*/

    private void setData(SharedPreferences prefs, String name, String email, String age, String gender) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("full_name", name);
        editor.putString("email", email);
        editor.putString("age", age);
        editor.putString("gender", gender);
        editor.commit();
        Toast.makeText(MainActivity.this, "Data saved!", Toast.LENGTH_SHORT).show();
    }

}