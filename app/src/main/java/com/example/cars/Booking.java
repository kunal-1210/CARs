package com.example.cars;

public class Booking {
    private int amount;
    private String carId;
    private long createdAt;
    private String startDate;
    private String endDate;
    private String ownerId;
    private PickupLocation pickupLoco; // optional if you use lat/lng
    private String pickupaddress;
    private String pickupdate;
    private String pickuptime;
    private int status;
    private int totalDays;
    private String transactionId;
    private String userId;
    private String verificationcode;
    private String verificationcoder;
    private boolean pickupNotificationSent;
    private boolean dropoffNotificationSent;
    private String ownerfcmToken;
    private String userfcmToken;

    public Booking() {
        // Needed for Firebase
    }
    private String tempBookingId;

    public String getTempBookingId() {
        return tempBookingId;
    }

    public void setTempBookingId(String tempBookingId) {
        this.tempBookingId = tempBookingId;
    }

    public boolean isDropoffNotificationSent() {
        return dropoffNotificationSent;
    }

    public boolean isPickupNotificationSent() {
        return pickupNotificationSent;
    }

    // Getters

    public String getOwnerfcmToken() {
        return ownerfcmToken;
    }
    public String getUserfcmToken(){
        return userfcmToken;
    }

    public int getAmount() { return amount; }
    public String getCarId() { return carId; }
    public long getCreatedAt() { return createdAt; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getOwnerId() { return ownerId; }
    public PickupLocation getPickupLoco() { return pickupLoco; }
    public String getPickupAddress() { return pickupaddress; }
    public String getPickupDate() { return pickupdate; }
    public String getPickupTime() { return pickuptime; }
    public int getStatus() { return status; }
    public int getTotalDays() { return totalDays; }
    public String getTransactionId() { return transactionId; }
    public String getUserId() { return userId; }
    public String getVerificationCode() { return verificationcode; }
    public String getVerificationCoder() { return verificationcoder; }
    // Setters (for new bookings)
    public void setPickupAddress(String pickupaddress) { this.pickupaddress = pickupaddress; }
    public void setPickupDate(String pickupdate) { this.pickupdate = pickupdate; }
    public void setPickupTime(String pickuptime) { this.pickuptime = pickuptime; }
    public void setVerificationCode(String verificationcode) { this.verificationcode = verificationcode; }
    public void setVerificationCoder(String verificationcoder) { this.verificationcoder = verificationcoder; }
    // Nested class for pickup location
    public static class PickupLocation {
        private double latitude;
        private double longitude;

        public PickupLocation() {}

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
}
