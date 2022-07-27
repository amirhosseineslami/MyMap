package com.example.mymap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener, GoogleMap.OnMarkerClickListener {
    private static final int OPEN_SETTINGS_LOCATION_REQ_CODE = 400;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    public static final int PERMISSION_LOCATION_REQ_CODE = 130;
    private SupportMapFragment supportMapFragment;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private SettingsClient settingsClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location userLastLocation;
    private GoogleMap myGoogleMap;
    public static final int GOOGLE_API_AVAILABILITY_ERROR_CODE = 146;
    public static final int GOOGLE_API_AVAILABILITY_REQ_CODE = 34;

    private Marker currentLocationMarker;
    private Location destinationLocation;
    private Marker destinationMarker;
    TextView distanceTextView;
    private boolean isGpsOn = false;
    private AlertDialog alertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        distanceTextView = findViewById(R.id.textView);
        permissionIsGranted();
    }


    private void permissionIsGranted() {
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        supportMapFragment.getMapAsync(this);
        initLocationRequest();
        sendLocationRequest();
        settingsClient = LocationServices.getSettingsClient(MainActivity.this);
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        isGpsOn = true;
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_LOCATION_REQ_CODE);
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes
                                    .RESOLUTION_REQUIRED:
                                isGpsOn = false;
                                showGPSAlertDialog();
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Toast.makeText(MainActivity.this, "check the settings!", Toast.LENGTH_SHORT).show();
                                break;
                        }

                    }
                });

    }

    private void showGPSAlertDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("GPS")
                .setMessage("GPS is required for processing of the Application please turn it on.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), OPEN_SETTINGS_LOCATION_REQ_CODE);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isGpsOn = false;
                    }
                })
                //.setCancelable(false)
                .show();
    }

    private void initLocationRequest() {
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3000)
                .setFastestInterval(2000);
    }

    private void sendLocationRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        locationSettingsRequest = builder
                .addLocationRequest(locationRequest)
                .build();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_LOCATION_REQ_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionIsGranted();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_LOCATION_REQ_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_SETTINGS_LOCATION_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                isGpsOn = true;
                alertDialog.cancel();
            }
            permissionIsGranted();
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            userLastLocation = locationResult.getLastLocation();
            LatLng latLng = new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }
            currentLocationMarker = myGoogleMap.addMarker(new MarkerOptions().title("You")
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            //myGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,14f));

        }
    };

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng latLng = null;
        SharedPreferences sharedPreferences = getSharedPreferences("lastLocation", MODE_PRIVATE);
        Double longitude = (double) sharedPreferences.getFloat("longitude", 0);
        Double latitude = (double) sharedPreferences.getFloat("latitude", 0);
        if (!(latitude == 0 && longitude == 0)) {
            latLng = new LatLng(latitude, longitude);
        }
        if (latLng == null) {
            latLng = new LatLng(29, 55);
        }
        myGoogleMap = googleMap;
        myGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        myGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
        /*
        currentLocationMarker = myGoogleMap.addMarker(new MarkerOptions().position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .draggable(false)
                .title("You"));
        */
        myGoogleMap.getUiSettings().setZoomGesturesEnabled(true);
        myGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_LOCATION_REQ_CODE);
            return;
        }
        myGoogleMap.setMyLocationEnabled(true);
        myGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);


        myGoogleMap.setOnMapLongClickListener(this);
        myGoogleMap.setOnMarkerDragListener(this);
        myGoogleMap.setOnMarkerClickListener(this);

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, GOOGLE_API_AVAILABILITY_ERROR_CODE, GOOGLE_API_AVAILABILITY_REQ_CODE, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Toast.makeText(MainActivity.this, "Play services is not available!", Toast.LENGTH_SHORT).show();
                }
            }).show();
        }
    }

    @Override
    protected void onStop() {
        if (userLastLocation != null) {
            SharedPreferences sharedPreferences = getSharedPreferences("lastLocation", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("longitude", (float) userLastLocation.getLongitude());
            editor.putFloat("latitude", (float) userLastLocation.getLatitude());
            editor.apply();
        }
        super.onStop();
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {

        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        destinationLocation = new Location("");
        destinationLocation.setLatitude(latLng.latitude);
        destinationLocation.setLongitude(latLng.longitude);
        destinationMarker = myGoogleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .draggable(true)
                .position(latLng)
                .title("destination"));
        if (isGpsOn) {
            distanceTextView.setVisibility(View.VISIBLE);
            distanceTextView.setText("Distance: " + provideDistanceFromUserToDestinationTxt());
        }
    }

    @Override
    public void onMarkerDrag(@NonNull Marker marker) {
        LatLng latLng = marker.getPosition();
        destinationLocation.setLongitude(latLng.longitude);
        destinationLocation.setLatitude(latLng.latitude);
        if (isGpsOn) {
            distanceTextView.setText("Distance: " + provideDistanceFromUserToDestinationTxt());
        }
    }

    @Override
    public void onMarkerDragEnd(@NonNull Marker marker) {
    }

    @Override
    public void onMarkerDragStart(@NonNull Marker marker) {
        Toast.makeText(this, "Set your destination", Toast.LENGTH_SHORT).show();
    }

    private String provideDistanceFromUserToDestinationTxt() {
        float distance = userLastLocation.distanceTo(destinationLocation);
        String distanceStr;
        if (distance < 1000) {
            distanceStr = String.format("%.3f", distance);
            distanceStr = distanceStr + " m";
        } else {
            distance /= 1000;
            distanceStr = String.format("%.3f", distance);
            distanceStr = distanceStr + " km";
        }
        return distanceStr;
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Marker")
                .setMessage("Do you want to delete this marker?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        marker.remove();
                        distanceTextView.setVisibility(View.INVISIBLE);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();
        return false;
    }

}