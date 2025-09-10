package com.example.cars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cars.databinding.ActivityVerifyPd2Binding;
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

public class VerifyPD2 extends AppCompatActivity implements OnMapReadyCallback {

    private @NonNull ActivityVerifyPd2Binding binding;
    private String bookingId;
    private DatabaseReference bookingsRef, carsRef;
    private GoogleMap mMap;
    private String currentUserId;
    private String verificationCoder; // From DB
    private boolean isOwner; // To track role
    String ownerFcmToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyPd2Binding.inflate(getLayoutInflater());
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
                    Toast.makeText(VerifyPD2.this, "Booking not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String carId = snapshot.child("carId").getValue(String.class);
                String ownerId = snapshot.child("ownerId").getValue(String.class);
                String userId = snapshot.child("userId").getValue(String.class);
                String address = snapshot.child("pickupaddress").getValue(String.class);
                Double lat = snapshot.child("pickuploco").child("latitude").getValue(Double.class);
                Double lng = snapshot.child("pickuploco").child("longitude").getValue(Double.class);
                verificationCoder = snapshot.child("verificationcoder").getValue(String.class);
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
                    binding.tvPickupAddressLabel.setText("Pickup Address:");
                    binding.contact0.setText("Renter Contact:");
                    binding.etPickupCode.setVisibility(View.VISIBLE);
                    binding.btnConfirmPickup.setVisibility(View.GONE);
                    binding.tvInstruction.setText("Only share the code with the renter when the car is handed back to you.");

                    // Code is non-editable for host
                    binding.etPickupCode.setText(verificationCoder);
                    binding.etPickupCode.setFocusable(false);
                    binding.etPickupCode.setClickable(false);
                    binding.etPickupCode.setLongClickable(false);
                    binding.etPickupCode.setKeyListener(null);
                    binding.etPickupCode.setBackground(null);

                    binding.stepBookingConfirmed.setBackgroundResource(R.drawable.step_circle_active);
                    binding.stepAwaitingPickup.setBackgroundResource(R.drawable.step_circle_active);
                    binding.stepAwaitingPickup.setText("Drop off");
                    binding.stepTripInProgress.setBackgroundResource(R.drawable.step_square_active);

                    binding.tvPickuptime.setText("Pickup time:");
                    binding.tvPickuptimeval.setText(formattedTime );

                    fetchUserPhone(userId);
                } else {
                    binding.tvPickupAddressLabel.setText("drop off Address:");
                    binding.contact0.setText("Owner Contact:");
                    binding.etPickupCode.setVisibility(View.VISIBLE);
                    binding.tvInstruction.setText("Enter the code provided by the host.");
                    binding.tvPickuptime.setText("Drop off time:");
                    binding.tvPickuptimeval.setText(formattedTime );
                    binding.btnConfirmPickup.setText("Car is returned to host ");
                    binding.stepBookingConfirmed.setBackgroundResource(R.drawable.step_circle_active);
                    binding.stepAwaitingPickup.setBackgroundResource(R.drawable.step_circle_active);
                    binding.stepTripInProgress.setBackgroundResource(R.drawable.step_square_active);

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

                    binding.tvCarno.setText(carno != null ? "Vehicle No.:"+ carno :"-");
                    binding.tvCarName.setText(carName);
                    binding.tvseat.setText(seats != null ? "Seats: " + seats : "-");
                    binding.tvfuel.setText(fuel != null ? fuel : "-");
                    binding.tvtrans.setText(transmission != null ? transmission : "-");
                    if (imageUrl != null) Glide.with(VerifyPD2.this).load(imageUrl).into(binding.ivCarImage);
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
                    ownerFcmToken = snapshot.child("fcmToken").getValue(String.class);;
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleConfirmAction() {
        if (isOwner) {
            // Host simply updates status to show the car is returned
            bookingsRef.child("status").setValue(2).addOnCompleteListener(task -> {
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

            if (verificationCoder != null && verificationCoder.equals(enteredCode)) {
                bookingsRef.child("status").setValue(2).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Car returned confirmed!", Toast.LENGTH_SHORT).show();
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
        if (ownerFcmToken != null && !ownerFcmToken.isEmpty()) {
            new Thread(() -> {
                try {
                    FcmSender sender = new FcmSender();
                    Log.d("FCM", "sendNotification() called with token: ");
                    sender.sendNotification(
                        ownerFcmToken,
                        "Car returned!",
                        "Your has been returned successfully. Awaiting drop off",
                        null



                    );
                } catch (Exception e) {
                    Log.e("FCM", "Error sending notification", e);
                }
            }).start();
        }
        Intent intent = new Intent(VerifyPD2.this, Mybookings.class);
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
