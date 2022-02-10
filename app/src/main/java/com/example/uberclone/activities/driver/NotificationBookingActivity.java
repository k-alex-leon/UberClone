package com.example.uberclone.activities.driver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.activities.client.DetailRequestActivity;
import com.example.uberclone.activities.client.MapClientBookingActivity;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientBookingProvider;
import com.example.uberclone.providers.GeofireProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

public class NotificationBookingActivity extends AppCompatActivity {

    private ClientBookingProvider mClientBookingProvider;
    private GeofireProvider mGeofireProvider;
    private AuthProvider mAuthProvider;

    private MediaPlayer mMediaPLayer;

    private TextView mTxtVOrigin, mTxtVDestination, 
            mTxtVTime, mTxtVDistance, mTxtCounter;
    
    private Button mBtnAccept, mBtnReject;

    private String mExtraIdClient, mExtraOrigin,
            mExtraDestination, mExtraTime, mExtraDistance;

    ListenerRegistration mListenerRegistration = null;

    // hacemos el contador de la noti
    private int mCounter = 15;
    private Handler mHandler;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mCounter = mCounter - 1;
            mTxtCounter.setText(String.valueOf(mCounter));
            if (mCounter > 0){
                initTimer();
            }else{
                rejectBooking();
            }
        }
    };

    private void initTimer() {
        mHandler = new Handler();
        mHandler.postDelayed(runnable, 1000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_booking);

        mClientBookingProvider = new ClientBookingProvider();

        mTxtVOrigin = findViewById(R.id.txtVOriginNotification);
        mTxtVDestination = findViewById(R.id.txtVDestinationNotification);
        mTxtVTime = findViewById(R.id.txtVTimeNotification);
        mTxtVDistance = findViewById(R.id.txtVDistanaceNotification);
        mTxtCounter = findViewById(R.id.txtVCounterNotification);

        mExtraIdClient = getIntent().getStringExtra("idClient");
        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");
        mExtraTime = getIntent().getStringExtra("time");
        mExtraDistance = getIntent().getStringExtra("distance");

        mTxtVOrigin.setText(mExtraOrigin);
        mTxtVDestination.setText(mExtraDestination);
        mTxtVTime.setText(mExtraTime);
        mTxtVDistance.setText(mExtraDistance);

        mMediaPLayer = MediaPlayer.create(this, R.raw.alert);
        mMediaPLayer.setLooping(true);

        mBtnAccept = findViewById(R.id.btnAcceptBookingNotification);
        mBtnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptBooking();
            }
        });
        
        mBtnReject = findViewById(R.id.btnRejectBookingNotification);
        mBtnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectBooking();
            }
        });

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        initTimer();
        checkStatusBooking();

    }

    private void checkStatusBooking(){
       mListenerRegistration = mClientBookingProvider.getStatus(mExtraIdClient).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    System.err.println("Escuchador fallo " + error);
                }

                if (!value.exists()){

                    if (mHandler != null) mHandler.removeCallbacks(runnable);
                    Intent i = new Intent(NotificationBookingActivity.this, MapDriverActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        });
    }

    private void acceptBooking() {
        // nos aseguramos que el contador no siga corriendo
        if (mHandler != null) mHandler.removeCallbacks(runnable);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mGeofireProvider.removeLocation(mAuthProvider.getUid());


        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(mExtraIdClient, "accept");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        // iniciar actividad apenas aceptada la noti
        Intent i = new Intent(NotificationBookingActivity.this , MapDriverBookingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.setAction(Intent.ACTION_RUN);
        // pasamos el idClient al mapDriverBooking
        i.putExtra("idClient", mExtraIdClient);
        startActivity(i);
    }

    private void rejectBooking() {
        // nos aseguramos que el contador no siga corriendo
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(mExtraIdClient, "cancel");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        Intent i = new Intent(NotificationBookingActivity.this, MapDriverActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPLayer != null){
            if (mMediaPLayer.isPlaying()){
                mMediaPLayer.pause();
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mMediaPLayer != null){
            if (mMediaPLayer.isPlaying()){
                mMediaPLayer.release();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPLayer != null){
            if (!mMediaPLayer.isPlaying()){
                mMediaPLayer.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // nos aseguramos que el contador no siga corriendo
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        // controlando el sonido de la noti
        if (mMediaPLayer != null){
            if (mMediaPLayer.isPlaying()){
                mMediaPLayer.pause();
            }
        }
        // quitamos la consulta en tiempo real
        if (mListenerRegistration != null){
            mListenerRegistration.remove();
        }
    }
}