package com.android.uberclone;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Marker mPickupMarker;

    private DatabaseReference mAssignedRiderPickupLocationReference;
    private ValueEventListener mAssignedRiderPickupLocationReferenceListener;

    private static boolean mCheckLoginStatus = false;
    private String mRiderId = "";

    private LinearLayout mRiderInfo;
    private TextView rider_name;
    private TextView rider_phone_number;
    private ImageView rider_profile_pic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);

        rider_name = findViewById(R.id.rider_name);
        rider_phone_number = findViewById(R.id.rider_phone_number);
        rider_profile_pic = findViewById(R.id.rider_profile_pic);
        mRiderInfo = findViewById(R.id.rider_info);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.driver_map);
        mapFragment.getMapAsync(this);

        // driver logout button
        Button mLogout = findViewById(R.id.driver_logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCheckLoginStatus = true;

                // Signout the driver
                signingOutDriver();
                finish();
                return;
            }
        });

        Button settings = findViewById(R.id.driver_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingActivity.class);
                startActivity(intent);
            }
        });

        // get assigned rider information
        getAssignedRider();
    }

    // Signout the driver
    public void signingOutDriver() {

        // disconnect driver from app
        disconnectDriver();

        // signing out driver
        FirebaseAuth.getInstance().signOut();

        // move to main activity
        Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
        startActivity(intent);
    }

    // get assigned rider information
    private void getAssignedRider() {
        // get driver id
        String driverId = FirebaseAuth.getInstance().getUid();

        // get reference of assigned Rider Id
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.USERS)
                .child(FirebaseConstants.DRIVERS).child(driverId).child(FirebaseConstants.RIDERS_REQUEST)
                .child(FirebaseConstants.RIDER_ID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // check if rider assigned to any driver
                // if exist
                if (dataSnapshot.exists()) {
                    // get assigned rider id
                    mRiderId = dataSnapshot.getValue().toString();
                    // get assigned rider pickup location
                    getAssignedRiderPickupLocation();
                    // get assigned rider info
                    getAssignedRiderInfo();
                    // get assigned rider destination
                    getAssignedRiderDestination();


                } else {
                    // if not exist reset rider id
                    mRiderId = "";

                    // remove assigned rider marker on driver's map
                    if (mPickupMarker != null) {
                        mPickupMarker.remove();
                    }

                    // remove assign rider pickup location listener
                    if (mAssignedRiderPickupLocationReference != null) {
                        mAssignedRiderPickupLocationReference.removeEventListener(mAssignedRiderPickupLocationReferenceListener);
                    }

                    mRiderInfo.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // get assigned rider pickup location
    private void getAssignedRiderPickupLocation() {

        mAssignedRiderPickupLocationReference = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.RIDERS_REQUEST)
                .child(mRiderId).child(FirebaseConstants.LOCATION);
        // add value event listener on rider request location
        mAssignedRiderPickupLocationReferenceListener = mAssignedRiderPickupLocationReference
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && !mRiderId.equals("")) {
                            List<Object> map = (List<Object>) dataSnapshot.getValue();

                            double locationLat = 0;
                            double locationLng = 0;

                            // get assigned rider latitude
                            if (map.get(0) != null) {
                                locationLat = Double.parseDouble(map.get(0).toString());
                            }
                            // get assigned rider longitude
                            if (map.get(1) != null) {
                                locationLng = Double.parseDouble(map.get(1).toString());
                            }

                            // store the latitude and longitude in riderLatLng
                            LatLng riderLatLng = new LatLng(locationLat, locationLng);

                            // add marker of assigned rider on driver's map
                            mPickupMarker = mMap.addMarker(new MarkerOptions().position(riderLatLng)
                                    .title("Pickup location"));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    // get assigned rider info
    private void getAssignedRiderInfo() {

        mRiderInfo.setVisibility(View.VISIBLE);

        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child(FirebaseConstants.USERS).child(FirebaseConstants.RIDERS).child(user_id);

        databaseReference.child(FirebaseConstants.USERS).child(FirebaseConstants.RIDERS).child(user_id)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                            if (map.get(FirebaseConstants.NAME) != null) {
                                rider_name.setText(map.get(FirebaseConstants.NAME).toString());
                            }

                            if (map.get(FirebaseConstants.PHONE_NUMBER) != null) {
                                rider_phone_number.setText(map.get(FirebaseConstants.PHONE_NUMBER).toString());
                            }

                            if (map.get(FirebaseConstants.PROFILE_IMAGE_URL) != null) {
                                Picasso.with(getApplication())
                                        .load(map.get(FirebaseConstants.PROFILE_IMAGE_URL).toString())
                                        .placeholder(R.drawable.progress_animation)
                                        .into(rider_profile_pic);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    // get assigned rider information
    private void getAssignedRiderDestination() {
        // get driver id
        String driverId = FirebaseAuth.getInstance().getUid();

        // get reference of assigned Rider Id
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.USERS)
                .child(FirebaseConstants.DRIVERS).child(driverId).child(FirebaseConstants.RIDERS_REQUEST)
                .child(FirebaseConstants.RIDER_ID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    // get assigned rider id
                    mRiderId = dataSnapshot.getValue().toString();

                } else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        buildGoogleApiClint();
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClint() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // set location request parameter's of driver
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            Log.e("MapActivity", "Security exception occured while getting user location");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {

            mLastLocation = location;

            // update location of driver on location changed
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference(FirebaseConstants.DRIVERS_AVAILABLE);
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference(FirebaseConstants.DRIVERS_WORKING);

            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            // change the driver status w.r.t assigned rider (available to working) or (working to available)
            switch (mRiderId) {
                // if the driver has no assigned rider then its status is Available
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                // otherwise Working
                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
        startActivity(intent);
    }

    // disconnect the driver
    public void disconnectDriver() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FirebaseConstants.DRIVERS_AVAILABLE);

        GeoFire geoFire = new GeoFire(reference);
        geoFire.removeLocation(userId);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!mCheckLoginStatus) {
            disconnectDriver();
        }
    }
}
