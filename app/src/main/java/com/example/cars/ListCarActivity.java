package com.example.cars;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListCarActivity extends AppCompatActivity {

    FloatingActionButton hostAddCarFab;

    private RecyclerView recyclerView;
    private List<car> carList;
    private CarAdapter carAdapter;
    private DatabaseReference carRef;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_car);

        // Initialize FloatingActionButton
        hostAddCarFab = findViewById(R.id.host_add_car);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        carList = new ArrayList<>();
        carAdapter = new CarAdapter(this, carList);
        recyclerView.setAdapter(carAdapter);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        carRef = FirebaseDatabase.getInstance().getReference("Cars");

        fetchUserCars();



        // Set click listener
        hostAddCarFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to activity_upload
                Intent intent = new Intent(ListCarActivity.this, activity_upload.class);
                startActivity(intent);
            }
        });
    }
    private void fetchUserCars() {
        if (currentUser == null) return;

        String myUid = currentUser.getUid();

        carRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                carList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    car car = snap.getValue(car.class);
                    if (car != null) {
                        car.setCarId(snap.getKey()); // 👈 This sets the carId
                    }
                    if (car != null && car.owner_uid != null && car.owner_uid.equals(myUid)) {
                        carList.add(car); // only your cars
                    }
                }

                carAdapter.notifyDataSetChanged(); // update RecyclerView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ListCarActivity.this, "Failed to load cars", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
