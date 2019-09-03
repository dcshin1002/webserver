package com.lge.pickitup;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String ERROR_STR_WEAK_PASSWORD = "ERROR_WEAK_PASSWORD";
    private static final String ERROR_STR_EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE";
    private static final String ERROR_STR_INVALID_CREDENTIAL = "ERROR_INVALID_CREDENTIAL";
    private static final String ERROR_STR_NETWORK_FAIL = "ERROR_NETWORK_CONNECTION_FAIL";

    private static final String LOG_TAG = "CreateAccountActivity";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private AlertDialog.Builder mAuthErrDialog;

    private TextView mTvAccount;
    private TextView mTvPassword;
    private TextView mTvPasswordConfirm;
    private TextView mTvDisplayname;
    private TextView mTvPhoneNum;

    private EditText mEtAccount;
    private EditText mEtPassword;
    private EditText mEtPasswordConfirm;
    private EditText mEtDisplayName;
    private EditText mEtPhoneNum;

    private Button mBtnCreateAccount;

    private ProgressDialog mProgDialog;
    private AlertDialog.Builder mAlertErrDialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        //Get FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        initResources();

        //Make AuthStateListener to know oAuth's auth state
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    private void initResources() {
        mTvAccount = findViewById(R.id.titleEmail);
        mTvPassword = findViewById(R.id.titlePassword);
        mTvPasswordConfirm = findViewById(R.id.titlePasswordConfirm);
        mTvDisplayname = findViewById(R.id.titleDisplayName);
        mTvPhoneNum = findViewById(R.id.titlMobileNum);

        mEtAccount = findViewById(R.id.editEmail);
        mEtPassword = findViewById(R.id.editPassword);
        mEtPasswordConfirm = findViewById(R.id.editPasswordConfirm);
        mEtDisplayName = findViewById(R.id.editDisplayName);
        mEtPhoneNum = findViewById(R.id.editMobileNum);

        mBtnCreateAccount = findViewById(R.id.btnCreateAccount);

        mBtnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCreateUserAccount(mEtAccount.getText().toString(), // account email
                        mEtPassword.getText().toString(),   // password
                        mEtPasswordConfirm.getText().toString(), // confirm password
                        mEtDisplayName.getText().toString(), // display name
                        mEtPhoneNum.getText().toString());  // phone number
            }
        });

        mAlertErrDialogBuilder = new AlertDialog.Builder(CreateAccountActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //register AuthStateListener
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //remove AuthStateListener
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void startCreateUserAccount(String email, String password,
                                        String confirmPassword, String displayName, String phoneNum) {
        if (!isValidEmail(email)) {
            Log.e(LOG_TAG, "startCreateUserAccount: email account input is invalid ");
            mAlertErrDialogBuilder.setTitle(getString(R.string.invalid_email_alert_title));
            mAlertErrDialogBuilder.setMessage(getString(R.string.invalid_email_alert_message));
            mAlertErrDialogBuilder.setPositiveButton(R.string.dialog_title_confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        if (!isValidPassword(password)) {
            Log.e(LOG_TAG, "startCreateUserAccount: password input is invalid");
            mAlertErrDialogBuilder.setTitle(getString(R.string.invalid_password_alert_title));
            mAlertErrDialogBuilder.setMessage(getString(R.string.invalid_password_alert_message));
            mAlertErrDialogBuilder.setPositiveButton(R.string.dialog_title_confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Log.e(LOG_TAG, "startCreateUserAccount: confirm password input is invalid");
            mAlertErrDialogBuilder.setTitle(getString(R.string.invalid_password_confirm_alert_title));
            mAlertErrDialogBuilder.setMessage(getString(R.string.invalid_password_confirm_alert_message));
            mAlertErrDialogBuilder.setPositiveButton(R.string.dialog_title_confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        if (!isValidDisplayName(displayName)) {
            Log.e(LOG_TAG, "starctCreateUserAccount: display input name is invalid");
            mAlertErrDialogBuilder.setTitle(getText(R.string.invalid_display_name_alert_title));
            mAlertErrDialogBuilder.setMessage(getText(R.string.invalid_display_name_alert_message));
            mAlertErrDialogBuilder.setPositiveButton(R.string.dialog_title_confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        if (!isValidPhoneNumber(phoneNum)) {
            Log.e(LOG_TAG, "startCreateUserAccount: phone number input is invalid");
            mAlertErrDialogBuilder.setTitle(getText(R.string.invalid_phone_num_alert_title));
            mAlertErrDialogBuilder.setMessage(getText(R.string.invalid_phone_num_alert_message));
            mAlertErrDialogBuilder.setPositiveButton(R.string.dialog_title_confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        showProgressDialog();

        final UserProfileChangeRequest pofileReq = new UserProfileChangeRequest.Builder()
                .setDisplayName(mEtDisplayName.getText().toString()).build();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(LOG_TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        hideProgressDialog();

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            user.updateProfile(pofileReq);
                            Log.d(LOG_TAG, "update user profile = " + user.getDisplayName());
                            if (Arrays.asList(Utils.ADMIN_UIDS).contains(user.getUid())) {
                                startActivity(new Intent(CreateAccountActivity.this, MainMenuActivity.class));
                            } else {
                                Intent intent_service = new Intent(CreateAccountActivity.this, CourierLocationUploadService.class);
                                intent_service.putExtra(Utils.KEY_COURIER_NAME, mEtDisplayName.getText().toString()); //user.getDisplayName()
                                intent_service.putExtra(Utils.KEY_DB_DATE, Utils.getTodayDateStr());
                                startService(intent_service);
                                Intent intent = new Intent(CreateAccountActivity.this, ParcelListActivity.class);
                                intent.putExtra(Utils.KEY_COURIER_NAME, mEtDisplayName.getText().toString()); //user.getDisplayName()
                                intent.putExtra(Utils.KEY_DB_DATE, Utils.getTodayDateStr());
                                startActivity(intent);
                            }

                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                String code = e.getErrorCode();
                                Log.d(LOG_TAG, "FirebaseAuthWeakPasswordException, Error code : " + code);
                                showAuthErrorDialog(code);
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                String code = e.getErrorCode();
                                Log.d(LOG_TAG, "FirebaseAuthInvalidCredentialsException, Error code : " + code);
                                showAuthErrorDialog(code);
                            } catch (FirebaseAuthUserCollisionException e) {
                                String code = e.getErrorCode();
                                Log.d(LOG_TAG, "FirebaseAuthUserCollisionException, Error code : " + code);
                                showAuthErrorDialog(code);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
    }

    private boolean isValidPassword(String target) {
        Pattern p = Pattern.compile("(^.*(?=.{6,100})(?=.*[0-9])(?=.*[a-zA-Z]).*$)");

        Matcher m = p.matcher(target);
        if (m.find() && !target.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isValidEmail(String target) {
        if (target == null || TextUtils.isEmpty(target)) {
            return false;
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    private boolean isValidDisplayName(String target) {
        if (target == null || TextUtils.isEmpty(target)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isValidPhoneNumber(String target) {
        if (target == null || TextUtils.isEmpty(target)) {
            return false;
        } else {
            return Patterns.PHONE.matcher(target).matches();
        }
    }

    private void showProgressDialog() {
        if (mProgDialog == null) {
            mProgDialog = new ProgressDialog(CreateAccountActivity.this);
            mProgDialog.setCancelable(false);
        }

        mProgDialog.setMessage(getText(R.string.creating_new_account));
        mProgDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgDialog != null && mProgDialog.isShowing()) {
            mProgDialog.dismiss();
        }
    }

    private void showAuthErrorDialog(String err) {
        mAuthErrDialog = new AlertDialog.Builder(CreateAccountActivity.this);

        if (err.equals(ERROR_STR_WEAK_PASSWORD)) {
            mAuthErrDialog.setTitle(getString(R.string.weak_passworkd_alert_title))
                    .setMessage(getString(R.string.weak_password_alert_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_EMAIL_ALREADY_IN_USE)) {
            mAuthErrDialog.setTitle(getString(R.string.email_already_in_use_alert_title))
                    .setMessage(getString(R.string.email_already_in_use_alert_message))
                    .setPositiveButton(R.string.dialog_title_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else if (err.equals(ERROR_STR_INVALID_CREDENTIAL)) {
            mAuthErrDialog.setTitle(getString(R.string.invalid_credential_alert_title))
                    .setMessage(getString(R.string.invalid_credential_alert_message))
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
