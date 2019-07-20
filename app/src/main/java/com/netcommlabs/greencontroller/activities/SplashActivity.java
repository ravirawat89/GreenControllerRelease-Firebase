package com.netcommlabs.greencontroller.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.netcommlabs.greencontroller.R;
import com.netcommlabs.greencontroller.model.PreferenceModel;
import com.netcommlabs.greencontroller.utilities.MySharedPreference;

/**
 * Created by Netcomm on 3/7/2018.
 */

public class SplashActivity extends Activity {

    String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_splash);

       // FirebaseDatabase.getInstance().setPersistenceEnabled(true);  //update data to firebase database when user is online

        // [START set_firestore_settings]
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        startNow();
    }

    private void startNow() {
        {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    //PreferenceModel model = MySharedPreference.getInstance(SplashActivity.this).getsharedPreferenceData();
                    String userid = MySharedPreference.getInstance(SplashActivity.this).getStringData("UserID");
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user != null)
                            userId = user.getUid();
                   // if (model.getUser_id() != null && model.getUser_id().length() > 0) {
                    if (user != null)
                    {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        //finish();
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginAct.class));
                    }
                    finish();
                }
            }, 3000);
        }
    }


}
