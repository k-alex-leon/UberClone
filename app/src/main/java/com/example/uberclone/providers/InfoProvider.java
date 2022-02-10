package com.example.uberclone.providers;


import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class InfoProvider {

    private CollectionReference mCollection;

    public InfoProvider(){
        mCollection = FirebaseFirestore.getInstance().collection("Info");
    }

    public Task<DocumentSnapshot> getInfo(){
        return mCollection.document("Cost").get();
    }
}
