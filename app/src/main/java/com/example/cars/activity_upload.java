package com.example.cars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class activity_upload extends AppCompatActivity implements OnMapReadyCallback {

    private EditText mileage, carbrand, carmodel, perdayrent, seats, vehiclenum;
    private String fuelType, ftransmissiontype;
    private static final int REQUEST_IMAGE_PICK = 1;

    private HorizontalScrollView horizontalscroll;
    private LinearLayout imagePreviewLayout;
    private List<Uri> imageUriList = new ArrayList<>();

    private GoogleMap previewMap;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload1);

        vehiclenum = findViewById(R.id.vehicle_number_plate);
        mileage = findViewById(R.id.mileage);
        carbrand = findViewById(R.id.car_brand);
        carmodel = findViewById(R.id.car_model);
        perdayrent = findViewById(R.id.per_day_rate);
        seats = findViewById(R.id.seats);

        RadioGroup fuelTypeGroup = findViewById(R.id.radio_group_fuel_type);
        RadioGroup transmissiontype = findViewById(R.id.transmission_group);
        Button addCarButton = findViewById(R.id.add_car_button);
        Button pickLocationButton = findViewById(R.id.pick_location_button);
        Button uploadImageButton = findViewById(R.id.upload_image_button);

        horizontalscroll = findViewById(R.id.horizontal_scroll);
        imagePreviewLayout = findViewById(R.id.image_preview);

        latitude = getIntent().getDoubleExtra("selectedLat", 0.0);
        longitude = getIntent().getDoubleExtra("selectedLng", 0.0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_preview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        pickLocationButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            startActivityForResult(intent, 1001);
        });

        uploadImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        addCarButton.setOnClickListener(view -> {
            String vehicle_number = vehiclenum.getText().toString().trim();
            String pattern = "^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$*";

            if (!vehicle_number.matches(pattern)) {
                vehiclenum.setError("invalid number or format");
                return;
            }

            int selectedId = fuelTypeGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = findViewById(selectedId);
                fuelType = selectedRadioButton.getText().toString();
            } else {
                Toast.makeText(activity_upload.this, "Please select a fuel type", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedfuleId = transmissiontype.getCheckedRadioButtonId();
            if (selectedfuleId != -1) {
                RadioButton selectedFule = findViewById(selectedfuleId);
                ftransmissiontype = selectedFule.getText().toString();
            } else {
                Toast.makeText(activity_upload.this, "Please select a transmission type", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadAllMediaToCloudinary(imageUriList, uploadedUrls -> {
                if (uploadedUrls == null) return;

                Map<String, Object> carData = new HashMap<>();
                carData.put("vehicle_no", vehicle_number);
                carData.put("mileage", mileage.getText().toString().trim());
                carData.put("owner_uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                carData.put("brand", carbrand.getText().toString().trim());
                carData.put("model", carmodel.getText().toString().trim());
                carData.put("fuel", fuelType);
                carData.put("transmission", ftransmissiontype);
                carData.put("seats", seats.getText().toString().trim());
                carData.put("price_per_day", perdayrent.getText().toString().trim());
                carData.put("latitude", latitude);
                carData.put("longitude", longitude);
                carData.put("media_urls", uploadedUrls);

                String carId = FirebaseDatabase.getInstance().getReference("Cars").push().getKey();
                DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("Cars").child(carId);
                carRef.setValue(carData)
                    .addOnSuccessListener(aVoid -> Toast.makeText(activity_upload.this, "Car added successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Log.e("UploadCar", "Failed to save car: " + e.getMessage());
                        Toast.makeText(activity_upload.this, "Failed to save car", Toast.LENGTH_SHORT).show();
                    });
            });

            Intent intent = new Intent(getApplicationContext(), ListCarActivity.class);
            startActivity(intent);
            finish();
        });
    }

    interface OnUploadCompleteListener {
        void onComplete(List<String> uploadedUrls);
    }

    private void uploadAllMediaToCloudinary(List<Uri> uriList, OnUploadCompleteListener listener) {
        new Thread(() -> {
            List<String> uploadedUrls = new ArrayList<>();
            try {
                for (Uri uri : uriList) {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Map<String, String> options = new HashMap<>();
                    options.put("public_id", "car_media_" + System.currentTimeMillis());

                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType != null && mimeType.startsWith("video")) {
                        options.put("resource_type", "video");
                    } else {
                        options.put("resource_type", "image");
                    }

                    Map uploadResult = MainActivity.cloudinary.uploader().upload(inputStream, options);
                    String secureUrl = (String) uploadResult.get("secure_url");
                    uploadedUrls.add(secureUrl);
                }

                runOnUiThread(() -> listener.onComplete(uploadedUrls));

            } catch (Exception e) {
                Log.e("CloudinaryUpload", "Upload failed: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload - failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    listener.onComplete(null);
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            imageUriList.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    imageUriList.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                imageUriList.add(uri);
            }

            imagePreviewLayout.removeAllViews();
            for (Uri uri : imageUriList) {
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null && mimeType.startsWith("video")) {
                    VideoView videoView = new VideoView(this);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(300, 300);
                    layoutParams.setMargins(8, 0, 8, 0);
                    videoView.setLayoutParams(layoutParams);
                    videoView.setVideoURI(uri);
                    videoView.seekTo(1);
                    videoView.setOnClickListener(v -> {
                        if (videoView.isPlaying()) videoView.pause();
                        else videoView.start();
                    });
                    imagePreviewLayout.addView(videoView);
                } else {
                    ImageView imageView = new ImageView(this);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(300, 300);
                    layoutParams.setMargins(8, 0, 8, 0);
                    imageView.setLayoutParams(layoutParams);
                    imageView.setImageURI(uri);
                    imagePreviewLayout.addView(imageView);
                }
            }

            imagePreviewLayout.setVisibility(View.VISIBLE);
            horizontalscroll.setVisibility(View.VISIBLE);

        } else if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            latitude = data.getDoubleExtra("selectedLat", 0.0);
            longitude = data.getDoubleExtra("selectedLng", 0.0);
            Log.d("UploadActivity", "Selected Location: Lat = " + latitude + ", Lng = " + longitude);
            showMapPreview(latitude, longitude);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        previewMap = googleMap;
        LatLng selectedLatLng = new LatLng(latitude, longitude);
        previewMap.addMarker(new MarkerOptions().position(selectedLatLng).title("Selected Location"));
        previewMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 12));
        previewMap.getUiSettings().setAllGesturesEnabled(false);
    }

    private void showMapPreview(double lat, double lng) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_preview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                LatLng selectedLocation = new LatLng(lat, lng);
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(selectedLocation).title("Selected Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 12));
            });
        }
    }
}
