package com.android.taal_rider;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.taal_rider.AppConstants.DESTINATION_LATITUDE;
import static com.android.taal_rider.AppConstants.DESTINATION_LONGITUDE;
import static com.android.taal_rider.AppConstants.DRIVERS;
import static com.android.taal_rider.AppConstants.DRIVERS_AVAILABLE;
import static com.android.taal_rider.AppConstants.DRIVER_RATING;
import static com.android.taal_rider.AppConstants.LOCATION;
import static com.android.taal_rider.AppConstants.RIDERS_REQUEST;
import static com.android.taal_rider.AppConstants.RIDER_ID;
import static com.android.taal_rider.AppConstants.RIDER_REQUEST;
import static com.android.taal_rider.AppConstants.RIDE_STATUS;
import static com.android.taal_rider.AppConstants.SERVICE;
import static com.android.taal_rider.AppConstants.USERS;
import static com.android.taal_rider.AppConstants.USER_DETAILS;

public class RiderMapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private static GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SupportMapFragment mMapFragment;

    private LatLng mPickupLocation;
    private LatLng mDestinationLatLng;
    private Marker mDriverMarker;
    private Marker mPickupMarker;
    private GeoQuery mGeoQuery;
    private DatabaseReference mDriverLocationReference;
    private DatabaseReference mDriveHasEndedRef;
    private ValueEventListener mDriverLocationReferenceListener;
    private ValueEventListener mDriveHasEndedRefListener;

    private LinearLayout mDriverInfo;
    private TextView mDriverName;
    private TextView mDriverPhoneNumber;
    private TextView mDriverCar;
    private ImageView mDriverProfilePic;
    private ImageView mCancelUber;
    private Button mCallUber;
    private Button mSettings;
    private Button mLogout;
    private Button mHistory;
    private RadioButton mRadioButton;
    private RadioGroup mRadioGroup;
    private RatingBar mRatingBar;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // if gps is disabled show a message dialog to turn on gps
        String mProvider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!mProvider.contains("gps")) {
            buildAlertMessageNoGps();
        }

        // initializing views
        mDriverName = findViewById(R.id.r_driver_name);
        mDriverPhoneNumber = findViewById(R.id.r_driver_phone_number);
        mDriverCar = findViewById(R.id.r_driver_car);
        mDriverProfilePic = findViewById(R.id.r_driver_profile_pic);
        mDriverInfo = findViewById(R.id.r_driver_info);
        mRatingBar = findViewById(R.id.r_driver_rating_bar);
        mRadioGroup = findViewById(R.id.rider_radio_group);
        mLogout = findViewById(R.id.rider_logout);
        mSettings = findViewById(R.id.rider_settings);
        mCallUber = findViewById(R.id.call_uber);
        mCancelUber = findViewById(R.id.cancel_uber);
        mHistory = findViewById(R.id.history);

        // radio button select to uber x onStart
        mRadioGroup.check(R.id.rider_uber_x);

        // get rider id
        mRiderId = FirebaseAuth.getInstance().getUid();

        // set destination latitude and longitude to 0, 0
        mDestinationLatLng = new LatLng(0.0, 0.0);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rider_map);

        // rider logout button
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // disconnect location services
                disconnectLocationService();

                // Signing out user
                FirebaseAuth.getInstance().signOut();

                // move to RiderMapActivity
                Intent intent = new Intent(RiderMapActivity.this, RiderLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // rider setting button
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RiderMapActivity.this, RiderSettingActivity.class);
                startActivity(intent);
            }
        });

        // Request Uber and set rider location to RiderRequest node under user id
        mCallUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callUber();
            }
        });

        // Cancel Uber request
        mCancelUber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRequestBol) {
                    endRide();
                }
            }
        });

        // History of previous rides
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RiderMapActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // google maps search bar
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // Get info about the selected place.
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

        // check for available drivers
//        availableDrivers();
    }

    // check for available drivers
    private void availableDrivers() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(DRIVERS_AVAILABLE);
        reference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists()) {
                    List<Object> map = (List<Object>) dataSnapshot.child(LOCATION).getValue();
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

                    // add a marker of assigned driver on rider's map
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.driver_car)));
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // remove the driver marker from rider's map
                if (mDriverMarker != null) {
                    mDriverMarker.remove();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // if gps is disabled alert box shows for
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("GPS Network Not Enabled")
                .setCancelable(false)
                .setTitle("GPS Alert")
                .setPositiveButton("Turn it On",
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    @SuppressWarnings("unused") final DialogInterface dialog,
                                    @SuppressWarnings("unused") final int id) {
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog,
                                                @SuppressWarnings("unused") final int id) {
                                dialog.cancel();

                            }
                        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void requestContactsPermission() {

        // get permission if it is not granted
        if (ContextCompat.checkSelfPermission(RiderMapActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {

            // if the permission is already granted initialize maps fragment
            mMapFragment.getMapAsync(this);
        }
    }

    // permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // if user granted the permission initialize maps fragment
                    mMapFragment.getMapAsync(this);
                } else {

                    // if user cancel the permission
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    // cancel uber request
    private void callUber() {
        if (mRequestBol == false) {
            mRequestBol = true;

            // initializing mRadioButton
            mRadioButton = findViewById(mRadioGroup.getCheckedRadioButtonId());

            // get the service, the rider want to use
            mRequestService = mRadioButton.getText().toString();

            // get reference of RiderRequest node
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference(RIDERS_REQUEST);

            // adding rider's current location to database under RidersRequest node
            GeoFire geoFire = new GeoFire(reference);
            geoFire.setLocation(mRiderId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

            // marker is added to rider's map
            mPickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mPickupMarker = mMap.addMarker(new MarkerOptions().position(mPickupLocation).title("Pick Here")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.rider_pickup_location)));

            mCallUber.setText("Getting your Drive");

            // get nearest driver on rider's request
            getClosestDriver();
        }
    }

    // get closest driver
    private void getClosestDriver() {
        // get reference of DriversAvailable node
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(DRIVERS_AVAILABLE);

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
                            .child(USERS).child(DRIVERS).child(key).child(USER_DETAILS);

                    mRiderDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();

                                if (driverMap.get(SERVICE).equals(mRequestService)) {
                                    mDriverFound = true;
                                    mDriverFoundID = key;

                                    Toast.makeText(RiderMapActivity.this,
                                            "Driver ID: " + mDriverFoundID, Toast.LENGTH_SHORT).show();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                                            .child(USERS).child(DRIVERS).child(mDriverFoundID).child(RIDER_REQUEST);

                                    HashMap map = new HashMap();
                                    map.put(RIDER_ID, mRiderId);
                                    map.put(AppConstants.DESTINATION, mDestination);
                                    map.put(DESTINATION_LATITUDE, mDestinationLatLng.latitude);
                                    map.put(DESTINATION_LONGITUDE, mDestinationLatLng.longitude);
                                    map.put(RIDE_STATUS, "");
                                    driverRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                            checkForRiderRequest();

                                        }
                                    });
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
                // if the driver not found on given mRadius then increase the mRadius and check again
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

    private void checkForRiderRequest() {
        Toast.makeText(RiderMapActivity.this,
                "checkForRiderRequest()", Toast.LENGTH_SHORT).show();

        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                .child(USERS).child(DRIVERS).child(mDriverFoundID).child(RIDER_REQUEST).child(RIDE_STATUS);

        driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String value = dataSnapshot.getValue(String.class);

                if (!TextUtils.isEmpty(value)) {
                    if (value.equals("true")) {
                        Toast.makeText(RiderMapActivity.this,
                                "driver accepted the ride", Toast.LENGTH_SHORT).show();

                        mCallUber.setText("Looking for driver location...");

                        // get exact location of driver
                        getDriverLocation();

                        // get assigned driver info and show on rider's map
                        getDriverInfo();

                        // if ride is ended
//                      getHasRideEnded();
                    } else if (value.equals("false")) {
                        Toast.makeText(RiderMapActivity.this,
                                "driver cancel the ride", Toast.LENGTH_SHORT).show();

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(USERS).child(DRIVERS)
                                .child(mDriverFoundID).child(RIDER_REQUEST);
                        reference.removeValue();

                        mDriverFound = false;
                        mDriverFoundID = "";
                        mRadius += 2;

                        getClosestDriver();
                    } else {
                        Toast.makeText(RiderMapActivity.this,
                                "not working", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // get assigned rider information
    private void getHasRideEnded() {

        // get reference of assigned Rider Id
        mDriveHasEndedRef = FirebaseDatabase.getInstance().getReference().child(USERS)
                .child(DRIVERS).child(mDriverFoundID).child(RIDER_REQUEST).child(RIDER_ID);

        mDriveHasEndedRefListener = mDriveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {


                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endRide() {
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
                    .child(USERS).child(DRIVERS).child(mDriverFoundID)
                    .child(RIDER_REQUEST);

            // remove the assigned rider from driver
            reference.removeValue();
            mDriverFoundID = null;
        }

        // reset values to default
        mDriverFound = false;
        mRadius = 1;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(RIDERS_REQUEST);

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

    // get assigned driver info and show on rider's map
    private void getDriverInfo() {

        // show driver details on rider maps
        mDriverInfo.setVisibility(View.VISIBLE);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child(USERS).child(DRIVERS).child(mDriverFoundID);

        // get values from user details node
        databaseReference.child(USER_DETAILS).addValueEventListener(new ValueEventListener() {
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

        // get values from rating node
        databaseReference.child(DRIVER_RATING).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int ratingSum = 0;
                    int ratingsTotal = 0;

                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }

                    if (ratingsTotal != 0) {
                        mRatingBar.setRating(ratingSum / ratingsTotal);
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

        mDriverLocationReference = FirebaseDatabase.getInstance().getReference().child(AppConstants.DRIVERS_AVAILABLE)
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
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.driver_car)));
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
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
    }

    // disconnect the driver
    public void disconnectLocationService() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finishAffinity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
