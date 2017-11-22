package com.android.taal_rider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class RiderSettingActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;

    private EditText mRiderName;
    private EditText mRiderNumber;
    private ImageView mRiderProfileImage;
    private Button mRiderSaveInfo;

    private static final int RC_PHOTO_PICKER = 2;

    private String mRiderID;
    private String mImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_setting);

        mRiderName = findViewById(R.id.rider_name);
        mRiderNumber = findViewById(R.id.rider_number);
        mRiderProfileImage = findViewById(R.id.rider_profile_image);
        mRiderSaveInfo = findViewById(R.id.save_rider_info);

        mRiderID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child(AppConstants.USERS)
                .child(AppConstants.RIDERS).child(mRiderID).child(AppConstants.USER_DETAILS);
        mDatabaseReference.keepSynced(true);

        mRiderProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        mRiderName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().length() > 0) {
                    mRiderSaveInfo.setEnabled(true);
                } else {
                    mRiderSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mRiderNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().length() > 0) {
                    mRiderSaveInfo.setEnabled(true);
                } else {
                    mRiderSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mRiderSaveInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save user info to database
                saveUserInfo();
            }
        });

        // get rider info and populate settings activity
        getRiderInfo();
    }

    // save user data into database
    private void saveUserInfo() {
        // username, phone number, image url
        Map userInfo = new HashMap();

        userInfo.put(AppConstants.NAME, mRiderName.getText().toString());
        userInfo.put(AppConstants.PHONE_NUMBER, mRiderNumber.getText().toString());
        userInfo.put(AppConstants.PROFILE_IMAGE_URL, mImageUrl);

        mDatabaseReference.updateChildren(userInfo, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Intent intent = new Intent(RiderSettingActivity.this, RiderMapActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    // get rider info and populate settings activity
    private void getRiderInfo() {

        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get(AppConstants.NAME) != null) {
                        mRiderName.setText(map.get(AppConstants.NAME).toString().toUpperCase());
                    }

                    if (map.get(AppConstants.PHONE_NUMBER) != null) {
                        mRiderNumber.setText(map.get(AppConstants.PHONE_NUMBER).toString());
                    }

                    if (map.get(AppConstants.PROFILE_IMAGE_URL) != null) {
                        Picasso.with(getApplication())
                                .load(map.get(AppConstants.PROFILE_IMAGE_URL).toString())
                                .placeholder(R.drawable.progress_animation)
                                .into(mRiderProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * onActivityResult processes the result of login requests
     **/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // request code for profile image
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            // get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = FirebaseStorage.getInstance().getReference()
                    .child(AppConstants.PROFILE_IMAGES).child(AppConstants.RIDERS).child(mRiderID);

            mRiderProfileImage.setImageURI(selectedImageUri);

            photoRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Uri imageUrl = taskSnapshot.getDownloadUrl();

                    if (imageUrl != null) {
                        mImageUrl = imageUrl.toString();
                    }
                }
            });
        }
    }
}
