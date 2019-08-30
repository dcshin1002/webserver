package com.lge.pickitup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "MainMenuActivity";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mCurrentUser;

    private TextView mTvAccountName;
    private TextView mTvSignOutText;
    private ImageView mIvConnStatus;

    private ImageView mIvUploadMenuIcon;
    private ImageView mIvViewParcleListIcon;
    private ImageView mIvViewItemPerCourierIcon;
    private ImageView mIvViewInMapIcon;
    private ImageView mIvCaptureAndUploadIcon;
    private ImageView mIvClusterAndRouteIcon;

    private TextView mTvUploadMenuTitle;
    private TextView mTvViewParcelListTitle;
    private TextView mTvViewItemPerCourierTitle;
    private TextView mTvViewInMapTitle;
    private TextView mTvCaptureAndUploadTitle;
    private TextView mTvClusterAndRouteTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_menu);
        initResources();

        //Get FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        //Make AuthStateListener to know oAuth's auth state
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    mCurrentUser = user;
                    Log.d(LOG_TAG, "onAuthStateChanged: signed in to UID :" + user.getUid());
                    Log.d(LOG_TAG, "onAuthStateChanged: signed in to email:" + user.getEmail());
                    Log.d(LOG_TAG, "onAuthStateChanged: signed in to display name:" + user.getDisplayName());
                } else {
                    mCurrentUser = null;
                    // User is signed out
                    Log.d(LOG_TAG, "onAuthStateChanged: signed_out");

                    Intent intent = new Intent(MainMenuActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                updateConnectUI();
            }
        };
    }

    private void updateConnectUI() {
        if (mCurrentUser != null) {
            mIvConnStatus.setImageDrawable(getDrawable(R.mipmap.activity_connect_account_settings_connected));
            mTvAccountName.setText(mCurrentUser.getDisplayName() + " (" + mCurrentUser.getEmail() + ")");
            mTvAccountName.setBackground(getDrawable(R.drawable.connected_account_border));
            mTvSignOutText.setBackground(getDrawable(R.drawable.active_border2));
            mTvSignOutText.setClickable(true);
        } else {
            mIvConnStatus.setImageDrawable(getDrawable(R.mipmap.activity_connect_account_settings_disconnected));
            mTvAccountName.setText(getText(R.string.disconnected_text));
            mTvAccountName.setBackground(getDrawable(R.drawable.disconnected_account_border));
            mTvSignOutText.setBackground(getDrawable(R.drawable.disconnected_account_border));
            mTvSignOutText.setClickable(false);
        }
    }

    private void initResources() {
        mTvAccountName = findViewById(R.id.conn_account_name);
        mIvConnStatus = findViewById(R.id.conn_image);
        mTvSignOutText = findViewById(R.id.sign_out_text);

        mTvSignOutText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder signOutAlert = new AlertDialog.Builder(MainMenuActivity.this);

                signOutAlert.setTitle(getText(R.string.sign_out_alert_title))
                        .setMessage(getText(R.string.sign_out_alert_message))
                        .setPositiveButton(getText(R.string.text_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mAuth.signOut();
                                dialogInterface.dismiss();
                            }
                        }).setNegativeButton(getText(R.string.text_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();

            }
        });

        mTvUploadMenuTitle = findViewById(R.id.upload_menu_title);
        mTvViewParcelListTitle = findViewById(R.id.parcel_list_menu_title);
        mTvViewItemPerCourierTitle = findViewById(R.id.courier_menu_title);
        mTvViewInMapTitle = findViewById(R.id.mapview_menu_title);

        mIvUploadMenuIcon = findViewById(R.id.upload_menu);
        mIvViewParcleListIcon = findViewById(R.id.parcel_list_menu);
        mIvViewItemPerCourierIcon = findViewById(R.id.courier_menu);
        mIvViewInMapIcon = findViewById(R.id.mapview_menu);
        mIvCaptureAndUploadIcon = findViewById(R.id.send_msg_and_upload_img);
        mIvClusterAndRouteIcon = findViewById(R.id.cluster_and_route);

        mIvUploadMenuIcon.setOnClickListener(this);
        mIvViewParcleListIcon.setOnClickListener(this);
        mIvViewItemPerCourierIcon.setOnClickListener(this);
        mIvViewInMapIcon.setOnClickListener(this);
        mIvCaptureAndUploadIcon.setOnClickListener(this);
        mIvClusterAndRouteIcon.setOnClickListener(this);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.upload_menu:
                Log.d(LOG_TAG, "onClick to upload menu");
                startActivity(new Intent(this, FileListActivity.class));
                break;

            case R.id.parcel_list_menu:
                Log.d(LOG_TAG, "onClick to parcel list menu");
                Intent intent = new Intent(this, ParcelListActivity.class);
                intent.putExtra(Utils.KEY_COURIER_NAME, getString(R.string.all_couriers));
                intent.putExtra(Utils.KEY_DB_DATE, Utils.getTodayDateStr());
                startActivity(intent);
                break;

            case R.id.courier_menu:
                Log.d(LOG_TAG, "onClick to courier menu");
                break;

            case R.id.mapview_menu:
                Log.d(LOG_TAG, "onClick to mapview menu");
                break;

            case R.id.send_msg_and_upload_img:
                startActivity(new Intent(this, UploadImageActivity.class));
                break;
            case R.id.cluster_and_route:
                Log.d(LOG_TAG, "onClick to cluster_and_route menu");
                startActivity(new Intent(this, ClusterAndRouteActivity.class));
                break;
        }
    }
}