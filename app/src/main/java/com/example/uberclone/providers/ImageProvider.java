package com.example.uberclone.providers;

import android.content.Context;

import com.example.uberclone.utils.CompressorBitmapImage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Date;

public class ImageProvider {

    StorageReference mStorage;

    public ImageProvider(){
        mStorage = FirebaseStorage.getInstance().getReference();
    }

    public UploadTask saveImage(Context context, File file){
        byte[] imageByte = CompressorBitmapImage.getImage(context,file.getPath(),500,500);
        StorageReference storageReference = mStorage.child(new Date() + ".jpg");
        mStorage = storageReference;
        UploadTask task = storageReference.putBytes(imageByte);
        return task;
    }

    public StorageReference getmStorage(){
        return mStorage;
    }
}
