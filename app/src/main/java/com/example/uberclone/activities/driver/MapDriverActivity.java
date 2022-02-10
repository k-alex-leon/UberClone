package com.example.uberclone.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.activities.HistoryActivity;
import com.example.uberclone.activities.MainActivity;
import com.example.uberclone.activities.ProfileActivity;
import com.example.uberclone.models.Driver;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.DriversProvider;
import com.example.uberclone.providers.GeofireProvider;
import com.example.uberclone.providers.TokenProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

public class MapDriverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AuthProvider mAuthProvider;
    private DriversProvider mDriversProvider;
    private TokenProvider mTokenProvider;
    private String carBrand, carPlate;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Button btnConnect;
    private Boolean mIsConnect = false;

    private AlertDialog dialogRegisterCar;

    private Marker mMarker;
    private LatLng mCurrentLatLng;
    private ValueEventListener mListener;

    private GeofireProvider mGeofireProvider;

    Toolbar mToolbar;

    private final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    // para localizar ubicacion del usuario
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFussedLocation;

    LocationCallback mLocationCallBack = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    locationResult.getLastLocation();
                    // obteniendo datos de ubicacion
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (mMarker != null) {
                        mMarker.remove();
                    }
                    // agregando marcador
                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude()))
                            .title("You're position.")
                            .icon(BitmapFromVector(MapDriverActivity.this, R.drawable.icon_car)));

                    //Obtener localizacion de usuario en tiempo real
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(17f)
                                    .build()
                    ));
                    // actualizando ubic de driver en tiempo real
                    updateLocation();
                }
            }
            super.onLocationResult(locationResult);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver);

        mAuthProvider = new AuthProvider();
        mDriversProvider = new DriversProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();

        mFussedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDriver);
        mMapFragment.getMapAsync(this);

        // activar o desactivar la disponibilidad del driver
        btnConnect = findViewById(R.id.btnDriverConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsConnect){
                    disconnect();
                }else{
                    startLocation();
                }
            }
        });

        // config de toolbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        //quitar titulo de toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // validacion de datos en la bd
        checkDataCarExist();

        // creando token
        generateToken();

        // consultando el estado de driver
        isDriverWorking();
    }

    private void isDriverWorking() {
        // addValueEventListener() permite escuchar cambios en tiempo real
        mListener = mGeofireProvider.isDriverWorking(mAuthProvider.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    disconnect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null){
            mGeofireProvider.isDriverWorking(mAuthProvider.getUid()).removeEventListener(mListener);
        }
    }

    /*
     *  METODOS PARA ICONO DE UBICACION
     * */
    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /*
     *  METODOS PARA MAPS DE GOOOGLE
     * */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // mostramos el tipo de mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(false);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);

    }

    // cambiar el estado del driver a desconectado
    // para dejar de recibir servicios
    private void disconnect(){
        btnConnect.setText("INNACTIVE");
        btnConnect.setBackgroundColor(Color.GRAY);
        mIsConnect = false;
        // eliminamos datos de ubic de la bd
        mGeofireProvider.removeLocation(mAuthProvider.getUid());
        if (mFussedLocation != null){
            mFussedLocation.removeLocationUpdates(mLocationCallBack);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (gpsActived()) {
                    btnConnect.setText("AVAILABLE");
                    btnConnect.setBackgroundColor(Color.parseColor("#673AB7"));
                    mIsConnect = true;
                    mFussedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                }
                else {
                    showAlertDialogNoGPS();
                }
            }
            else {
                checkLocationPermissions();
            }
        } else {
            if (gpsActived()) {
                mFussedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
            }
            else {
                showAlertDialogNoGPS();
            }
        }
    }

    // validando los permisos de ubicacion del usuario
    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning!")
                        .setMessage("This app needs gps access to work.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MapDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MapDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    // si el permiso de ubicacion ha sido aceptado
    // pasamos la info al map
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (gpsActived()) {
                        mFussedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                    } else {
                        showAlertDialogNoGPS();
                    }
                } else {
                    checkLocationPermissions();
                }
            } else {
                checkLocationPermissions();
            }
        }
    }

    // validacion del GPS activo
    private boolean gpsActived() {
        boolean isActive = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isActive = true;
        }
        return isActive;
    }


    // alert para activar el gps del usuario
    private void showAlertDialogNoGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning!")
                .setMessage("Please activate the gps to continue")
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SETTINGS_REQUEST_CODE);
                    }
                }).create().show();
    }

    // si la respuesta del alertdialog resulta en la activacion del gps
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActived()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mFussedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
        }
        else {
            showAlertDialogNoGPS();
        }
    }


    private void updateLocation() {
        if (mAuthProvider.existSession() && mCurrentLatLng != null) {
            mGeofireProvider.saveLocation(mAuthProvider.getUid(), mCurrentLatLng);
        }
    }



    /*
     *  VERIFICACION DE DATOS DE VEHICULO
     * */
    private void checkDataCarExist() {
        mDriversProvider.getUserById(mAuthProvider.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.contains("vehicleBrand")) {
                    String brand = documentSnapshot.getString("vehicleBrand");
                    if (brand == null) {
                        showDialogDataCar();
                    }

                }
            }
        });
    }

    private void showDialogDataCar() {

        AlertDialog.Builder builderRegisterCar = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.register_car, null);
        TextInputEditText txtCarBrand = view.findViewById(R.id.txtVehicleBrand);
        TextInputEditText txtCarPlate = view.findViewById(R.id.txtVehiclePlate);
        Button btnRegisterCar = view.findViewById(R.id.btnRegisterCar);

        builderRegisterCar.setView(view);
        builderRegisterCar.setCancelable(false);

        dialogRegisterCar = builderRegisterCar.create();
        dialogRegisterCar.setCancelable(false);
        dialogRegisterCar.show();

        btnRegisterCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carBrand = txtCarBrand.getText().toString();
                carPlate = txtCarPlate.getText().toString();

                if (!carBrand.isEmpty() && !carPlate.isEmpty()) {
                    Driver driver = new Driver();
                    driver.setIdUser(mAuthProvider.getUid());
                    driver.setVehicleBrand(carBrand.toUpperCase(Locale.ROOT));
                    driver.setVehiclePlate(carPlate.toUpperCase(Locale.ROOT));

                    updateDataDriver(driver);
                } else {
                    if (carBrand.isEmpty()) {
                        txtCarBrand.setError("Cannot be empty!");
                    } else {
                        txtCarPlate.setError("Cannot be empty!");
                    }
                }

            }
        });

    }

    private void updateDataDriver(Driver driver) {
        mDriversProvider.updateDataCar(driver).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    dialogRegisterCar.dismiss();
                } else {
                    Toast.makeText(MapDriverActivity.this, "Data saved.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /*
     *  METODOS TOOLBAR
     * */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_profile){
            Intent i = new Intent(MapDriverActivity.this, ProfileActivity.class);
            startActivity(i);
        }
        if (item.getItemId() == R.id.action_history){
            Intent i = new Intent(MapDriverActivity.this, HistoryActivity.class);
            startActivity(i);
        }
        if (item.getItemId() == R.id.action_logout){
            logout();
        }
        return super.onOptionsItemSelected(item);
    }

    // cerrar sesion
    private void logout() {
        disconnect();
        mAuthProvider.logout();
        Intent i = new Intent(MapDriverActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    // generando token cuando el user entra el activ
    public void generateToken(){
        mTokenProvider.create(mAuthProvider.getUid());
    }
}