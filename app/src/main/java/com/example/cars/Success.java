package com.example.cars;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.cars.databinding.ActivitySuccessBinding;

public class Success extends AppCompatActivity {
    private ActivitySuccessBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String Owner = getIntent().getStringExtra("owner");
        String firstimg0 = getIntent().getStringExtra("firstimage");
        String paymentId = getIntent().getStringExtra("paymentId");
        String brand = getIntent().getStringExtra("brand");
        String model = getIntent().getStringExtra("model");
        String fuel = getIntent().getStringExtra("fuel");
        String seats = getIntent().getStringExtra("seats");
        String transmission = getIntent().getStringExtra("transmission");
        int amount = getIntent().getIntExtra("amount", 0);
        String startDate = getIntent().getStringExtra("startDate");
        String endDate = getIntent().getStringExtra("endDate");
        int totalDays = getIntent().getIntExtra("totalDays", 0);
        String ownerId = getIntent().getStringExtra("ownerId");
        String carId = getIntent().getStringExtra("carId");

        Glide.with(this).load(firstimg0).into(binding.imgCar);
        binding.tvCarTitle.setText(brand + " • " + model);
        binding.tvCarMeta.setText(transmission + " • " + fuel + " • " + seats + " seats");
        binding.tvAmount.setText("₹" + amount);
        binding.tvStartDate.setText(startDate);
        binding.tvEndDate.setText(endDate);
        binding.tvTotalDays.setText(String.valueOf(totalDays));
        binding.tvTxnId.setText(paymentId);
        binding.tvOwnerName.setText(Owner);

        binding.btnViewMyBookings.setOnClickListener(V ->{
            Intent intent = new Intent(Success.this ,Mybookings.class);
            startActivity(intent);
        });

        binding.btnGoHome.setOnClickListener(V ->{
            Intent intent = new Intent(Success.this ,MainActivity.class);
            startActivity(intent);
        });

    }

} 