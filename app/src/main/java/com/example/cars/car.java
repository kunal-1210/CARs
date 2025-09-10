package com.example.cars;

import java.util.List;

public class car {

    public String mileage, vehicle_no, carId, brand, model, price_per_day, owner_uid, fuel, seats, transmission;
    public List<String> media_urls;
    public double latitude, longitude;
    public boolean availability; // ✅ Added availability field

    public car() {
        // required empty constructor for Firebase
    }

    public car(String vehicle_no, String mileage, String brand, String model, List<String> media_urls,
               String uid, int price, String fuel, String seats, String transmission,
               double latitude, double longitude, boolean availability) {

        this.vehicle_no = vehicle_no;
        this.mileage = mileage;
        this.brand = brand;
        this.model = model;
        this.media_urls = media_urls;
        this.price_per_day = String.valueOf(price);
        this.owner_uid = uid;
        this.fuel = fuel;
        this.seats = seats;
        this.transmission = transmission;
        this.latitude = latitude;
        this.longitude = longitude;
        this.availability = availability; // ✅ Initialize availability
    }

    // Getters
    public String getVehicle_no() { return vehicle_no; }
    public String getMileage() { return mileage; }
    public String getCarId() { return carId; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getPrice_per_day() { return price_per_day; }
    public String getOwner_uid() { return owner_uid; }
    public String getFuel() { return fuel; }
    public String getSeats() { return seats; }
    public String getTransmission() { return transmission; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<String> getMedia_urls() { return media_urls; }
    public boolean isAvailability() { return availability; } // ✅ Getter for availability

    // Setters
    public void setVehicle_no(String vehicle_no) { this.vehicle_no = vehicle_no; }
    public void setMileage(String mileage) { this.mileage = mileage; }
    public void setCarId(String carId) { this.carId = carId; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setModel(String model) { this.model = model; }
    public void setFuel(String fuel) { this.fuel = fuel; }
    public void setOwner_uid(String owner_uid) { this.owner_uid = owner_uid; }
    public void setPrice_per_day(String price_per_day) { this.price_per_day = price_per_day; }
    public void setSeats(String seats) { this.seats = seats; }
    public void setTransmission(String transmission) { this.transmission = transmission; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setMedia_urls(List<String> media_urls) { this.media_urls = media_urls; }
    public void setAvailability(boolean availability) { this.availability = availability; } // ✅ Setter for availability
}
