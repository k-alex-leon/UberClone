package com.example.uberclone.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.uberclone.activities.driver.MapDriverBookingActivity;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientBookingProvider;
import com.example.uberclone.providers.GeofireProvider;

public class AcceptReceiver extends BroadcastReceiver {

    private ClientBookingProvider mClientBookingProvider;
    private GeofireProvider mGeofireProvider;
    private AuthProvider mAuthProvider;

    // se ejecuta cuando damos aceptar a la noti de solicitud de servicio
    @Override
    public void onReceive(Context context, Intent intent) {

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mGeofireProvider.removeLocation(mAuthProvider.getUid());

        String idClient = intent.getStringExtra("idClient");
        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(idClient, "accept");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        // iniciar actividad apenas aceptada la noti
        Intent i = new Intent(context, MapDriverBookingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.setAction(Intent.ACTION_RUN);
        // pasamos el idClient al mapDriverBooking
        i.putExtra("idClient", idClient);
        context.startActivity(i);
    }
}
