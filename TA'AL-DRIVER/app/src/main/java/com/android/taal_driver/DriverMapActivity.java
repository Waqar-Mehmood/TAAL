package com.android.taal_driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
    private SupportMapFragment mMapFragment;

    private DatabaseReference mAssignedRiderPickupLocationReference;
    private ValueEventListener mAssignedRiderPickupLocationReferenceListener;

    private final int LOCATION_REQUEST_CODE = 1;
    private static boolean mCheckLoginStatus = false;
    private String mRiderId = "";
    private String mDriverId;

    private LinearLayout mRiderInfo;
    private TextView mRiderName;
    private TextView mRiderPhoneNumber;
    private TextView mRiderDestination;
    private ImageView mRiderProfilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);

        mDriverId = FirebaseAuth.getInstance().getUid();

        mRiderName = findViewById(R.id.d_rider_name);
        mRiderPhoneNumber = findViewById(R.id.d_rider_phone_number);
        mRiderDestination = findViewById(R.id.d_rider_destination);
        mRiderProfilePic = findViewById(R.id.d_rider_profile_pic);
        mRiderInfo = findViewById(R.id.d_rider_info);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.driver_map);

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

        // request permission to access gps
        requestContactsPermission();

        // get assigned rider information
        getAssignedRider();
    }

    public void requestContactsPermission() {
        // get permission if it is not granted
        if (ContextCompat.checkSelfPermission(DriverMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    // Signout the driver
    public void signingOutDriver() {

        // disconnect driver from app
        disconnectDriver();

        // signing out driver
        FirebaseAuth.getInstance().signOut();

        // move to main activity
        Intent intent = new Intent(DriverMapActivity.this, DriverLoginActivity.class);
        startActivity(intent);
    }

    // get assigned rider information
    private void getAssignedRider() {

        // get reference of assigned Rider Id
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(AppConstants.USERS)
                .child(AppConstants.DRIVERS).child(mDriverId).child(AppConstants.RIDER_REQUEST)
                .child(AppConstants.RIDER_ID);

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
//                    getAssignedRiderDestination();


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
                    mRiderName.setText("");
                    mRiderPhoneNumber.setText("");
                    mRiderDestination.setText("Destination: --");
                    mRiderProfilePic.setImageResource(R.drawable.unknown_profile_pic);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // get assigned rider pickup location
    private void getAssignedRiderPickupLocation() {

        mAssignedRiderPickupLocationReference = FirebaseDatabase.getInstance().getReference()
                .child(AppConstants.RIDERS_REQUEST).child(mRiderId).child(AppConstants.LOCATION);
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
                            mPickupMarker = mMap.addMarker(new MarkerOptions().position(riderLatLng).title("Pickup location")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.rider_location)));
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

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child(AppConstants.USERS).child(AppConstants.RIDERS).child(mRiderId)
                .child(AppConstants.USER_DETAILS);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get(AppConstants.NAME) != null) {
                        mRiderName.setText(map.get(AppConstants.NAME).toString().toUpperCase());
                    }

                    if (map.get(AppConstants.PHONE_NUMBER) != null) {
                        mRiderPhoneNumber.setText(map.get(AppConstants.PHONE_NUMBER).toString());
                    }

                    if (map.get(AppConstants.PROFILE_IMAGE_URL) != null) {
                        Picasso.with(getApplication())
                                .load(map.get(AppConstants.PROFILE_IMAGE_URL).toString())
                                .placeholder(R.drawable.progress_animation)
                                .into(mRiderProfilePic);
                    } else {
                        mRiderProfilePic.setImageResource(R.drawable.unknown_profile_pic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    // get assigned rider destination
    private void getAssignedRiderDestination() {
        // get driver id
        String driverId = FirebaseAuth.getInstance().getUid();

        // get reference of assigned Rider destination
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(AppConstants.USERS)
                .child(AppConstants.DRIVERS).child(driverId).child(AppConstants.RIDERS_REQUEST)
                .child(AppConstants.DESTINATION);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    // get assigned rider destination
                    String destination = dataSnapshot.getValue().toString();
                    mRiderDestination.setText("Destination: " + destination);
                } else {
                    mRiderDestination.setText("Destination: --");
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
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

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

            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference(AppConstants.DRIVERS_AVAILABLE);
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference(AppConstants.DRIVERS_WORKING);

            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            // change the driver status w.r.t assigned rider (available to working) or (working to available)
            switch (mRiderId) {
                // if the driver has no assigned rider then its status is Available
                case "":
                    geoFireWorking.removeLocation(mDriverId);
                    geoFireAvailable.setLocation(mDriverId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                // otherwise Working
                default:
                    geoFireAvailable.removeLocation(mDriverId);
                    geoFireWorking.setLocation(mDriverId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    // disconnect the driver
    public void disconnectDriver() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(AppConstants.DRIVERS_AVAILABLE);

        GeoFire geoFire = new GeoFire(reference);
        geoFire.removeLocation(mDriverId);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!mCheckLoginStatus) {
            disconnectDriver();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishAffinity();
    }
}
