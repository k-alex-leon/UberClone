package com.example.uberclone.activities.driver;

import androidx.annotation.Nullable;
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
import com.example.uberclone.providers.ClientsProvider;
import com.example.uberclone.providers.HistoryBookingProvider;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;

public class CalificationClientActivity extends AppCompatActivity {

    private ClientBookingProvider mClientBookingProvider;
    private ClientsProvider mClientsProvider;
    private AuthProvider mAuthProvider;

    private HistoryBooking mHistoryBooking;
    private HistoryBookingProvider mHistoryBookingProvider;

    private TextView mtxtVClientName, mTxtVOrigin,
                        mTxtVDestination,mTxtCostTrip;

    private RatingBar mRatinBarClientCalification;

    private Button mBtnSendClientCalification;

    private String mExtraIdClient, mIdHistoryBooking;
    private double mExtraCost = 0;

    private float mCalification = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calification_client);

        mClientBookingProvider = new ClientBookingProvider();
        mClientsProvider = new ClientsProvider();
        mHistoryBookingProvider = new HistoryBookingProvider();
        mAuthProvider = new AuthProvider();

        mtxtVClientName = findViewById(R.id.txtVClientNameCalification);
        mTxtVOrigin = findViewById(R.id.txtVOriginClientCalification);
        mTxtVDestination = findViewById(R.id.txtVDestinationClientCalification);
        mTxtCostTrip = findViewById(R.id.txtVCostTripDriver);

        mRatinBarClientCalification = findViewById(R.id.ratingClientCalification);
        mRatinBarClientCalification.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float calification, boolean fromUser) {
                mCalification = calification;
            }
        });

        mExtraCost = getIntent().getDoubleExtra("cost", 0);
        mExtraIdClient = getIntent().getStringExtra("idClient");
        // damos formato al cost para evitar los decimales
        mTxtCostTrip.setText("$ " + String.format("%.1f", mExtraCost));

        // mostrar info en pantalla
        getClientBooking();

        mBtnSendClientCalification = findViewById(R.id.btnCalificationClient);
        mBtnSendClientCalification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calificate();
            }
        });

    }

    private void getClientBooking(){
        mClientBookingProvider.getClientBooking(mExtraIdClient).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){

                    ClientBooking clientBooking = documentSnapshot.toObject(ClientBooking.class);
                    mHistoryBooking = new HistoryBooking(
                            mExtraIdClient+mAuthProvider.getUid(),
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

                    mTxtVOrigin.setText(documentSnapshot.getString("origin"));
                    mTxtVDestination.setText(documentSnapshot.getString("destination"));

                    mClientsProvider.getUserById(mExtraIdClient).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot docSnap) {
                            if (docSnap.exists()){
                                mtxtVClientName.setText(docSnap.getString("username"));
                            }
                        }
                    });

                }
            }
        });
    }



    private void calificate() {
        if (mCalification > 0 ){

            mHistoryBooking.setCalificationClient(mCalification);
            mHistoryBooking.setTimestamp(new Date().getTime());
            mIdHistoryBooking = mHistoryBooking.getIdHistoryBooking();
            mHistoryBookingProvider.getHistoryBooking(mIdHistoryBooking)
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()){

                                mHistoryBooking.setCalificationDriver(documentSnapshot.getDouble("calificationDriver"));
                                // actualiza los datos del objeto en caso de ya estar creado
                                mHistoryBookingProvider.updateCalificationClient(mIdHistoryBooking , mHistoryBooking)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(CalificationClientActivity.this, "Calification send!", Toast.LENGTH_SHORT).show();
                                                Intent i = new Intent(CalificationClientActivity.this, MapDriverActivity.class);
                                                startActivity(i);
                                                finish();
                                            }
                                        });

                            }else{
                                // si no existe crea los datos en la bd
                                mHistoryBookingProvider.create(mHistoryBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(CalificationClientActivity.this, "Calification send!", Toast.LENGTH_SHORT).show();
                                        Intent i = new Intent(CalificationClientActivity.this, MapDriverActivity.class);
                                        startActivity(i);
                                        finish();
                                    }
                                });
                            }
                        }
                    });

        }else{
            Toast.makeText(CalificationClientActivity.this, "Send a calification!", Toast.LENGTH_SHORT).show();
        }
    }

}