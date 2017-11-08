package com.android.uberclone;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RiderLoginActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private static final String TAG = "UberClone";

    private Uri mImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
//                    addUserDetails(user);

                    Intent intent = new Intent(RiderLoginActivity.this, RiderMapActivity.class);
                    startActivity(intent);

                } else {
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

    private void addUserDetails(final FirebaseUser user) {
        final String user_id = user.getUid();
        mDatabaseReference.child(FirebaseConstants.USERS).child(FirebaseConstants.RIDERS).child(user_id).setValue(true);

        setContentView(R.layout.activity_rider_login);

        Button profile_image = findViewById(R.id.profile_image);
        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        Button done_button = findViewById(R.id.done_button);
        done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText user_phonenumber = findViewById(R.id.phone_number);
                String phoneNumber = user_phonenumber.getText().toString();

                if (!TextUtils.isEmpty(phoneNumber)) {

                    Map userInfo = new HashMap();
                    userInfo.put(FirebaseConstants.NAME, user.getDisplayName());
                    userInfo.put(FirebaseConstants.PHONE_NUMBER, phoneNumber);

                    if (mImageUrl != null) {
                        userInfo.put(FirebaseConstants.PROFILE_IMAGE_URL, mImageUrl.toString());
                    }

                    mDatabaseReference.child(FirebaseConstants.USERS).child(FirebaseConstants.RIDERS).child(user_id)
                            .updateChildren(userInfo);

                    Intent intent = new Intent(RiderLoginActivity.this, RiderMapActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(RiderLoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
    }

    /**
     * onActivityResult processes the result of login requests
     **/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // if login requset is successful show a toast,
            // otherwise tell user that the request failed and exit.
            if (resultCode == RESULT_OK) {
                Toast.makeText(RiderLoginActivity.this, "Signed in successfully!!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(RiderLoginActivity.this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // request code for profile image
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            final String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = FirebaseStorage.getInstance().getReference()
                    .child(FirebaseConstants.PROFILE_IMAGES).child(user_id);


            photoRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    mImageUrl = taskSnapshot.getDownloadUrl();
                }
            });
        }
    }
}