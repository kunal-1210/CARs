package com.example.cars;
public class UserModel {
    public String name;
    public String email;
    public String phone;

    public UserModel() {} // Required for Firebase

    public UserModel(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}

