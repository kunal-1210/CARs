package com.example.cars;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private Context context;
    private List<Booking> bookingList;
    private String currentUserId;

    public BookingAdapter(Context context, List<Booking> bookingList, String currentUserId) {
        this.context = context;
        this.bookingList = bookingList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.bkcardview, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // ---- Step 1: Fetch Car Data using carId ----
        DatabaseReference carRef = FirebaseDatabase.getInstance().getReference("Cars").child(booking.getCarId());
        carRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String brand = snapshot.child("brand").getValue(String.class);
                    String model = snapshot.child("model").getValue(String.class);
                    String carname = brand + " " + model;
                    String imageUrl = snapshot.child("media_urls").child("0").getValue(String.class); // First image

                    holder.carName.setText(carname);
                    holder.carPrice.setText("₹" + booking.getAmount() + " paid");

                    Glide.with(context)
                        .load(imageUrl)
                        .into(holder.carImage);

                    holder.itemView.setOnClickListener(v -> {
                        switch (booking.getStatus()) {
                            case 0:{
                            Intent intent = new Intent(context, VerifyPD.class);
                            intent.putExtra("bookingId", booking.getTempBookingId()); // Pass only bookingId
                            context.startActivity(intent);
                            break;}
                            case 1:{
                                Intent intent = new Intent(context, VerifyPD2.class);
                                intent.putExtra("bookingId", booking.getTempBookingId()); // Pass only bookingId
                                context.startActivity(intent);
                                break;}
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // ---- Step 2: Set booking dates ----
        holder.bookingSDate.setText(booking.getStartDate());
        holder.bookingEDate.setText(booking.getEndDate());

        String pickuptime = booking.getPickupTime();
        String formattedTime = "Time not set"; // Default fallback

        try {
            if (pickuptime != null && !pickuptime.isEmpty()) {
                // Convert 24hr to 12hr
                SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(pickuptime);

                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    formattedTime = outputFormat.format(date);
                }
            }
        } catch (Exception e) {
            formattedTime = "Invalid time"; // If parsing fails
        }

        // ---- Step 3: Decide label based on user role ----
        if (booking.getUserId().equals(currentUserId)) {
            holder.tvDateLabel.setText("Pickup:");
            holder.tvDateValue.setText(booking.getPickupDate() + " " + formattedTime);
        } else if (booking.getOwnerId().equals(currentUserId)) {
            holder.tvDateLabel.setText("Drop-off:");
            holder.tvDateValue.setText(booking.getPickupDate() + " " + formattedTime);
            holder.stepAwaitingPickup.setText("Drop off");
        }

        // ---- Step 4: Update Progress Tracker ----
        updateProgressUI(holder, booking.getStatus());
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    // --------- HELPER METHOD TO UPDATE PROGRESS ---------
    private void updateProgressUI(BookingViewHolder holder, int status) {
        // 0 = Booked, 1 = Awaiting Pickup, 2 = In Trip, 3 = Returned
        holder.stepBookingConfirmed.setBackgroundResource(R.drawable.step_circle_inactive);
        holder.stepAwaitingPickup.setBackgroundResource(R.drawable.step_circle_inactive);
        holder.stepTripInProgress.setBackgroundResource(R.drawable.step_square_inactive);
        holder.stepCarReturned.setBackgroundResource(R.drawable.step_circle_inactive);


        switch (status) {
            case 0:
                holder.stepBookingConfirmed.setBackgroundResource(R.drawable.step_circle_active);
                break;
            case 1:
                holder.stepBookingConfirmed.setBackgroundResource(R.drawable.step_circle_active);
                holder.stepAwaitingPickup.setBackgroundResource(R.drawable.step_circle_active);
                holder.stepTripInProgress.setBackgroundResource(R.drawable.step_square_active);
                break;
            case 2:
                holder.stepBookingConfirmed.setBackgroundResource(R.drawable.step_circle_active);
                holder.stepAwaitingPickup.setBackgroundResource(R.drawable.step_circle_active);
                holder.stepTripInProgress.setBackgroundResource(R.drawable.step_square_activee);
                holder.stepCarReturned.setBackgroundResource(R.drawable.step_circle_active);
                holder.greyOverlay.setVisibility(View.VISIBLE);
                break;
        }
    }

    // --------- VIEW HOLDER CLASS ---------
    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView carImage;
        TextView carName, carPrice, bookingSDate, bookingEDate, tvDateLabel, tvDateValue;
        TextView stepBookingConfirmed, stepAwaitingPickup, stepTripInProgress, stepCarReturned;
        View greyOverlay;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);

            carImage = itemView.findViewById(R.id.carImage);
            carName = itemView.findViewById(R.id.carName);
            carPrice = itemView.findViewById(R.id.carPrice);
            bookingSDate = itemView.findViewById(R.id.bookingSDate);
            bookingEDate = itemView.findViewById(R.id.bookingEDate);
            tvDateLabel = itemView.findViewById(R.id.tvDateLabel);
            tvDateValue = itemView.findViewById(R.id.tvDateValue);

            greyOverlay = itemView.findViewById(R.id.greyOverlay);
            stepBookingConfirmed = itemView.findViewById(R.id.stepBookingConfirmed);
            stepAwaitingPickup = itemView.findViewById(R.id.stepAwaitingPickup);
            stepTripInProgress = itemView.findViewById(R.id.stepTripInProgress);
            stepCarReturned = itemView.findViewById(R.id.stepCarReturned);
        }
    }
}
