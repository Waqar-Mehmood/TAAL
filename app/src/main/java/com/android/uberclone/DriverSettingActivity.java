package com.android.uberclone;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;

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

public class DriverSettingActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;

    private EditText mDriverName;
    private EditText mDriverNumber;
    private EditText mDriverCar;
    private ImageView mDriverProfileImage;
    private Button mDriverSaveInfo;
    private RadioButton mRadioButton;
    private RadioGroup mRadioGroup;

    private static final int RC_PHOTO_PICKER = 2;

    private String mDriverID;
    private String mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setting);

        mDriverName = findViewById(R.id.driver_name);
        mDriverNumber = findViewById(R.id.driver_number);
        mDriverCar = findViewById(R.id.driver_car);
        mDriverProfileImage = findViewById(R.id.driver_profile_image);
        mDriverSaveInfo = findViewById(R.id.save_driver_info);

        mRadioGroup = findViewById(R.id.driver_radio_group);
        mRadioGroup.check(R.id.driver_uber_x);

        mDriverID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child(AppConstants.USERS)
                .child(AppConstants.DRIVERS).child(mDriverID).child(AppConstants.USER_DETAILS);

        mDriverProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        mDriverName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mDriverSaveInfo.setEnabled(true);
                } else {
                    mDriverSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mDriverNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mDriverSaveInfo.setEnabled(true);
                } else {
                    mDriverSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mDriverCar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mDriverSaveInfo.setEnabled(true);
                } else {
                    mDriverSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mDriverSaveInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // save user info to database
                saveUserInfo();

                Intent intent = new Intent(DriverSettingActivity.this, DriverMapActivity.class);
                startActivity(intent);
            }
        });

        // get driver info and populate settings activity
        getUserInfo();
    }

    // save user info to database
    private void saveUserInfo() {

        mRadioButton = findViewById(mRadioGroup.getCheckedRadioButtonId());

        Map userInfo = new HashMap();

        // username, phone number, driver car
        userInfo.put(AppConstants.NAME, mDriverName.getText().toString());
        userInfo.put(AppConstants.PHONE_NUMBER, mDriverNumber.getText().toString());
        userInfo.put(AppConstants.DRIVER_CAR, mDriverCar.getText().toString());
        userInfo.put(AppConstants.SERVICE, mRadioButton.getText().toString());

        mDatabaseReference.updateChildren(userInfo);
    }

    // get driver info and populate settings activity
    private void getUserInfo() {

        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get(AppConstants.NAME) != null) {
                        mDriverName.setText(map.get(AppConstants.NAME).toString().toUpperCase());
                    }

                    if (map.get(AppConstants.PHONE_NUMBER) != null) {
                        mDriverNumber.setText(map.get(AppConstants.PHONE_NUMBER).toString());
                    }

                    if (map.get(AppConstants.DRIVER_CAR) != null) {
                        mDriverCar.setText(map.get(AppConstants.DRIVER_CAR).toString().toUpperCase());
                    }

                    if (map.get(AppConstants.SERVICE) != null) {
                        mService = map.get(AppConstants.SERVICE).toString();
                        switch (mService) {
                            case "UberX":
                                mRadioGroup.check(R.id.driver_uber_x);
                                break;
                            case "UberBlack":
                                mRadioGroup.check(R.id.driver_uber_black);
                                break;
                            case "UberXl":
                                mRadioGroup.check(R.id.driver_uber_xl);
                                break;
                        }
                    }

                    if (map.get(AppConstants.PROFILE_IMAGE_URL) != null) {
                        Picasso.with(getApplication())
                                .load(map.get(AppConstants.PROFILE_IMAGE_URL).toString())
                                .placeholder(R.drawable.progress_animation)
                                .into(mDriverProfileImage);
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
                    .child(AppConstants.PROFILE_IMAGES).child(AppConstants.DRIVERS).child(mDriverID);

            mDriverProfileImage.setImageURI(selectedImageUri);

            photoRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Uri imageUrl = taskSnapshot.getDownloadUrl();

                    Map userInfo = new HashMap();

                    if (imageUrl != null) {
                        userInfo.put(AppConstants.PROFILE_IMAGE_URL, imageUrl.toString());

                        mDatabaseReference.updateChildren(userInfo);
                    }
                }
            });
        }
    }
}
