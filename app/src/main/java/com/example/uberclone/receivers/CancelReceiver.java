package com.example.uberclone.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.uberclone.providers.ClientBookingProvider;

public class CancelReceiver extends BroadcastReceiver {
    private ClientBookingProvider mClientBookingProvider;

    // se ejecuta cuando damos aceptar a la noti de solicitud de servicio
    @Override
    public void onReceive(Context context, Intent intent) {

        String idClient = intent.getStringExtra("idClient");
        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(idClient, "cancel");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
    }
}
