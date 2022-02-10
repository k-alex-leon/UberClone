package com.example.uberclone.activities.client;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.models.ClientBooking;
import com.example.uberclone.models.HistoryBooking;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientBookingProvider;
import com.example.uberclone.providers.DriversProvider;
import com.example.uberclone.providers.HistoryBookingProvider;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

public class CalificationDriverActivity extends AppCompatActivity {


    private ClientBookingProvider mClientBookingProvider;
    private DriversProvider mDriverProvider;
    private AuthProvider mAuthProvider;

    private HistoryBooking mHistoryBooking;
    private HistoryBookingProvider mHistoryBookingProvider;

    private TextView mtxtVDriverName, mTxtVOrigin,
            mTxtVDestination,mTxtCostTrip;

    private RatingBar mRatinBarClientCalification;

    private Button mBtnSendDriverCalification;

    private String mIdHistoryBooking;

    private float mCalification = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calification_driver);

        mClientBookingProvider = new ClientBookingProvider();
        mDriverProvider = new DriversProvider();
        mHistoryBookingProvider = new HistoryBookingProvider();
        mAuthProvider = new AuthProvider();

        mtxtVDriverName = findViewById(R.id.txtVDriverNameCalification);
        mTxtVOrigin = findViewById(R.id.txtVOriginDriverCalification);
        mTxtVDestination = findViewById(R.id.txtVDestinationDriverCalification);
        mTxtCostTrip = findViewById(R.id.txtVCostTripClient);

        mRatinBarClientCalification = findViewById(R.id.ratingDriverCalification);
        mRatinBarClientCalification.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float calification, boolean fromUser) {
                mCalification = calification;
            }
        });

        mBtnSendDriverCalification = findViewById(R.id.btnCalificationDriver);

        // mostrar info en pantalla
        getClientBooking();

        mBtnSendDriverCalification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calificate();
            }
        });

    }

    private void getClientBooking(){
        mClientBookingProvider.getClientBooking(mAuthProvider.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){

                    ClientBooking clientBooking = documentSnapshot.toObject(ClientBooking.class);
                    mHistoryBooking = new HistoryBooking(
                            mAuthProvider.getUid()+clientBooking.getIdDriver(),
                            clientBooking.getIdClient(),
                            clientBooking.getIdDriver(),
                            clientBooking.getOrigin(),
                            clientBooking.getDestination(),
                            clientBooking.getTime(),
                            clientBooking.getKm(),
                            clientBooking.getStatus(),
                            clientBooking.getOriginLat(),
                            clientBooking.getOriginLng(),
                            clientBooking.getDestinationLat(),
                            clientBooking.getDestinationLng()
                    );

                    double cost = clientBooking.getCost();
                    mTxtCostTrip.setText("$ " + String.format("%.1f", cost));
                    mTxtVOrigin.setText(documentSnapshot.getString("origin"));
                    mTxtVDestination.setText(documentSnapshot.getString("destination"));

                    mDriverProvider.getUserById(mHistoryBooking.getIdDriver()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot docSnap) {
                            if (docSnap.exists()){
                                mtxtVDriverName.setText(docSnap.getString("username"));
                            }
                        }
                    });

                }
            }
        });
    }



    private void calificate() {
        if (mCalification > 0 ){

            mHistoryBooking.setCalificationDriver(mCalification);
            mHistoryBooking.setTimestamp(new Date().getTime());
            mIdHistoryBooking = mHistoryBooking.getIdHistoryBooking();
            mHistoryBookingProvider.getHistoryBooking(mIdHistoryBooking)
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()){
                                mHistoryBooking.setCalificationClient(documentSnapshot.getDouble("calificationClient"));
                                // hace un update al contenido creado en la bd
                                mHistoryBookingProvider.updateCalificationDriver(mIdHistoryBooking, mHistoryBooking)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(CalificationDriverActivity.this, "Calification send!", Toast.LENGTH_SHORT).show();
                                                Intent i = new Intent(CalificationDriverActivity.this, MapClientActivity.class);
                                                startActivity(i);
                                                finish();
                                            }
                                        });
                            }else{

                                // crea el objeto en la bd
                                mHistoryBookingProvider.create(mHistoryBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(CalificationDriverActivity.this, "Calification send!", Toast.LENGTH_SHORT).show();
                                        Intent i = new Intent(CalificationDriverActivity.this, MapClientActivity.class);
                                        startActivity(i);
                                        finish();
                                    }
                                });
                            }
                        }
                    });

        }else{
            Toast.makeText(CalificationDriverActivity.this, "Send a calification!", Toast.LENGTH_SHORT).show();
        }
    }


}