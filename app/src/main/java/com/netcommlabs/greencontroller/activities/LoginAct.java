package com.netcommlabs.greencontroller.activities;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.netcommlabs.greencontroller.Dialogs.ErroScreenDialog;
import com.netcommlabs.greencontroller.Interfaces.APIResponseListener;
import com.netcommlabs.greencontroller.R;
import com.netcommlabs.greencontroller.constant.UrlConstants;
import com.netcommlabs.greencontroller.model.ModalAddressModule;
import com.netcommlabs.greencontroller.model.ModalDvcMD;
import com.netcommlabs.greencontroller.model.ModalValveMaster;
import com.netcommlabs.greencontroller.model.ModalValveSessionData;
import com.netcommlabs.greencontroller.model.PreferenceModel;
import com.netcommlabs.greencontroller.services.ProjectWebRequest;
import com.netcommlabs.greencontroller.sqlite_db.DatabaseHandler;
import com.netcommlabs.greencontroller.utilities.MySharedPreference;
import com.netcommlabs.greencontroller.utilities.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.netcommlabs.greencontroller.activities.ActvityCheckRegisteredMobileNo.LANDED_FROM;


public class LoginAct extends AppCompatActivity implements View.OnClickListener, APIResponseListener {


    private LoginAct mContext;
    private TextView tvForgtPassEvent, tvLoginEvent, tvSignUpEvent;
    private LinearLayout llLoginFB, llLoginGoogle;
    private EditText etPhoneEmail, etPassword;
    private ProjectWebRequest request;
    private DatabaseHandler databaseHandler;

    private FirebaseAuth firebaseAuth;             // Test: Firebase integration
    private ProgressDialog progressDialog;          // Test: Firebase integration
    private GoogleSignInClient mGoogleSignInClient;  //Test: Firebase google sign in
    private static final int RC_SIGN_IN = 123;
    private String name, email, phoneNumber;
    private String idToken;
    public static final int TAG_NO_INTERNET = 1000000001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        initBase();
    }

    private void initBase() {
        mContext = this;
        /*if (!NetworkUtils.isConnected(mContext)) {
            Toast.makeText(mContext, "Please check your net connection", Toast.LENGTH_SHORT).show();
        }*/

//********************************************** FIREBASE GOOGLE SIGN IN IMPLEMENTATION***********************************************************
        //initializing firebase auth object
        firebaseAuth = FirebaseAuth.getInstance();                  // Test: Firebase integration
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)    //Test: google sign in
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_DARK);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

//*******************************************************************************************************************************************
        etPhoneEmail = (EditText) findViewById(R.id.etPhoneEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        tvForgtPassEvent = (TextView) findViewById(R.id.tvForgtPassEvent);
        tvLoginEvent = (TextView) findViewById(R.id.tvLoginEvent);
        tvSignUpEvent = (TextView) findViewById(R.id.tvSignUpEvent);

        llLoginFB = (LinearLayout) findViewById(R.id.llLoginFB);
        llLoginGoogle = (LinearLayout) findViewById(R.id.llLoginGoogle);

        tvSignUpEvent.setOnClickListener(this);
        tvLoginEvent.setOnClickListener(this);
        tvForgtPassEvent.setOnClickListener(this);

        Stetho.initializeWithDefaults(mContext);
    }




    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvSignUpEvent:
                Intent i = new Intent(LoginAct.this, RegistraionActivity.class);
                startActivity(i);
                break;
            case R.id.tvLoginEvent:
                 validationLogin();
                 //hitApi();
                 break;
            case R.id.tvForgtPassEvent:
                 Intent intent = new Intent(LoginAct.this, ActvityCheckRegisteredMobileNo.class);
                 startActivity(intent);
                 //finish();
                 break;
  //***********************************************************************************************************************
            case R.id.sign_in_button:        //Test: Firebase google sign in BUTTON
                 //signIn();
                 checkNetConnection();
                 break;
  //************************************************************************************************************************
        }

    }

    //****************************Check internet connection for google sign in**********************************************
    public void checkNetConnection()
    {
        if (NetworkUtils.isConnected(mContext))
        {
            signIn();
        }
        else {
            ErroScreenDialog.showErroScreenDialog(mContext, "Kindly check your net connection!", TAG_NO_INTERNET);
        }
    }
//******************************** Firebase google sign in functions************************************************************
    private void signIn() {
        if(FirebaseAuth.getInstance().getCurrentUser() != null)
        {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(LoginAct.this, "User signed out...", Toast.LENGTH_SHORT).show();
        }
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void userPhoneAuth()      // Function to register phone with google sign in account
    {
        Intent intent = new Intent(LoginAct.this, ActvityCheckRegisteredMobileNo.class);
        intent.putExtra(LANDED_FROM, "Google SignIn");
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> taskResult = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(taskResult);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            //authenticating with firebase
            firebaseAuthWithGoogle(account);

            // Signed in successfully, show authenticated UI.


        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            //Log.w(mContext, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(LoginAct.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct)               // Firebase google sign in implementation
    {
        Log.d("GoogleLogin", "firebaseAuthWithGoogle:" + acct.getId());

        //getting the auth credential
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        //Now using firebase we are signing in the user here
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("GoogleLogin", "signInWithCredential:success");

                           // name = acct.getDisplayName();
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            name = user.getDisplayName();
                            email = user.getEmail();
                            final String uid = user.getUid();
                            phoneNumber = user.getPhoneNumber();
                            MySharedPreference.getInstance(LoginAct.this).setUserName(name);                // save user details in shared prefs
                            MySharedPreference.getInstance(LoginAct.this).setUserEmail(email);
                            MySharedPreference.getInstance(LoginAct.this).setStringData("UserID", uid);

                            Toast.makeText(LoginAct.this, "User Signed In: "+ name, Toast.LENGTH_SHORT).show();
                            if(phoneNumber == null)
                            {
                                userPhoneAuth();
                             }
                            //hitApi();
                            //Toast.makeText(LoginAct.this, "Authentication Successful..", Toast.LENGTH_SHORT).show();
                           else {
                                // Write a user data to the database
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference myRef = database.getReference("User");
                                myRef.child(user.getUid()).child("username").setValue(name);
                                myRef.child(user.getUid()).child("email Id").setValue(email);
                                myRef.child(user.getUid()).child("Phone").setValue(phoneNumber)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Write was successful!
                                                Toast.makeText(LoginAct.this, "write successful.", Toast.LENGTH_SHORT).show();
                                                // ...
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Write failed
                                                Toast.makeText(LoginAct.this, "write failed!!", Toast.LENGTH_SHORT).show();
                                                // ...
                                            }
                                        });


                            }
                            //myRef.push().child("Username").setValue(name);
                            //myRef.push().child("Email").setValue(email);
                            //myRef.push().child("Phone mumber").setValue(phoneNumber);
                            //************************************ Firebase: Start main Activity after successfull Google sign in*****************************
                            /*MySharedPreference.getInstance(LoginAct.this).setMOBILE("9711209087");
                            Intent intent = new Intent(LoginAct.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);*/
                            //*********************************************************************************************************************************


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("GoogleLogin", "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginAct.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }

                        // ...
                    }
                });
    }
//*******************************************Alternative Firebase account sign in using email and password********************************************************************************************************

    private void validationLogin() {
       /* if (!NetworkUtils.isConnected(mContext)) {
            Toast.makeText(mContext, "Please check your net connection", Toast.LENGTH_SHORT).show();
            return;
        }*/

        String email = etPhoneEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();

        if (etPhoneEmail.getText().toString().trim().length() <= 0 || etPhoneEmail.getText().toString().trim().length() <= 0) {
            Toast.makeText(this, "Please Enter Email Address or Mobile no", Toast.LENGTH_SHORT).show();
            return;
        }

        if (etPassword.getText().toString().trim().length() <= 0) {
            Toast.makeText(this, "Please Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

       if(etPhoneEmail.getText().toString().trim().length() == 10)
            hitApi();
        else {
            //*****************************************Test: Firebase integration****************************************************//
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                //that means user is already logged in
                hitApi();
                Toast.makeText(this, "Already signed in..", Toast.LENGTH_SHORT).show();
            } else {

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    Toast.makeText(LoginAct.this, user + " signedIn successfully", Toast.LENGTH_SHORT).show();
                                    hitApi();

                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(LoginAct.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }

                                // ...
                            }
                        });

            }
            //******************************************************************************************************************
        }

    }

    private void hitApi() {
        try {
            request = new ProjectWebRequest(this, getParamForLogin(), UrlConstants.LOGIN, this, UrlConstants.LOGIN_TAG);
            request.execute();
        } catch (Exception e) {
            clearRef();
            e.printStackTrace();
        }
    }

    private JSONObject getParamForLogin() {

        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put(PreferenceModel.TokenKey, PreferenceModel.TokenValues);
            if((etPhoneEmail.getText().toString().trim().length() > 0) && (etPassword.getText().toString().trim().length() > 0))
            {
            object.put("uname", etPhoneEmail.getText().toString());
            object.put("password", etPassword.getText().toString());
            }
            else{                                            //Test: Firebase google sign in
                object.put("uname", email);
                object.put("password", name+"123");
            }                                                //********************************

        } catch (Exception e) {
        }
        return object;
    }

    @Override
    public void onSuccess(JSONObject object, int Tag) {

        if (Tag == UrlConstants.LOGIN_TAG) {
            if (object.optString("status").equals("success")) {
                databaseHandler = DatabaseHandler.getInstance(mContext);

                PreferenceModel model = new Gson().fromJson(object.toString(), PreferenceModel.class);
                MySharedPreference.getInstance(this).setUserDetail(model);
                MySharedPreference.getInstance(this).setUser_img(object.optString("image"));

                try {
                    JSONObject objectWithData;
                    ModalAddressModule modalAddressModule;
                    JSONArray jsonArrayAddress = object.getJSONArray("addresses");
                    for (int i = 0; i < jsonArrayAddress.length(); i++) {
                        objectWithData = jsonArrayAddress.getJSONObject(i);
                        if (i == 0) {
                            modalAddressModule = new ModalAddressModule(objectWithData.optString("id"), objectWithData.optString("flat_house_building"), objectWithData.optString("tower_street"), objectWithData.optString("area_land_loca"), objectWithData.optString("pin_code"), objectWithData.optString("city"), objectWithData.optString("state"), objectWithData.optInt("status"), 1, objectWithData.optString("address_name"), objectWithData.optDouble("place_lat"), objectWithData.optDouble("place_longi"), objectWithData.optString("place_well_known_name"), objectWithData.optString("place_Address"));
                        } else {
                            modalAddressModule = new ModalAddressModule(objectWithData.optString("id"), objectWithData.optString("flat_house_building"), objectWithData.optString("tower_street"), objectWithData.optString("area_land_loca"), objectWithData.optString("pin_code"), objectWithData.optString("city"), objectWithData.optString("state"), objectWithData.optInt("status"), 0, objectWithData.optString("address_name"), objectWithData.optDouble("place_lat"), objectWithData.optDouble("place_longi"), objectWithData.optString("place_well_known_name"), objectWithData.optString("place_Address"));
                        }
                        databaseHandler.insertAddressModuleFromServer(modalAddressModule);
                    }

                    JSONArray jsonArrayDevices = object.getJSONArray("devices");
                    for (int i = 0; i < jsonArrayDevices.length(); i++) {
                        objectWithData = jsonArrayDevices.getJSONObject(i);
                        ModalDvcMD modalDvcMD = new ModalDvcMD(objectWithData.optString("address_id"), objectWithData.optString("dvc_uuid"), objectWithData.optString("dvc_name"), objectWithData.optString("dvc_mac"), objectWithData.optInt("dvc_valve_num"), objectWithData.optString("dvc_type"), objectWithData.optString("dvc_qr_code"), objectWithData.optString("dvc_op_type_aprd_string"), objectWithData.optString("dvc_op_type_con_discon"), objectWithData.optString("dvc_last_connected"), objectWithData.optInt("dvc_is_show_status"), objectWithData.optInt("dvc_op_type_aed"), objectWithData.optString("dvc_crted_dt"), objectWithData.optString("dvc_updated_dt"));
                        databaseHandler.insertDeviceModuleFromServer(modalDvcMD);
                    }

                    JSONArray jsonArrayValve = object.getJSONArray("devices_valves_master");
                    ModalValveMaster modalValveMaster;
                    for (int i = 0; i < jsonArrayValve.length(); i++) {
                        objectWithData = jsonArrayValve.getJSONObject(i);
                        if (i == 0) {
                            modalValveMaster = new ModalValveMaster(objectWithData.optString("dvc_uuid"), objectWithData.optString("valve_uuid"), objectWithData.optString("valve_name"), 1, objectWithData.optString("valve_op_ty_spp"), objectWithData.optString("valve_op_ty_flush_on_off"), objectWithData.optInt("valve_op_ty_int"), objectWithData.optString("valve_crt_dt"), objectWithData.optString("valve_update_dt"));
                        } else {
                            modalValveMaster = new ModalValveMaster(objectWithData.optString("dvc_uuid"), objectWithData.optString("valve_uuid"), objectWithData.optString("valve_name"), 0, objectWithData.optString("valve_op_ty_spp"), objectWithData.optString("valve_op_ty_flush_on_off"), objectWithData.optInt("valve_op_ty_int"), objectWithData.optString("valve_crt_dt"), objectWithData.optString("valve_update_dt"));
                        }
                        databaseHandler.insertValveMasterFromServer(modalValveMaster);
                    }

                    JSONArray jsonArrayValveSesn = object.getJSONArray("devices_valves_session");
                    for (int i = 0; i < jsonArrayValveSesn.length(); i++) {
                        objectWithData = jsonArrayValveSesn.getJSONObject(i);
                        ModalValveSessionData modalValveSessionData = new ModalValveSessionData(objectWithData.optString("valve_uuid"), objectWithData.optString("valve_name_sesn"), objectWithData.optInt("valve_sesn_dp"), objectWithData.optInt("valve_sesn_duration"), objectWithData.optInt("valve_sesn_quant"), objectWithData.optInt("valve_sesn_slot_num"), objectWithData.optString("valve_sun_tp"), objectWithData.optString("valve_mon_tp"), objectWithData.optString("valve_tue_tp"), objectWithData.optString("valve_wed_tp"), objectWithData.optString("valve_thu_tp"), objectWithData.optString("valve_fri_tp"), objectWithData.optString("valve_sat_tp"), objectWithData.optInt("valve_sesn_op_ty_int"), objectWithData.optString("valve_sesn_crt_dt"));
                        databaseHandler.insertValveSesnMasterFromServer(modalValveSessionData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                databaseHandler.closeDB();
                Intent intent = new Intent(LoginAct.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                //  ActvityOtp.getTagRegistartion("Registration");
                //finish();
            } else {
                Toast.makeText(this, "" + object.optString("message"), Toast.LENGTH_SHORT).show();

            }

        }
    }


    @Override
    public void onFailure(int tag, String error, int Tag, String erroMsg) {
        clearRef();

        if (Tag == UrlConstants.LOGIN_TAG) {
            ErroScreenDialog.showErroScreenDialog(this, tag, erroMsg, this);
        }
    }

    @Override
    public void doRetryNow(int Tag) {
        clearRef();
        hitApi();
    }

    void clearRef() {
        if (request != null) {
            request = null;
        }
    }

    @Override
    protected void onStart()             // Test: Firebase sign out if user already logged in
    {
        super.onStart();
        /*if(FirebaseAuth.getInstance().getCurrentUser() != null)
        {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(LoginAct.this, "User signed out...", Toast.LENGTH_SHORT).show();
        }*/
    }
}
