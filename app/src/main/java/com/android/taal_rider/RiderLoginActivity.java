package com.android.taal_rider;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

import static com.android.taal_rider.AppConstants.RIDERS;
import static com.android.taal_rider.AppConstants.USERS;
import static com.android.taal_rider.AppConstants.USER_DETAILS;

public class RiderLoginActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private DatabaseReference mDatabaseReference;
    private ValueEventListener mValueEventListener;

    private String mRiderId;

    private static final int RC_SIGN_IN = 1;

    static boolean mCalledAlready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize offline Firebase database
        if (!mCalledAlready) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            mCalledAlready = true;
        }

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Firebase Auth state listener for user Signup and Signin
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                // if user successfully logged in
                if (user != null) {

                    // get rider id
                    mRiderId = firebaseAuth.getUid();

                    // check rider details
                    checkUserDetails();
                }
                // otherwise show firebase signup page
                else {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(), RC_SIGN_IN);
                }
            }
        };
    }

    // check driver details
    private void checkUserDetails() {
        // add listener on Rider - user details to check weather data exists or not
        mValueEventListener = mDatabaseReference.child(USERS).child(RIDERS).child(mRiderId).child(USER_DETAILS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // if driver details exists than goto maps activity
                        if (dataSnapshot.exists()) {
                            Intent intent = new Intent(RiderLoginActivity.this, RiderMapActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        // otherwise goto settings activity
                        else {
                            Intent intent = new Intent(RiderLoginActivity.this, RiderSettingActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // if login requset is successful saver rider info to DB,
            // otherwise tell user that the request failed and exit.
            if (resultCode == RESULT_OK) {
                Toast.makeText(RiderLoginActivity.this, "Signed in successfully!!", Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                setContentView(R.layout.activity_rider_login);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null)
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

        if (mValueEventListener != null)
            mDatabaseReference.removeEventListener(mValueEventListener);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // exit app on back press
        finishAffinity();
    }
}