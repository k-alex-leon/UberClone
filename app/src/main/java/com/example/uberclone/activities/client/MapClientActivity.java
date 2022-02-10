package com.example.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.uberclone.R;
import com.example.uberclone.activities.HistoryActivity;
import com.example.uberclone.activities.MainActivity;
import com.example.uberclone.activities.ProfileActivity;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.GeofireProvider;
import com.example.uberclone.providers.TokenProvider;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapClientActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AuthProvider mAuthProvider;
    private TokenProvider mTokenProvider;

    // lista para iconos de drivers
    List<Marker> mDriversMarkers = new ArrayList<>();

    // campo necesario para guardar la ubic del user
    private LatLng mCurrentLatLng;
    private ValueEventListener mListener;

    // provider encargado de actualizar los drivers
    private GeofireProvider mGeofireProvider;

    Toolbar mToolbar;

    private final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    // para el autocomplete de places
    private PlacesClient mPlaces;
    private AutocompleteSupportFragment mAutocomplete;
    private AutocompleteSupportFragment mAutocompleteDestination;

    // var punto de origen
    private String mOrigin;
    private LatLng mOriginLatLong;

    // var punto de destino
    private String mDestination;
    private LatLng mDestinationLatLong;

    // btn request
    Button mBtnRequestDriver;

    // para localizar ubicacion del usuario
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFussedLocation;

    // captura de movimiento de camara
    private GoogleMap.OnCameraIdleListener mCameraListener;

    private boolean mIsFirstTime = true;

    LocationCallback mLocationCallBack = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    locationResult.getLastLocation();
                    // obteniendo datos de ubicacion
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    //Obtener localizacion de usuario en tiempo real
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(17f)
                                    .build()
                    ));

                    if (mIsFirstTime){
                        // cambiamos valor de variable para que
                        // el metodo de buscar conductores no se ejecute mas de 1 vez
                        mIsFirstTime = false;
                        getActiveDrivers();
                        // limitar el area de busqueda de los places
                        limitSearchLocation();
                    }

                }
            }
            super.onLocationResult(locationResult);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();

        mFussedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapClient);
        mMapFragment.getMapAsync(this);

        // config de toolbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        //quitar titulo de toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mBtnRequestDriver = findViewById(R.id.btnRequestDriver);
        mBtnRequestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDriver();
            }
        });

        // AUTOCOMPLETE DE PLACES
        // en caso de que no se inicialice
        if (!Places.isInitialized()){
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }

        // instanciando y agregando evento de cliklistener
        mPlaces = Places.createClient(this);

        instanceAutocompleteOrigin();
        instanceAutocompleteDestination();

        // escuchando el cambio de camara que realice el usuario
        cameraListener();

        // crea token
        generateToken();
    }

    private void requestDriver() {

        if (mOriginLatLong != null && mDestinationLatLong != null){
            Intent i = new Intent(MapClientActivity.this, DetailRequestActivity.class);
            i.putExtra("origin_lat", mOriginLatLong.latitude);
            i.putExtra("origin_lng", mOriginLatLong.longitude);
            i.putExtra("destination_lat", mDestinationLatLong.latitude);
            i.putExtra("destination_lng", mDestinationLatLong.longitude);
            i.putExtra("origin", mOrigin);
            i.putExtra("destination", mDestination);
            startActivity(i);
        }else{
            Toast.makeText(MapClientActivity.this, "Select an origin and destination...", Toast.LENGTH_LONG).show();
        }
    }


    // limitamos el area de busqueda
    private void limitSearchLocation(){
        // distance = 5km
        LatLng nortSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000, 0);
        LatLng sourthSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000, 180);


        mAutocomplete.setCountry("COL");
        mAutocomplete.setLocationBias(RectangularBounds.newInstance(sourthSide, nortSide));

        mAutocompleteDestination.setCountry("COL");
        mAutocompleteDestination.setLocationBias(RectangularBounds.newInstance(sourthSide, nortSide));
    }

    private void cameraListener(){
        mCameraListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try {
                    Geocoder geocoder = new Geocoder(MapClientActivity.this);
                    mOriginLatLong = mMap.getCameraPosition().target;
                    // pasamos al array las coordenadas seleccionadas
                    // por el user y el num de resultados que queremos obtener
                    List<Address> addressList = geocoder.getFromLocation(mOriginLatLong.latitude, mOriginLatLong.longitude, 1);
                    String country = addressList.get(0).getCountryName();
                    String city = addressList.get(0).getLocality();
                    String address = addressList.get(0).getAddressLine(0);
                    // guardamos el origen
                    mOrigin = city + " " + address;
                    // pasamos los strings a la caja de texto del autocomplete
                    mAutocomplete.setText(city + " " + address);
                }catch (Exception e){
                    Log.d("Error" , "Message error"+ " "+ e.getMessage());
                }
            }
        };
    }

    private void instanceAutocompleteOrigin(){
        mAutocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placesAutocompleteOrigin);
        mAutocomplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        mAutocomplete.setHint("Origin");
        mAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {

            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // obtenemos datos de punto de partida
                mOrigin = place.getName();
                mOriginLatLong = place.getLatLng();
                Log.d("PLACE", "Name"+ " " + mOrigin);
                Log.d("PLACE", "Lat" + mOriginLatLong.latitude);
                Log.d("PLACE", "Long" + mOriginLatLong.longitude);
            }
        });
    }

    private void instanceAutocompleteDestination(){
        mAutocompleteDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placesAutocompleteDestination);
        mAutocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        mAutocompleteDestination.setHint("Destination");
        mAutocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {

            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // obtenemos datos de punto de partida
                mDestination = place.getName();
                mDestinationLatLong = place.getLatLng();
                Log.d("PLACE", "Name" +" "+mDestination);
                Log.d("PLACE", "Lat" + mDestinationLatLong.latitude);
                Log.d("PLACE", "Long" + mDestinationLatLong.longitude);
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

    /*
     *  METODOS PARA MAPS DE GOOOGLE
     * */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // mostramos el tipo de mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setOnCameraIdleListener(mCameraListener);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);
        // el mapa se termina de cargar y empieza a localizar usuario
        startLocation();
    }


    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (gpsActived()) {
                    mFussedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
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
                mMap.setMyLocationEnabled(true);
            }
            else {
                showAlertDialogNoGPS();
            }
        }
    }

    private void getActiveDrivers(){
        // radus son los km que se toman en cuenta para buscar driver
        mGeofireProvider.getActiveDrivers(mCurrentLatLng, 5).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // actualizando los drivers que se conectan
                // agregamos marcadores
                for(Marker marker : mDriversMarkers){
                    if (marker.getTag() != null){
                        if (marker.getTag().equals(key)){
                            return;
                        }
                    }
                }
                // obteniendo coordenadas del (los) drivers
                LatLng driverLatLng = new LatLng(location.latitude, location.longitude);
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(driverLatLng).title("Driver available")
                        .icon(BitmapFromVector(MapClientActivity.this ,R.drawable.icon_car)));
                // asignamos id al marcador
                marker.setTag(key);
                // pasando los marcadores al array de drivers
                mDriversMarkers.add(marker);
            }

            @Override
            public void onKeyExited(String key) {
                // actualizando los drivers que se desconectan
                for(Marker marker : mDriversMarkers){
                    if (marker.getTag() != null){
                        if (marker.getTag().equals(key)){
                            // eliminamos los marcadores
                            marker.remove();
                            mDriversMarkers.remove(marker);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // actualizando los drivers que estan en movimiento
                for(Marker marker : mDriversMarkers){
                    if (marker.getTag() != null){
                        if (marker.getTag().equals(key)){
                            // actializando coordenadas de driver
                            marker.setPosition(new LatLng(location.latitude, location.longitude));
                        }
                    }
                }

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
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
                                ActivityCompat.requestPermissions(MapClientActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MapClientActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
                        mMap.setMyLocationEnabled(true);
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
            mMap.setMyLocationEnabled(true);
        }
        // para evitar error de mostrar dialog despues de seleccionar un lugar
        else if (requestCode == SETTINGS_REQUEST_CODE && !gpsActived()) {
            showAlertDialogNoGPS();
        }
    }

    /*
     *  METODOS TOOLBAR
     * */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile){
            Intent i = new Intent(MapClientActivity.this, ProfileActivity.class);
            startActivity(i);
        }
        if (item.getItemId() == R.id.action_history){
            Intent i = new Intent(MapClientActivity.this, HistoryActivity.class);
            startActivity(i);
        }

        if (item.getItemId() == R.id.action_logout){
            logout();
        }
        return super.onOptionsItemSelected(item);
    }

    // cerrar sesion
    private void logout() {
        mAuthProvider.logout();
        Intent i = new Intent(MapClientActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    // generando token cuando el user entra el activ
    public void generateToken(){
        mTokenProvider.create(mAuthProvider.getUid());
    }
}