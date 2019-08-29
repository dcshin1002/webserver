package com.lge.pickitup;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "FileListActivity";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mCurrentUser;

    private TextView mTvAccountName;
    private TextView mTvSignOutText;
    private ImageView mIvConnStatus;
    private ImageButton mIbGoToParcelListBtn;

    private List<String> mFileList = new ArrayList<>();
    private ListView mLvFileListView;
//    private ArrayAdapter mFileListAdapter;

    private FileItemAdapter mFlAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
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
                }
                updateConnectUI();
            }
        };

        readFileList();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    private void initResources() {
        mTvAccountName = findViewById(R.id.conn_account_name);
        mIvConnStatus = findViewById(R.id.conn_image);
        mTvSignOutText = findViewById(R.id.sign_out_text);

        mTvSignOutText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder signOutAlert = new AlertDialog.Builder(FileListActivity.this);

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

        mIbGoToParcelListBtn = findViewById(R.id.goto_parcel_list_btn);
        mLvFileListView = findViewById(R.id.file_listView);

        mIbGoToParcelListBtn.setOnClickListener(this);

//        mFileListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mFileList);
        mFlAdapter = new FileItemAdapter(FileListActivity.this, R.layout.file_listview_row, mFileList);
//        mLvFileListView.setAdapter(mFileListAdapter);
        mLvFileListView.setAdapter(mFlAdapter);
        mLvFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String selected = (String) adapterView.getItemAtPosition(position);

                AddressFacade addressPoster = new AddressFacade(FileListActivity.this);
                addressPoster.init(selected);
            }
        });
    }

    private void updateConnectUI() {
        if (mCurrentUser != null) {
            mIvConnStatus.setImageDrawable(getDrawable(R.mipmap.activity_connect_account_settings_connected));
            mTvAccountName.setText(mCurrentUser.getDisplayName() + " (" +mCurrentUser.getEmail() +")");
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.goto_parcel_list_btn :
                startActivity(new Intent(FileListActivity.this, ParcelListActivity.class));
                break;

            default:
                Log.e(LOG_TAG, "Something wrong, unknown view id");
                break;
        }
    }

    private void readFileList() {
        String sdcardPath = null;
        String sdcardStat = Environment.getExternalStorageState();
        if(sdcardStat.equals(Environment.MEDIA_MOUNTED)) {
            sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File addrDir = new File(sdcardPath + "/address/") ;
            if (addrDir.exists() && addrDir.isDirectory()) {
                File[] files = addrDir.listFiles();
                if (files != null) {
                    Log.i(LOG_TAG, files.length + " will be listed");
                    for (File file : files) {
                        Log.i(LOG_TAG, "file name is " + file.getName());
                        mFileList.add(file.getName());
                    }
                }
            }
            mFlAdapter.notifyDataSetChanged();
        }
    }

    private class FileItemAdapter extends ArrayAdapter<String> {

        public FileItemAdapter(Context context, int resource, List<String> list) {
            super(context, resource, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(LOG_TAG, "getView, position = " + position);
            View v = convertView;

            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.file_listview_row, null);
            }

            String itemStr = getItem(position);
            Log.d(LOG_TAG, "getView itemString = " + itemStr);

            if (itemStr != null) {
                TextView tvFileName = v.findViewById(R.id.file_name_text);

                if (tvFileName != null) {
                    tvFileName.setText(itemStr);
                }
            }

            return v;
        }
    }
}
