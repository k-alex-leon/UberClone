package com.example.uberclone.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.models.Client;
import com.example.uberclone.models.Driver;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientsProvider;
import com.example.uberclone.providers.DriversProvider;
import com.example.uberclone.providers.ImageProvider;
import com.example.uberclone.utils.FileUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.io.File;
import java.util.Date;

public class ProfileActivity extends AppCompatActivity {

    private ClientsProvider mClientProvider;
    private DriversProvider mDriverProvider;
    private AuthProvider mAuthProvider;
    private ImageProvider mImageProvider;

    private TextInputEditText mTEtUsername;
    private ImageView mImgVEditPhoto, mImgVEditName,
            mImgVCloseEditName, mImgVSendName, mImgVPhotoProfile, mImgVGoBack;

    private TextView mTxtVUsername, mTxtVEmail, mTxtVBrand, mTxtVPlate;
    private LinearLayout mLnLVehicle;

    private File mImageFile;
    private String mImageUrl, mImageProfile, mUsername;
    private final int GALLERY_REQUEST = 1;

    private Boolean mIsClient;

    ListenerRegistration mListenerClient, mListenerDriver;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mClientProvider = new ClientsProvider();
        mDriverProvider = new DriversProvider();
        mAuthProvider = new AuthProvider();
        mImageProvider = new ImageProvider();

        mTxtVUsername = findViewById(R.id.txtVUsername);
        mTxtVEmail = findViewById(R.id.txtVEmail);
        mTxtVBrand = findViewById(R.id.txtVBrand);
        mTxtVPlate = findViewById(R.id.txtVPLate);

        mLnLVehicle = findViewById(R.id.LnLVehicle);
        mTEtUsername = findViewById(R.id.tEtEditUsername);
        mImgVEditName = findViewById(R.id.imgVEditName);
        mImgVEditPhoto = findViewById(R.id.imgVEditPhoto);
        mImgVPhotoProfile = findViewById(R.id.imgVPhotoProfile);
        mImgVSendName = findViewById(R.id.imgVSendName);
        mImgVCloseEditName = findViewById(R.id.imgVCloseEditName);

        mImgVGoBack = findViewById(R.id.imgVGoBack);

        mImgVGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getUser();
    }

    private void getUser() {

        mListenerClient = mClientProvider.getUser(mAuthProvider.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value.exists()){
                    mIsClient = true;

                    if (value.contains("username")){
                        String userName = value.getString("username");
                        mTxtVUsername.setText(userName);
                    }

                    if (value.contains("email")){
                        String email = value.getString("email");
                        mTxtVEmail.setText(email);
                    }

                    mImgVEditPhoto.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openGallery();
                        }
                    });

                    mImgVEditName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditName();
                        }
                    });

                    // image profile
                    if (value.contains("imageProfile")){
                        mImageProfile = value.getString("imageProfile");
                        if (mImageProfile != null){
                            if(!mImageProfile.isEmpty()){
                                Picasso.get().load(mImageProfile).into(mImgVPhotoProfile);
                            }
                        }
                    }
                }else{

                    mListenerDriver = mDriverProvider.getUser(mAuthProvider.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (value.exists()){
                                mIsClient = false;

                                mLnLVehicle.setVisibility(View.VISIBLE);
                                if (value.contains("username")){
                                    String userName = value.getString("username");
                                    mTxtVUsername.setText(userName);
                                }

                                if (value.contains("email")){
                                    String email = value.getString("email");
                                    mTxtVEmail.setText(email);
                                }

                                if (value.contains("vehicleBrand")){
                                    String brand = value.getString("vehicleBrand");
                                    mTxtVBrand.setText(brand);
                                }

                                if (value.contains("vehiclePlate")){
                                    String plate = value.getString("vehiclePlate");
                                    mTxtVPlate.setText(plate);
                                }

                                mImgVEditPhoto.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        openGallery();
                                    }
                                });

                                mImgVEditName.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        EditName();
                                    }
                                });

                                // image profile
                                if (value.contains("imageProfile")){
                                    mImageProfile = value.getString("imageProfile");
                                    if (mImageProfile != null){
                                        if(!mImageProfile.isEmpty()){
                                            Picasso.get().load(mImageProfile).into(mImgVPhotoProfile);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void EditName() {
            mTxtVUsername.setVisibility(View.GONE);
            mImgVCloseEditName.setVisibility(View.VISIBLE);
            mTEtUsername.setVisibility(View.VISIBLE);
            mImgVSendName.setVisibility(View.VISIBLE);
            mImgVEditName.setVisibility(View.GONE);

            mUsername = mTxtVUsername.getText().toString();

            mTEtUsername.setText(mUsername);

                mImgVCloseEditName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mImgVCloseEditName.setVisibility(View.GONE);
                        mTxtVUsername.setVisibility(View.VISIBLE);
                        mTEtUsername.setVisibility(View.GONE);
                        mImgVSendName.setVisibility(View.GONE);
                        mImgVEditName.setVisibility(View.VISIBLE);

                    }
                });

                mImgVSendName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mImgVCloseEditName.setVisibility(View.GONE);
                        mTxtVUsername.setVisibility(View.VISIBLE);
                        mTEtUsername.setVisibility(View.GONE);
                        mImgVSendName.setVisibility(View.GONE);
                        mImgVEditName.setVisibility(View.VISIBLE);

                        if (mIsClient){
                            updateNameClient();
                        }else{
                            updateNameDriver();
                        }


                    }
                });

    }

    private void updateNameDriver() {
        mUsername = mTEtUsername.getText().toString();
        Driver driver = new Driver();
        driver.setIdUser(mAuthProvider.getUid());
        driver.setUsername(mUsername);
        driver.setTimestamp(new Date().getTime());
        mDriverProvider.updateDriver(driver).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ProfileActivity.this, "Name send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNameClient() {
        mUsername = mTEtUsername.getText().toString();
        Client client = new Client();
        client.setIdUser(mAuthProvider.getUid());
        client.setUsername(mUsername);
        client.setTimestamp(new Date().getTime());
        mClientProvider.updateClient(client).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ProfileActivity.this, "Name send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // validando si el user elige una img
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK){
            try {
                mImageFile = FileUtil.from(this, data.getData());
                // pasa la img select a la interface
                mImgVPhotoProfile.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath()));
                imageSelected();
            }catch (Exception e){
                Log.d("ERROR", "Mensaje" + e.getMessage());
            }
        }
    }

    private void imageSelected() {
        mImageProvider.saveImage(this, mImageFile)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                           mImageProvider.getmStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                               @Override
                               public void onSuccess(Uri uri) {
                                   mImageUrl = uri.toString();

                                   if (mIsClient){
                                       mClientProvider.updateImage(mAuthProvider.getUid(), mImageUrl)
                                               .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                   @Override
                                                   public void onComplete(@NonNull Task<Void> task) {
                                                       if (task.isSuccessful()){
                                                           Toast.makeText(ProfileActivity.this, "Image saved", Toast.LENGTH_SHORT).show();
                                                       }else{
                                                           Toast.makeText(ProfileActivity.this, "Image error", Toast.LENGTH_SHORT).show();
                                                       }
                                                   }
                                               });
                                   }else{
                                       mDriverProvider.updateImage(mAuthProvider.getUid(), mImageUrl)
                                               .addOnCompleteListener(new OnCompleteListener<Void>() {
                                           @Override
                                           public void onComplete(@NonNull Task<Void> task) {
                                               if (task.isSuccessful()){
                                                   Toast.makeText(ProfileActivity.this, "Image saved", Toast.LENGTH_SHORT).show();
                                               }else{
                                                   Toast.makeText(ProfileActivity.this, "Image error", Toast.LENGTH_SHORT).show();
                                               }
                                           }
                                       });
                                   }



                               }
                           });
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListenerClient != null){
            mListenerClient.remove();
        }
        if (mListenerDriver != null){
            mListenerDriver.remove();
        }
    }
}