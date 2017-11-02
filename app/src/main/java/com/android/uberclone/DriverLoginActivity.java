package com.android.uberclone;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

public class DriverLoginActivity extends AppCompatActivity {

    private static final String TAG = "UberClone";
    private FirebaseDatabase mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mDatabaseRef;

    private static final int RC_SIGN_IN = 1;

    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mFirebaseAuth = FirebaseDatabase.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    Toast.makeText(DriverLoginActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();

                } else {
                    // if user is not logged in or isn't registered, verify phone number
                    EmailVerification();
                }
            }
        };

        mEmail = findViewById(R.id.driver_email);
        mPassword = findViewById(R.id.driver_password);
        mLogin = findViewById(R.id.driver_login);
        mRegistration = findViewById(R.id.driver_registration);
    }

    public void EmailVerification() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(
                                Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER)
                                        .build())).build(), RC_SIGN_IN);
    }

    @Override
    public void onPause() {
        super.onPause();
        // remove AuthStateListener when activity is paused
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
}
