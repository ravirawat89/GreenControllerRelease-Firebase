package com.netcommlabs.greencontroller.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.netcommlabs.greencontroller.Dialogs.ErroScreenDialog;
import com.netcommlabs.greencontroller.Interfaces.APIResponseListener;
import com.netcommlabs.greencontroller.R;
import com.netcommlabs.greencontroller.activities.LoginAct;
import com.netcommlabs.greencontroller.activities.MainActivity;
import com.netcommlabs.greencontroller.constant.TagConstant;
import com.netcommlabs.greencontroller.constant.UrlConstants;
import com.netcommlabs.greencontroller.model.ModalAddressModule;
import com.netcommlabs.greencontroller.model.PreferenceModel;
import com.netcommlabs.greencontroller.services.ProjectWebRequest;
import com.netcommlabs.greencontroller.sqlite_db.DatabaseHandler;
import com.netcommlabs.greencontroller.utilities.MySharedPreference;
import com.netcommlabs.greencontroller.utilities.NetworkUtils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Android on 12/6/2017.
 */

public class FragConnectedQR extends Fragment implements APIResponseListener {

    private static final int REQUEST_CODE_FOR_ADDRESS_BOOK = 1001;
    private static final int REQUEST_ADDADDRESS_QRCONNECT = 101;
    private MainActivity mContext;
    private View view;
    private LinearLayout llAddDeviceAddressConctd;
    public TextView tvScanQREvent, tvNextConctdEvent, tvDvcName, tvTitleConctnt;
    private DatabaseHandler databaseHandler;
    public ImageView ivEditDvcName, ivSaveDvcName;
    public EditText etEditDvcName, etQRManually;
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";
    private String device_name;
    private String dvc_mac_address;
    int valveNum;
    private ModalAddressModule modalAddressModule;
    private static String dvcNameEdited = "", qrCodeEdited = "";
    ProjectWebRequest request;
    private PreferenceModel preference;
    private LinearLayout address_selection_layout;
    private List<ModalAddressModule> listMdalAddressModules;
    private String selectedExistingAddressID = "";
    private String radioName;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.conted_qr_act, null);

        initViews(view);
        initBase();
        initListeners();

        return view;
    }

    private void initViews(View view) {
        preference = MySharedPreference.getInstance(mContext).getsharedPreferenceData();
        tvDvcName = view.findViewById(R.id.tvDvcName);
        ivEditDvcName = view.findViewById(R.id.ivEditDvcName);
        ivSaveDvcName = view.findViewById(R.id.ivSaveDvcName);
        etEditDvcName = view.findViewById(R.id.etEditDvcName);
        llAddDeviceAddressConctd = view.findViewById(R.id.llAddDeviceAddressConctd);
        etQRManually = view.findViewById(R.id.etQRManually);
        tvScanQREvent = view.findViewById(R.id.tvScanQREvent);
        tvNextConctdEvent = view.findViewById(R.id.tvNextConctdEvent);

    }

    private void initBase() {
        Bundle bundle = this.getArguments();
        device_name = bundle.getString(EXTRA_NAME);
        dvc_mac_address = bundle.getString(EXTRA_ID);
        tvTitleConctnt = mContext.tvToolbar_title;
        tvTitleConctnt.setText(device_name + " Connected");
        if (!dvcNameEdited.isEmpty()) {
            tvDvcName.setText(dvcNameEdited);
        }
        if (!qrCodeEdited.isEmpty()) {
            etQRManually.setText(qrCodeEdited);
        }
        etEditDvcName.setText(device_name);
        databaseHandler = DatabaseHandler.getInstance(mContext);
    }

    private void initListeners() {
        ivEditDvcName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dvcNameStrng = tvDvcName.getText().toString();
                tvDvcName.setVisibility(View.GONE);
                ivEditDvcName.setVisibility(View.GONE);
                etEditDvcName.setVisibility(View.VISIBLE);
                ivSaveDvcName.setVisibility(View.VISIBLE);
                etEditDvcName.setText(dvcNameStrng);
                etEditDvcName.setSelection(dvcNameStrng.length());
            }
        });

        ivSaveDvcName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dvcNameEdited = etEditDvcName.getText().toString();
                if (device_name.equals(dvcNameEdited)) {
                    Toast.makeText(mContext, "Please update device default name", Toast.LENGTH_SHORT).show();
                    etEditDvcName.requestFocus();
                    etEditDvcName.setSelection(dvcNameEdited.length());
                    return;
                }
                if (dvcNameEdited.isEmpty()) {
                    Toast.makeText(mContext, "Device name can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                //No device name should be duplicate
                if (databaseHandler.getAllDeviceName().size() > 0) {
                    for (int i = 0; i < databaseHandler.getAllDeviceName().size(); i++) {
                        if (databaseHandler.getAllDeviceName().get(i).equalsIgnoreCase(dvcNameEdited)) {
                            Toast.makeText(mContext, "This device name already exists with app", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                tvDvcName.setVisibility(View.VISIBLE);
                ivEditDvcName.setVisibility(View.VISIBLE);
                etEditDvcName.setVisibility(View.GONE);
                ivSaveDvcName.setVisibility(View.GONE);
                tvDvcName.setText(dvcNameEdited);
                Toast.makeText(mContext, "Device name edited", Toast.LENGTH_SHORT).show();
            }
        });

        llAddDeviceAddressConctd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etEditDvcName.getVisibility() == View.VISIBLE) {
                    Toast.makeText(mContext, "Please save device name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!NetworkUtils.isConnected(mContext)) {
                    Toast.makeText(mContext, "Please check your Network Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                listMdalAddressModules = databaseHandler.getAddressWithLocation("");
                if (listMdalAddressModules.size() > 0) {
                    FragAddressBook fragAddressBook = new FragAddressBook();
//                    if (modalAddressModule != null) {
//                        Bundle bundle = new Bundle();
//                        bundle.putString(FragAddressBook.KEY_ADDRESS_TRANSFER, "userSelectedAddress");
//                        fragAddressBook.setArguments(bundle);
//                    }
                    //First child---then parent
                    fragAddressBook.setTargetFragment(FragConnectedQR.this, REQUEST_CODE_FOR_ADDRESS_BOOK);
                    //Adding Fragment(FragAddressBook)
                    MyFragmentTransactions.replaceFragment(mContext, fragAddressBook, TagConstant.ADDRESS_BOOK, mContext.frm_lyt_container_int, true);

                } else {
                    FragAddEditAddress fragAddEditAddress = new FragAddEditAddress();
                    if (modalAddressModule != null) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(FragAddEditAddress.KEY_ADDRESS_TRANSFER, modalAddressModule);
                        fragAddEditAddress.setArguments(bundle);
                    }
                    //First child---then parent
                    fragAddEditAddress.setTargetFragment(FragConnectedQR.this, REQUEST_ADDADDRESS_QRCONNECT);
                    //Adding Fragment(FragAddEditAddress)
                    MyFragmentTransactions.replaceFragment(mContext, fragAddEditAddress, TagConstant.ADD_ADDRESS, mContext.frm_lyt_container_int, true);
                }
            }
        });

        tvScanQREvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator.forSupportFragment(FragConnectedQR.this).initiateScan();
            }
        });

        etQRManually.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                etQRManually.setCursorVisible(true);
                etQRManually.setFocusableInTouchMode(true);
                return false;
            }
        });

        tvNextConctdEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dvcNameEdited = etEditDvcName.getText().toString();
                if (device_name.equals(dvcNameEdited)) {
                    Toast.makeText(mContext, "Please update device default name", Toast.LENGTH_SHORT).show();
                    etEditDvcName.requestFocus();
                    etEditDvcName.setSelection(dvcNameEdited.length());
                    return;
                }
                if (dvcNameEdited.isEmpty()) {
                    Toast.makeText(mContext, "Device name can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (etEditDvcName.getVisibility() == View.VISIBLE) {
                    Toast.makeText(mContext, "Please save device name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (modalAddressModule == null && selectedExistingAddressID.isEmpty()) {
                    Toast.makeText(mContext, "Please provide Device Installation Address", Toast.LENGTH_SHORT).show();
                    return;
                }
                qrCodeEdited = etQRManually.getText().toString();
                if (qrCodeEdited.isEmpty()) {
                    Toast.makeText(mContext, "Please provide QR information", Toast.LENGTH_SHORT).show();
                    etQRManually.setCursorVisible(true);
                    etQRManually.requestFocus();
                    return;
                } else if (!qrCodeEdited.isEmpty() && qrCodeEdited.equalsIgnoreCase("QR8")) {
                    valveNum = 8;
                } else {
                    Toast.makeText(mContext, "Please enter a valid input", Toast.LENGTH_SHORT).show();
                    etQRManually.setCursorVisible(true);
                    etQRManually.requestFocus();
                    return;
                }
                //Avoid adding same device again with app
                if (databaseHandler.getAllDeviceMAC().size() > 0) {
                    for (int i = 0; i < databaseHandler.getAllDeviceMAC().size(); i++) {
                        if (databaseHandler.getAllDeviceMAC().get(i).equalsIgnoreCase(dvc_mac_address)) {
                            Toast.makeText(mContext, "This device already exists with app", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                if (!selectedExistingAddressID.isEmpty()) {
                    long insertedRowID = databaseHandler.insertDeviceModule(selectedExistingAddressID, dvcNameEdited, dvc_mac_address, qrCodeEdited, valveNum);
                    if (insertedRowID != -1) {
                        databaseHandler.insertDeviceModuleLog(databaseHandler.insertedDvcUUID);

                        saveDeviceDetails(dvcNameEdited);           //Save device details to firestore
                        //Replacing current Fragment by (FragDeviceMAP)
                        MyFragmentTransactions.replaceFragment(mContext, new FragDeviceMAP(), TagConstant.DEVICE_MAP, mContext.frm_lyt_container_int, false);
                        dvcNameEdited = "";
                        qrCodeEdited = "";
                        Toast.makeText(mContext, "Device and Address now registered with app", Toast.LENGTH_LONG).show();

                       // hitApiForSaveAddress();  //save address and device details to firestore
                    }
                } else {
                   hitApiForSaveAddress();

                }

                /*long insertedAddressUniqueID = databaseHandler.insertAddressModule(modalAddressModule);
                databaseHandler.insertDeviceModule(insertedAddressUniqueID, dvcNameEdited, dvc_mac_address, qrCodeEdited, valveNum);
                //ModalDeviceModule modalBleDevice = new ModalDeviceModule(dvcNameEdited, dvc_mac_address, modalAddressModule, valveNum);
                //databaseHandler.addBLEDevice(modalBleDevice);*/
            }

        });
    }

    private void saveDeviceDetails(String dvc_Name)         // Save device registration details in firestore database when first time connected
    {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            //**********Store data in Firestore*********************************************//
            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Calendar c = Calendar.getInstance();
            System.out.println("Current time => " + c.getTime());

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());

            Map<String, Object> usr_dvc = new HashMap<>();
            usr_dvc.put("Device", dvc_Name);
            usr_dvc.put("mac Id", dvc_mac_address);
            if (device_name.equals("Pebble"))
                usr_dvc.put("device type", "MVC");
            else
                usr_dvc.put("device type", "Tubby");
            usr_dvc.put("Address(tag)", radioName);
            usr_dvc.put("created on", formattedDate);
            db.collection(user.getEmail()).document("User devices").collection(dvc_Name).document("Registration details")
                    .set(usr_dvc);
        }
    }

    private void hitApiForSaveAddress() {

        ////************************************ Firebase add address first time***************************************************************************************//
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {

            //***************************************************************************************************************************************************************
        /*try {
            request = new ProjectWebRequest(mContext, getParam(), UrlConstants.ADD_ADDRESS, this, UrlConstants.ADD_ADDRESS_TAG);
            request.execute();
        } catch (Exception e) {
            clearRef();
            e.printStackTrace();
        }*/
            saveDeviceDetails(dvcNameEdited);

            //**********Store data in Firestore*********************************************//
            // Access a Cloud Firestore instance from your Activity
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Create a new user with a first and last name
            Map<String, Object> Usr = new HashMap<>();
            Usr.put("address_name", modalAddressModule.getAddressRadioName());
            Usr.put("flat_house_building", modalAddressModule.getFlat_num());
            Usr.put("tower_street", modalAddressModule.getStreetName());
            Usr.put("area_land_loca", modalAddressModule.getLocality_landmark());
            Usr.put("pin_code", modalAddressModule.getPinCode());
            Usr.put("city", modalAddressModule.getCity());
            Usr.put("state", modalAddressModule.getState());
            Usr.put("place_lat", modalAddressModule.getLatitudeLocation());
            Usr.put("place_longi", modalAddressModule.getLongitudeLocation());
            Usr.put("place_well_known_name", modalAddressModule.getPlaceWellKnownName());
            Usr.put("place_Address", modalAddressModule.getPlaceAddress());
            // Add a new document with a generated ID
            db.collection(user.getEmail()).document("User address").collection("Address type").document(modalAddressModule.getAddressRadioName())
                    .set(Usr)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Write was successful!
                            Toast.makeText(mContext, "write successful.", Toast.LENGTH_SHORT).show();

                            //***********Move to next fragment FragDeviceMAP***************************

                            long insertedAddressUniqueID = databaseHandler.insertAddressModule(user.getUid(), modalAddressModule);        //change to user.getUid()
                            if (insertedAddressUniqueID != 0) {
                                long insertedRowID = databaseHandler.insertDeviceModule(databaseHandler.getAddressUUID(), dvcNameEdited, dvc_mac_address, qrCodeEdited, valveNum);
                                if (insertedRowID != -1) {
                                    databaseHandler.insertDeviceModuleLog(databaseHandler.insertedDvcUUID);

                                    MyFragmentTransactions.replaceFragment(mContext, new FragDeviceMAP(), TagConstant.DEVICE_MAP, mContext.frm_lyt_container_int, false);
                                    dvcNameEdited = "";
                                    qrCodeEdited = "";
                                    Toast.makeText(mContext, "Device and Address now registered with app", Toast.LENGTH_LONG).show();

                                }
                            }
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


    private void clearRef() {
        if (request != null) {
            request = null;
        }
    }

    private JSONObject getParam() {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put(PreferenceModel.TokenKey, PreferenceModel.TokenValues);
            object.put("user_id", preference.getUser_id());
            object.put("add_edit", "add");
            object.put("address_name", modalAddressModule.getAddressRadioName());
            object.put("flat_house_building", modalAddressModule.getFlat_num());
            object.put("tower_street", modalAddressModule.getStreetName());
            object.put("area_land_loca", modalAddressModule.getLocality_landmark());
            object.put("pin_code", modalAddressModule.getPinCode());
            object.put("city", modalAddressModule.getCity());
            object.put("state", modalAddressModule.getState());
            object.put("place_lat", modalAddressModule.getLatitudeLocation());
            object.put("place_longi", modalAddressModule.getLongitudeLocation());
            object.put("place_well_known_name", modalAddressModule.getPlaceWellKnownName());
            object.put("place_Address", modalAddressModule.getPlaceAddress());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_ADDRESS_BOOK && resultCode == RESULT_OK) {
            if (data.getStringExtra("KEY_selected_Address_ID") != null) {
                selectedExistingAddressID = data.getStringExtra("KEY_selected_Address_ID");
                radioName = data.getStringExtra("KEY_radio_name");
                Toast.makeText(mContext, "Existing address selected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Selected address not copied, try again", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_ADDADDRESS_QRCONNECT && resultCode == RESULT_OK) {
            if (data.getSerializableExtra("mdlAddressLocation") != null) {
                modalAddressModule = (ModalAddressModule) data.getSerializableExtra("mdlAddressLocation");
                Toast.makeText(mContext, "Address Saved", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(mContext, "No data from address", Toast.LENGTH_SHORT).show();

            }
        }
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(mContext, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onSuccess(JSONObject call, int Tag) {
        if (Tag == UrlConstants.ADD_ADDRESS_TAG) {
            long insertedAddressUniqueID = databaseHandler.insertAddressModule(call.optString("address_id"), modalAddressModule);
            if (insertedAddressUniqueID != 0) {
                long insertedRowID = databaseHandler.insertDeviceModule(databaseHandler.getAddressUUID(), dvcNameEdited, dvc_mac_address, qrCodeEdited, valveNum);
                if (insertedRowID != -1) {
                    databaseHandler.insertDeviceModuleLog(databaseHandler.insertedDvcUUID);
                    //Replacing current Fragment by (FragDeviceMAP)



                    MyFragmentTransactions.replaceFragment(mContext, new FragDeviceMAP(), TagConstant.DEVICE_MAP, mContext.frm_lyt_container_int, false);
                    dvcNameEdited = "";
                    qrCodeEdited = "";
                    Toast.makeText(mContext, "Device and Address now registered with app", Toast.LENGTH_LONG).show();
                }
            }

        }

    }

    @Override
    public void onFailure(int tag, String error, int Tag, String erroMsg) {
        clearRef();
        if (Tag == UrlConstants.ADD_ADDRESS_TAG) {
            ErroScreenDialog.showErroScreenDialog(mContext, tag, erroMsg, this);
        }
    }

    @Override
    public void doRetryNow(int Tag) {
        clearRef();
        if (Tag == UrlConstants.ADD_ADDRESS_TAG) {
            hitApiForSaveAddress();
        }

    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /* @Override
     public void onFailure(String error, int Tag, String erroMsg) {

     }

     @Override
     public void doRetryNow() {

     }
 <<<<<<< HEAD

 }
 =======
 */
}
