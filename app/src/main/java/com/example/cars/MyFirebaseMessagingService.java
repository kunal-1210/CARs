package com.example.cars;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification() != null
            ? remoteMessage.getNotification().getTitle()
            : "Booking Update";

        String body = remoteMessage.getNotification() != null
            ? remoteMessage.getNotification().getBody()
            : "You have a new booking";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "car_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // your app icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "car_channel",
                "Car Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }
        manager.notify(1, builder.build());
    }

    // ✅ This method is called when a new token is generated or refreshed
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("fcmToken");

            tokenRef.setValue(token)
                .addOnSuccessListener(aVoid ->
                    android.util.Log.d("FCM", "Token updated in DB: " + token))
                .addOnFailureListener(e ->
                    android.util.Log.e("FCM", "Failed to update token", e));
        }
    }
}
