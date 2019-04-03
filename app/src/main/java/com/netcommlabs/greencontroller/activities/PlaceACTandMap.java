package com.netcommlabs.greencontroller.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.netcommlabs.greencontroller.R;

public class PlaceACTandMap extends AppCompatActivity implements OnMapReadyCallback, EditText.OnEditorActionListener {

    private PlaceAutocompleteFragment place_autocomplete_fragment;
    Button btnACACT;
    private static final int PLACE_AC_REQUEST_CODE = 1;
    private LatLng placeLatLong;
    private Place place;
    private String name;
    private MapFragment mapFragment;
    private View mapView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_ac_and_map);

      /*  mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        mapView.setVisibility(View.GONE);*/

        /*btnACACT = findViewById(R.id.btnACACT);
        btnACACT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent placeIntent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(PlaceACTandMap.this);
                    startActivityForResult(placeIntent, PLACE_AC_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });*/

        place_autocomplete_fragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        //place_autocomplete_fragment.get
        AppCompatImageButton button = findViewById(R.id.place_autocomplete_clear_button);
        //editText.setOnEditorActionListener(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PlaceACTandMap.this, "Clear button", Toast.LENGTH_SHORT).show();
            }
        });
        place_autocomplete_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Toast.makeText(PlaceACTandMap.this, "" + place, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(PlaceACTandMap.this, "" + status.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_AC_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                place = PlaceAutocomplete.getPlace(this, data);
                Log.e("@@@ PLACE", "" + place);
                name = place.getName().toString();

                placeLatLong = place.getLatLng();
                latLongOnGoogleMap();
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.e("@@@  P ERROR", status.getStatusMessage());

                //Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                Log.e("@@@ P ERROR", "User cancelled operation");

            }
        }
    }

    private void latLongOnGoogleMap() {
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //googleMap.setMyLocationEnabled(true);
        //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLong, 13));
        mapView.setVisibility(View.VISIBLE);
        googleMap.addMarker(new MarkerOptions().title(name).position(placeLatLong));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(placeLatLong).zoom(16).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
