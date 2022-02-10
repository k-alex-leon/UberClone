package com.example.uberclone.providers;

import com.example.uberclone.models.Client;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ClientsProvider {
    private CollectionReference mCollectClient;

    public ClientsProvider(){
        mCollectClient = FirebaseFirestore.getInstance().collection("Clients");
    }

    public Task<Void> createClient(Client client){
        return mCollectClient.document(client.getIdUser()).set(client);
    }

    public Task<DocumentSnapshot> getUserById(String id){
        return mCollectClient.document(id).get();
    }

    public DocumentReference getUser(String id){
        return mCollectClient.document(id);
    }


    public Task<Void> updateClient(Client client){
        // si desea agregar mas valores los incluye en el map.put
        Map<String, Object> map = new HashMap<>();
        map.put("username", client.getUsername());
        map.put("timestamp", client.getTimestamp());
        return mCollectClient.document(client.getIdUser()).update(map);
    }

    public Task<Void> updateImage(String idClient, String imageUrl){
        return mCollectClient.document(idClient).update("imageProfile", imageUrl);
    }
}
