package com.example.uberclone.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthProvider {

    private FirebaseAuth mAuth;

    public AuthProvider(){
        mAuth = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> login(String email, String password){
        return mAuth.signInWithEmailAndPassword(email, password);
    }

   // TODO faltan metodos para ingresar con google

    public Task<AuthResult> register(String email, String password){
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    public FirebaseUser getUserSession(){
        if (mAuth.getCurrentUser() != null){
            return mAuth.getCurrentUser();
        }else{
            return null;
        }
    }

    public String getUid(){
        if (mAuth.getCurrentUser() != null){
            return mAuth.getCurrentUser().getUid();
        }else{
            return null;
        }
    }

    public String getEmail(){
        if (mAuth.getCurrentUser() != null){
            return mAuth.getCurrentUser().getEmail();
        }else{
            return null;
        }
    }

    public void logout(){
        if (mAuth != null){
            mAuth.signOut();
        }else{
            return;
        }
    }

    public boolean existSession() {
        boolean exist = false;
        if (mAuth.getCurrentUser() != null) {
            exist = true;
        }
        return exist;
    }
}
