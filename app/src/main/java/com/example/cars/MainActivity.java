package com.example.cars;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    FirebaseAuth auth;
    FirebaseUser user;
    DrawerLayout drawer_layout;
    ImageButton menuimgbtn;
    NavigationView navigationview;
    public static Cloudinary cloudinary;
    private GoogleMap mMap;
    private SearchView searchView;

    private double searchedLat = 0.0;
    private double searchedLng = 0.0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE =1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        navigationview = findViewById(R.id.navigationview);
        View headerView = navigationview.getHeaderView(0);
        TextView userDetails = headerView.findViewById(R.id.user_details);
        ImageView profileimg = headerView.findViewById(R.id.profile_img);

        drawer_layout = findViewById(R.id.drawerlayout);
        menuimgbtn = findViewById(R.id.menu_img_btn);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();


        if (cloudinary == null) {
            Map config = new HashMap();
            config.put("cloud_name", "dsepll5ts");
            config.put("api_key", "761936899267157");
            config.put("api_secret", "D3B50sysp1IHSpmPrnZHVzcSLig");
            cloudinary = new Cloudinary(config);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
            ||  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        }



        if (user != null) {
            String userId = user.getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    String city = dataSnapshot.child("city").getValue(String.class);
                    String imageUrl = dataSnapshot.child("profile_picture").getValue(String.class);

                    if (username != null) {
                        userDetails.setText(username);
                    } else {
                        userDetails.setText("No username found");
                        showProfileSetup();
                    }

                    if (phone == null || city == null) {
                        showProfileSetup();
                    }

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(MainActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.person)
                            .error(R.drawable.person)
                            .into(profileimg);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    userDetails.setText("Error fetching user details");
                    Log.e("FirebaseError", "Database read failed: " + databaseError.getMessage());
                }
            });
        } else {
            userDetails.setText("Not logged in");
        }


        menuimgbtn.setOnClickListener(view -> drawer_layout.open());

        navigationview.setNavigationItemSelectedListener(item -> {
            int itemID = item.getItemId();

            if (itemID == R.id.nav_setting) {
                Toast.makeText(MainActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "setting clicked ");
            } else if (itemID == R.id.btn_rent_car) {
                startActivity(new Intent(MainActivity.this, RentCarActivity.class));
            } else if (itemID == R.id.btn_list_car) {
                startActivity(new Intent(MainActivity.this, ListCarActivity.class));
            } else if (itemID == R.id.logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Login.class));
                finish();
            }
            drawer_layout.close();
            return false;
        });

        userDetails.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, profile_setup.class);
            startActivity(intent);
        });

        searchView = findViewById(R.id.location_search);

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            Log.e("MainActivity", "map fragment found and sync set");
        } else {
            Toast.makeText(this, "Map Fragment not found!", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Map Fragment is null");
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String location) {

                Log.d("LocationSearch", "Search submitted: " + location);

                if (mMap == null) {
                    Toast.makeText(MainActivity.this, "Map not ready", Toast.LENGTH_SHORT).show();
                    Log.e("LocationSearch", "Map is null");
                    return true;
                }

                if (location == null || location.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a location", Toast.LENGTH_SHORT).show();
                    Log.w("LocationSearch", "Empty location input");
                    return true;
                }

                if (!Geocoder.isPresent()) {
                    Toast.makeText(MainActivity.this, "Geocoder not available", Toast.LENGTH_SHORT).show();
                    Log.e("LocationSearch", "Geocoder not available on device");
                    return true;
                }

                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                try {
                    List<Address> addressList = geocoder.getFromLocationName(location, 1);
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        Log.d("LocationSearch", "Found coordinates: " + latLng);
                        searchedLat = address.getLatitude();
                        searchedLng = address.getLongitude();

                        mMap.clear();

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                        Toast.makeText(MainActivity.this, "Location found: " + location, Toast.LENGTH_SHORT).show();

                        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            fusedLocationClient.getLastLocation().addOnSuccessListener(currentLocation  -> {
                                if (currentLocation  != null) {
                                    LatLng myLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                    BitmapDescriptor myIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE); // Blue marker
                                    mMap.addMarker(new MarkerOptions()
                                        .position(myLocation)
                                        .title("You are here")
                                        .icon(myIcon));

                                    Log.d("UserLocation", "Marker added at your current location: " + myLocation);
                                }
                            });
                        }




                        DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("Cars");

                        carRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot carSnap : snapshot.getChildren()) {
                                    Double carLat = carSnap.child("latitude").getValue(Double.class);
                                    Double carLng = carSnap.child("longitude").getValue(Double.class);

                                    if (carLat != null && carLng != null) {
                                        double distance = calculateDistance(searchedLat, searchedLng, carLat, carLng);
                                        Log.d("CarDistanceCheck", "Car at " + carLat + "," + carLng + " is " + distance + " km away");
                                        if (distance <= 5.0) {
                                            // ✅ Car is within 5km, show pin
                                            LatLng carLatLng = new LatLng(carLat, carLng);
                                            mMap.addMarker(new MarkerOptions()
                                                .position(carLatLng)
                                                .title("Available Car"));

                                            Log.d("MapMarker", "Added car marker at: " + carLatLng);
                                        }else {
                                            Log.d("MapMarker", "Car ignored (too far): " + distance + " km");
                                        }
                                    }else {
                                        Log.w("MapMarker", "Car lat/lng is null");
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("CarFetch", "Failed to fetch cars: " + error.getMessage());
                            }
                        });


                    } else {
                        Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                        Log.w("LocationSearch", "No results from Geocoder");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Geocoder error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("LocationSearch", "IOException: ", e);
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize the map
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this);
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission is required to show your position on the map", Toast.LENGTH_LONG).show();
            }
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("MapReady", "Google Map is ready");

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check permission before requesting location (important!)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider requesting permissions here
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {

                if (location != null) {
                    double currentLat = location.getLatitude();
                    double currentLng = location.getLongitude();

                    LatLng myLocation = new LatLng(currentLat, currentLng);
                    BitmapDescriptor myIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE); // Blue marker
                    mMap.addMarker(new MarkerOptions()
                        .position(myLocation)
                        .title("You are here")
                        .icon(myIcon));

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));

                    DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("Cars");

                    carRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot carSnap : snapshot.getChildren()) {
                                Double carLat = carSnap.child("latitude").getValue(Double.class);
                                Double carLng = carSnap.child("longitude").getValue(Double.class);


                                if (carLat != null && carLng != null) {
                                    double distance = calculateDistance(currentLat, currentLng, carLat, carLng);
                                    Log.d("CarDistanceCheck", "Car at " + carLat + "," + carLng + " is " + distance + " km away");
                                    if (distance <= 5.0) {
                                        // ✅ Car is within 5km, show pin
                                        LatLng carLatLng = new LatLng(carLat, carLng);
                                        mMap.addMarker(new MarkerOptions()
                                            .position(carLatLng)
                                            .title("Available Car"));

                                        Log.d("MapMarker", "Added car marker at: " + carLatLng);
                                    }else {
                                        Log.d("MapMarker", "Car ignored (too far): " + distance + " km");
                                    }
                                }else {
                                    Log.w("MapMarker", "Car lat/lng is null");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("CarFetch", "Failed to fetch cars: " + error.getMessage());
                        }
                    });


                } else {
                    Toast.makeText(this, "Couldn't get location", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }


    private void showProfileSetup() {
        Intent intent = new Intent(MainActivity.this, profile_setup.class);
        startActivity(intent);
    }
}
