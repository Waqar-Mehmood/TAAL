package com.example.pak_pc.sampleuber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {
    private EditText email,password;
    private Button login,register;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);



            firebaseAuth = FirebaseAuth.getInstance();
            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user !=null){
                        Intent intent = new Intent(CustomerLoginActivity.this,CustomerMapActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            };

            email = findViewById(R.id.email);
            password = findViewById(R.id.password);
            login = findViewById(R.id.login);
            register = findViewById(R.id.register);

            register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String Email =  email.getText().toString();
                    final String Password= password.getText().toString();
                    firebaseAuth.createUserWithEmailAndPassword(Email,Password).addOnCompleteListener
                            (CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(!task.isSuccessful() ){
                                       // Log.e(TAG, "onComplete: ", );
                                        Toast.makeText(CustomerLoginActivity.this,"Sign up error",Toast.LENGTH_LONG).show();
                                    }else {
                                        String user_id = firebaseAuth.getCurrentUser().getUid();
                                        DatabaseReference user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
                                        user_db.setValue(true);
                                    }

                                }
                            });
                }
            });

            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String Email =  email.getText().toString();
                    final String Password= password.getText().toString();
                    firebaseAuth.signInWithEmailAndPassword(Email,Password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful() ) {
                                Toast.makeText(CustomerLoginActivity.this, "Sign in error", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                }
            });

        }
        @Override
        protected void onStart(){
            super.onStart();
            firebaseAuth.addAuthStateListener(authStateListener);
        }
        @Override
        protected void onStop(){
            super.onStop();
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }