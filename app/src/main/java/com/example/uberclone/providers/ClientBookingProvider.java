package com.example.uberclone.providers;

import com.example.uberclone.models.ClientBooking;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ClientBookingProvider {

    CollectionReference mCollection;

    public ClientBookingProvider(){
        mCollection = FirebaseFirestore.getInstance().collection("ClientBooking");
    }

    public Task<Void> create(ClientBooking clientBooking){
        return mCollection.document(clientBooking.getIdClient()).set(clientBooking);
    }

    public Task<Void> updateStatus(String idClient, String status){
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        return mCollection.document(idClient).update(map);
    }

    public Task<Void> updateIdHistoryBooking(String idClientBooking, String idDriver){
        return mCollection.document(idClientBooking).update("idHistoryBooking", idClientBooking + idDriver);
    }

    public Task<Void> updateCost(String idCLientBooking, double cost){
        return mCollection.document(idCLientBooking).update("cost", cost);
    }

    // escuchar los cambios que se realicen en tiempo real
    public DocumentReference getStatus(String idClient) {
    return mCollection.document(idClient);
    }

    public Task<DocumentSnapshot> getClientBooking(String idClient){
        return mCollection.document(idClient).get();
    }

    public void delete(String idClientBooking){
        mCollection.document(idClientBooking).delete();
    }
}
