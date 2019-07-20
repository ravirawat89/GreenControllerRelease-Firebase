package com.netcommlabs.greencontroller.utilities;

import android.Manifest;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.netcommlabs.greencontroller.Constants;
import com.netcommlabs.greencontroller.Fragments.FragAddEditSesnPlan;
import com.netcommlabs.greencontroller.Fragments.FragAvailableDevices;
import com.netcommlabs.greencontroller.Fragments.FragDeviceDetails;
import com.netcommlabs.greencontroller.Fragments.FragDeviceMAP;
import com.netcommlabs.greencontroller.Fragments.MyFragmentTransactions;
import com.netcommlabs.greencontroller.activities.MainActivity;
import com.netcommlabs.greencontroller.constant.TagConstant;
import com.netcommlabs.greencontroller.model.DataTransferModel;
import com.netcommlabs.greencontroller.services.BleAdapterService;
import com.netcommlabs.greencontroller.sqlite_db.DatabaseHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.MODE_PRIVATE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Created by Android on 12/7/2017.
 */

public class BLEAppLevel {

    private static BLEAppLevel bleAppLevel;
    private MainActivity mContext;
    private String macAddress;
    private BleAdapterService bluetooth_le_adapter;
    private boolean back_requested = false;
    private Fragment myFragment;
    //private static Fragment myFragmentDD;
    private boolean isBLEConnected = false;
    private int alert_level, totalPlayValvesCount = 0, totalPauseValvesCount = 0, totalPlayPauseValvesCount, pauseIndex = 1, playIndex = 1, stopIndex = 1;
    private String cmdTypeName;
    private static int dataSendingIndex = 0;
    private static boolean oldTimePointsErased = FALSE;
    private ArrayList<DataTransferModel> listSingleValveData;
    private int etDisPntsInt = 0;
    private int etDurationInt = 0;
    private int etWaterQuantWithDPInt = 0;
    private boolean isServiceBound = false;
    private static FragDeviceDetails fragDeviceDetails;
    private SharedPreferences pref, devicePrefs;
    private int valve_number = 1, deviceType;
    public static volatile int LogReadingIndex = 0;           // read index for data from esp32
    private File logFile, alarmsFile;                          // log .txt file for data received
    private FileOutputStream logOutputHandler, alarmOutputHandler;
    private String deviceName, dvcLoc;
    private int planId = 1;

    public static BLEAppLevel getInstance(MainActivity mContext, Fragment myFragment, String macAddress) {
        if (bleAppLevel == null) {
            bleAppLevel = new BLEAppLevel(mContext, myFragment, macAddress);
        }
        return bleAppLevel;
    }

    public static BLEAppLevel getInstanceOnly() {
        if (bleAppLevel != null) {
            return bleAppLevel;
        }
        return null;
    }

    /*public static BLEAppLevel getInstanceOnlyDDFragment(Fragment myFragment) {
        if (bleAppLevel != null) {
            myFragmentDD = myFragment;
            return bleAppLevel;
        }
        return null;
    }*/

    private BLEAppLevel(MainActivity mContext, Fragment myFragment, String macAddress) {
        this.mContext = mContext;
        this.macAddress = macAddress;
        this.myFragment = myFragment;
        initBLEDevice();
    }

    private void initBLEDevice() {
        Intent gattServiceIntent = new Intent(mContext, BleAdapterService.class);
        mContext.bindService(gattServiceIntent, service_connection, BIND_AUTO_CREATE);
        isServiceBound = true;
    }

    private final ServiceConnection service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(message_handler);
            if (bluetooth_le_adapter != null) {
                bluetooth_le_adapter.connect(macAddress);
            } else {
                //showMsg("onConnect: bluetooth_le_adapter=null");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    private Handler message_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            byte[] b = null;
            //message handling logic
            switch (msg.what) {
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                    //showMsg(text);
                    break;
                case BleAdapterService.GATT_CONNECTED:
                    //we're connected
                    showMsg("CONNECTED");
                    bluetooth_le_adapter.discoverServices();
                    break;
                case BleAdapterService.GATT_DISCONNECT:
                    //we're disconnected
                    isBLEConnected = false;
                    showMsg("Device disconnected");
                    mContext.MainActBLEgotDisconnected(macAddress);
                    disconnectBLECompletely();
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
                    //validate services and if ok...
                    List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                    boolean time_point_service_present = false;
                    boolean current_time_service_present = false;
                    boolean pots_service_present = false;
                    boolean battery_service_present = false;
                    boolean valve_controller_service_present = false;
                    boolean pebble_service_present = false;

                    for (BluetoothGattService svc : slist) {
                        Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + "INSTANCE=" + svc.getInstanceId());
                        String serviceUuid = svc.getUuid().toString().toUpperCase();
                       /* if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.TIME_POINT_SERVICE_SERVICE_UUID)) {
                            time_point_service_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.CURRENT_TIME_SERVICE_SERVICE_UUID)) {
                            current_time_service_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.POTS_SERVICE_SERVICE_UUID)) {
                            pots_service_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.BATTERY_SERVICE_SERVICE_UUID)) {
                            battery_service_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID)) {
                            valve_controller_service_present = true;
                            continue;
                        }*/
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.PEBBLE_SERVICE_UUID)) {
                            pebble_service_present = true;
                            continue;
                        }
                    }
                    //if (time_point_service_present && current_time_service_present && pots_service_present && battery_service_present) {
                    if(pebble_service_present){
                        showMsg("Device has expected services");
                        isBLEConnected = true;
                        //Recent connected device MAC save in SP
                        MySharedPreference msp = MySharedPreference.getInstance(mContext);
                        msp.setConnectedDvcMacAdd(macAddress);
                        //Calculating date, time and save in SP
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy, HH:mm:ss");
                        String formattedDate = df.format(c.getTime());
                        msp.setLastConnectedTime(formattedDate);

                        mContext.MainActBLEgotConnected();
                        //Setting current time to BLE
                        onSetTime();

                        //LogReadingIndex = MySharedPreference.getInstance(mContext).getLogIndex();      // Read last updated log read index for file

                        if (myFragment instanceof FragAvailableDevices) {
                            ((FragAvailableDevices) myFragment).dvcIsReadyNowNextScreen();
                        }
                    } else {
                        bluetooth_le_adapter.disconnect();
                        showMsg("Device does not have expected GATT services");
                        bleAppLevel = null;
                        ((FragAvailableDevices) myFragment).dvcIsStrangeStopEfforts();
                    }
                    break;
                case BleAdapterService.GATT_CHARACTERISTIC_READ:
                    bundle = msg.getData();
                    Log.d(Constants.TAG, "Service=" + bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase() + " Characteristic=" + bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase());
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase().equals(BleAdapterService.BATTERY_LEVEL_CHARACTERISTIC_UUID))
                    {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        if (b.length > 0)
                        {
                            long weight = ((16777216 * bluetooth_le_adapter.convertByteToInt(b[0])) + (65536 * bluetooth_le_adapter.convertByteToInt(b[1])) + (256 * bluetooth_le_adapter.convertByteToInt(b[2])) + bluetooth_le_adapter.convertByteToInt(b[3]));
                            int cal = bluetooth_le_adapter.convertByteToInt(b[4]);
                            deviceType = bluetooth_le_adapter.convertByteToInt(b[5]);
                            showMsg("Received " + b.toString() + "from Pebble.");

                            pref = mContext.getSharedPreferences("MyPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putLong("Weight_Data", weight);
                            editor.putInt("Calibration_Status", cal);
                            editor.putInt("Device_Code", deviceType);
                            editor.apply();

                            devicePrefs = mContext.getSharedPreferences("DevicePref", MODE_PRIVATE);
                            SharedPreferences.Editor dvcEditor = devicePrefs.edit();

                            if(deviceType == 3)        // Device: Tubby
                            {
                                dvcEditor.putInt("Valve_Number", 0);
                                dvcEditor.putString("Tubby_addr", macAddress);
                                dvcEditor.apply();
                            }
                            if(deviceType == 2)         // Device: MVC
                            {
                                dvcEditor.putString("MVC_addr", macAddress);
                                dvcEditor.apply();
                            }

                            if(isBLEConnected)
                                onReadLog();
                            //MyFragmentTransactions.replaceFragment(mContext, fragDevice, TagConstant.DEVICE_DETAILS, mContext.frm_lyt_container_int, true);
                        }
                    }

                    if(bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase().equals(BleAdapterService.LOG_CHARACTERISTIC_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        String str = "",eventData = "", time_point;
                        Map<String, Object> logEvent = new HashMap<>();
                        Map<String, Object> connectEvent = new HashMap<>();
                        String[] dayOfWeek ={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
                        if(b.length > 0) {
                            if (b[0]!= 0) {

                                int volume=0;
                                int duration=0;
                                long date = ((16777216 * bluetooth_le_adapter.convertByteToInt(b[1])) + (65536 * bluetooth_le_adapter.convertByteToInt(b[2])) + (256 * bluetooth_le_adapter.convertByteToInt(b[3])) + bluetooth_le_adapter.convertByteToInt(b[4]));          // receive date of log events from device
                                //showMsg("date = " + date);
                                //Date eventDate = new Date(date * 1000); //Arduino provides seconds since 1 Jan 1970. Android uses milliseconds since 1 Jan 1970. So, multiplying by 1000.
                                String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(date * 1000));

                                String logDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(date * 1000));

                                byte[] bytes = {b[5], b[6], b[7], b[8], b[9], b[10]};


                                StringBuilder sb = new StringBuilder();    // convert received MAC id of device into string
                                for (byte data : bytes) {
                                    sb.append(String.format("%02X", data));
                                }
                                //byte[] bytes = {b[5], b[6], b[7], b[8], b[9], b[10], b[11], b[12], b[13], b[14]};

                                    /*try {
                                        str = new String(bytes, "UTF-8");  // Best way to decode using "UTF-8"
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }*/
                                //}
                                dvcLoc = mContext.getDeviceLocation();          // get device location when connected

                                switch(b[0])                                    // convert different event codes into events to store in firestore
                                {
                                    case  1 :   str = "Device connected ";
                                                eventData = sb.toString();
                                                connectEvent.put("Event Id", Integer.toHexString(b[0]));
                                                connectEvent.put("Event", "Device connected");
                                                connectEvent.put("Event time", dateString);
                                                connectEvent.put("MAC", eventData);
                                                connectEvent.put("Device Location", dvcLoc);
                                                break;

                                    case  2 :   str = "Device disconnected ";
                                                eventData = sb.toString();
                                                connectEvent.put("Event Id", Integer.toHexString(b[0]));
                                                connectEvent.put("Event", "Device disconnected");
                                                connectEvent.put("Event time", dateString);
                                                connectEvent.put("MAC", eventData);
                                                connectEvent.put("Device Location", dvcLoc);
                                                break;

                                    case 17 :   if(deviceType == 2)
                                                {
                                                str = "Valve " + bluetooth_le_adapter.convertByteToInt(b[5]) + " open";
                                                duration = (256 * bluetooth_le_adapter.convertByteToInt(b[11])) + bluetooth_le_adapter.convertByteToInt(b[12]);
                                                volume = (256 * bluetooth_le_adapter.convertByteToInt(b[13])) + bluetooth_le_adapter.convertByteToInt(b[14]);
                                                eventData = "Volume = " + volume + " " + "Duration = " + duration;
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Valve open");
                                                logEvent.put("Event time", dateString);
                                                }
                                                else if(deviceType == 3)
                                                {
                                                    str = "water pump open";
                                                    duration = (256 * bluetooth_le_adapter.convertByteToInt(b[11])) + bluetooth_le_adapter.convertByteToInt(b[12]);
                                                    volume = (256 * bluetooth_le_adapter.convertByteToInt(b[13])) + bluetooth_le_adapter.convertByteToInt(b[14]);
                                                    eventData = "Volume = " + volume + " " + "Duration = " + duration;
                                                    logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                    logEvent.put("Event", "Water pump open");
                                                    logEvent.put("Event time", dateString);
                                                }
                                                 break;

                                    case 18 :   if(deviceType == 2)      // device = 2 for MVC
                                              {
                                                str = "Valve " +bluetooth_le_adapter.convertByteToInt(b[5])+ " close";
                                                volume = (256 * bluetooth_le_adapter.convertByteToInt(b[13])) + bluetooth_le_adapter.convertByteToInt(b[14]);
                                                eventData = "Counter value = "+volume;
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Valve close");
                                                logEvent.put("Volume(in ml)", Integer.toString(volume));
                                                logEvent.put("Event time", dateString);
                                              }
                                              else if(deviceType == 3)    // device = 3 for Tubby
                                              {
                                                  str = "Water pump closed";
                                                  volume = (256 * bluetooth_le_adapter.convertByteToInt(b[13])) + bluetooth_le_adapter.convertByteToInt(b[14]);
                                                  eventData = "Weight = "+volume;
                                                  logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                  logEvent.put("Event", "Water pump close");
                                                  logEvent.put("Volume(in ml)", Integer.toString(volume));
                                                  logEvent.put("Event time", dateString);
                                              }
                                                break;

                                    case 19 :   str = "Timepoint missed";
                                                time_point = (dayOfWeek[b[7]-1]+" "+bluetooth_le_adapter.convertByteToInt(b[8])+":"+bluetooth_le_adapter.convertByteToInt(b[9])+":"+bluetooth_le_adapter.convertByteToInt(b[10]));
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Time point missed");
                                                logEvent.put("Missed Time point", time_point);
                                                logEvent.put("Event time", dateString);


                                    case 20 :   str = "Old timepoints erased: ";
                                                eventData = "Valve "+bluetooth_le_adapter.convertByteToInt(b[5]) ;
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Old timepoints erased");
                                                logEvent.put("Event time", dateString);
                                                break;

                                    case 21 :   str = "New session time point loaded: ";
                                                duration = (256 * bluetooth_le_adapter.convertByteToInt(b[11])) + bluetooth_le_adapter.convertByteToInt(b[12]);
                                                volume = (256 * bluetooth_le_adapter.convertByteToInt(b[13])) + bluetooth_le_adapter.convertByteToInt(b[14]);
                                                eventData = (dayOfWeek[b[7]-1]+" "+bluetooth_le_adapter.convertByteToInt(b[8])+":"+bluetooth_le_adapter.convertByteToInt(b[9])+":"+bluetooth_le_adapter.convertByteToInt(b[10]) + " Volume="+volume+" Duration="+duration);
                                                time_point = (dayOfWeek[b[7]-1]+" "+bluetooth_le_adapter.convertByteToInt(b[8])+":"+bluetooth_le_adapter.convertByteToInt(b[9])+":"+bluetooth_le_adapter.convertByteToInt(b[10]));
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "New session time point loaded");
                                                logEvent.put(("Time point " + planId), time_point);
                                                logEvent.put("Volume(in ml)", Integer.toString(volume));
                                                logEvent.put("Duration", Integer.toString(duration));
                                                logEvent.put("Event time", dateString);
                                                planId++;
                                                break;

                                    case 81 :   str = "Flush open: Valve "+ bluetooth_le_adapter.convertByteToInt(b[5]);
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Flush open");
                                                logEvent.put("Event time", dateString);
                                                break;

                                    case 82 :   str = "Flush close: Valve "+ bluetooth_le_adapter.convertByteToInt(b[5]);
                                                volume = (256 * bluetooth_le_adapter.convertByteToInt(b[13])) + bluetooth_le_adapter.convertByteToInt(b[14]);
                                                eventData = "Counter value = "+volume;
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Flush close");
                                                logEvent.put("Volume(in ml)", Integer.toString(volume));
                                                logEvent.put("Event time", dateString);
                                                break;

                                    case 97 :   str =  "Pebble Start";
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Pebble Start");
                                                logEvent.put("Event time", dateString);
                                                break;

                                    case 98 :   str = "Pebble Stop";
                                                eventData = " Valve "+bluetooth_le_adapter.convertByteToInt(b[5])+" alarms erased";
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Pebble Stop");
                                                logEvent.put("Event time", dateString);
                                                break;

                                    case 99 :   str = "Pebble pause";
                                                logEvent.put("Event Id", Integer.toHexString(b[0]));
                                                logEvent.put("Event", "Pebble Stop");
                                                logEvent.put("Event time", dateString);
                                                break;

                                    case 113 :  str = "No. of pots";
                                                duration = (256 * bluetooth_le_adapter.convertByteToInt(b[11])) + bluetooth_le_adapter.convertByteToInt(b[12]);
                                                eventData = ": "+duration;
                                                break;
                                }
                                //showMsg("Event code=" + Integer.toHexString(b[0]) + " " + dateString + ", " + sb.toString() + ","+ volume + ","+ duration);
                                //showMsg(dateString + " " +"Event="+Integer.toHexString(b[0])+" "+str +" "+eventData);

                                //writeToFile("Event code="+Integer.toHexString(b[0])+" "+eventDate.toString());
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null)
                                {
                                    /*FirebaseDatabase database = FirebaseDatabase.getInstance();
                                    DatabaseReference myRef = database.getReference("User/"+user.getUid());
                                    myRef.child("Log data").child(dateString);
                                    myRef.child("Log data").child(dateString).child("Event Id: "+ Integer.toHexString(b[0]));
                                    myRef.child("Log data").child(dateString).child("Event Id: "+ Integer.toHexString(b[0])).child("Event").setValue(str);
                                    myRef.child("Log data").child(dateString).child("Event Id: "+ Integer.toHexString(b[0])).child("Data").setValue(eventData);*/
                                    /*Map<String, Object> taskMap = new HashMap<>();
                                    taskMap.put("Event Id", Integer.toHexString(b[0]));
                                    taskMap.put("Time", dateString);
                                    taskMap.put("Event occured", str);
                                    taskMap.put("Data", eventData);
                                    myRef.push().updateChildren(taskMap);*/

                                    // Access a Cloud Firestore instance from your Activity
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                                    // Create a new user with a first and last name
                                    deviceName = MySharedPreference.getInstance(mContext).getDvcNameFromDvcDetails();

                                    if(deviceName.isEmpty())
                                        deviceName = macAddress;

                                    if((b[0] == 1) || (b[0] == 2))
                                    {                                                  // store device connections logs in separate file
                                        // Add a new document with a generated ID
                                        db.collection(user.getEmail()).document("User devices").collection(deviceName).document("Connection logs").collection(logDate).document(dateString)
                                                .set(connectEvent, SetOptions.merge())
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // Write was successful!
                                                        Toast.makeText(mContext, "write successful.", Toast.LENGTH_SHORT).show();
                                                        onReadLogDynamic();
                                                    }

                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Write failed
                                                        Toast.makeText(mContext, "write failed!!", Toast.LENGTH_SHORT).show();
                                                        // ...
                                                    }
                                                });
                                    }
                                    else
                                        {
                                            db.collection(user.getEmail()).document("User devices").collection(deviceName).document("Device logs").collection("Valve " + bluetooth_le_adapter.convertByteToInt(b[5])).document("Event logs").collection(logDate).document(dateString)
                                                    .set(logEvent, SetOptions.merge())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            // Write was successful!
                                                            Toast.makeText(mContext, "write successful.", Toast.LENGTH_SHORT).show();
                                                            onReadLogDynamic();
                                                        }

                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            // Write failed
                                                            Toast.makeText(mContext, "write failed!!", Toast.LENGTH_SHORT).show();
                                                            // ...
                                                        }
                                                    });
                                        }
                                }
                            // ******************************** Create log file in phone to save esp32 log data*************************************
                                if (createLogFile()) {
                                    try {

                                        //logOutputHandler.write(("Event code=" + Integer.toHexString(b[0]) + ", " + dateString +" "+ sb.toString()+ ", "+ volume + ", "+ duration+ "\n").getBytes());
                                        logOutputHandler.write((dateString + " "+"Event="+Integer.toHexString(b[0])+" "+str +" "+eventData+ "\n").getBytes());
                                        alarmOutputHandler.write((Integer.toHexString(b[0]) + "," + dateString + "," + str + "," + eventData +"\n").getBytes());

                                        logOutputHandler.close();
                                        alarmOutputHandler.close();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                              //***************************************************************************************************************************
                            }


                        } else {
                            showMsg("No log data");
                        }
                    }
                    break;
                case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    bundle = msg.getData();
                    //ACK for command button valve
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase().equals(BleAdapterService.COMMAND_CHARACTERISTIC_UUID))
                    {
                        Log.e("@@@@@@@@@@@@", "ACK for command valve");

                        if (myFragment instanceof FragDeviceMAP) {
                            if (cmdTypeName.equals("PAUSE")) {
                                pauseIndex++;
                                if (pauseIndex <= totalPlayValvesCount) {
                                    cmdDvcPause(null, "", 0);
                                } else {
                                    pauseIndex = 1;
                                    ((FragDeviceMAP) myFragment).dvcLongPressBLEDone(cmdTypeName);
                                }
                            }
                        }

                        if (myFragment instanceof FragDeviceMAP) {
                            if (cmdTypeName.equals("PLAY")) {
                                playIndex++;
                                if (playIndex <= totalPauseValvesCount) {
                                    cmdDvcPlay(null, "", 0);
                                } else {
                                    playIndex = 1;
                                    ((FragDeviceMAP) myFragment).dvcLongPressBLEDone(cmdTypeName);
                                }
                            }
                        }

                        if (myFragment instanceof FragDeviceMAP) {
                            if (cmdTypeName.equals("STOP")) {
                                stopIndex++;
                                if (stopIndex <= totalPlayPauseValvesCount) {
                                    cmdDvcStop(null, "", 0);
                                } else {
                                    stopIndex = 1;
                                    ((FragDeviceMAP) myFragment).dvcLongPressBLEDone(cmdTypeName);
                                }
                            }
                        }


                        if (myFragment instanceof FragDeviceDetails) {
                            if (cmdTypeName.equals("STOP")) {
                                ((FragDeviceDetails) myFragment).cmdButtonACK("STOP");
                            } else if (cmdTypeName.equals("PAUSE")) {
                                ((FragDeviceDetails) myFragment).cmdButtonACK("PAUSE");
                            } else if (cmdTypeName.equals("PLAY")) {
                                ((FragDeviceDetails) myFragment).cmdButtonACK("PLAY");
                            } else if (cmdTypeName.equals("FLUSH ON")) {
                                ((FragDeviceDetails) myFragment).cmdButtonACK("FLUSH ON");
                            } else if (cmdTypeName.equals("FLUSH OFF")) {
                                ((FragDeviceDetails) myFragment).cmdButtonACK("FLUSH OFF");
                            }

                        }

                    }
                    //ACK for writing Time Points
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase().equals(BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID)) {
                        Log.e("@@@ACK RECEIVED FOR ", "" + dataSendingIndex);
                        if (oldTimePointsErased == FALSE) {
                            oldTimePointsErased = TRUE;
                            if (dataSendingIndex < listSingleValveData.size()) {
                                startSendData();
                            } else {

                                dataSendingIndex = 0;
                            }
                        } else {
                            dataSendingIndex++;
                            if (dataSendingIndex < listSingleValveData.size()) {
                                startSendData();
                            } else {
                                if (myFragment instanceof FragAddEditSesnPlan) {
                                    ((FragAddEditSesnPlan) myFragment).doneWrtingAllTP();
                                    dataSendingIndex = 0;
                                    oldTimePointsErased = FALSE;
                                }
                            }
                        }

                    }

                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.ALERT_LEVEL_CHARACTERISTIC)
                            && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase().equals(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {
                            setAlertLevel((int) b[0]);
                            showMsg("Received " + b.toString() + "from Pebble.");
                        }
                    }

                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase().equals(BleAdapterService.CURRENT_TIME_CHARACTERISTIC_UUID))
                    {
                        if(isBLEConnected)
                            onReadWeight();
                    }

                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.LOG_CHARACTERISTIC_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        if (b.length > 0)
                        {
                            showMsg("Read characteristic : " + LogReadingIndex);
                            if(bluetooth_le_adapter.readCharacteristic(BleAdapterService.PEBBLE_SERVICE_UUID, BleAdapterService.LOG_CHARACTERISTIC_UUID) == TRUE)
                            {
                                if(LogReadingIndex == 0)
                                   showMsg("Log Event Read");
                                LogReadingIndex++;

                            } else {
                                if(LogReadingIndex == 0)
                                   showMsg("Log Event Read Failed");
                            }
                        }
                    }
                    break;
            }
        }
    };

    public void cmdDvcPause(FragDeviceMAP fragDeviceMAP, String cmdTypeName, int totalPlayValvesCount) {
        if (fragDeviceMAP != null) {
            myFragment = fragDeviceMAP;
            this.cmdTypeName = cmdTypeName;
            this.totalPlayValvesCount = totalPlayValvesCount;
        }
        byte cmd = (byte) ((16 * pauseIndex)+4);
        if(deviceType == 3)                     // Tubby: single fixed pause command for single valve
            cmd = 4;
        // Command for PAUSE
        byte[] valveCommand = {cmd};
        /*bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
        );
    }

    public void cmdDvcPlay(FragDeviceMAP fragDeviceMAP, String cmdTypeName, int totalPauseValvesCount) {
        if (fragDeviceMAP != null) {
            myFragment = fragDeviceMAP;
            this.cmdTypeName = cmdTypeName;
            this.totalPauseValvesCount = totalPauseValvesCount;
        }
        // Command for PLAY
        byte cmd = (byte) ((16 * playIndex)+2);
        if(deviceType == 3)                     // Tubby: single fixed Play command for single valve
            cmd = 2;
        byte[] valveCommand = {cmd};
        /*bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
        );
    }

    public void cmdDvcStop(FragDeviceMAP fragDeviceMAP, String cmdTypeName, int totalPlayPauseValvesCount) {
        if (fragDeviceMAP != null) {
            myFragment = fragDeviceMAP;
            this.cmdTypeName = cmdTypeName;
            this.totalPlayPauseValvesCount = totalPlayPauseValvesCount;
        }
        // Command for STOP
        byte cmd = (byte) ((16 * stopIndex)+3);
        if(deviceType == 3)                     // Tubby: single fixed Stop command for single valve
            cmd = 3;
        byte[] valveCommand = {cmd};
        /*bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
        );
    }

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                //((TextView) findViewById(R.id.msgTextView)).setText(msg);
            }
        });
    }

    public boolean getBLEConnectedOrNot() {
        return isBLEConnected;
    }

    public String getDvcAddress()       //send BLE connected device mac address
    {
      if(getBLEConnectedOrNot())
        return macAddress;
      else
          return null;
    }

    /* public void onSetTime() {
/*<<<<<<< HEAD
        //Getting +5:30 time zone
        int plusFiveThirtyZone = (5 * 60 * 60 * 1000) + (30 * 60 * 1000);

        *//*String[] ids = TimeZone.getAvailableIDs(+5 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(+5 * 60 * 60 * 1000, ids[0]);*//*

        String[] ids = TimeZone.getAvailableIDs(+plusFiveThirtyZone);
        SimpleTimeZone pdt = new SimpleTimeZone(+plusFiveThirtyZone, ids[0]);

        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
=======*/
       /* Calendar calendar = Calendar.getInstance();
        //Set present time as data packet
        byte hours = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        byte minutes = (byte) calendar.get(Calendar.MINUTE);
        byte seconds = (byte) calendar.get(Calendar.SECOND);
        byte DATE = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        byte MONTH = (byte) (calendar.get(Calendar.MONTH) + 1);
        int iYEARMSB = (calendar.get(Calendar.YEAR) / 256);
        int iYEARLSB = (calendar.get(Calendar.YEAR) % 256);
        byte bYEARMSB = (byte) iYEARMSB;
        byte bYEARLSB = (byte) iYEARLSB;
        byte[] currentTime = {hours, minutes, seconds, DATE, MONTH, bYEARMSB, bYEARLSB};

        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.CURRENT_TIME_CHARACTERISTIC_UUID, currentTime
        );
    }*/

   private void onReadWeight()
   {
       if(bluetooth_le_adapter.readCharacteristic(
               //BleAdapterService.BATTERY_SERVICE_SERVICE_UUID,
               BleAdapterService.PEBBLE_SERVICE_UUID,
               BleAdapterService.BATTERY_LEVEL_CHARACTERISTIC_UUID
       ) == TRUE) {
           showMsg("weight read");


       } else {
           showMsg("weight read failed");
       }
   }

    private void onReadLog()
    {
        int iLogReadingIndexMSB = LogReadingIndex / 256;
        int iLogReadingIndexLSB = LogReadingIndex % 256;
        byte readingIndex0 = (byte) iLogReadingIndexMSB;
        byte readingIndex1 = (byte) iLogReadingIndexLSB;
        byte[] readingIndex = {readingIndex0, readingIndex1};
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.LOG_CHARACTERISTIC_UUID, readingIndex
        );
    }

    private void onReadLogDynamic()
    {
        int iLogReadingIndexMSB = LogReadingIndex / 256;
        int iLogReadingIndexLSB = LogReadingIndex % 256;
        byte readingIndex0 = (byte) iLogReadingIndexMSB;
        byte readingIndex1 = (byte) iLogReadingIndexLSB;
        byte[] readingIndex = {readingIndex0, readingIndex1};
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.LOG_CHARACTERISTIC_UUID, readingIndex
        );
    }

    public void onSetTime() {
        Calendar calendar = Calendar.getInstance();

        //Set present time as data packet
        long systemTimeMillis = System.currentTimeMillis();
        int systemTime = (int) (systemTimeMillis / 1000);

        /*byte hours = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        byte minutes = (byte) calendar.get(Calendar.MINUTE);
        byte seconds = (byte) calendar.get(Calendar.SECOND);
        byte DATE = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        byte MONTH = (byte) ((calendar.get(Calendar.MONTH)) + 1);
        int iYEARMSB = (calendar.get(Calendar.YEAR) / 256);
        int iYEARLSB = (calendar.get(Calendar.YEAR) % 256);
        byte bYEARMSB = (byte) iYEARMSB;
        byte bYEARLSB = (byte) iYEARLSB;*/

        byte currentTimeH = (byte) ((systemTime & 0xFF000000)>>24);
        byte currentTimeMH = (byte) ((systemTime & 0x00FF0000)>>16);
        byte currentTimeML = (byte) ((systemTime & 0x0000FF00)>>8);
        byte currentTimeL = (byte) (systemTime & 0x000000FF);

        //Set 1,2,3,4,5,6,7 as data packet
        /*byte hours = (byte) 1;
        byte minutes = (byte) 2;
        byte seconds = (byte) 3;
        byte DATE = (byte) 4;
        byte MONTH = (byte) 5;
        //int iYEARMSB = (calendar.get(Calendar.YEAR) / 256);
        //int iYEARLSB = (calendar.get(Calendar.YEAR) % 256);
        //byte bYEARMSB = (byte) iYEARMSB;
        //byte bYEARLSB = (byte) iYEARLSB;
        byte bYEARMSB = (byte) 6;
        byte bYEARLSB = (byte) 7;*/

        //byte[] currentTime = {hours, minutes, seconds, DATE, MONTH, bYEARMSB, bYEARLSB};
        byte[] currentTime = {currentTimeH, currentTimeMH, currentTimeML, currentTimeL};

        /*bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.CURRENT_TIME_SERVICE_SERVICE_UUID,
                BleAdapterService.CURRENT_TIME_CHARACTERISTIC_UUID, currentTime
        );*/
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.CURRENT_TIME_CHARACTERISTIC_UUID, currentTime
        );
    }

    private void setAlertLevel(int alert_level) {
        this.alert_level = alert_level;
        switch (alert_level) {
            case 0:
                Toast.makeText(mContext, "Alert level " + alert_level, Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(mContext, "Alert level " + alert_level, Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(mContext, "Alert level " + alert_level, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void eraseOldTimePoints(FragAddEditSesnPlan fragAddEditSesnPlan, int etDisPntsInt, int etDurationInt, int etWaterQuantWithDPInt, ArrayList<DataTransferModel> listSingleValveData, int valveCount, String dvc_name) {
        myFragment = fragAddEditSesnPlan;

        this.etDisPntsInt = etDisPntsInt;
        this.etDurationInt = etDurationInt;
        this.etWaterQuantWithDPInt = etWaterQuantWithDPInt;
        this.listSingleValveData = listSingleValveData;
        this.valve_number = valveCount;
        this.deviceName = dvc_name;

        byte[] timePoint = {0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) valveCount};
        /*bluetooth_le_adapter.writeCharacteristic(BleAdapterService.TIME_POINT_SERVICE_SERVICE_UUID,
                BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID, timePoint);*/
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID, timePoint);
    }

    public void disconnectBLECompletely() {
        if (bluetooth_le_adapter != null)
        {
            try {
                if (bleAppLevel != null) {
                    bleAppLevel = null;
                    if (isServiceBound) {
                        mContext.unbindService(service_connection);
                        isServiceBound = false;
                    }
                    if (getBLEConnectedOrNot()) {
                        bluetooth_le_adapter.disconnect();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cmdButtonMethod(Fragment cmdOriginFragment, String cmdTypeName, int valve) {
        myFragment = cmdOriginFragment;
        this.cmdTypeName = cmdTypeName;

        if (cmdTypeName.equals("PLAY"))
        {
            byte[] valveCommand = {(byte) ((16 * valve)+2)};       //valve commands for different valve number
            if (bluetooth_le_adapter != null) {
                /*bluetooth_le_adapter.writeCharacteristic(
                        BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                        BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
                bluetooth_le_adapter.writeCharacteristic(
                        BleAdapterService.PEBBLE_SERVICE_UUID,
                        BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
                );
            }
        } else if (cmdTypeName.equals("STOP")) {
            byte[] valveCommand = {(byte) ((16 * valve)+3)};
            if (bluetooth_le_adapter != null) {
                /*bluetooth_le_adapter.writeCharacteristic(
                        BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                        BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
                bluetooth_le_adapter.writeCharacteristic(
                        BleAdapterService.PEBBLE_SERVICE_UUID,
                        BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
                );
            }
        } else if (cmdTypeName.equals("PAUSE")) {
            byte[] valveCommand = {(byte) ((16 * valve)+4)};
            /*bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                    BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
            bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.PEBBLE_SERVICE_UUID,
                    BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
            );
        } else if (cmdTypeName.equals("FLUSH ON")) {
            byte[] valveCommand = {(byte) ((16 * valve)+1)};
            /*bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                    BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
            bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.PEBBLE_SERVICE_UUID,
                    BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
            );
        } else if (cmdTypeName.equals("FLUSH OFF")) {
            byte[] valveCommand = {(byte) ((16 * valve)+5)};
            /*bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.VALVE_CONTROLLER_SERVICE_UUID,
                    BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand*/
            bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.PEBBLE_SERVICE_UUID,
                    BleAdapterService.COMMAND_CHARACTERISTIC_UUID, valveCommand
            );
        }
        saveValveEvent(cmdTypeName, valve);
    }

    void startSendData() {
/*<<<<<<< HEAD
        //Log.e("@@@ INDEX", "" + dataSendingIndex);
=======*/
        Calendar calendar = Calendar.getInstance();

        //Log.e("@@@@@@@@@@@", "" + dataSendingIndex);
        //byte index = (byte) (listSingleValveData.get(dataSendingIndex).getIndex() + 1);
        byte index = (byte) (dataSendingIndex + 1);
        byte hours = (byte) listSingleValveData.get(dataSendingIndex).getHourOfDay();
        byte minutes = 0;
        byte seconds = 1;
        byte dayOfTheWeek = (byte) listSingleValveData.get(dataSendingIndex).getDayOfWeek();
        byte valveNumber = (byte) valve_number;  // extra byte for valve number of which time points sending

/*<<<<<<< HEAD
        int iDurationMSB = (etDurationInt / 256);
        int iDurationLSB = (etDurationInt % 256);
        byte bDurationMSB = (byte) iDurationMSB;
        byte bDurationLSB = (byte) iDurationLSB;

        int iVolumeMSB = (etWaterQuantWithDPInt / 256);
        int iVolumeLSB = (etWaterQuantWithDPInt % 256);
=======*/
        int iDurationMSB = (etDurationInt / 256);
        int iDurationLSB = (etDurationInt % 256);
        byte bDurationMSB = (byte) iDurationMSB;
        byte bDurationLSB = (byte) iDurationLSB;

        int iVolumeMSB = (etWaterQuantWithDPInt / 256);
        int iVolumeLSB = (etWaterQuantWithDPInt % 256);
        byte bVolumeMSB = (byte) iVolumeMSB;
        byte bVolumeLSB = (byte) iVolumeLSB;

        //Log.e("@@@ ADD/EDIT VOLUME ", "INPUT: " + etWaterQuantWithDPInt + "\n Int /256: " + iVolumeMSB + "\n Int %256: " + iVolumeLSB + "\n bVolumeMSB: " + bVolumeMSB + "\n bVolumeLSB: " + bVolumeLSB);

        listSingleValveData.get(dataSendingIndex).setIndex(index);
        listSingleValveData.get(dataSendingIndex).setbDurationLSB(bDurationLSB);
        listSingleValveData.get(dataSendingIndex).setbDurationMSB(bDurationMSB);
        listSingleValveData.get(dataSendingIndex).setbVolumeLSB(bVolumeLSB);
        listSingleValveData.get(dataSendingIndex).setbVolumeMSB(bVolumeMSB);
        listSingleValveData.get(dataSendingIndex).setMinutes(0);
        listSingleValveData.get(dataSendingIndex).setSeconds(0);
        listSingleValveData.get(dataSendingIndex).setQty(etWaterQuantWithDPInt);
        listSingleValveData.get(dataSendingIndex).setDuration(etDurationInt);
        listSingleValveData.get(dataSendingIndex).setDischarge(etDisPntsInt);

        Log.e("@@@ ADD/EDIT", "INDEX: " + index + "\n DOW: " + dayOfTheWeek + "\n HRS: " + hours + "\n MIN: " + 0 + "\n SEC: " + 0 + "\n DMSB: " + bDurationMSB + "\n DLSB: " + bDurationLSB + "\n VMSB: " + bVolumeMSB + "\n VLSB: " + bVolumeLSB);

        //Log.e("@@", "" + index + "-" + dayOfTheWeek + "-" + hours + "-" + 0 + "-" + 0 + "-" + bDurationMSB + "-" + bDurationLSB + "-" + bVolumeMSB + "-" + bVolumeLSB);
        byte[] timePoint = {index, dayOfTheWeek, hours, 0, seconds, bDurationMSB, bDurationLSB, bVolumeMSB, bVolumeLSB, valveNumber};
        /*bluetooth_le_adapter.writeCharacteristic(BleAdapterService.TIME_POINT_SERVICE_SERVICE_UUID,
                BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID, timePoint);*/
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID, timePoint);

        saveValvePlan();

    }

    //******************************Create Log data text logFile for received data from device i.e Tubby or MVC**********************************
    boolean createLogFile()
    {
        //Toast.makeText(getBaseContext(), "inside create", Toast.LENGTH_SHORT).show();
        if(!(isExternalStorageWritable()&&isStoragePermissionGranted()))
            return false;	// Check if the external storage is available for write
        else {
            String dirPath = Environment.getExternalStorageDirectory().getPath()+"/Logger/";
            //String dirPath = getFilesDir().getAbsolutePath()+"/Logger/";
            File directory = new File(dirPath);
            logFile = new File(dirPath+"/LogData.txt");
            alarmsFile = new File(dirPath+"/AlarmsPlan.csv");

            if(!(logFile.exists()))
            {
                try {
                    if (!(directory.exists() && directory.isDirectory())) {    //If the Directory does not exist: create a new one
                        if (directory.mkdir()) {
                            System.out.println("Directory created");
                        } else {
                            System.out.println("Directory is not created");
                        }
                    }
                    logOutputHandler = new FileOutputStream(logFile);    //// Create File for log data
                    logOutputHandler.write(("\n").getBytes());
                    if(!(alarmsFile.exists()))
                    {
                        alarmOutputHandler = new FileOutputStream(alarmsFile);    //// Create File for alarms plan loaded
                        //logOutputHandler.write(("\n").getBytes());
                        alarmOutputHandler.write(("Event Id " + "," + "Time " + "," + "Event occured " + "," + "Data" + "\n").getBytes());
                    }
                    else
                        alarmOutputHandler = new FileOutputStream(alarmsFile, true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                try{
                    logOutputHandler = new FileOutputStream(logFile, true);
                    // logOutputHandler.write(("\n\n").getBytes());
                    if(alarmsFile.exists())
                        alarmOutputHandler = new FileOutputStream(alarmsFile, true);
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }

        }
        return true;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        //Toast.makeText(getBaseContext(),state, Toast.LENGTH_SHORT).show();
        //showMsg(state);
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23)
        {
            if (mContext.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("LOG PERMISSION","Permission is granted");
                return true;
            } else {

                Log.v("LOG PERMISSION","Permission is revoked");
                ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("LOG PERMISSION","Permission is granted");
            return true;
        }
    }

    public void saveValvePlan()                     // Function to save device timepoint plans as per valve number and data in firestore database
    {
        String[] dayOfWeek ={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        String tp_day = dayOfWeek[listSingleValveData.get(dataSendingIndex).getDayOfWeek()-1];
        String tp_hour = Integer.toString(listSingleValveData.get(dataSendingIndex).getHourOfDay()) + ":00";
        String tp_vol = Integer.toString(etWaterQuantWithDPInt);
        String tp_duration = Integer.toString(etDurationInt);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Calendar c = Calendar.getInstance();
            System.out.println("Current time => "+c.getTime());

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = df.format(c.getTime());
            // Create a new user with a first and last name
            Map<String, Object> plan_data = new HashMap<>();
            plan_data.put("Day", tp_day);
            plan_data.put("Time", tp_hour);
            plan_data.put("Volume", tp_vol);
            plan_data.put("Duration", tp_duration);

            // Add a new document with a generated ID
            db.collection(user.getEmail()).document("User devices").collection(deviceName).document("Valve "+ valve_number).collection("Session plans on "+ formattedDate).document(Integer.toString(dataSendingIndex + 1))
                    .set(plan_data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Write was successful!
                            Toast.makeText(mContext, "write successful.", Toast.LENGTH_SHORT).show();
                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Write failed
                            Toast.makeText(mContext, "write failed!!", Toast.LENGTH_SHORT).show();
                            // ...
                        }
                    });
        }
    }

    private void saveValveEvent(String event, int valNum)                 // Function to save device events as per valve number and data in firestore database
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Calendar c = Calendar.getInstance();
            System.out.println("Current time => "+c.getTime());

            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = date.format(c.getTime());

            SimpleDateFormat date_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedTime = date_time.format(c.getTime());

            deviceName = MySharedPreference.getInstance(mContext).getDvcNameFromDvcDetails();

            // Create a new user with a first and last name
            Map<String, Object> cmd_data = new HashMap<>();
            cmd_data.put("Event", event);
            cmd_data.put("Time", formattedTime);

            if(deviceType == 3)
                valNum = 1;
            // Add a new document with a generated ID
            db.collection(user.getEmail()).document("User devices").collection(deviceName).document("Valve "+ valNum).collection("Valve events").document(formattedTime).set(cmd_data, SetOptions.merge());
        }

    }


}
