package com.android.uberclone;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

    private TextView mContactName;
    private TextView mContactNumber;
    private ImageView mProfileImage;
    private Button mSaveInfo;

    private static final int RC_PHOTO_PICKER = 2;
    private static final String TAG = "uber clone";

    private String mUserID;
    private Boolean mCheckOnce = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_setting);

        mContactName = findViewById(R.id.contact_name);
        mContactNumber = findViewById(R.id.contact_number);
        mProfileImage = findViewById(R.id.profile_image);
        mSaveInfo = findViewById(R.id.save_info_done_button);

        mUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.USERS)
                .child(FirebaseConstants.RIDERS).child(mUserID).child(FirebaseConstants.USER_DETAILS);

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        mContactName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSaveInfo.setEnabled(true);
                } else {
                    mSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mContactNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSaveInfo.setEnabled(true);
                } else {
                    mSaveInfo.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mSaveInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContactName.getText().toString().trim().length() > 0) {
                    Map userInfo = new HashMap();

                    userInfo.put(FirebaseConstants.NAME, mContactName.getText().toString());

                    mDatabaseReference.updateChildren(userInfo);
                }

                if (mContactNumber.getText().toString().trim().length() > 0) {
                    Map userInfo = new HashMap();

                    userInfo.put(FirebaseConstants.PHONE_NUMBER, mContactNumber.getText().toString());

                    mDatabaseReference.updateChildren(userInfo);

                }

                Intent intent = new Intent(RiderSettingActivity.this, RiderMapActivity.class);
                startActivity(intent);
            }
        });

        getUserInfo();

    }

    private void getUserInfo() {

        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mCheckOnce) {
                    mCheckOnce = false;
                    if (dataSnapshot.exists()) {
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                        if (map.get(FirebaseConstants.NAME) != null) {
                            mContactName.setText(map.get(FirebaseConstants.NAME).toString().toUpperCase());
                        }

                        if (map.get(FirebaseConstants.PHONE_NUMBER) != null) {
                            mContactNumber.setText(map.get(FirebaseConstants.PHONE_NUMBER).toString().toUpperCase());
                        }

                        if (map.get(FirebaseConstants.PROFILE_IMAGE_URL) != null) {
                            Picasso.with(getApplication())
                                    .load(map.get(FirebaseConstants.PROFILE_IMAGE_URL).toString().toUpperCase())
                                    .placeholder(R.drawable.progress_animation)
                                    .into(mProfileImage);
                        }
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
                    .child(FirebaseConstants.PROFILE_IMAGES).child(FirebaseConstants.RIDERS).child(mUserID);

            mProfileImage.setImageURI(selectedImageUri);

            photoRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Uri imageUrl = taskSnapshot.getDownloadUrl();

                    Map userInfo = new HashMap();

                    if (imageUrl != null) {
                        userInfo.put(FirebaseConstants.PROFILE_IMAGE_URL, imageUrl.toString());

                        mDatabaseReference.updateChildren(userInfo);
                    }
                }
            });
        }
    }
}
