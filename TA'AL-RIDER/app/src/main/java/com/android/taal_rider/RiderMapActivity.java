package com.android.taal_rider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SupportMapFragment mMapFragment;

    private LatLng mPickupLocation;
    private LatLng mDestinationLatLng;
    private Marker mDriverMarker;
    private Marker mPickupMarker;
    private GeoQuery mGeoQuery;
    private DatabaseReference mDriverLocationReference;
    private ValueEventListener mDriverLocationReferenceListener;

    private LinearLayout mDriverInfo;
    private TextView mDriverName;
    private TextView mDriverPhoneNumber;
    private TextView mDriverCar;
    private ImageView mDriverProfilePic;
    private Button mCallUber;
    private RadioButton mRadioButton;
    private RadioGroup mRadioGroup;

    private final int LOCATION_REQUEST_CODE = 1;
    private int mRadius = 1;
    private boolean mDriverFound = false;
    private boolean mRequestBol = false;
    private String mDriverFoundID;
    private String mDestination;
    private String mRequestService;
    private String mRiderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);

        mDriverName = findViewById(R.id.r_driver_name);
        mDriverPhoneNumber = findViewById(R.id.r_driver_phone_number);
        mDriverCar = findViewById(R.id.r_driver_car);
        mDriverProfilePic = findViewById(R.id.r_driver_profile_pic);
        mDriverInfo = findViewById(R.id.r_driver_info);
        mRadioGroup = findViewById(R.id.rider_radio_group);

        mRadioGroup.check(R.id.rider_uber_x);

        mRiderId = FirebaseAuth.getInstance().getUid();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rider_map);

        // rider logout button
        Button mLogout = findViewById(R.id.rider_logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // move to MainActivity
                Intent intent = new Intent(RiderMapActivity.this, RiderLoginActivity.class);
                startActivity(intent);

                // Signing out user
                FirebaseAuth.getInstance().signOut();
            }
        });

        Button mSettings = findViewById(R.id.rider_settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RiderMapActivity.this, RiderSettingActivity.class);
                startActivity(intent);
            }
        });

        // Request Uber and set user location to RiderRequest node under user_id
        mCallUber = findViewById(R.id.call_uber);
        mCallUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callUber();
            }
        });

        // Cancel Uber request and remove user location from RiderRequest node
        ImageView mCancelUber = findViewById(R.id.cancel_uber);
        mCancelUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelUberRequest();
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                mDestination = place.getName().toString();
                mDestinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });

        // request permission to access gps
        requestContactsPermission();
    }

    public void requestContactsPermission() {

        Log.d("permission", "requestContactsPermission()");

        // get permission if it is not granted
        if (ContextCompat.checkSelfPermission(RiderMapActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d("permission", "access permission");

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            Log.d("permission", "loading map");

            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("permission", "loading map");

                    mMapFragment.getMapAsync(this);
                } else {

                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void callUber() {
        if (mRequestBol == false) {
            mRequestBol = true;

            mRadioButton = findViewById(mRadioGroup.getCheckedRadioButtonId());

            mRequestService = mRadioButton.getText().toString();

            // get reference of RiderRequest node
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference(AppConstants.RIDERS_REQUEST);

            // adding rider's current location to database under RidersRequest node
            GeoFire geoFire = new GeoFire(reference);
            geoFire.setLocation(mRiderId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

            // marker is added to rider's map
            mPickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mPickupMarker = mMap.addMarker(new MarkerOptions().position(mPickupLocation).title("Pick Here")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.rider_location)));

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
                        .child(AppConstants.USERS).child(AppConstants.DRIVERS).child(mDriverFoundID)
                        .child(AppConstants.RIDER_REQUEST);

                // remove the assigned rider from driver
                reference.removeValue();
                mDriverFoundID = null;
            }

            // reset values to default
            mDriverFound = false;
            mRadius = 1;

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(AppConstants.RIDERS_REQUEST);

            // remove location of rider form RiderRequest node
            GeoFire geoFire = new GeoFire(reference);
            geoFire.removeLocation(mRiderId);

            // remove rider pickup marker on riders's map
            if (mPickupMarker != null) {
                mPickupMarker.remove();
            }

            // remove assigned driver marker from rider's map
            if (mDriverMarker != null) {
                mDriverMarker.remove();
            }

            mCallUber.setText("Call Uber");

            mDriverInfo.setVisibility(View.GONE);
            mDriverName.setText("");
            mDriverPhoneNumber.setText("");
            mDriverCar.setText("");
            mDriverProfilePic.setImageResource(R.drawable.unknown_profile_pic);
        }
    }

    // get closest driver
    private void getClosestDriver() {
        // get mDriverLocationReference of DriversAvailable node
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(AppConstants.DRIVERS_AVAILABLE);

        // put mDriverLocationReference to GeoFire
        GeoFire geoFire = new GeoFire(reference);

        // check the nearest drive with given mRadius
        mGeoQuery = geoFire.queryAtLocation(new GeoLocation(mPickupLocation.latitude,
                mPickupLocation.longitude), mRadius);

        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, GeoLocation location) {
                if (!mDriverFound && mRequestBol) {

                    DatabaseReference mRiderDatabase = FirebaseDatabase.getInstance().getReference()
                            .child(AppConstants.USERS).child(AppConstants.DRIVERS).child(key).child(AppConstants.USER_DETAILS);

                    mRiderDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();

                                if (mDriverFound) {
                                    return;
                                }

                                if (driverMap.get(AppConstants.SERVICE).equals(mRequestService)) {
                                    mDriverFound = true;
                                    mDriverFoundID = key;

                                    Log.d("uber123", "mDriverFoundID: " + mDriverFoundID);

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                                            .child(AppConstants.USERS).child(AppConstants.DRIVERS).child(mDriverFoundID)
                                            .child(AppConstants.RIDER_REQUEST);

                                    HashMap map = new HashMap();
                                    map.put(AppConstants.RIDER_ID, mRiderId);
//                                    map.put(AppConstants.DESTINATION, mDestination);
//                                    map.put(AppConstants.DESTINATION_LATITUDE, mDestinationLatLng.latitude);
//                                    map.put(AppConstants.DESTINATION_LONGITUDE, mDestinationLatLng.longitude);
                                    driverRef.updateChildren(map);

                                    mCallUber.setText("Looking for driver location...");

                                    // get exact location of driver
                                    getDriverLocation();

                                    // get assigned driver info and show on rider's map
                                    getDriverInfo();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
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

    // get assigned driver info and show on rider's map
    private void getDriverInfo() {

        mDriverInfo.setVisibility(View.VISIBLE);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child(AppConstants.USERS).child(AppConstants.DRIVERS).child(mDriverFoundID)
                .child(AppConstants.USER_DETAILS);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get(AppConstants.NAME) != null) {
                        mDriverName.setText(map.get(AppConstants.NAME).toString().toUpperCase());
                    }

                    if (map.get(AppConstants.PHONE_NUMBER) != null) {
                        mDriverPhoneNumber.setText(map.get(AppConstants.PHONE_NUMBER).toString());
                    }

                    if (map.get(AppConstants.DRIVER_CAR) != null) {
                        mDriverCar.setText(map.get(AppConstants.DRIVER_CAR).toString().toUpperCase());
                    }

                    if (map.get(AppConstants.PROFILE_IMAGE_URL) != null) {
                        Picasso.with(getApplication())
                                .load(map.get(AppConstants.PROFILE_IMAGE_URL).toString())
                                .placeholder(R.drawable.progress_animation)
                                .into(mDriverProfilePic);
                    } else {
                        mDriverProfilePic.setImageResource(R.drawable.unknown_profile_pic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // get exact location of driver
    private void getDriverLocation() {

        mDriverLocationReference = FirebaseDatabase.getInstance().getReference().child(AppConstants.DRIVERS_WORKING)
                .child(mDriverFoundID).child(AppConstants.LOCATION);

        mDriverLocationReferenceListener = mDriverLocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && mRequestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

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
                        mCallUber.setText("Driver Found " + String.valueOf(distance));
                    }

                    // add a marker of assigned driver on rider's map
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.driver_car)));
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        mLastLocation = location;

        // update location of rider on location changed
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishAffinity();
    }
}