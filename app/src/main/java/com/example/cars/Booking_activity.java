package com.example.cars;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Booking_activity extends AppCompatActivity implements OnMapReadyCallback, PaymentResultListener {
    TextView startDate, endDate ,totalDays,ttlrent,days,grandval;
    EditText addressDetails;
    Calendar calendar, startCalender, endCalender;
    ImageView carimg;
    Button pay ;
    private GoogleMap mMap;
    private LatLng carLocation;
    private float radiusMeters = 1000f; // 1 km
    private Marker pickupMarker;
    private ActivityResultLauncher<Intent> mapLauncher;
    private LatLng pickupLocation = null;
    private int TotalDays = 0;
    int price1;
    int ttlrent0;
    int deposit=3000;
    int grandamt;
String firstimg0;
String Owner;
String userPhone,carname,username,ownername,ownerId,date ,time, address;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userPhone = snapshot.child("phone").getValue(String.class);
                        username = snapshot.child("username").getValue(String.class);
                        Log.d("MainActivity", "Fetched user phone: " + userPhone);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Failed to fetch phone: " + error.getMessage());
                }
            });
        }

        ownerId = getIntent().getStringExtra("ownerId");
        Owner = getIntent().getStringExtra("owner");
        double carLat = getIntent().getDoubleExtra("latitude", 0);
        double carLng = getIntent().getDoubleExtra("longitude", 0);
        carLocation = new LatLng(carLat, carLng);
        CheckBox checkboxTerms = findViewById(R.id.checkbox_terms);
        Button btnBookNow = findViewById(R.id.proceedToPayment);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (ownerId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(ownerId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        ownername = snapshot.child("username").getValue(String.class);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Failed to fetch phone: " + error.getMessage());
                }
            });
        }

        addressDetails = findViewById(R.id.addressDetails);
        pay = findViewById(R.id.proceedToPayment);
        View View = findViewById(R.id.mapOverlay);
        startDate = findViewById(R.id.startDate);
        totalDays = findViewById(R.id.totalDays);
        endDate = findViewById(R.id.endDate);
        carimg = findViewById(R.id.carImage);

        calendar = Calendar.getInstance();
        startCalender = Calendar.getInstance();
        endCalender = Calendar.getInstance();

        String brand = getIntent().getStringExtra("brand");
        String model = getIntent().getStringExtra("model");
        String fuel = getIntent().getStringExtra("fuel");
        String seats = getIntent().getStringExtra("seats");
        String transmission = getIntent().getStringExtra("transmission");
        String firstimg = getIntent().getStringExtra("firstImg");
        String price = getIntent().getStringExtra("price");
        price1 = Integer.parseInt(price);



        days = findViewById(R.id.units3);
        ttlrent = findViewById(R.id.ttl);
        TextView deposit0 = findViewById(R.id.units0);
        TextView carbrandmodel = findViewById(R.id.carBrandModel);
        TextView fueld = findViewById(R.id.fuel_d);
        TextView seatsd = findViewById(R.id.seats_d);
        TextView transmissiond = findViewById(R.id.transmission_d);
        TextView price0 = findViewById(R.id.units1);
        grandval = findViewById(R.id.grandTotalValue);

        deposit0.setText(String.valueOf(deposit));
        totalDays.setText(TotalDays +" days");
        carbrandmodel.setText(brand+" "+model);
        carname = brand+" "+ model;

        price0.setText(price);
        fueld.setText(fuel+" * ");
        seatsd.setText("seats:"+seats+" * ");
        transmissiond.setText(transmission+" ");
        if (firstimg != null) {
            Glide.with(this).load(firstimg).into(carimg);
            firstimg0 = firstimg;
        }
        mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    double pickupLat = result.getData().getDoubleExtra("pickup_lat", 0.0);
                    double pickupLng = result.getData().getDoubleExtra("pickup_lng", 0.0);
                    pickupLocation = new LatLng(pickupLat, pickupLng);
                    Log.d("Booking_activity", "Pickup confirmed: " + pickupLat + ", " + pickupLng);

                    if (mMap != null) {
                        updateMiniMap();
                    }
                }
            }
        );
        checkboxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnBookNow.setEnabled(true);
                btnBookNow.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.button_active));
            } else {
                btnBookNow.setEnabled(false);
                btnBookNow.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.button_disabled));            }
        });



        Checkout.preload(getApplicationContext());
        pay.setOnClickListener(v ->{
            startPayment(ttlrent0);
        });

        View.setOnClickListener(v -> {
            Intent intent = new Intent(Booking_activity.this, MapsActivity2.class);
            intent.putExtra("latitude", carLat);
            intent.putExtra("longitude", carLng);
            mapLauncher.launch(intent);  // ✅ use mapLauncher, not startActivity
        });
        // Start Date Picker
        startDate.setOnClickListener(v -> showDatePicker(startDate , true));
        // End Date Picker
        endDate.setOnClickListener(v -> showDatePicker(endDate, false));

        TextView pickupDate = findViewById(R.id.pickupDate);
        TextView pickupTime = findViewById(R.id.pickupTime);

        pickupDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    pickupDate.setText(date);
                }, year, month, day);
            datePickerDialog.show();
        });

// Time Picker
        pickupTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    pickupTime.setText(time);
                }, hour, minute, true);
            timePickerDialog.show();
        });

    }
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Generates number between 100000-999999
        return String.valueOf(code);
    }
    private BitmapDescriptor bitmapFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(
            vectorDrawable.getIntrinsicWidth(),
            vectorDrawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    private void updateMiniMap() {
        mMap.clear();

        // Always show car marker
        if (carLocation != null) {
            mMap.addMarker(new MarkerOptions()
                .position(carLocation)
                .title("Car Location"));
        }

        // If pickup location is set, also show it
        if (pickupLocation != null) {
            mMap.addMarker(new MarkerOptions()
                .position(pickupLocation)
                .title("Pickup Location").icon(bitmapFromVector(R.drawable.ic_location_pin)));

            // Show both markers in view
            LatLngBounds bounds = new LatLngBounds.Builder()
                .include(carLocation)
                .include(pickupLocation)
                .build();
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } else {
            // Default: just focus on car
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 14f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        updateMiniMap();

    }
    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        try {
            Log.d("Payment", "Success: " + razorpayPaymentID);
            // ✅ Show confirmation
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userId = currentUser.getUid();

            String carId = getIntent().getStringExtra("carId");
            String verificationCode = generateVerificationCode();
            String verificationCoder = generateVerificationCode();
            String ownerFcmToken = getIntent().getStringExtra("ofcmToken");
            String userFcmToken = getIntent().getStringExtra("ufcmToken");

            int amount = grandamt;
            int status = 0;

            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("userId", userId);
            bookingData.put("ownerId", ownerId);
            bookingData.put("carId", carId);
            bookingData.put("transactionId", razorpayPaymentID); // using payment ID as key
            bookingData.put("totalDays", TotalDays);
            bookingData.put("amount", amount);
            bookingData.put("startDate", startDate.getText().toString());
            bookingData.put("endDate", endDate.getText().toString());
            bookingData.put("status", status); // active = currently renting
            bookingData.put("createdAt", System.currentTimeMillis()); // timestamp
            bookingData.put("pickuploco", pickupLocation );
            bookingData.put("pickupdate", ((TextView)findViewById(R.id.pickupDate)).getText().toString());
            bookingData.put("pickuptime", ((TextView)findViewById(R.id.pickupTime)).getText().toString() );
            bookingData.put("pickupaddress", addressDetails.getText().toString() );
            bookingData.put("verificationcode", verificationCode );
            bookingData.put("verificationcoder", verificationCoder );
            bookingData.put("userfcmToken", userFcmToken );
            bookingData.put("ownerfcmToken", ownerFcmToken );
            bookingData.put("pickupNotificationSent", false);
            bookingData.put("dropoffNotificationSent", false);
            bookingData.put("carname", carname);
            bookingData.put("rentername", username);
            bookingData.put("hostname", ownername);



            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("bookings");
            dbRef.child(razorpayPaymentID).setValue(bookingData);
            Log.d("Booking_activity", "Writing bookingData: " + bookingData);
            Log.d("Booking_activity", "Using key: " + razorpayPaymentID);

            DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("cars").child(carId);
            carRef.child("availability").setValue(false)
                .addOnSuccessListener(aVoid -> Log.d("Booking_activity", "Car marked as unavailable"))
                .addOnFailureListener(e -> Log.e("Booking_activity", "Failed to update availability", e));


            if (ownerFcmToken != null && !ownerFcmToken.isEmpty() &&
                userFcmToken != null && !userFcmToken.isEmpty()) {

                new Thread(() -> {
                    try {
                        FcmSender sender = new FcmSender();

                        // 🔹 Notify the Owner
                        Log.d("FCM", "sendNotification() called with token: " + ownerFcmToken);
                        sender.sendNotification(
                            ownerFcmToken,
                            "Car Booked!",
                            "Your " + carname + " has been booked by " + username + ". Awaiting drop off",
                            razorpayPaymentID
                        );

                        // 🔹 Notify the Renter
                        Log.d("FCM", "sendNotification() called with token: " + userFcmToken);
                        sender.sendNotification(
                            userFcmToken,
                            "Car Booked!",
                            "Your booking for " + carname + " has been booked successfully. Awaiting pickup",
                            razorpayPaymentID
                        );

                    } catch (Exception e) {
                        Log.e("FCM", "Error sending notification", e);
                    }
                }).start();
            }


            // For now:
            Intent intent = new Intent(this, Success.class);
            intent.putExtra("firstimage", firstimg0);
            intent.putExtra("paymentId", razorpayPaymentID);
            intent.putExtra("brand", getIntent().getStringExtra("brand"));
            intent.putExtra("model", getIntent().getStringExtra("model"));
            intent.putExtra("fuel", getIntent().getStringExtra("fuel"));
            intent.putExtra("seats", getIntent().getStringExtra("seats"));
            intent.putExtra("transmission", getIntent().getStringExtra("transmission"));
            intent.putExtra("amount", grandamt);
            intent.putExtra("startDate", startDate.getText().toString());
            intent.putExtra("endDate", endDate.getText().toString());
            intent.putExtra("totalDays", TotalDays);
            intent.putExtra("ownerId", getIntent().getStringExtra("ownerId")); // Host
            intent.putExtra("carId", getIntent().getStringExtra("carId"));
            intent.putExtra("owner",Owner);
            startActivity(intent);
            finish();
        }catch (Exception e){
            Log.e("Payment", "Error in onPaymentSuccess", e);
        }
    }

    @Override
    public void onPaymentError(int code, String response) {
        Log.e("Payment", "Failed: " + response);
        // ❌ You can show a failed screen or toast
    }
    private void startPayment(int amount) {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_R8URig5sWt4BS2");  // use your test key
        checkout.setImage(R.drawable.baseline_android_24); // optional logo

        try {
            JSONObject options = new JSONObject();
            options.put("name", "Car Rental App");
            options.put("description", "Booking Payment");
            options.put("currency", "INR");

            // Razorpay expects amount in paise
            options.put("amount", grandamt * 100);

            JSONObject prefill = new JSONObject();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            prefill.put("email", currentUser.getEmail());
            prefill.put("contact",userPhone );
            options.put("prefill", prefill);

            checkout.open(this, options);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Payment", "Error in starting Razorpay Checkout", e);
        }
    }


    private void showDatePicker(TextView dateField , boolean isStartDate) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                dateField.setText(selectedDate);


                if (isStartDate) {
                    startCalender.set(selectedYear, selectedMonth, selectedDay);
                } else {
                    endCalender.set(selectedYear, selectedMonth, selectedDay);
                }

                if (!startDate.getText().toString().isEmpty() && !endDate.getText().toString().isEmpty()) {
                    // Both dates are picked, calculate difference
                    long diffMillis = endCalender.getTimeInMillis() - startCalender.getTimeInMillis();
                    long diffDays = diffMillis / (24 * 60 * 60 * 1000);

                    if (diffDays >= 0) {
                        TotalDays = (int) (diffDays + 1);
                        totalDays.setText(String.valueOf(diffDays + 1));
                        days.setText(String.valueOf(TotalDays + " days"));
                        // include start date
                        ttlrent0 = price1 * TotalDays;
                        grandamt = ttlrent0 + deposit;
                        grandval.setText(String.valueOf(grandamt));
                        ttlrent.setText(String.valueOf(ttlrent0));


                    } else {
                        totalDays.setText("0"); // if end < start
                    }
                } else {
                    // One or both dates are not picked yet
                    totalDays.setText("0");
                }
            },
            year, month, day
        );
        datePickerDialog.show();

    }
}