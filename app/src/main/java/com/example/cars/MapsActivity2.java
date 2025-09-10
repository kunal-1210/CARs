package com.example.cars;

import static android.content.ContentValues.TAG;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.cars.databinding.ActivityMaps2Binding;

public class MapsActivity2 extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMaps2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMaps2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnflocation.setOnClickListener(v -> {
            LatLng centerLatLng = mMap.getCameraPosition().target; // marker at center

            Intent resultIntent = new Intent();
            resultIntent.putExtra("pickup_lat", centerLatLng.latitude);
            resultIntent.putExtra("pickup_lng", centerLatLng.longitude);

            setResult(RESULT_OK, resultIntent);
            finish(); // closes MapsActivity2 and goes back
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        double carLat = getIntent().getDoubleExtra("latitude", 0);
        double carLng = getIntent().getDoubleExtra("longitude", 0);
        LatLng carLocation = new LatLng(carLat, carLng);
        mMap.addMarker(new MarkerOptions()
            .position(carLocation)
            .title("Hosts car location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 15));
        mMap.addCircle(new CircleOptions()
            .center(carLocation)   // Center = car's location
            .radius(2000)          // radius in meters (2000m = 2km)
            .strokeColor(Color.BLUE)   // Circle border color
            .strokeWidth(2f)           // Border thickness
            .fillColor(0x220000FF));   // Transparent fill (alpha + blue)
    }
}