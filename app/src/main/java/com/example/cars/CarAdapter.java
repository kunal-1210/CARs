package com.example.cars;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.*;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private Context context;
    private List<car> carList;

    public CarAdapter(Context context, List<car> carList) {
        this.context = context;
        this.carList = carList;
    }

    @NonNull
    @Override
    public CarAdapter.CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_up_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        car car=carList.get(position);
        holder.brandModel.setText(car.brand+" "+car.model);
        holder.price.setText("₹ "+ car.price_per_day+"/day");
        holder.vehicle_no.setText(car.getVehicle_no());
        holder.availability0.setText(car.isAvailability() ? "Available" : "Not Available");
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Info_car.class);
            intent.putExtra("carId", car.carId); // assuming your car class has `carId`
            context.startActivity(intent);
        });
        if (car.media_urls != null && !car.media_urls.isEmpty()) {
            String imageUrl = car.media_urls.get(0);
            Log.d("CarAdapter", "Image URL: " + car.media_urls.get(0));
// fallback

            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholdwe) // shown while loading
                .error(R.drawable.placeholdwe)       // shown if it fails
                .into(holder.carImage);

        } else {
            holder.carImage.setImageResource(R.drawable.placeholdwe);

        }


    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder{
        ImageView carImage;
        TextView brandModel, price , availability0 , vehicle_no;

        public CarViewHolder(@NonNull View itemView){
            super(itemView);
            carImage = itemView.findViewById(R.id.car_img);
            brandModel=itemView.findViewById(R.id.car_brand_model);
            price=itemView.findViewById(R.id.car_price);
            availability0=itemView.findViewById(R.id.car_availability0);
            vehicle_no=itemView.findViewById(R.id.car_num0);

        }

    }
}
