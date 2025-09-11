package com.example.cars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Info_car extends AppCompatActivity {

    private EditText mileagetext, vehiclenoedittext, brandEditText, modelEditText, rateEditText, seatsEditText;
    private RadioGroup fuelGroup, transmissionGroup;
    private RadioButton petrol, diesel, cng, electric, manual, automatic;
    private TextView availabilitytxt;
    private SwitchCompat availabilitySwitch;

    private LinearLayout imagePreviewLayout;
    private HorizontalScrollView scrollView;

    private String carId;
    private DatabaseReference carRef;

    private Button updatebtn;
    private List<Uri> imageUriList = new ArrayList<>();
    private static final int REQUEST_IMAGE_PICK = 1;
    private double newLat;
    private double newLng;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_car);

        // Get carId from Intent
        carId = getIntent().getStringExtra("carId");
        if (carId == null) {
            Toast.makeText(this, "Car ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase reference
        carRef = FirebaseDatabase.getInstance().getReference("Cars").child(carId);

        // Link views
        vehiclenoedittext = findViewById(R.id.vehicle_number_plate);
        mileagetext = findViewById(R.id.mileage);
        brandEditText = findViewById(R.id.car_brand);
        modelEditText = findViewById(R.id.car_model);
        rateEditText = findViewById(R.id.per_day_rate);
        seatsEditText = findViewById(R.id.seats);
        availabilitySwitch = findViewById(R.id.switch_availability);

        fuelGroup = findViewById(R.id.radio_group_fuel_type);
        petrol = findViewById(R.id.radio_petrol);
        diesel = findViewById(R.id.radio_diesel);
        cng = findViewById(R.id.radio_cng);
        electric = findViewById(R.id.radio_electric);

        transmissionGroup = findViewById(R.id.transmission_group);
        manual = findViewById(R.id.manual_radio);
        automatic = findViewById(R.id.automatic_radio);

        imagePreviewLayout = findViewById(R.id.image_preview);
        scrollView = findViewById(R.id.horizontal_scroll);

        updatebtn = findViewById(R.id.update_button);
        Button uploadImageButton = findViewById(R.id.upload_image_button);
        Button pickLocationBtn = findViewById(R.id.pick_location_button);
        Button deleteBtn = findViewById(R.id.delete_button);


        deleteBtn.setOnClickListener(v -> {
            carRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Car deleted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // close activity
                } else {
                    Toast.makeText(this, "Failed to delete car", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Load car data
        loadCarDetails();
        pickLocationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivityForResult(intent, 1001); // 1001 is the request code
        });
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            carRef.child("availability").setValue(isChecked)
                .addOnSuccessListener(aVoid -> {
                    String status = isChecked ? "Car is now Available" : "Car is now Unavailable";
                    Toast.makeText(Info_car.this, status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Info_car.this, "Failed to update availability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });

        updatebtn.setOnClickListener(v ->updatedetails());
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                String[] mimeTypes = {"image/*", "video/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, REQUEST_IMAGE_PICK);
            }
        });

    }


    private void uploadAllMediaToCloudinary(List<Uri> uriList, activity_upload.OnUploadCompleteListener listener) {
        new Thread(() -> {
            List<String> uploadedUrls = new ArrayList<>();
            try {
                for (Uri uri : uriList) {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Map<String, String> options = new HashMap<>();
                    options.put("public_id", "car_media_" + System.currentTimeMillis());

                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType != null && mimeType.startsWith("video")) {
                        options.put("resource_type", "video");  // Tell Cloudinary: this is a 🎥
                    } else {
                        options.put("resource_type", "image");  // Tell Cloudinary: this is a 📷
                    }

                    Map uploadResult = MainActivity.cloudinary.uploader().upload(inputStream, options);
                    String secureUrl = (String) uploadResult.get("secure_url");
                    uploadedUrls.add(secureUrl);
                }

                runOnUiThread(() -> listener.onComplete(uploadedUrls));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    listener.onComplete(null); // return null on failure
                });
            }
        }).start();
    }


    private void updatedetails(){
        String brand = brandEditText.getText().toString().trim();
        String model = modelEditText.getText().toString().trim();
        String price = rateEditText.getText().toString().trim();
        String seats = seatsEditText.getText().toString().trim();
        String vehicleNo = vehiclenoedittext.getText().toString().trim();
        String mileage = mileagetext.getText().toString().trim();

        int selectedFuelId = fuelGroup.getCheckedRadioButtonId();
        RadioButton selectedFuelButton = findViewById(selectedFuelId);
        String fuel = selectedFuelButton != null ? selectedFuelButton.getText().toString().toLowerCase() : "";

        int selectedTransId = transmissionGroup.getCheckedRadioButtonId();
        RadioButton selectedTransButton = findViewById(selectedTransId);
        String transmission = selectedTransButton != null ? selectedTransButton.getText().toString().toLowerCase() : "";

        if (brand.isEmpty() || model.isEmpty() || price.isEmpty() || seats.isEmpty() || vehicleNo.isEmpty() || mileage.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // First get existing media URLs from Firebase
        carRef.child("media_urls").get().addOnSuccessListener(dataSnapshot -> {
            List<String> mediaUrls = new ArrayList<>();
            if (dataSnapshot.exists()) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    String url = snap.getValue(String.class);
                    if (url != null) mediaUrls.add(url);
                }
            }

            // Only upload if user selected new images
            if (!imageUriList.isEmpty()) {
                uploadAllMediaToCloudinary(imageUriList, uploadedUrls -> {
                    if (uploadedUrls != null && !uploadedUrls.isEmpty()) {
                        mediaUrls.clear();  // replace old media URLs
                        mediaUrls.addAll(uploadedUrls);
                    }
                    saveCarData(brand, model, price, seats, vehicleNo, mileage, fuel, transmission, mediaUrls);
                });
            } else {
                saveCarData(brand, model, price, seats, vehicleNo, mileage, fuel, transmission, mediaUrls);
            }
        });
    }

    private void saveCarData(String brand, String model, String price, String seats, String vehicleNo,
                             String mileage, String fuel, String transmission, List<String> mediaUrls) {

        Map<String, Object> carData = new HashMap<>();
        carData.put("brand", brand);
        carData.put("model", model);
        carData.put("price_per_day", price);
        carData.put("seats", seats);
        carData.put("vehicle_no", vehicleNo);
        carData.put("mileage", mileage);
        carData.put("fuel", fuel);
        carData.put("transmission", transmission);
        carData.put("media_urls", mediaUrls);
        carData.put("availability", availabilitySwitch.isChecked());
        if (newLat != 0.0 && newLng != 0.0) {
            carData.put("latitude", newLat);
            carData.put("longitude", newLng);
        }

        carRef.updateChildren(carData).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Car details updated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getApplicationContext(), ListCarActivity.class));
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            newLat = data.getDoubleExtra("latitude", 0.0);
            newLng = data.getDoubleExtra("longitude", 0.0);

            // Update the map preview
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_preview);
            if (mapFragment != null) {
                mapFragment.getMapAsync(googleMap -> {
                    LatLng carLocation = new LatLng(newLat, newLng);
                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(carLocation).title("New Car Location"));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 14));
                });
            }
        }

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            imageUriList.clear();  // clear old images
            imagePreviewLayout.removeAllViews(); // clear preview
            imagePreviewLayout.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.VISIBLE);

            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUriList.add(imageUri);
                    addImageToPreview(imageUri);
                }
            } else if (data.getData() != null) {
                // Single image selected
                Uri imageUri = data.getData();
                imageUriList.add(imageUri);
                addImageToPreview(imageUri);
            }
        }
    }
    private void addImageToPreview(Uri imageUri) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
        imageView.setPadding(8, 8, 8, 8);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(imageUri).into(imageView);
        imagePreviewLayout.addView(imageView);
    }


    private void loadCarDetails() {
        carRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    car c = snapshot.getValue(car.class);
                    if (c != null) {
                        brandEditText.setText(c.getBrand());
                        modelEditText.setText(c.getModel());
                        rateEditText.setText(c.getPrice_per_day());
                        seatsEditText.setText(c.getSeats());
                        vehiclenoedittext.setText(c.getVehicle_no());
                        mileagetext.setText(c.getMileage());
                        availabilitySwitch.setChecked(c.isAvailability());


                        // Fuel type
                        if (c.getFuel() != null) {
                            switch (c.getFuel().toLowerCase()) {
                                case "petrol":
                                    petrol.setChecked(true);
                                    break;
                                case "diesel":
                                    diesel.setChecked(true);
                                    break;
                                case "cng":
                                    cng.setChecked(true);
                                    break;
                                case "electric":
                                    electric.setChecked(true);
                                    break;
                            }
                        }

                        // Transmission
                        if (c.getTransmission() != null) {
                            switch (c.getTransmission().toLowerCase()) {
                                case "manual":
                                    manual.setChecked(true);
                                    break;
                                case "automatic":
                                    automatic.setChecked(true);
                                    break;
                            }
                        }

                        // Load images from media_urls
                        if (snapshot.child("media_urls").exists()) {
                            imagePreviewLayout.setVisibility(View.VISIBLE);
                            scrollView.setVisibility(View.VISIBLE);
                            for (DataSnapshot urlSnap : snapshot.child("media_urls").getChildren()) {
                                String imageUrl = urlSnap.getValue(String.class);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    ImageView imageView = new ImageView(Info_car.this);
                                    imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
                                    imageView.setPadding(8, 8, 8, 8);
                                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    Glide.with(Info_car.this).load(imageUrl).into(imageView);
                                    imagePreviewLayout.addView(imageView);
                                }
                            }
                        }

                        // Load location into map
                        if (snapshot.hasChild("latitude") && snapshot.hasChild("longitude")) {
                            Double lat = snapshot.child("latitude").getValue(Double.class);
                            Double lng = snapshot.child("longitude").getValue(Double.class);
                            if (lat != null && lng != null) {
                                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.map_preview);
                                if (mapFragment != null) {
                                    mapFragment.getMapAsync(googleMap -> {
                                        LatLng carLocation = new LatLng(lat, lng);
                                        googleMap.clear();
                                        googleMap.addMarker(new MarkerOptions().position(carLocation).title("Car Location"));
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 14));
                                    });
                                }
                            }
                        }

                    }
                } else {
                    Toast.makeText(this, "Car not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to fetch car", Toast.LENGTH_SHORT).show();
                Log.e("Info_car", "Firebase error: ", task.getException());
            }
        });
    }

}
