package com.example.uberclone.providers;

import androidx.annotation.NonNull;

import com.example.uberclone.models.Client;
import com.example.uberclone.models.Driver;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DriversProvider {

    private CollectionReference mCollectDriver;

    public DriversProvider(){
        mCollectDriver = FirebaseFirestore.getInstance().collection("Drivers");
    }

    public Task<Void> createDriver(@NonNull Driver driver){
        return mCollectDriver.document(driver.getIdUser()).set(driver);
    }

    public Task<DocumentSnapshot> getUserById(String id){
        return mCollectDriver.document(id).get();
    }

    public Task<Void> updateDriver(Driver driver){
        // si desea agregar mas valores los incluye en el map.put
        Map<String, Object> map = new HashMap<>();
        map.put("username", driver.getUsername());
        map.put("timestamp", driver.getTimestamp());
        return mCollectDriver.document(driver.getIdUser()).update(map);
    }

    public DocumentReference getUser(String id){
        return mCollectDriver.document(id);
    }

    public Task<Void> updateDataCar(Driver driver){
        // si desea agregar mas valores los incluye en el map.put
        Map<String, Object> map = new HashMap<>();
        map.put("vehicleBrand", driver.getVehicleBrand());
        map.put("vehiclePlate", driver.getVehiclePlate());
        return mCollectDriver.document(driver.getIdUser()).update(map);
    }

    public Task<Void> updateImage(String idDriver, String imageUrl){
        return mCollectDriver.document(idDriver).update("imageProfile", imageUrl);
    }
}
