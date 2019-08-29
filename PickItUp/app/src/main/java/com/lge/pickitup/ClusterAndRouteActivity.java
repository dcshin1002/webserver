package com.lge.pickitup;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ClusterAndRouteActivity extends AppCompatActivity implements View.OnClickListener, ProcessingCallback {
    private static final String LOG_TAG = "ClusterAndRouteActivity";
    private final Calendar myCalendar = Calendar.getInstance();
    private DatePickerDialog mDatePickerDialog;
    private TmsWASFragment networkFragment;
    private boolean processing = false;
    private SimpleDateFormat mSdf;
    private String mOldDateStr;
    private int mCourierNumber;
    private TextView mTextCourierNumber;
    private TextView mTextCourierDate;
    private Button mBtnClusterAndRoute;
    private Button mBtnMatchingCourierSection;
    private View.OnTouchListener mTouchListner;
    private FirebaseDatabaseConnector mFbConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_and_route);

        mTextCourierNumber = (TextView) findViewById(R.id.text_courier_number);
        mTextCourierDate = (TextView) findViewById(R.id.text_courier_date2);
        mBtnClusterAndRoute = (Button) findViewById(R.id.btn_process_cluster_and_route);
        mBtnMatchingCourierSection = (Button) findViewById(R.id.btn_matchingCourierSection);

        mTouchListner = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mTextCourierNumber.setText(Integer.toString(mCourierNumber));
                }
                return false;
            }
        };

        mBtnClusterAndRoute.setOnClickListener(this);
        mTextCourierDate.setOnClickListener(this);
        mBtnMatchingCourierSection.setOnClickListener(this);
        //mTextCourierNumber.setOnTouchListener(mTouchListner);
        mTextCourierNumber.setEnabled(false);

        Bundle b = getIntent().getExtras();
        String dateStr;
        mOldDateStr = mTextCourierDate.getText().toString();

        if (b != null) {
            dateStr = b.getString(Utils.KEY_DB_DATE);
            mCourierNumber = Integer.parseInt(b.getString(Utils.KEY_COURIER_NUMBER));
        } else {
            dateStr = Utils.getTodayDateStr();
            mCourierNumber = -1;
        }

        mTextCourierDate.setText(dateStr);
        if (mCourierNumber == -1) {
            mTextCourierNumber.setText(getString(R.string.default_courier_number));
        } else {
            mTextCourierNumber.setText(Integer.toString(mCourierNumber));
        }

        // initially disabled buttons
        mBtnClusterAndRoute.setEnabled(false);
        mBtnMatchingCourierSection.setEnabled(false);

        mSdf = new SimpleDateFormat("yyyy-MM-dd");

        mDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                mTextCourierDate.setText(mSdf.format(newDate.getTime()));

                // parcel list gathering
                mFbConnector.getJobStatusFromFirebaseDatabase(mTextCourierDate.getText().toString(), new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long numElements = dataSnapshot.getChildrenCount();
                        if (numElements == 0) {
                            mBtnClusterAndRoute.setEnabled(false);
                            mBtnMatchingCourierSection.setEnabled(false);
                        } else {
                            mBtnClusterAndRoute.setEnabled(true);
                            TmsStatusItem jobStatus = dataSnapshot.getValue(TmsStatusItem.class);
                            if (jobStatus.route_job.equals("started") || jobStatus.route_job.equals("queued")) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ClusterAndRouteActivity.this);
                                builder.setTitle(getString(R.string.cluster_route_str));
                                builder.setMessage(String.format(getResources().getString(R.string.alert_message_running_clustering), mTextCourierDate.getText().toString()));
                                builder.setCancelable(true);
                                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                builder.show();
                            }
                            if (jobStatus.route_job.equals("init") || jobStatus.route_job.equals("finished")) {
                                mBtnClusterAndRoute.setEnabled(true);
                            } else {
                                mBtnClusterAndRoute.setEnabled(false);
                            }
                            if (jobStatus.route_job.equals("finished")) {
                                mBtnMatchingCourierSection.setEnabled(true);
                            } else {
                                mBtnMatchingCourierSection.setEnabled(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(LOG_TAG, "getParcelListFromFirebaseDatabase, ValueEventListener.onCancelled", databaseError.toException());
                    }
                });
            }
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));

        networkFragment = TmsWASFragment.getInstance(getSupportFragmentManager(), "https://tmsproto-py.herokuapp.com");

        mFbConnector = new FirebaseDatabaseConnector(this);

        // parcel list gathering
        mFbConnector.getJobStatusFromFirebaseDatabase(mTextCourierDate.getText().toString(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long numElements = dataSnapshot.getChildrenCount();
                if (numElements > 0) {
                    TmsStatusItem jobStatus = dataSnapshot.getValue(TmsStatusItem.class);

                    if (jobStatus.route_job.equals("started") || jobStatus.route_job.equals("queued")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ClusterAndRouteActivity.this);
                        builder.setTitle(getString(R.string.cluster_route_str));
                        builder.setMessage(String.format(getResources().getString(R.string.alert_message_running_clustering), mTextCourierDate.getText().toString()));
                        builder.setCancelable(true);
                        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        builder.show();
                    }
                    if (jobStatus.route_job.equals("init") || jobStatus.route_job.equals("finished")) {
                        mBtnClusterAndRoute.setEnabled(true);
                    } else {
                        mBtnClusterAndRoute.setEnabled(false);
                    }
                    if (jobStatus.route_job.equals("finished")) {
                        mBtnMatchingCourierSection.setEnabled(true);
                    } else {
                        mBtnMatchingCourierSection.setEnabled(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(LOG_TAG, "getParcelListFromFirebaseDatabase, ValueEventListener.onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_process_cluster_and_route:
                if (!processing && networkFragment != null) {
                    try {
                        int update = -1;
                        if (!mTextCourierNumber.getText().toString().equals(getString(R.string.default_courier_number))) {
                            update = Integer.parseInt(mTextCourierNumber.getText().toString());
                        }
                        mCourierNumber = update;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                    mBtnClusterAndRoute.setEnabled(false);
                    processing = true;
                    networkFragment.startProcess(mTextCourierDate.getText().toString(), mCourierNumber);

                    AlertDialog.Builder builder = new AlertDialog.Builder(ClusterAndRouteActivity.this);
                    builder.setTitle(getString(R.string.cluster_route_str));
                    builder.setMessage(String.format(getResources().getString(R.string.alert_message_start_distribution), mTextCourierDate.getText().toString()));
                    builder.setCancelable(true);
                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    builder.show();
                    Log.i(LOG_TAG, "Process Clustering & Routing");
                }
                break;
            case R.id.text_courier_date2:
                mDatePickerDialog.show();
                break;
            case R.id.btn_matchingCourierSection:
                Intent intent = new Intent(this, CourierSectionMatchingActivity.class);
                intent.putExtra(Utils.KEY_DB_DATE, mTextCourierDate.getText().toString());
                startActivity(intent);
                break;
        }
    }

    @Override
    public void updateFromProcess(Object result) {
        Log.i(LOG_TAG, "Clustering & Routing process updated !!");
        // Update your UI here based on result of download.
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch (progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                Log.i(LOG_TAG, "Clustering & Routing process encounter ERROR !!");
                break;
            case Progress.CONNECT_SUCCESS:
                Log.i(LOG_TAG, "Clustering & Routing process connected !!");
                break;
            case Progress.PROCESS_IN_PROGRESS:
                Log.i(LOG_TAG, "Clustering & Routing process is in progress !!");
                break;
            case Progress.PROCESS_SUCCESS:
                Log.i(LOG_TAG, "Clustering & Routing process success !!");
                break;
        }
    }

    @Override
    public void finishProcessing(boolean sucess) {
        Log.i(LOG_TAG, "finishProcessing()");
        if (networkFragment != null) {
            networkFragment.cancelProcess();
        }
        processing = false;
        Toast.makeText(ClusterAndRouteActivity.this,
                (sucess ? "완료" : "실패"), Toast.LENGTH_SHORT).show();
        mBtnClusterAndRoute.setEnabled(true);
    }
}