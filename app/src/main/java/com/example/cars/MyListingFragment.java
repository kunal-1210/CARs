package com.example.cars; // change to your package

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cars.databinding.FragmentMyTripsBinding; // view binding
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyListingFragment extends Fragment {

    private FragmentMyTripsBinding binding;
    private BookingAdapter bookingAdapter;
    private List<Booking> bookingList = new ArrayList<>();
    private DatabaseReference bookingRef;
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyTripsBinding.inflate(inflater, container, false);

        binding.recyclerMyTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingAdapter = new BookingAdapter(getContext(), bookingList, currentUserId);
        binding.recyclerMyTrips.setAdapter(bookingAdapter);

        bookingRef = FirebaseDatabase.getInstance().getReference("bookings");

        fetchMyListings();

        return binding.getRoot();
    }

    private void fetchMyListings() {
        bookingRef.orderByChild("ownerId").equalTo(currentUserId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    bookingList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Booking booking = ds.getValue(Booking.class);
                        if (booking != null){

                            String bookingId = ds.getKey();

                            // Temporary hack: don't save to DB, just keep it in object
                            booking.setTempBookingId(bookingId);
                            bookingList.add(booking);}
                    }
                    bookingAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // avoid memory leaks
    }
}
