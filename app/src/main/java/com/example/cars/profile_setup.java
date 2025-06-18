package com.example.cars;

import static android.content.ContentValues.TAG;
import static com.example.cars.MainActivity.cloudinary;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class profile_setup extends AppCompatActivity {

    private EditText usernameEditText, phoneEditText, cityEditText;
    private ImageView profileImageView;
    private Uri profileImageUri;
    private Button saveButton;

    private static final int PICK_IMAGE_REQUEST = 1;  // Code for selecting image


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_setup);  // Connects to the XML layout

        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        cityEditText = findViewById(R.id.cityEditText);
        profileImageView = findViewById(R.id.profileImageView);
        saveButton = findViewById(R.id.saveButton);

        // Set up image picker when ImageView is clicked
        profileImageView.setOnClickListener(v -> pickImage());

        // Set up save button to save profile data
        saveButton.setOnClickListener(v -> saveProfileData());
    }

    // Open the image gallery to pick a profile picture
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    // Handle the result of the image picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            profileImageUri = data.getData();
            profileImageView.setImageURI(profileImageUri);  // Display the selected image in ImageView
        }
    }

    // Save profile data to Firebase
    private void saveProfileData() {
        String username = usernameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        String city = cityEditText.getText().toString();

        // Validate the input
        if (username.isEmpty() || phone.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase reference for the Users node
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Create a map for the profile data
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("username", username);
        userProfile.put("phone", phone);
        userProfile.put("city", city);

        // If profile picture is selected, upload it to Cloudinary
        if (profileImageUri != null) {
            uploadProfilePictureToCloudinary(userId, userProfile);
        } else {
            // If no profile picture is selected, save the data without it
            database.child(userId).updateChildren(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after saving
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void uploadProfilePictureToCloudinary(String userId, Map<String, Object> userProfile) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(profileImageUri);

                Map<String, String> uploadOptions = new HashMap<>();
                uploadOptions.put("public_id", "profile_image_" + System.currentTimeMillis());

                // Cloudinary upload (MUST be on background thread)
                Map uploadResult = cloudinary.uploader().upload(inputStream, uploadOptions);
                String imageUrl = (String) uploadResult.get("secure_url");

                userProfile.put("profile_picture", imageUrl);

                // Firebase update on main thread
                runOnUiThread(() -> FirebaseDatabase.getInstance().getReference("Users")
                    .child(userId).updateChildren(userProfile)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileSetup", "Failed to save profile: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                    }));

            } catch (Exception e) {
                Log.e("ProfileSetup", "Unexpected error during Cloudinary upload: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start(); // Start the background thread
    }

}