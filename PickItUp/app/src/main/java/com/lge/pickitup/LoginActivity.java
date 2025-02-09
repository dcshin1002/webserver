package com.lge.pickitup;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG = "LoginActivity";

    private static final String ERROR_STR_INVALID_EMAIL = "ERROR_INVALID_EMAIL";
    private static final String ERROR_STR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND";
    private static final String ERROR_STR_EMPTY_INPUTS = "ERROR_EMPTY_INPUTS";
    private static final String ERROR_STR_USER_DISABLED = "ERROR_USER_DISABLED";
    private static final String ERROR_STR_WRONG_PASSWORD = "ERROR_WRONG_PASSWORD";
    private static final String ERROR_STR_NETWORK_FAIL = "ERROR_NETWORK_CONNECTION_FAIL";
    final int MY_PERMISSIONS = 999;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private AlertDialog.Builder mAuthErrDialog;

    private EditText mEditTextEmail;
    private EditText mEditTextPassword;
    private Button mBtnSignIn;
    private Button mBtnCreateAccount;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Display App signing key to access Kakao APIs
        Log.i(LOG_TAG, "Signing key =  " + Utils.getKeyHash(this));

        // Initialize required resources
        initResources();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)  != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA},
                    MY_PERMISSIONS);
        } else {
            Utils.initLocation(this);
        }
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    Utils.mCurrentUser = user;
                    Utils.mCurrentUserItem = Utils.mHashUserList.get(user.getUid());
                    mEditTextEmail.setText("");
                    mEditTextPassword.setText("");

                    if (Utils.isRootAuth() || Utils.isConsignorAuth()) {
                        startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                    } else {
                        Intent intent_service = new Intent(LoginActivity.this, CourierLocationUploadService.class);
                        intent_service.putExtra(Utils.KEY_COURIER_NAME, user.getDisplayName());
                        intent_service.putExtra(Utils.KEY_DB_DATE, Utils.getTodayDateStr());
                        startService(intent_service);

                        Intent intent = new Intent(LoginActivity.this, ParcelListActivity.class);
                        intent.putExtra(Utils.KEY_COURIER_NAME, user.getDisplayName());
                        intent.putExtra(Utils.KEY_DB_DATE, Utils.getTodayDateStr());
                        startActivity(intent);
                    }
                    finish();
                } else {
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
        Utils.getUserListFromFirebase(mAuth, mAuthListener);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED
                ) {
                    Utils.initLocation(this);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void initResources() {
        mEditTextEmail = findViewById(R.id.editEmail);
        mEditTextPassword = findViewById(R.id.editPassword);
        mEditTextEmail.setText("");
        mEditTextPassword.setText("");
        mBtnSignIn = findViewById(R.id.signInButton);
        mBtnCreateAccount = findViewById(R.id.createAccountButton);

        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "Sign-in button is pressed");

                String email = mEditTextEmail.getText().toString();
                String password = mEditTextPassword.getText().toString();

                if (email != null && password != null) {
                    if (!email.isEmpty() && !password.isEmpty()) {

                        Log.d(LOG_TAG, "trySignInWithEmail");
                        startSignInWithEmail(mEditTextEmail.getText().toString(),
                                mEditTextPassword.getText().toString());
                    } else {
                        showAuthErrorDialog(ERROR_STR_EMPTY_INPUTS);
                    }
                } else {
                    Log.d(LOG_TAG, "some field is empty");
                }
            }
        });

        mBtnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "Create user account text is pressed");
                startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        //register AuthStateListener


    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void startSignInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(LOG_TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
//                            Log.w(LOG_TAG, "signInWithEmail:failed", task.getException());
//                            Toast.makeText(LoginActivity.this, "Authentication failed",
//                            Toast.LENGTH_SHORT).show();
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                String code = e.getErrorCode();
                                Log.d(LOG_TAG, "FirebaseAuthInvalidUserException, Error code : " + code);
                                showAuthErrorDialog(code);
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                String code = e.getErrorCode();
                                Log.d(LOG_TAG, "FirebaseAuthInvalidCredentialsException, Error code : " + code);
                                showAuthErrorDialog(code);
                            } catch (FirebaseNetworkException e) {
                                Log.d(LOG_TAG, "FirebaseNetworkException, Error message : " + e.getMessage());
                                showAuthErrorDialog(ERROR_STR_NETWORK_FAIL);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(LOG_TAG, "onComplete, user\'s UID = " + user.getUid() + ", display name= " + user.getDisplayName());
                            }

                        }
                    }
                });
    }

    private boolean isValidEmail(String target) {
        if (target == null || TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    private void showAuthErrorDialog(String err) {
        mAuthErrDialog = new AlertDialog.Builder(LoginActivity.this);

        if (err.equals(ERROR_STR_INVALID_EMAIL)) {
            mAuthErrDialog.setTitle(getString(R.string.invalid_email_alert_title))
                    .setMessage(getString(R.string.invalid_email_alert_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_USER_NOT_FOUND)) {
            mAuthErrDialog.setTitle(getString(R.string.user_not_found_alert_title))
                    .setMessage(getString(R.string.user_not_found_alert_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_USER_DISABLED)) {
            mAuthErrDialog.setTitle(getString(R.string.user_disabled_alert_title))
                    .setMessage(getString(R.string.user_disabled_alert_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_WRONG_PASSWORD)) {
            mAuthErrDialog.setTitle(getString(R.string.wrong_password_alert_title))
                    .setMessage(getString(R.string.wrong_password_alert_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_EMPTY_INPUTS)) {
            mAuthErrDialog.setTitle(getString(R.string.empty_input_field_alert_title))
                    .setMessage(R.string.empty_input_field_alert_message)
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_NETWORK_FAIL)) {
            mAuthErrDialog.setTitle(getText(R.string.network_failure_title))
                    .setMessage(getText(R.string.network_failure_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        }
    }
}
