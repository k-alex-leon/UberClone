package com.example.uberclone.providers;

import com.example.uberclone.models.HistoryBooking;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class HistoryBookingProvider {

    CollectionReference mCollection;

    public HistoryBookingProvider(){
        mCollection = FirebaseFirestore.getInstance().collection("HistoryBooking");
    }

    public Task<Void> create(HistoryBooking historyBooking){
        return mCollection.document(historyBooking.getIdHistoryBooking()).set(historyBooking);
    }


    public Task<Void> updateCalificationClient(String idHistoryBooking, HistoryBooking historyBooking){
        return mCollection.document(idHistoryBooking).set(historyBooking);
    }

    public Task<Void> updateCalificationDriver(String idHistoryBooking, HistoryBooking historyBooking){

        return mCollection.document(idHistoryBooking).set(historyBooking);
    }

    public Task<DocumentSnapshot> getHistoryBooking(String idHistoryBooking){
        return mCollection.document(idHistoryBooking).get();
    }

    public Query getHistoryByIdClient(String idClient){
        return mCollection.whereEqualTo("idClient", idClient);
    }

    public Query getHistoryByIdDriver(String idDriver){
        return mCollection.whereEqualTo("idDriver", idDriver);
    }
}
