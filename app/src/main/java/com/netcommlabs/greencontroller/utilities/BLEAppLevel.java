package com.netcommlabs.greencontroller.utilities;

import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
                            editor.commit();

                            devicePrefs = mContext.getSharedPreferences("DevicePref", MODE_PRIVATE);
                            SharedPreferences.Editor dvcEditor = devicePrefs.edit();

                            if(deviceType == 3)        // Device: Tubby
                            {
                                dvcEditor.putInt("Valve_Number", 0);
                                dvcEditor.putString("Tubby_addr", macAddress);
                                dvcEditor.commit();
                            }
                            if(deviceType == 2)         // Device: MVC
                            {
                                dvcEditor.putString("MVC_addr", macAddress);
                                dvcEditor.commit();
                            }
                            //MyFragmentTransactions.replaceFragment(mContext, fragDevice, TagConstant.DEVICE_DETAILS, mContext.frm_lyt_container_int, true);
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

   public void onReadWeight()
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

    public void eraseOldTimePoints(FragAddEditSesnPlan fragAddEditSesnPlan, int etDisPntsInt, int etDurationInt, int etWaterQuantWithDPInt, ArrayList<DataTransferModel> listSingleValveData, int valveCount) {
        myFragment = fragAddEditSesnPlan;

        this.etDisPntsInt = etDisPntsInt;
        this.etDurationInt = etDurationInt;
        this.etWaterQuantWithDPInt = etWaterQuantWithDPInt;
        this.listSingleValveData = listSingleValveData;
        this.valve_number = valveCount;

        byte[] timePoint = {0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) valveCount};
        /*bluetooth_le_adapter.writeCharacteristic(BleAdapterService.TIME_POINT_SERVICE_SERVICE_UUID,
                BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID, timePoint);*/
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.PEBBLE_SERVICE_UUID,
                BleAdapterService.NEW_WATERING_TIME_POINT_CHARACTERISTIC_UUID, timePoint);
    }

    public void disconnectBLECompletely() {
        if (bluetooth_le_adapter != null) {
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

    }


}
