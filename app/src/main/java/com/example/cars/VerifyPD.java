package com.example.cars;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cars.databinding.ActivityVerifyPdBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VerifyPD extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityVerifyPdBinding binding;
    private String bookingId;
    private DatabaseReference bookingsRef, carsRef;
    private GoogleMap mMap;
    private String currentUserId;
    private String verificationCode; // From DB
    private boolean isOwner; // To track role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyPdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get bookingId from intent
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            Toast.makeText(this, "Booking ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings").child(bookingId);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        fetchBookingData();

        // Confirm Pickup/Return button click
        binding.btnConfirmPickup.setOnClickListener(v -> handleConfirmAction());
    }

    private void fetchBookingData() {
        binding.progressPickup.setVisibility(View.VISIBLE);

        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressPickup.setVisibility(View.GONE);
                if (!snapshot.exists()) {
                    Toast.makeText(VerifyPD.this, "Booking not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String carId = snapshot.child("carId").getValue(String.class);
                String ownerId = snapshot.child("ownerId").getValue(String.class);
                String userId = snapshot.child("userId").getValue(String.class);
                String address = snapshot.child("pickupaddress").getValue(String.class);
                Double lat = snapshot.child("pickuploco").child("latitude").getValue(Double.class);
                Double lng = snapshot.child("pickuploco").child("longitude").getValue(Double.class);
                verificationCode = snapshot.child("verificationcode").getValue(String.class);
                String pickuptime = snapshot.child("pickuptime").getValue(String.class);
                String pickupdate = snapshot.child("pickupdate").getValue(String.class);

                // Convert time from 24-hour to 12-hour format
                String formattedTime = pickuptime;
                if (pickuptime != null) {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        Date date = inputFormat.parse(pickuptime);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        formattedTime = outputFormat.format(date);
                    } catch (ParseException e) {
                        formattedTime = pickuptime; // fallback
                    }
                }

                isOwner = currentUserId.equals(ownerId);

                // Update UI based on role
                if (isOwner) {
                    binding.tvPickupAddressLabel.setText("Drop-off Address:");
                    binding.contact0.setText("Renter Contact:");
                    binding.etPickupCode.setVisibility(View.VISIBLE);
                    binding.btnConfirmPickup.setText("Car has been returned");
                    binding.tvInstruction.setText("Only share the code with the renter when the car is handed over.");

                    // Code is non-editable for host
                    binding.etPickupCode.setText(verificationCode);
                    binding.etPickupCode.setFocusable(false);
                    binding.etPickupCode.setClickable(false);
                    binding.etPickupCode.setLongClickable(false);
                    binding.etPickupCode.setKeyListener(null);
                    binding.etPickupCode.setBackground(null);
                    binding.btnConfirmPickup.setVisibility(View.GONE);
                    binding.stepAwaitingPickup.setText("Drop off");
                    binding.btnConfirmPickup.setVisibility(View.GONE);
                    binding.tvPickuptime.setText("Drop off time:");
                    binding.tvPickuptimeval.setText(formattedTime + "  " + pickupdate);

                    fetchUserPhone(userId);
                } else {
                    binding.tvPickupAddressLabel.setText("Pickup Address:");
                    binding.contact0.setText("Owner Contact:");
                    binding.etPickupCode.setVisibility(View.VISIBLE);
                    binding.btnConfirmPickup.setText("Car has been picked");
                    binding.tvInstruction.setText("Enter the code provided by the host.");
                    binding.tvPickuptime.setText("Pickup time:");
                    binding.tvPickuptimeval.setText(formattedTime + "  " + pickupdate);

                    fetchUserPhone(ownerId);
                }

                binding.tvPickupAddress.setText(address != null ? address : "N/A");

                // Map marker
                if (lat != null && lng != null && mMap != null) {
                    LatLng location = new LatLng(lat, lng);
                    String markerLabel = isOwner ? "Drop-off Location" : "Pickup Location";
                    mMap.addMarker(new MarkerOptions().position(location).title(markerLabel));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
                }

                // Fetch Car details
                fetchCarDetails(carId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressPickup.setVisibility(View.GONE);
            }
        });
    }

    private void fetchCarDetails(String carId) {
        carsRef = FirebaseDatabase.getInstance().getReference("Cars").child(carId);
        carsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot carSnap) {
                if (carSnap.exists()) {
                    String brand = carSnap.child("brand").getValue(String.class);
                    String carno = carSnap.child("vehicle_no").getValue(String.class);
                    String model = carSnap.child("model").getValue(String.class);
                    String carName = (brand != null ? brand : "") + " " + (model != null ? model : "");
                    String seats = carSnap.child("seats").getValue(String.class);
                    String fuel = carSnap.child("fuel").getValue(String.class);
                    String transmission = carSnap.child("transmission").getValue(String.class);
                    String imageUrl = carSnap.child("media_urls").child("0").getValue(String.class);

                    binding.tvCarName.setText(carName);
                    binding.tvCarno.setText(carno != null ? "Vehicle No.:"+ carno :"-");
                    binding.tvseat.setText(seats != null ? "Seats: " + seats : "-");
                    binding.tvfuel.setText(fuel != null ? fuel : "-");
                    binding.tvtrans.setText(transmission != null ? transmission : "-");
                    if (imageUrl != null) Glide.with(VerifyPD.this).load(imageUrl).into(binding.ivCarImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUserPhone(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Uinfo user = snapshot.getValue(Uinfo.class);
                    if (user != null) binding.contact.setText(user.getPhone());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleConfirmAction() {
        if (isOwner) {
            // Host simply updates status to show the car is returned
            bookingsRef.child("status").setValue(1).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Car return confirmed.", Toast.LENGTH_SHORT).show();
                    navigateToMyTrips();
                } else {
                    Toast.makeText(this, "Failed to update status.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Renter must enter code and verify
            String enteredCode = binding.etPickupCode.getText().toString().trim();
            if (enteredCode.isEmpty()) {
                Toast.makeText(this, "Please enter the code.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (verificationCode != null && verificationCode.equals(enteredCode)) {
                bookingsRef.child("status").setValue(1).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Car pickup confirmed!", Toast.LENGTH_SHORT).show();
                        navigateToMyTrips();
                    } else {
                        Toast.makeText(this, "Failed to update status.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Code incorrect
                Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToMyTrips() {
        Intent intent = new Intent(VerifyPD.this, Mybookings.class);
        intent.putExtra("openFragment", "myTrips");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }
}
