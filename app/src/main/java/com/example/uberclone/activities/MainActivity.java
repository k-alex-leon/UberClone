package com.example.uberclone.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.uberclone.R;
import com.example.uberclone.activities.client.MapClientActivity;
import com.example.uberclone.activities.driver.MapDriverActivity;
import com.example.uberclone.models.Client;
import com.example.uberclone.models.Driver;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientsProvider;
import com.example.uberclone.providers.DriversProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    Button btnDriver, btnClient;
    String email, password, confirmPass, username, carBrand, carPlate;
    AuthProvider mAuthProvider;
    DriversProvider mDriversProvider;
    ClientsProvider mClientsProvider;

    SharedPreferences mPref;


    // dialog de carga
    AlertDialog dialogProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPref.edit();

        mAuthProvider = new AuthProvider();
        mDriversProvider = new DriversProvider();
        mClientsProvider = new ClientsProvider();

        btnDriver = findViewById(R.id.btnDriver);
        btnClient = findViewById(R.id.btnClient);

        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("user", "client");
                editor.apply();
                showSignInDialog();
            }
        });

        btnDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("user", "driver");
                editor.apply();
                showSignInDialog();
            }
        });

        progressCharge();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mAuthProvider.getUserSession() != null){
            String user = mPref.getString("user", "");
            if (user.equals("client")){
                Intent i = new Intent(MainActivity.this, MapClientActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }else if (user.equals("driver")){
                Intent i = new Intent(MainActivity.this, MapDriverActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        }
    }


    private void showSignInDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.select_option_auth, null);
        //obteniendo referencias del layout
        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnRegister = view.findViewById(R.id.btnRegister);
        ImageView imgClose = view.findViewById(R.id.imgCloseDialog);
        LottieAnimationView selectAnimation = view.findViewById(R.id.selectAnimation);

        builder.setView(view);
        // pasamos el builder a dialog
        AlertDialog dialog = builder.create();
        // mostramos el dialog en pantalla
        dialog.show();

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showLoginDialog();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showRegisterDialog();
            }
        });
    }

    /*
     * LOGIN DE USUARIO
     * */

    private void showLoginDialog() {
        AlertDialog.Builder builderLogin = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.login_dialog, null);

        // obteniendo referencias
        TextInputEditText txtEmail = view.findViewById(R.id.txtInputEmail);
        TextInputEditText txtPass = view.findViewById(R.id.txtInputPass);
        Button btnStartLogin = view.findViewById(R.id.btnStartLogin);
        ImageView imgClose = view.findViewById(R.id.imgCloseLoginDialog);

        //pasando los datos a mostrar
        builderLogin.setView(view);

        AlertDialog dialogLogin = builderLogin.create();
        dialogLogin.show();

        btnStartLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                email = txtEmail.getText().toString();
                password = txtPass.getText().toString();

                validateLogin(txtEmail, email, password);
            }
        });

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogLogin.dismiss();
            }
        });

    }

    private void validateLogin(TextInputEditText txtEmail,
                               @NonNull String email, String password) {

        if (!email.isEmpty() && !password.isEmpty()){
            if (isEmailValid(email)){

                    dialogProgress.show();
                    loginUser(email, password);

            }else{
                txtEmail.setError("Invalid email.");
            }
        }else{
            Toast.makeText(this, "Cannot be empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser(String email, String password) {

        mAuthProvider.login(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                dialogProgress.dismiss();
                // validando que la tarea se completara
                if (task.isSuccessful()){

                    Toast.makeText(MainActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();

                    String user = mPref.getString("user", "");
                    if (user.equals("driver")){
                        Intent i = new Intent(MainActivity.this, MapDriverActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);

                    }
                    else if(user.equals("client")){

                        Intent i = new Intent(MainActivity.this, MapClientActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);

                    }
                }else{
                    Toast.makeText(MainActivity.this, "Wrong email or password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*
     * REGISTRO DE USUARIO
     * */

    private void showRegisterDialog() {
        AlertDialog.Builder builderRegister = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.register_dialog, null);
        TextInputEditText txtUsername = view.findViewById(R.id.txtRegisterUsername);
        TextInputEditText txtEmail = view.findViewById(R.id.txtRegisterEmail);
        final TextInputEditText txtPass = view.findViewById(R.id.txtRegisterPass);
        TextInputEditText txtConfirmPass = view.findViewById(R.id.txtRegisterConfirmPass);
        ImageView imgClose = view.findViewById(R.id.imgCloseRegisterDialog);
        Button btnRegister = view.findViewById(R.id.btnStartRegister);

        builderRegister.setView(view);

        AlertDialog dialogRegister = builderRegister.create();
        dialogRegister.show();

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogRegister.dismiss();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                username = txtUsername.getText().toString();
                email = txtEmail.getText().toString();
                password = txtPass.getText().toString();
                confirmPass = txtConfirmPass.getText().toString();
                dialogRegister.dismiss();
                validateRegistation(username,email,password,confirmPass, txtConfirmPass, txtEmail, txtUsername);
            }
        });
    }

    private void validateRegistation(@NonNull String username, String email,
                                     String password, String confirmPass,
                                     TextInputEditText txtConfirmPass,
                                     TextInputEditText txtEmail,
                                     TextInputEditText txtUsername) {

        if (!username.isEmpty() && !email.isEmpty()
                && !password.isEmpty() && !confirmPass.isEmpty()){

            if (password.equals(confirmPass)){

                if (password.length() >= 8){

                    if (username.length() <= 30 && username.length() >= 5){

                        if(isEmailValid(email)){
                            dialogProgress.show();
                            registerUser(username,email, password);
                        }else{
                            txtEmail.setError("Invalid email.");
                        }

                    }else{
                        txtUsername.setError("5 to 30 characters");
                    }


                }else{
                    txtConfirmPass.setError("Password too short (min 8).");
                }

            }else{
                txtConfirmPass.setError("Password do not match.");
            }
        }else{
            Toast.makeText(this, "Cannot be empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser(String username, String email, String password) {

        String user = mPref.getString("user", "");
       if (user.equals("driver")){

           mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
               @Override
               public void onComplete(@NonNull Task<AuthResult> task) {
                   if (task.isSuccessful()){

                       // agregando datos al modelo
                       Driver driver = new Driver();
                       driver.setIdUser(mAuthProvider.getUid());
                       driver.setEmail(email);
                       driver.setUsername(username);
                       driver.setTimestamp(new Date().getTime());

                       mDriversProvider.createDriver(driver).addOnCompleteListener(new OnCompleteListener<Void>() {
                           @Override
                           public void onComplete(@NonNull Task<Void> task) {
                               if (task.isSuccessful()){

                                   Toast.makeText(MainActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                                   Intent i = new Intent(MainActivity.this, MapDriverActivity.class);
                                   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                   startActivity(i);

                               }else{
                                   dialogProgress.dismiss();
                                   Toast.makeText(MainActivity.this, "Error creating user", Toast.LENGTH_SHORT).show();
                               }
                           }
                       });

                   }else{
                       Toast.makeText(MainActivity.this, "Error creating user.", Toast.LENGTH_SHORT).show();
                   }
               }
           });

       }
       else if (user.equals("client")){

           mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
               @Override
               public void onComplete(@NonNull Task<AuthResult> task) {
                   if (task.isSuccessful()){
                       // agregando datos al modelo
                       Client client = new Client();
                       client.setIdUser(mAuthProvider.getUid());
                       client.setEmail(email);
                       client.setUsername(username);
                       client.setTimestamp(new Date().getTime());

                       // pasando modelo para agregar a la coleccion usuarios
                       mClientsProvider.createClient(client).addOnCompleteListener(new OnCompleteListener<Void>() {
                           @Override
                           public void onComplete(@NonNull Task<Void> task) {
                               dialogProgress.dismiss();
                               if (task.isSuccessful()){
                                   Toast.makeText(MainActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                                   Intent i = new Intent(MainActivity.this, MapClientActivity.class);
                                   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                   startActivity(i);
                               }else{
                                   dialogProgress.dismiss();
                                   Toast.makeText(MainActivity.this, "Error sending data. Try again!", Toast.LENGTH_SHORT).show();
                               }
                           }
                       });

                   }else{
                       Toast.makeText(MainActivity.this, "Error crating user.", Toast.LENGTH_SHORT).show();
                   }
               }
           });
       }
    }


    // Animacion de carga
    public void progressCharge(){
        AlertDialog.Builder builderProgress = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.progress, null);
        builderProgress.setCancelable(false);
        builderProgress.setView(view);

        dialogProgress = builderProgress.create();

    }

    // validacion de un correo
    private boolean isEmailValid(String emailUser) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(emailUser);
        return matcher.matches();
    }


}