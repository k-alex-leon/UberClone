package com.example.uberclone.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.activities.client.DetailRequestActivity;
import com.example.uberclone.models.FCMBody;
import com.example.uberclone.models.FCMResponse;
import com.example.uberclone.models.Info;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientBookingProvider;
import com.example.uberclone.providers.ClientsProvider;
import com.example.uberclone.providers.DriversProvider;
import com.example.uberclone.providers.GeofireProvider;
import com.example.uberclone.providers.GoogleApiProvider;
import com.example.uberclone.providers.InfoProvider;
import com.example.uberclone.providers.NotificationProvider;
import com.example.uberclone.providers.TokenProvider;
import com.example.uberclone.utils.DecodePoints;
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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapDriverBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AuthProvider mAuthProvider;
    private DriversProvider mDriversProvider;
    private ClientsProvider mClientsProvider;
    private ClientBookingProvider mClientBookingProvider;
    private TokenProvider mTokenProvider;
    private NotificationProvider mNotificationProvider;
    private InfoProvider mInfoProvider;

    private String carBrand, carPlate;
    private Info mInfo;

    private LatLng mOriginLatLng,mDestinationLatLng;

    private GoogleApiProvider mGoogleApiProvider;

    private List<LatLng> mPolyLineList;
    private PolylineOptions mPolyLineOptions;

    private String mExtraIdClient;
    private TextView mTxtViewClientEmailBooking, mTxtViewClientNameBooking, mTxtVTimeBooking;
    private CircleImageView mImgClientBooking;

    private Button mBtnStarTrip, mBtnFinishTrip;

    @SuppressLint("UseSwitchCompatOrMaterialCode")

    private Boolean mIsConnect = false;
    private Boolean mIsFirstTime = true;
    private Boolean mIsCloseToClient = false;

    private Marker mMarker;
    private LatLng mCurrentLatLng;
    private ValueEventListener mListener;

    private GeofireProvider mGeofireProvider;

    private final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    // para localizar ubicacion del usuario
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFussedLocation;

    // medir la distancia transcurrida
    Location mPreviusLocation = new Location("");
    // validar si el driver ya inicio el viaje
    boolean mRideStart = false;

    double mDistanceInMeters = 1;
    int mMinutes = 0;
    int mSeconds = 0;
    boolean mSecondIsOver = false;
    Handler mHandler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mSeconds++;

            if (!mSecondIsOver){
                mTxtVTimeBooking.setText(mSeconds + " seg");
            }else{
                mTxtVTimeBooking.setText(mMinutes+ " min " + mSeconds +" seg");
            }

            if (mSeconds == 59){
                mSeconds = 0;
                mMinutes++;
                mSecondIsOver = true;
            }
            mHandler.postDelayed(runnable, 1000);
        }
    };

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
                    // al mDIstanceInMeters aumentamos su valor a medida que el driver se mueva
                    if (mRideStart){
                        mDistanceInMeters = mDistanceInMeters + mPreviusLocation.distanceTo(location);
                    }
                    mPreviusLocation = location;

                    // agregando marcador
                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude()))
                            .title("You're position.")
                            .icon(BitmapFromVector(MapDriverBookingActivity.this, R.drawable.icon_car)));

                    //Obtener localizacion de usuario en tiempo real
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(17f)
                                    .build()
                    ));
                    // actualizando ubic de driver en tiempo real
                    updateLocation();

                    if (mIsFirstTime){
                        mIsFirstTime = false;
                        // solicitando datos de viaje del cliente
                        getClientBooking();

                    }
                }
            }
            super.onLocationResult(locationResult);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver_booking);

        mAuthProvider = new AuthProvider();
        mDriversProvider = new DriversProvider();
        mClientsProvider = new ClientsProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mNotificationProvider = new NotificationProvider();
        mInfoProvider = new InfoProvider();

        mGeofireProvider = new GeofireProvider("drivers_working");
        mTokenProvider = new TokenProvider();

        mGoogleApiProvider = new GoogleApiProvider(MapDriverBookingActivity.this);

        // recibimos id de cliente para mostrar info en pantalla
        mExtraIdClient = getIntent().getStringExtra("idClient");
        mTxtViewClientNameBooking = findViewById(R.id.txtVClientNameBooking);
        mTxtViewClientEmailBooking = findViewById(R.id.txtVClientEmailBooking);
        mTxtVTimeBooking = findViewById(R.id.txtVTimeBooking);
        mImgClientBooking = findViewById(R.id.imgCClientBooking);

        mBtnStarTrip = findViewById(R.id.btnStarBooking);

        mBtnFinishTrip = findViewById(R.id.btnFinishBooking);

        mBtnStarTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // si el driver se aproxima al client
                if (mIsCloseToClient){
                    starBooking();
                }else{
                    Toast.makeText(MapDriverBookingActivity.this, "You must be close to the client", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnFinishTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishBooking();
            }
        });

        mFussedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDriverBooking);
        mMapFragment.getMapAsync(this);

        showClientInfo();
        getInfo();
    }

    private void calculateTrip(){
        if (mMinutes == 0){
            mMinutes = 1;
        }
        double priceMin = mMinutes * mInfo.getMin();
        double priceKm = (mDistanceInMeters / 1000) * mInfo.getKm();
        double total = priceMin + priceKm;
        // enviar costo
        mClientBookingProvider.updateCost(mExtraIdClient, total);

        mGeofireProvider.removeLocation(mAuthProvider.getUid());
        Intent i = new Intent(MapDriverBookingActivity.this, CalificationClientActivity.class);
        i.putExtra("idClient", mExtraIdClient);
        i.putExtra("cost", total);
        startActivity(i);
        finish();
    }

    // llamamos los costos de viaje
    private void getInfo() {
        mInfoProvider.getInfo().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    mInfo = documentSnapshot.toObject(Info.class);
                }
            }
        });
    }

    private void starBooking() {
        mRideStart = true;
        mClientBookingProvider.updateStatus(mExtraIdClient, "start");
        mBtnStarTrip.setVisibility(View.GONE);
        mBtnFinishTrip.setBackgroundColor(Color.DKGRAY);
        mBtnFinishTrip.setVisibility(View.VISIBLE);

        // limpia ruta anterior
        mMap.clear();
        // pone nuevo destimo para marcar nueva ruta
        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng)
                .title("Destination")
                .icon(BitmapFromVector(MapDriverBookingActivity.this, R.drawable.icon_destination)));
        drawRoute(mDestinationLatLng);

        sendNotification("Buckle up and let's go! ");

        // iniciando contador cuando el viaje comienza
        mHandler.postDelayed(runnable, 1000);
    }

    private void finishBooking() {
        mRideStart = false;
        sendNotification("Your journey ended!");
        mClientBookingProvider.updateIdHistoryBooking(mExtraIdClient, mAuthProvider.getUid());
        mClientBookingProvider.updateStatus(mExtraIdClient, "finish");

        if (mFussedLocation != null){
            mFussedLocation.removeLocationUpdates(mLocationCallBack);
        }
        if (mHandler != null){
            mHandler.removeCallbacks(runnable);
        }
        // calcula el viaje y termina el activity
        calculateTrip();
    }

    private double getDistanceBetwen(LatLng clientLatLng, LatLng driverLatLng){
        double distance;
        Location clientLocation = new Location("");
        Location driverLocation = new Location("");
        clientLocation.setLatitude(clientLocation.getLatitude());
        clientLocation.setLongitude(clientLocation.getLongitude());

        driverLocation.setLatitude(driverLocation.getLatitude());
        driverLocation.setLongitude(driverLocation.getLongitude());
        distance = clientLocation.distanceTo(driverLocation);

        return distance;
    }

    private void showClientInfo() {
        mClientsProvider.getUserById(mExtraIdClient).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    if (documentSnapshot.contains("username") && documentSnapshot.contains("email")){

                        if (documentSnapshot.contains("imageProfile")){
                            String imgUrl = documentSnapshot.getString("imageProfile");
                            if (imgUrl != null && !imgUrl.isEmpty()){
                                Picasso.get().load(imgUrl).into(mImgClientBooking);
                            }
                        }

                        String nameClient = documentSnapshot.getString("username");
                        String emailClient = documentSnapshot.getString("email");
                        mTxtViewClientNameBooking.setText(nameClient);
                        mTxtViewClientEmailBooking.setText(emailClient);
                    }
                }
            }
        });
    }

    /*
    * METODOS DE UBICACION
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

        startLocation();
    }


    // obtener la info del viaje solicitado por el client
    private void getClientBooking() {
        mClientBookingProvider.getClientBooking(mExtraIdClient).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    String origin = documentSnapshot.getString("origin");
                    // parseamos el contenido de documentSnapshot a un double para evitar error
                    double originLat = documentSnapshot.getDouble("originLat");
                    double originLng = documentSnapshot.getDouble("originLng");

                    String destination = documentSnapshot.getString("destination");

                    double destinationLatLat = documentSnapshot.getDouble("destinationLat");
                    double destinationLatLng = documentSnapshot.getDouble("destinationLng");
                    mMap.addMarker(new MarkerOptions().position(new LatLng(originLat, originLng))
                            .title("Client.")
                            .icon(BitmapFromVector(MapDriverBookingActivity.this, R.drawable.icon_location_user)));

                    mOriginLatLng = new LatLng(originLat, originLng);
                    mDestinationLatLng = new LatLng(destinationLatLat, destinationLatLng);

                    // dibujamos ruta de la ubic del driver a origen de client
                    drawRoute(mOriginLatLng);
                }
            }
        });
    }

    // dibujamos ruta del conductor al cliente
    private void drawRoute(LatLng latLng){
        mGoogleApiProvider.getDirections(mCurrentLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // recibimos respuesta del servidor
                try{
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polyLines = route.getJSONObject("overview_polyline");
                    String points = polyLines.getString("points");

                    mPolyLineList = DecodePoints.decodePoly(points);
                    mPolyLineOptions = new PolylineOptions();
                    mPolyLineOptions.color(Color.BLUE);
                    mPolyLineOptions.width(8f);
                    mPolyLineOptions.startCap(new SquareCap());
                    mPolyLineOptions.jointType(JointType.ROUND);
                    mPolyLineOptions.addAll(mPolyLineList);

                    mMap.addPolyline(mPolyLineOptions);

                    // obteniendo distancia y tiempo
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");


                }catch (Exception e){
                    Log.d("Error", "Error found " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // si falla la consulta
            }
        });
    }

    // cambiar el estado del driver a desconectado
    // para dejar de recibir servicios
    private void disconnect(){

        // eliminamos datos de ubic de la bd
        mGeofireProvider.removeLocation(mAuthProvider.getUid());
        if (mFussedLocation != null){
            mFussedLocation.removeLocationUpdates(mLocationCallBack);
        }
    }

    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (gpsActived()) {
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
                                ActivityCompat.requestPermissions(MapDriverBookingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MapDriverBookingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
            if (!mIsCloseToClient){
                if (mOriginLatLng != null && mCurrentLatLng != null){
                    double distance = getDistanceBetwen(mOriginLatLng, mCurrentLatLng); // METROS
                    if (distance <= 100){
                        mIsCloseToClient = true;
                        Toast.makeText(MapDriverBookingActivity.this, "Approaching the customer!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /*
     *  METODOS PARA ENVIAR NOTI
     * */
    private void sendNotification(String status) {
        mTokenProvider.getToken(mExtraIdClient).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.exists() && documentSnapshot.contains("token")){
                    String token = documentSnapshot.getString("token");
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "REQUEST STATUS");
                    map.put("body", status);


                    FCMBody fcmBody = new FCMBody(token,"high" , "4500s", map);
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null){
                                if (response.body().getSuccess() != 1){
                                    Toast.makeText(MapDriverBookingActivity.this, "Notification error.", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(MapDriverBookingActivity.this, "Notification error.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Toast.makeText(MapDriverBookingActivity.this, "Notification error", Toast.LENGTH_SHORT).show();
                            Log.d("ERROR", "Error: " + t.getMessage());
                        }
                    });
                }else{
                    Toast.makeText(MapDriverBookingActivity.this, "Client without token.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
}