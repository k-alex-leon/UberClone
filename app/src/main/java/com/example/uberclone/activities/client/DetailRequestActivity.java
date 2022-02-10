package com.example.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.models.ClientBooking;
import com.example.uberclone.models.FCMBody;
import com.example.uberclone.models.FCMResponse;
import com.example.uberclone.models.Info;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientBookingProvider;
import com.example.uberclone.providers.GeofireProvider;
import com.example.uberclone.providers.GoogleApiProvider;
import com.example.uberclone.providers.InfoProvider;
import com.example.uberclone.providers.NotificationProvider;
import com.example.uberclone.providers.TokenProvider;
import com.example.uberclone.utils.DecodePoints;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailRequestActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    Toolbar mToolbar;

    private double mExtraOriginLat, mExtraOriginLng;
    private double mExtraDestinationLat, mExtraDestinationLng;
    private String mExtraOrigin, mExtraDestination;

    private String mDistanceTxt, mDurationTxt;

    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private GoogleApiProvider mGoogleApiProvider;

    private List<LatLng> mPolyLineList;
    private PolylineOptions mPolyLineOptions;

    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;
    private ClientBookingProvider mClientBookingProvider;
    private AuthProvider mAuthProvider;
    private InfoProvider mInfoProvider;

    private TextView mTxtVOrigin, mTxtVDestination,
            mTxtVTime, mTxtVDistance, mTxtVCost;

    private Button mBtnRequestDriver;

    private GeofireProvider mGeofireProvider;
    private double mRadius = 0.1;

    private boolean mDriverFound = false;
    private String mIdDriverFound;
    private LatLng mDriverFoundLatLng;

    private ListenerRegistration listenerRegistration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_request);

        mAuthProvider = new AuthProvider();
        mInfoProvider = new InfoProvider();

        mGoogleApiProvider = new GoogleApiProvider(DetailRequestActivity.this);
        mGeofireProvider = new GeofireProvider("active_drivers");

        mNotificationProvider = new NotificationProvider();
        mTokenProvider = new TokenProvider();

        mClientBookingProvider = new ClientBookingProvider();
        // config de toolbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //quitar titulo de toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapClient);
        mMapFragment.getMapAsync(this);

        mTxtVOrigin = findViewById(R.id.txtViewOrigin);
        mTxtVDestination = findViewById(R.id.txtViewDestination);
        mTxtVTime = findViewById(R.id.txtViewTime);
        mTxtVDistance = findViewById(R.id.txtViewDistance);
        mTxtVCost = findViewById(R.id.txtViewCostDetail);
        mBtnRequestDriver = findViewById(R.id.btnRequestNow);

        mBtnRequestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWaitDriverAlertDialog();
            }
        });

        // recibiendo datos del intent
        mExtraOriginLat = getIntent().getDoubleExtra("origin_lat", 0);
        mExtraOriginLng = getIntent().getDoubleExtra("origin_lng", 0);
        mExtraDestinationLat = getIntent().getDoubleExtra("destination_lat", 0);
        mExtraDestinationLng = getIntent().getDoubleExtra("destination_lng", 0);
        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");

        mTxtVOrigin.setText(mExtraOrigin);
        mTxtVDestination.setText(mExtraDestination);

        mOriginLatLng = new LatLng(mExtraOriginLat, mExtraOriginLng);
        mDestinationLatLng = new LatLng(mExtraDestinationLat, mExtraDestinationLng);

    }

    private void showWaitDriverAlertDialog() {
        AlertDialog.Builder waitDriverBuilder = new AlertDialog.Builder(DetailRequestActivity.this);
        View view = getLayoutInflater().inflate(R.layout.wait_driver, null);
        // elementos dentro del layout
        TextView txtVLookingFor = view.findViewById(R.id.txtViewLookingFor);
        Button btnCancel = view.findViewById(R.id.btnCancelRequest);
        // propiedades del dialog
        waitDriverBuilder.setCancelable(false);
        waitDriverBuilder.setView(view);

        AlertDialog waitDriverDialog = waitDriverBuilder.create();
        waitDriverDialog.show();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRequest();
            }
        });

        getClosestDriver(txtVLookingFor);

    }

    private void cancelRequest() {
        mClientBookingProvider.delete(mAuthProvider.getUid());
        sendNotificationCancel();
    }

    private void sendNotificationCancel() {
        mTokenProvider.getToken(mIdDriverFound).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.exists() && documentSnapshot.contains("token")){
                    String token = documentSnapshot.getString("token");
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "REQUEST CANCELED");
                    map.put("body", "The user canceled the request");

                    FCMBody fcmBody = new FCMBody(token,"high" , "4500s", map);
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null){
                                if (response.body().getSuccess() == 1){
                                    Toast.makeText(DetailRequestActivity.this, "Request canceled.", Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(DetailRequestActivity.this, MapClientActivity.class);
                                    startActivity(i);
                                    finish();
                                }else{
                                    Toast.makeText(DetailRequestActivity.this, "Notification error.", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(DetailRequestActivity.this, "Notification error.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Toast.makeText(DetailRequestActivity.this, "Notification error", Toast.LENGTH_SHORT).show();
                            Log.d("ERROR", "Error: " + t.getMessage());
                        }
                    });
                }else{
                    Toast.makeText(DetailRequestActivity.this, "Drivers without token.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // buscar el conductor mas cercano
    private void getClosestDriver(TextView txtVLookingFor) {
        mGeofireProvider.getActiveDrivers(mOriginLatLng, mRadius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!mDriverFound){

                    mDriverFound = true;
                    mIdDriverFound = key;
                    mDriverFoundLatLng = new LatLng(location.latitude, location.longitude);
                    txtVLookingFor.setText("Driver found\nWaiting answer...");
                    Log.d("DRIVER", "Id driver found: "+ mIdDriverFound);

                    // creando solicitud en la bd
                    createClientBooking();
                    // enviar notificacion de solicitud a conductor encontrado
                    sendNotification();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // ingresa cuando termina la busqueda del conductor
                if (!mDriverFound){
                    // incrementando el radio de busqueda
                    mRadius = mRadius + 0.1f;
                    // si no encuentra ningun conductor finaliza el metodo getClosestDriver()
                    if (mRadius > 5){
                        txtVLookingFor.setText("No drivers found...");
                        Toast.makeText(DetailRequestActivity.this, "No drivers found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else{
                        txtVLookingFor.setText("Waiting for a driver...");
                        getClosestDriver(txtVLookingFor);
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void sendNotification() {
        mTokenProvider.getToken(mIdDriverFound).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.exists() && documentSnapshot.contains("token")){
                    String token = documentSnapshot.getString("token");
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "SERVICE REQUEST");
                    map.put("body", "Origin: " + "\n" +mExtraOrigin+"\nDestination: " + "\n" +mExtraDestination);

                    map.put("idClient", mAuthProvider.getUid());
                    map.put("origin", mExtraOrigin);
                    map.put("destination", mExtraDestination);
                    map.put("distance", mDistanceTxt);
                    map.put("time", mDurationTxt);

                    FCMBody fcmBody = new FCMBody(token,"high" , "4500s", map);
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null){
                                if (response.body().getSuccess() == 1){
                                    Toast.makeText(DetailRequestActivity.this, "Notification send.", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(DetailRequestActivity.this, "Notification error.", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(DetailRequestActivity.this, "Notification error.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Toast.makeText(DetailRequestActivity.this, "Notification error", Toast.LENGTH_SHORT).show();
                            Log.d("ERROR", "Error: " + t.getMessage());
                        }
                    });
                }else{
                    Toast.makeText(DetailRequestActivity.this, "Drivers without token.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createClientBooking(){
        // asignamos los datos al modelo
        ClientBooking clientBooking = new ClientBooking(
                mAuthProvider.getUid(),
                mIdDriverFound,
                mExtraOrigin,
                mExtraDestination,
                mDurationTxt,
                mDistanceTxt,
                "create",
                mExtraOriginLat,
                mExtraOriginLng,
                mExtraDestinationLat,
                mExtraDestinationLng
        );

        // creamos el doc en la bd
        mClientBookingProvider.create(clientBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                consultStatusRequest();
            }
        });
    }

    // consultando estado del viaje (escuchando cambios en doc en tiempo real)
    private void consultStatusRequest() {
       listenerRegistration = mClientBookingProvider.getStatus(mAuthProvider.getUid())
               .addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    System.err.println("Escuchador fallo " + error);
                }
                if (value != null && value.exists()){
                    // consultamos el estado de la consulta en tiempo real
                    String status = (String) value.get("status");
                    // si el driver acepta el servicio
                    if (status.contains("accept")){
                        Intent i = new Intent(DetailRequestActivity.this, MapClientBookingActivity.class);
                        startActivity(i);
                        // cuando cambiemos de activity no le permite volver
                        finish();
                    }
                    // si lo cancela
                    else if (status.contains("cancel")){
                        // TODO faltan metodos para cancelar el servicio
                        Toast.makeText(DetailRequestActivity.this, "Driver canceled you're request", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /*
     *  METODOS PARA UBICACION
     * */

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // mostramos el tipo de mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.addMarker(new MarkerOptions().position(mOriginLatLng)
                .title("Origin")
                .icon(BitmapFromVector(DetailRequestActivity.this, R.drawable.icon_location_user)));

        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng)
                .title("Destination")
                .icon(BitmapFromVector(DetailRequestActivity.this, R.drawable.icon_destination)));

        // movemos la camara del mapa
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                .target(mOriginLatLng)
                .zoom(15f)
                .build()
        ));

        drawRoute();
    }

    private void drawRoute(){
        mGoogleApiProvider.getDirections(mOriginLatLng, mDestinationLatLng).enqueue(new Callback<String>() {
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

                    mDistanceTxt = distance.getString("text");
                    mDurationTxt = duration.getString("text");

                    mTxtVDistance.setText(mDistanceTxt);
                    mTxtVTime.setText(mDurationTxt);

                    // separando el string para obtener el numero
                    String[] distanceAndKm = mDistanceTxt.split(" ");
                    // al dividir el string se convierte en array
                    // obtenemos el numero que queda en la posc [0]
                    double distanceValue = Double.parseDouble(distanceAndKm[0]);

                    String[] timeAndMin = mDurationTxt.split(" ");
                    double durationValue = Double.parseDouble(timeAndMin[0]);

                    calculateCost(distanceValue, durationValue);

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

    // CALCULAMOS COSTO DE VIAJE
    private void calculateCost(double distanceValue, double durationValue) {
        mInfoProvider.getInfo().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    Info info = documentSnapshot.toObject(Info.class);
                    double total = (distanceValue * info.getKm()) + (durationValue * info.getMin());
                    double totalmin = total - 0.5;
                    double totalmax = total + 0.5;
                    mTxtVCost.setText(String.valueOf("$" + totalmin + " - " + "$" + totalmax));
                }
            }
        });
    }

    /*
     *  METODO PARA ICONO DE UBICACION
     * */
    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // removemos la consulta en tiempo real
        if (listenerRegistration != null){
           listenerRegistration.remove();
        }
    }
}