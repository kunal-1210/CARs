package com.example.cars;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.*;

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
    String Oowner,ofcmToken,ufcmToken;
    String firstImageUrl = null;
    String currentUserId;

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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();



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
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d("FCM", "User token: " + token);
                    ufcmToken = token;

                    databaseReference.child("fcmToken").setValue(token); // ✅ save token under user profile
                });

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
            } else if (itemID == R.id.nav_rent_car) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                Toast.makeText(MainActivity.this, "Search for car or the available cars have marker dropped on map ", Toast.LENGTH_SHORT).show();
            } else if (itemID == R.id.nav_Mybookings) {
                startActivity(new Intent(MainActivity.this, Mybookings.class));
            }else if (itemID == R.id.nav_list_car) {
                startActivity(new Intent(MainActivity.this, ListCarActivity.class));
            } else if (itemID == R.id.nav_logout) {
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
                        DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("Cars");

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
                        carRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot carSnap : snapshot.getChildren()) {
                                    Double carLat = carSnap.child("latitude").getValue(Double.class);
                                    Double carLng = carSnap.child("longitude").getValue(Double.class);
                                    String ownerid = carSnap.child("owner_uid").getValue(String.class);
                                    if (carLat != null && carLng != null && ownerid !=null){
                                        double distance = calculateDistance(searchedLat, searchedLng, carLat, carLng);

                                        Boolean availability = carSnap.child("availability").getValue(Boolean.class); // ✅ Get availability

                                        // Skip cars that are NOT available
                                        if (availability == null || !availability) {
                                            Log.d("CarAvailability", "Car skipped (not available): " + carSnap.getKey());
                                            continue;
                                        }

                                        if (ownerid.equals(currentUserId)) {
                                            Log.d("MapMarker", "Skipped my own car: " + carSnap.getKey());
                                            continue;  // jumps to next car, no marker added
                                        }

                                        Log.d("CarDistanceCheck", "Car at " + carLat + "," + carLng + " is " + distance + " km away");
                                        if (distance <= 5.0 ) {

                                            // ✅ Car is within 5km, show pin
                                            LatLng carLatLng = new LatLng(carLat, carLng);
                                            String price = carSnap.child("price_per_day").getValue(String.class);
                                            Bitmap customIcon = createCustomMarker(price);
                                            String carId = carSnap.getKey();
                                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                .position(carLatLng)
                                                .icon(BitmapDescriptorFactory.fromBitmap(customIcon))
                                                .anchor(0.5f, 1.0f));
                                            marker.setTag(carId);

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
                                String ownerid = carSnap.child("owner_uid").getValue(String.class);

                                Boolean availability = carSnap.child("availability").getValue(Boolean.class); // ✅ Get availability

                                // Skip cars that are NOT available
                                if (availability == null || !availability) {
                                    Log.d("CarAvailability", "Car skipped (not available): " + carSnap.getKey());
                                    continue;
                                }

                                if (ownerid.equals(currentUserId)) {
                                    Log.d("MapMarker", "Skipped my own car: " + carSnap.getKey());
                                    continue;  // jumps to next car, no marker added
                                }
                                if (carLat != null && carLng != null) {
                                    double distance = calculateDistance(currentLat, currentLng, carLat, carLng);
                                    if (distance <= 5.0) {
                                        // ✅ Car is within 5km, show pin
                                        String price = carSnap.child("price_per_day").getValue(String.class); // or Double, as needed
                                        LatLng carLatLng = new LatLng(carLat, carLng);
                                        Bitmap customIcon = createCustomMarker(price);
                                        String carId = carSnap.getKey();
                                        Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(carLatLng)
                                            .icon(BitmapDescriptorFactory.fromBitmap(customIcon))
                                            .anchor(0.5f, 1.0f));
                                        marker.setTag(carId);  // 🟢 THIS IS NEEDED for .getTag() to work



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
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag != null) {
                String carId = tag.toString();
                showBottomSheet(carId);
            }
            return true; // true = consume the event and don't center the map
        });

    }


    private void showProfileSetup() {
        Intent intent = new Intent(MainActivity.this, profile_setup.class);
        startActivity(intent);
    }
    public Bitmap createCustomMarker(String priceText) {
        View markerView = LayoutInflater.from(this).inflate(R.layout.custom_marker, null);

        TextView txtPrice = markerView.findViewById(R.id.marker_price);
        txtPrice.setText("₹" + priceText);

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        return bitmap;
    }
    private void showBottomSheet(String carId) {
        DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("Cars").child(carId);
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        carRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot carsSnapshot) {
                car car = carsSnapshot.getValue(car.class);
                if (car != null) {
                    car.setCarId(carsSnapshot.getKey()); // ✅ Set the parent node key here
                }
                View bottomSheetView = LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.bottom_sheet, null);

                // Find views
                ViewPager2 viewPager = bottomSheetView.findViewById(R.id.mediaViewPager);
                TextView imageCount = bottomSheetView.findViewById(R.id.imageIndicator);
                TextView brandText = bottomSheetView.findViewById(R.id.brand);
                TextView modelText = bottomSheetView.findViewById(R.id.model);
                TextView priceText = bottomSheetView.findViewById(R.id.price);
                TextView fuelText = bottomSheetView.findViewById(R.id.fule);
                TextView seatsText = bottomSheetView.findViewById(R.id.seats);
                TextView typeText = bottomSheetView.findViewById(R.id.type);
                ImageView pfpimg = bottomSheetView.findViewById(R.id.pfpimg);
                TextView nameText = bottomSheetView.findViewById(R.id.name);
                TextView cityText = bottomSheetView.findViewById(R.id.city);
                Button bookBtn = bottomSheetView.findViewById(R.id.book_now); // ✅ Correct place to get it

                // Set text
                brandText.setText(nonNull(car.getBrand(), "Brand"));
                modelText.setText(nonNull(car.getModel(), "Model"));
                priceText.setText("₹" + nonNull(car.getPrice_per_day(), "0") + " / day");
                fuelText.setText(nonNull(car.getFuel(), "N/A"));
                seatsText.setText(nonNull(car.getSeats(), "N/A"));
                typeText.setText(nonNull(car.getTransmission(), "N/A"));


                // Load car image
                List<String> mediaUrls = car.getMedia_urls();
                if (mediaUrls != null && !mediaUrls.isEmpty()) {
                    MediaPagerAdapter adapter = new MediaPagerAdapter(MainActivity.this, mediaUrls);
                    viewPager.setAdapter(adapter);
                    imageCount.setText("1 / " + mediaUrls.size());

                    viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                            super.onPageSelected(position);
                            imageCount.setText((position + 1) + " / " + mediaUrls.size());
                        }
                    });
                }

                if (car.getOwner_uid() != null) {
                    Log.d("HostCheck", "Car has owner UID: " + car.getOwner_uid()); // check car owner UID
                    usersRef.child(car.getOwner_uid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (!userSnapshot.exists()) {
                                Log.w("HostCheck", "No user found for UID: " + car.getOwner_uid());
                                return;
                            }
                            Log.d("HostCheck", "UserSnapshot exists for UID: " + car.getOwner_uid());
                            Uinfo owner = userSnapshot.getValue(Uinfo.class);
                            if (owner != null) {
                                Log.d("HostCheck", "Owner username: " + owner.getUsername());
                                Log.d("HostCheck", "Owner city: " + owner.getCity());
                                Log.d("HostCheck", "Owner profile picture: " + owner.getProfile_picture());
                                nameText.setText(nonNull(owner.getUsername(), "Owner"));
                                Oowner = owner.getUsername();
                                ofcmToken = owner.getFcmToken();

                                cityText.setText(nonNull(owner.getCity(), "City"));

                                if (owner.getProfile_picture() != null && !owner.getProfile_picture().isEmpty()) {
                                    Glide.with(MainActivity.this)
                                        .load(owner.getProfile_picture())
                                        .into(pfpimg);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w("HostCheck", "Owner object is null for UID: " + car.getOwner_uid());
                        }
                    });

                }

                // ✅ Book Now click listener
                bookBtn.setOnClickListener(v -> {
                    for (String url : car.getMedia_urls()) {
                        if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png")) {
                            firstImageUrl = url;
                            break; // stop after finding the first image
                        }
                    }
                    Intent intent = new Intent(MainActivity.this, Booking_activity.class);
                    intent.putExtra("brand", car.getBrand());
                    intent.putExtra("model", car.getModel());
                    intent.putExtra("fuel", car.getFuel());
                    intent.putExtra("seats",car.getSeats());
                    intent.putExtra("transmission",car.getTransmission());
                    intent.putExtra("firstImg", firstImageUrl);
                    intent.putExtra("latitude", car.getLatitude());
                    intent.putExtra("longitude", car.getLongitude());
                    intent.putExtra("price", car.getPrice_per_day());
                    intent.putExtra("ownerId" , car.getOwner_uid());
                    intent.putExtra("carId" , car.getCarId());
                    intent.putExtra("owner",Oowner);
                    intent.putExtra("ofcmToken", ofcmToken);
                    intent.putExtra("ufcmToken", ufcmToken);


                    startActivity(intent);
                });

                // Show bottom sheet
                com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(MainActivity.this);
                dialog.setContentView(bottomSheetView);
                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error loading car info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to prevent nulls in text
    private String nonNull(String value, String fallback) {
        return value != null && !value.isEmpty() ? value : fallback;
    }




}
