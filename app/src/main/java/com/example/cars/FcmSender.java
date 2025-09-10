package com.example.cars;

import android.util.Log;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FcmSender {

    private static final String SERVER_URL = "https://fcm-server-kxn1.onrender.com/sendNotification";

    public void sendNotification(String token, String title, String body, String bookingId) throws IOException {
        Log.d("FCM", "sendNotification() 1 called with token: " + token);
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        Gson gson = new Gson();

        Map<String, Object> payload = new HashMap<>();
        payload.put("ownerFcmToken", token);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("bookingId", bookingId);

        RequestBody requestBody = RequestBody.create(
            gson.toJson(payload),
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build();

        Response response = client.newCall(request).execute();
        System.out.println("Notification response: " + response.body().string());
    }
}
