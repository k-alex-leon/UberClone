package com.example.uberclone.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.example.uberclone.R;
import com.example.uberclone.adapters.HistoryAdapter;
import com.example.uberclone.models.HistoryBooking;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.HistoryBookingProvider;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryActivity extends AppCompatActivity {

    private AuthProvider mAuthProvider;
    private RecyclerView mRecyclerViewHistory;
    private HistoryAdapter mHistoryAdapter;
    private HistoryBookingProvider mHistoryBookingProvider;

    private CircleImageView mImgGoBack;
    private ListenerRegistration mListenerRegistration;

    Query query = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mAuthProvider = new AuthProvider();
        mHistoryBookingProvider = new HistoryBookingProvider();

        mImgGoBack = findViewById(R.id.imgVHistoryGoBack);
        mImgGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRecyclerViewHistory = findViewById(R.id.recyclerVHistory);
        //LinearLayoutManager = muestra las cardview una debajo de la otra
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mRecyclerViewHistory.setLayoutManager(linearLayoutManager);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mListenerRegistration = mHistoryBookingProvider.getHistoryByIdClient(mAuthProvider.getUid()).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                //si no encuentra el idClient envia data de driver
                if (value.isEmpty()){
                    query = mHistoryBookingProvider.getHistoryByIdDriver(mAuthProvider.getUid());

                    FirestoreRecyclerOptions<HistoryBooking> options =
                            new FirestoreRecyclerOptions.Builder<HistoryBooking>()
                                    .setQuery(query, HistoryBooking.class)
                                    .build();

                    mHistoryAdapter = new HistoryAdapter(options, HistoryActivity.this);
                    mRecyclerViewHistory.setAdapter(mHistoryAdapter);
                    // escuchando los cambios de la bd
                    mHistoryAdapter.startListening();

                }else{
                    query = mHistoryBookingProvider.getHistoryByIdClient(mAuthProvider.getUid());

                    FirestoreRecyclerOptions<HistoryBooking> options =
                            new FirestoreRecyclerOptions.Builder<HistoryBooking>()
                                    .setQuery(query, HistoryBooking.class)
                                    .build();

                    mHistoryAdapter = new HistoryAdapter(options, HistoryActivity.this);
                    mRecyclerViewHistory.setAdapter(mHistoryAdapter);
                    // escuchando los cambios de la bd
                    mHistoryAdapter.startListening();

                }
            }
        });


    }

    @Override
    protected void onStop() {
        super.onStop();
        mHistoryAdapter.stopListening();
        if (mListenerRegistration != null){
            mListenerRegistration.remove();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHistoryAdapter.stopListening();
        if (mListenerRegistration != null){
            mListenerRegistration.remove();
        }
    }
}