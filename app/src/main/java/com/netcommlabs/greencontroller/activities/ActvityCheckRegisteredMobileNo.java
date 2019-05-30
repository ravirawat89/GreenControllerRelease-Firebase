package com.netcommlabs.greencontroller.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.netcommlabs.greencontroller.Dialogs.ErroScreenDialog;
import com.netcommlabs.greencontroller.Interfaces.APIResponseListener;
import com.netcommlabs.greencontroller.R;
import com.netcommlabs.greencontroller.constant.UrlConstants;
import com.netcommlabs.greencontroller.model.PreferenceModel;
import com.netcommlabs.greencontroller.services.ProjectWebRequest;
import com.netcommlabs.greencontroller.utilities.MySharedPreference;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static com.netcommlabs.greencontroller.activities.ActvityOtp.KEY_LANDED_FROM;
import static com.netcommlabs.greencontroller.activities.ActvityOtp.KEY_MOBILE_NUM;

/**
 * Created by Netcomm on 2/24/2018.
 */

public class ActvityCheckRegisteredMobileNo extends Activity implements View.OnClickListener, APIResponseListener {
    private TextView tvSubmiMobile, mobileText;
    private ProjectWebRequest request;
    private EditText et_mobile_no;
    public static final String LANDED_FROM = "landed_from";
    private String activityFrom, name, email;
    private FirebaseAuth mAuth;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private ProgressBar progressPhone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_registered_mobile);

        activityFrom = getIntent().getStringExtra(LANDED_FROM);
        mobileText = findViewById(R.id.enterText);

        tvSubmiMobile = findViewById(R.id.tv_submit_mobile);
        et_mobile_no = findViewById(R.id.et_mobile_no);
        progressPhone = findViewById(R.id.progressBarPhone);
        tvSubmiMobile.setOnClickListener(this);

        //Add it in the onCreate method, after calling method initFields()
        mAuth = FirebaseAuth.getInstance();

        if(activityFrom != null){
            mobileText.setText("Please enter your Mobile no. for google login");
            startPhoneVerify();
        }
    }

    void startPhoneVerify()            // verify mobile number enterd by user
    {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onVerificationCompleted(PhoneAuthCredential credential)
        {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //    verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //  detect the incoming verification SMS and perform verification without
            //  user action.
            Log.d("Phone verification", "onVerificationCompleted:" + credential);
            Toast.makeText(ActvityCheckRegisteredMobileNo.this, "Phone number verified successfully \n" + credential, Toast.LENGTH_SHORT).show();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();        // check user signed in
            name = user.getDisplayName();
            email = user.getEmail();
            final String uid = user.getUid();
            //final Uri photoUrl = user.getPhotoUrl();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("User");
            myRef.child(user.getUid()).child("username").setValue(name);
            myRef.child(user.getUid()).child("email Id").setValue(email);
            //myRef.child(user.getUid()).child("user photo").setValue(photoUrl.toString());
            myRef.child(user.getUid()).child("Phone").setValue(et_mobile_no.getText().toString().trim())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Write was successful!
                            MySharedPreference.getInstance(ActvityCheckRegisteredMobileNo.this).setStringData("UserID", uid);
                            MySharedPreference.getInstance(ActvityCheckRegisteredMobileNo.this).setMOBILE(et_mobile_no.getText().toString().trim());
                            //MySharedPreference.getInstance(ActvityCheckRegisteredMobileNo.this).setUser_img(photoUrl.toString());
                            Toast.makeText(ActvityCheckRegisteredMobileNo.this, "write successful.", Toast.LENGTH_SHORT).show();
                            // ...
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Write failed
                            Toast.makeText(ActvityCheckRegisteredMobileNo.this, "write failed!!", Toast.LENGTH_SHORT).show();
                            // ...
                        }
                    });

            progressPhone.setVisibility(View.GONE);
            //************************************ Firebase: Start main Activity after successfull Google sign in*****************************
            Intent intent = new Intent(ActvityCheckRegisteredMobileNo.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            //*********************************************************************************************************************************

                //signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w("Phone verification", "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(ActvityCheckRegisteredMobileNo.this, "Phone number varification failed\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                    Toast.makeText(ActvityCheckRegisteredMobileNo.this, "Phone number varification failed\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                // Show a message and update the UI
                // ...
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d("Phone verification", "onCodeSent:" + verificationId);

                //Save verification ID and resending token so we can use them later
                // String mVerificationId = verificationId;
                // String mResendToken = token;

                // ...
            }
        };
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_submit_mobile:

                if (et_mobile_no.getText().toString().trim().length() > 0) {
                    if (et_mobile_no.getText().toString().trim().length() == 10)
                    {
                        if(activityFrom != null)
                        {
                            Toast.makeText(ActvityCheckRegisteredMobileNo.this, "Verifying phone number..", Toast.LENGTH_SHORT).show();
                            progressPhone.setVisibility(View.VISIBLE);          // Set progress bar visible

                            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                    et_mobile_no.getText().toString(),        // Phone number to verify
                                    60,                 // Timeout duration
                                    TimeUnit.SECONDS,   // Unit of timeout
                                    this,               // Activity (for callback binding)
                                    mCallbacks);        // OnVerificationStateChangedCallbacks
                        }
                        else
                          hitApi();
                    }
                    else
                        {
                        Toast.makeText(ActvityCheckRegisteredMobileNo.this, " Enter valid Mobile number", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ActvityCheckRegisteredMobileNo.this, "Enter Mobile number", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    private void hitApi() {
        try {
            request = new ProjectWebRequest(this, getParam(), UrlConstants.FORGOT_PASSWORD, this, UrlConstants.FORGOT_PASSWORD_TAG);
            request.execute();
        } catch (Exception e) {
            clearRef();
            e.printStackTrace();
        }
    }

    private JSONObject getParam() {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put(PreferenceModel.TokenKey, PreferenceModel.TokenValues);
            object.put("mobile", et_mobile_no.getText().toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    private void clearRef() {
        if (request != null) {
            request = null;
        }
    }

    @Override
    public void onSuccess(JSONObject object, int Tag) {
        if (Tag == UrlConstants.FORGOT_PASSWORD_TAG) {
            if (object.optString("status").equals("success")) {

                Intent i = new Intent(ActvityCheckRegisteredMobileNo.this, ActvityOtp.class);
                i.putExtra("userId", object.optString("user_id"));
                //i.putExtra("mobile", et_mobile_no.getText().toString());
                //Intent i = new Intent(MainActivity.this, ActvityOtp.class);
                i.putExtra(KEY_LANDED_FROM, "Register User Varification");
                i.putExtra(KEY_MOBILE_NUM, et_mobile_no.getText().toString());
                //ActvityOtp.getTagData("My Profile", s);
                // i.putExtra("Tag",ActvityOtp.getTagData("My Profile"));
                //startActivity(i);
                //ActvityOtp.getTagVarificationUser("Register User Varification");
                startActivity(i);
                //  Toast.makeText(mContext, "", Toast.LENG_SHORT).show();
                Toast.makeText(this, "" + object.optString("message"), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "" + object.optString("message"), Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onFailure(int tag, String error, int Tag, String erroMsg) {
      /*  if (Tag == MessageConstants.NO_NETWORK_TAG) {
            ErroScreenDialog.showErroScreenDialog(this,tag, MessageConstants.No_NETWORK_MSG, this);
        }*/
        if (Tag == UrlConstants.FORGOT_PASSWORD_TAG) {
            ErroScreenDialog.showErroScreenDialog(this, tag, erroMsg, this);
        }
    }

    @Override
    public void doRetryNow(int Tag) {
        if (Tag == UrlConstants.FORGOT_PASSWORD_TAG) {
            hitApi();
        }


    }

   /* @Override
    public void onFailure(String error, int Tag, String erroMsg) {

    }

    @Override
    public void doRetryNow() {

    }*/
}
