package com.example.cars;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.cars.databinding.ActivityMapsBinding;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private SearchView searchView;

    private double selectedLat;
    private double selectedLng;
    private Button finalloc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Button finalloc = findViewById(R.id.btn_finalize_location);

        searchView = findViewById(R.id.search_location1);

        if (searchView != null) {
            int searchEditTextId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
            TextView searchEditText = searchView.findViewById(searchEditTextId);

            if (searchEditText != null) {
                searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

                searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String query = searchView.getQuery().toString();
                        if (!query.trim().isEmpty()) {
                            Log.d("LocationSearch", "Keyboard search key pressed: " + query);
                            searchView.setQuery(query, true);
                        }
                        return true;
                    }
                    return false;
                });
            } else {
                Log.e("MainActivity", "SearchView's EditText not found");
            }
        } else {
            Toast.makeText(this, "searchView not found!", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "searchView is null");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String location) {

                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault()) ;
                try{
                    List<Address> addressList = geocoder.getFromLocationName(location,1);
                    if (addressList != null && !addressList.isEmpty()){
                        Address address=addressList.get(0);
                        LatLng latLng=new LatLng(address.getLatitude(),address.getLongitude());
                         mMap.clear();
                         mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,12));

                    }else {
                        Toast.makeText(MapsActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                    Toast.makeText(MapsActivity.this,"geocoder erroe:" + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        finalloc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedLat != 0.0 && selectedLng != 0.0){
                    Toast.makeText(MapsActivity.this,"a location has been selected",Toast.LENGTH_SHORT).show();
                    Log.d("SelectedLocation", "Lat: " + selectedLat + ", Lng: " + selectedLng);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selectedLat", selectedLat);
                    resultIntent.putExtra("selectedLng", selectedLng);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Finish MapsActivity and return to UploadActivity

                }else {
                    Toast.makeText(MapsActivity.this, "Please select a location first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title("selected location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,12));

                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;

            }
        });
    }
}