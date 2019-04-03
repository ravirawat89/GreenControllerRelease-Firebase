package com.netcommlabs.greencontroller.utilities;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.netcommlabs.greencontroller.Interfaces.LocationDecetor;
import com.netcommlabs.greencontroller.activities.MainActivity;

/**
 * Created by Android on 12/7/2017.
 */

public class LocationUtils implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private MainActivity activity;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationDecetor detector;
    public static final int LocationTag = 10001;
    private static LocationUtils object;

    public static LocationUtils getInstance(MainActivity activity, LocationDecetor detector) {
        if (object == null) {
            object = new LocationUtils(activity, detector);
        }
        return object;
    }

    public LocationUtils(MainActivity activity, LocationDecetor detector) {
        this.activity = activity;
        this.detector = detector;
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        if (NetworkUtils.isConnected(activity)) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        } else {
            detector.onErrors("You are not Connected to internet");
            return;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest =LocationRequest.create();
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, listener);
        checkResolutionAndProceed();
    }

    @Override
    public void onConnectionSuspended(int i) {
        detector.onErrors("Connection Susespended by code" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult != null) {
            if (connectionResult.getErrorMessage() != null) {
                detector.onErrors(connectionResult.getErrorMessage());
            } else {
                detector.onErrors("Connection Failed");
            }
        }
    }

    com.google.android.gms.location.LocationListener listener = new com.google.android.gms.location.LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            detector.OnLocationChange(location);
        }
    };

    private void checkResolutionAndProceed() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        startProgressNow();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(activity, LocationTag);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                }
            }
        });
    }

    public void startProgressNow() {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, listener);
    }

    public void onStop() {
        object = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, listener);
            mGoogleApiClient.disconnect();
        }
    }
}
