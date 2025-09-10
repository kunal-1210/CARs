package com.example.cars; // change to your package

public class Uinfo {
    private String city;
    private String phone;
    private String profile_picture;
    private String username;
    private String fcmToken;

    // Empty constructor (needed for Firebase deserialization)
    public Uinfo() {
    }

    public Uinfo(String city, String phone, String profile_picture, String fcmcode,String username) {
        this.city = city;
        this.phone = phone;
        this.profile_picture = profile_picture;
        this.username = username;
        this.fcmToken = fcmcode;
    }
    public String getFcmToken() {
        return fcmToken;
    }
    public String getCity() {
        return city;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfile_picture() {
        return profile_picture;
    }

    public String getUsername() {
        return username;
    }
}
