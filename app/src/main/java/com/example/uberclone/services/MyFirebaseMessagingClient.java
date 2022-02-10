package com.example.uberclone.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.uberclone.R;
import com.example.uberclone.activities.driver.NotificationBookingActivity;
import com.example.uberclone.channel.NotificationHelper;
import com.example.uberclone.receivers.AcceptReceiver;
import com.example.uberclone.receivers.CancelReceiver;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingClient extends FirebaseMessagingService {

    private static final int NOTIFICATION_CODE = 100;

    // nos permite generar un token de usuario para usar las notificaciones
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

    }

    // en este metodo recibimos las notf push que llegan desde el serv
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");
        String idClient = data.get("idClient");

        String origin = data.get("origin");
        String destination = data.get("destination");
        String time = data.get("time");
        String distance = data.get("distance");

        if (title != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                if (title.contains("SERVICE REQUEST")){

                    showNotificationApiOreoActions(title, body, idClient);
                    showNotificationActivity(idClient, origin, destination,time,distance);
                }
                else if (title.contains("REQUEST CANCELED")){
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(2);
                    showNotificationApiOreo(title, body);
                }
                else{
                    showNotificationApiOreo(title, body);
                }
            }else{
                if (title.contains("SERVICE REQUEST")){
                    showNotificationActions(title, body, idClient);
                    showNotificationActivity(idClient, origin, destination,time,distance);
                }
                else if (title.contains("REQUEST CANCELED")){
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(2);
                    showNotification(title, body);
                }
                else{
                    showNotification(title, body);
                }
            }
        }
    }

    private void showNotificationActivity(String idClient, String origin,
                                          String destination, String time, String distance) {
        // encender el disp aunque esté apagado
        PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        if (!isScreenOn){
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                                "AppName:MyLock"
            );
            wakeLock.acquire(10000);
        }
        Intent i = new Intent(getBaseContext(), NotificationBookingActivity.class);
        i.putExtra("idClient", idClient);
        i.putExtra("origin", origin);
        i.putExtra("destination", destination);
        i.putExtra("time", time);
        i.putExtra("distance", distance);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    /*
    *  VERSIONES >= OREO
    * */

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationApiOreo(String title, String body) {

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getNotification(title, body, sound);

        notificationHelper.getManager().notify(1, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationApiOreoActions(String title, String body, String idClient) {

        // ACEPTAR
        Intent acceptIntent = new Intent(this, AcceptReceiver.class);
        acceptIntent.putExtra("idClient", idClient);

        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE,
                acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action acceptAction = new Notification.Action.Builder(
                R.mipmap.ic_launcher,
                "Accept",
                acceptPendingIntent
        ).build();
        // CANCELAR
        Intent cancelIntent = new Intent(this, CancelReceiver.class);
        cancelIntent.putExtra("idClient", idClient);

        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE,
                cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action cancelAction = new Notification.Action.Builder(
                R.mipmap.ic_launcher,
                "Cancel",
                cancelPendingIntent
        ).build();

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getNotificationActions(title, body,sound, acceptAction, cancelAction);

        notificationHelper.getManager().notify(2, builder.build());

    }


    /*
     *  VERSIONES < OREO
     * */
    private void showNotification(String title, String body) {

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotificationAllApi(title, body, sound);

        notificationHelper.getManager().notify(1, builder.build());
    }



    private void showNotificationActions(String title, String body, String idClient) {

        // ACEPTAR
        Intent acceptIntent = new Intent(this, AcceptReceiver.class);
        acceptIntent.putExtra("idClient", idClient);

        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE,
                acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action acceptAction = new NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                "Accept",
                acceptPendingIntent
        ).build();

        // CANCELAR
        Intent cancelIntent = new Intent(this, CancelReceiver.class);
        cancelIntent.putExtra("idClient", idClient);

        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE,
                cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action cancelAction = new NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                "Accept",
                cancelPendingIntent
        ).build();

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotificationAllApiActions(title, body, sound, acceptAction, cancelAction);

        notificationHelper.getManager().notify(2, builder.build());
    }
}
