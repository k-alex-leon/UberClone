package com.example.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.activities.driver.MapDriverBookingActivity;
import com.example.uberclone.models.FCMBody;
import com.example.uberclone.models.FCMResponse;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientBookingProvider;
import com.example.uberclone.providers.DriversProvider;
import com.example.uberclone.providers.GeofireProvider;
import com.example.uberclone.providers.GoogleApiProvider;
import com.example.uberclone.providers.TokenProvider;
import com.example.uberclone.utils.DecodePoints;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
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
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapClientBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AuthProvider mAuthProvider;
    private ClientBookingProvider mClientBookingProvider;
    private GoogleApiProvider mGoogleApiProvider;
    private DriversProvider mDriverProvider;


    private Marker mMarker;

    private List<LatLng> mPolyLineList;
    private PolylineOptions mPolyLineOptions;

    LatLng mDriverLatLng, mOriginLatLng, mDestinationLatLng;

    private TextView mTxtVDriverNameBooking, mTxtVDriverEmailBooking,
                    mTxtVDriverBrandBooking, mTxtVDriverPlateBooking,
                    mTxtVStatus;

    private CircleImageView mImgCDriverBooking;

    private CardView mCardViewStatus;

    // provider encargado de actualizar los drivers
    private GeofireProvider mGeofireProvider;

    private Boolean mIsFirstTime = true;

    Toolbar mToolbar;

    private final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;


    // para localizar ubicacion del usuario
    private FusedLocationProviderClient mFussedLocation;

    private ValueEventListener mListener;
    private ListenerRegistration mListenerStatus;
    private String mIdDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client_booking);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("drivers_working");
        mClientBookingProvider = new ClientBookingProvider();
        mGoogleApiProvider = new GoogleApiProvider(MapClientBookingActivity.this);
        mDriverProvider = new DriversProvider();

        mTxtVDriverNameBooking = findViewById(R.id.txtVDriverNameBooking);
        mTxtVDriverEmailBooking = findViewById(R.id.txtVDriverEmailBooking);
        mTxtVDriverBrandBooking = findViewById(R.id.txtVDriverBrandBooking);
        mTxtVDriverPlateBooking = findViewById(R.id.txtVDriverPlateBooking);
        mTxtVStatus = findViewById(R.id.txtVStatus);
        mImgCDriverBooking = findViewById(R.id.imgCDriverBooking);

        mCardViewStatus = findViewById(R.id.cardViewStatus);

        mFussedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapClientBooking);
        mMapFragment.getMapAsync(this);

        getStatus();
        getClientBooking();
    }

    private void getStatus() {
        mListenerStatus = mClientBookingProvider.getStatus(mAuthProvider.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value.exists()){
                    String status = (String) value.get("status");
                    if (status.contains("accept")){
                        mCardViewStatus.setBackgroundColor(Color.BLUE);
                        mTxtVStatus.setTextColor(Color.WHITE);
                        mTxtVStatus.setText("Status: " + status);
                    }else if(status.contains("start")){
                        mCardViewStatus.setBackgroundColor(Color.GREEN);
                        mTxtVStatus.setTextColor(Color.WHITE);
                        mTxtVStatus.setText("Status: " + status);
                        startBooking();
                    }else if(status.contains("finish")){
                        mCardViewStatus.setBackgroundColor(Color.LTGRAY);
                        mTxtVStatus.setText("Status: " + status);
                        finishBooking();
                    }
                }
            }
        });
    }

    private void finishBooking() {
        Intent i = new Intent(MapClientBookingActivity.this, CalificationDriverActivity.class);
        startActivity(i);
        finish();
    }

    private void startBooking() {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng)
                .title("Destination")
                .icon(BitmapFromVector(MapClientBookingActivity.this, R.drawable.icon_destination)));
        drawRoute(mDestinationLatLng);
    }

    // obtener la info del viaje solicitado por el client
    private void getClientBooking() {
        mClientBookingProvider.getClientBooking(mAuthProvider.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    mIdDriver = documentSnapshot.getString("idDriver");
                    String origin = documentSnapshot.getString("origin");
                    // parseamos el contenido de documentSnapshot a un double para evitar error
                    double originLat = documentSnapshot.getDouble("originLat");
                    double originLng = documentSnapshot.getDouble("originLng");

                    String destination = documentSnapshot.getString("destination");
                    double destinationLat = documentSnapshot.getDouble("destinationLat");
                    double destinationLng = documentSnapshot.getDouble("destinationLng");


                    mOriginLatLng = new LatLng(originLat, originLng);
                    mDestinationLatLng = new LatLng(destinationLat, destinationLng);

                    mMap.addMarker(new MarkerOptions().position(mOriginLatLng)
                            .title("You're position")
                            .icon(BitmapFromVector(MapClientBookingActivity.this, R.drawable.icon_location_user)));

                    // obteniendo datos del conductor
                    getDriverInfo();
                    // obteniendo ubic del driver asignado
                    getDriverLocation();
                }
            }
        });
    }

    private void getDriverInfo() {
        mDriverProvider.getUserById(mIdDriver).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){

                    if (documentSnapshot.contains("imageProfile")){
                        String imgUrl = documentSnapshot.getString("imageProfile");
                        if (imgUrl != null && !imgUrl.isEmpty()){
                            Picasso.get().load(imgUrl).into(mImgCDriverBooking);
                        }
                    }

                    mTxtVDriverNameBooking.setText(documentSnapshot.getString("username"));
                    mTxtVDriverEmailBooking.setText(documentSnapshot.getString("email"));
                    mTxtVDriverBrandBooking.setText(documentSnapshot.getString("vehicleBrand"));
                    mTxtVDriverPlateBooking.setText(documentSnapshot.getString("vehiclePlate"));
                }
            }
        });
    }

    private void getDriverLocation() {
        mListener = mGeofireProvider.getDriverLocation(mIdDriver).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    double lat = Double.parseDouble(snapshot.child("0").getValue().toString());
                    double lng = Double.parseDouble(snapshot.child("1").getValue().toString());

                    mDriverLatLng = new LatLng(lat, lng);
                    if (mMarker != null){
                        mMarker.remove();
                    }

                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(lat, lng))
                            .title("You're driver.")
                            .icon(BitmapFromVector(MapClientBookingActivity.this, R.drawable.icon_car)));

                    if (mIsFirstTime){
                        mIsFirstTime = false;

                        // movemos la camara del mapa
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(mDriverLatLng)
                                        .zoom(15f)
                                        .build()
                        ));

                        // dibujamos ruta de la ubic del driver a origen de client
                        drawRoute(mOriginLatLng);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // dibujamos ruta del conductor al cliente
    private void drawRoute(LatLng latLng){
        mGoogleApiProvider.getDirections(mDriverLatLng, latLng).enqueue(new Callback<String>() {
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // mostramos el tipo de mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null){
            mGeofireProvider.getDriverLocation(mIdDriver).removeEventListener(mListener);
        }
        if(mListenerStatus != null){
            mListenerStatus.remove();
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
}