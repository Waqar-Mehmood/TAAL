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

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

import java.util.HashMap;
import java.util.List;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Button mLogout, mCallUber;
    private ImageView mCancelUber;

    private LatLng mPickupLocation;
    private Marker mDriverMarker;
    private Marker mPickupMarker;
    private GeoQuery mGeoQuery;
    private DatabaseReference mDriverLocationReference;
    private ValueEventListener mDriverLocationReferenceListener;

    private int mRadius = 1;
    private boolean mDriverFound = false;
    private boolean mRequestBol = false;
    private String mDriverFoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rider_map);
        mapFragment.getMapAsync(this);

        // rider logout button
        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // move to MainActivity
                Intent intent = new Intent(RiderMapActivity.this, MainActivity.class);
                startActivity(intent);

                // Signing out user
                FirebaseAuth.getInstance().signOut();
            }
        });

        // Request Uber and set user location to RiderRequest node under user_id
        mCallUber = (Button) findViewById(R.id.call_uber);
        mCallUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callUber();
            }
        });

        // Cancel Uber request and remove user location from RiderRequest node
        mCancelUber = findViewById(R.id.cancel_uber);
        mCancelUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelUberRequest();
            }
        });
    }

    private void callUber() {
        if (mRequestBol == false) {
            mRequestBol = true;

            // Getting user id
            String userId = FirebaseAuth.getInstance().getUid();

            // get reference of RiderRequest node
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("RidersRequest");

            // adding rider's current location to database under RidersRequest node
            GeoFire geoFire = new GeoFire(reference);
            geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

            // marker is added to rider's map
            mPickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mPickupMarker = mMap.addMarker(new MarkerOptions().position(mPickupLocation).title("Pick Here"));

            mCallUber.setText("Getting your Drive");

            // get nearest driver on rider's request
            getClosestDriver();
        }
    }

    private void cancelUberRequest() {
        if (mRequestBol) {
            mRequestBol = false;

            // remove GeoQuery listener
            if (mGeoQuery != null) {
                mGeoQuery.removeAllListeners();
            }

            // remove mDriverLocationReference listener
            if (mDriverLocationReference != null) {
                mDriverLocationReference.removeEventListener(mDriverLocationReferenceListener);
            }

            if (mDriverFoundID != null) {
                // get mDriverLocationReference of driver found form Drivers
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Drivers").child(mDriverFoundID);

                // remove the assigned rider from driver
                reference.setValue(true);
                mDriverFoundID = null;
            }

            // reset values to default
            mDriverFound = false;
            mRadius = 1;

            // get user id
            String userId = FirebaseAuth.getInstance().getUid();

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("RiderRequest");

            // remove location of rider form RiderRequest node
            GeoFire geoFire = new GeoFire(reference);
            geoFire.removeLocation(userId);

            // remove rider pickup marker on riders's map
            if (mPickupMarker != null) {
                mPickupMarker.remove();
            }

            // remove assigned driver marker from rider's map
            if (mDriverMarker != null) {
                mDriverMarker.remove();
            }

            mCallUber.setText("Call Uber");
        }
    }

    private void getClosestDriver() {
        // get mDriverLocationReference of DriversAvailable node
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("DriversAvailable");

        // put mDriverLocationReference to GeoFire
        GeoFire geoFire = new GeoFire(reference);

        // check the nearest drive with given mRadius
        mGeoQuery = geoFire.queryAtLocation(new GeoLocation(mPickupLocation.latitude,
                mPickupLocation.longitude), mRadius);

        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!mDriverFound && mRequestBol) {
                    mDriverFound = true;
                    mDriverFoundID = key;

                    // get mDriverLocationReference of driver found form Drivers
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(mDriverFoundID);

                    // get user id
                    String riderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    // add the user id under the nearest found driver
                    HashMap map = new HashMap();
                    map.put("RiderId", riderId);
                    reference.updateChildren(map);

                    mCallUber.setText("Looking for driver location...");

                    // get exact location of driver
                    getDriverLocation();
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
                // if the driver on found on given mRadius then increase the mRadius and check again
                if (!mDriverFound) {
                    mRadius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    // get exact location of driver
    private void getDriverLocation() {

        mDriverLocationReference = FirebaseDatabase.getInstance().getReference().child("DriversWorking")
                .child(mDriverFoundID).child("l");

        mDriverLocationReferenceListener = mDriverLocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && mRequestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    mCallUber.setText("Driver Found");

                    // get assigned driver latitude
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    // get assigned driver longitude
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    // store the latitude and longitude in driverLatLng
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    // remove the driver marker from rider's map
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    // location of rider
                    Location location1 = new Location("");
                    location1.setLatitude(mPickupLocation.latitude);
                    location1.setLongitude(mPickupLocation.longitude);

                    // location of driver
                    Location location2 = new Location("");
                    location2.setLatitude(driverLatLng.latitude);
                    location2.setLongitude(driverLatLng.longitude);

                    // calculate distance from rider to driver
                    float distance = location1.distanceTo(location2);

                    // if distance less than 100 meters, it notify the rider that driver arrived
                    if (distance < 100) {
                        mCallUber.setText("Driver Arrived");
                    } else {
                        // otherwise it shows the distance on map
                        mCallUber.setText("Driver Found" + String.valueOf(distance));
                    }

                    // add a marker of assigned driver on rider's map
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
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
        // set location request parameter's of rider
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            Log.e("MapActivity", "Security exception occured while gettling user location");
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
        mLastLocation = location;

        // update location of rider on location changed
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(RiderMapActivity.this, MainActivity.class);
        startActivity(intent);
    }
}