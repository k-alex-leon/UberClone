package com.example.uberclone.channel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.uberclone.R;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationHelper extends ContextWrapper {

    private static final String CHANNEL_ID = "com.example.uberclone";
    private static final String CHANNEL_NAME = "UberClone";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);
        // validando la version de android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannels();
        }
    }

    // validando la version de android
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels(){
        NotificationChannel notificationChannel = new
                NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLightColor(Color.GRAY);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(notificationChannel);
    }

    public NotificationManager getManager(){
        if (manager == null){
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    /*
    * NOTIFICACIONES PARA ANDROID >= OREO
    * */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotification(String title, String body, Uri soundUri){
        return  new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_notification_car)
                .setStyle(new Notification.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    // notificaciones con acciones
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotificationActions(String title, String body, Uri soundUri,
                                                       Notification.Action acceptAction,
                                                       Notification.Action cancelAction){

        return  new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_notification_car)
                .addAction(acceptAction)
                .addAction(cancelAction)
                .setStyle(new Notification.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    /*
     * NOTIFICACIONES PARA ANDROID < OREO
     * */

    public NotificationCompat.Builder getNotificationAllApi(String title, String body, Uri soundUri){
        return  new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_notification_car)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    // notificaciones con acciones
    public NotificationCompat.Builder getNotificationAllApiActions(String title, String body, Uri soundUri,
                                                                   NotificationCompat.Action acceptAction,
                                                                   NotificationCompat.Action cancelAction){
        return  new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .addAction(acceptAction)
                .addAction(cancelAction)
                .setSmallIcon(R.drawable.ic_notification_car)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title));
    }
}
