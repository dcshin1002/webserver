package com.lge.pickitup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ClusterAndRouteActivity extends AppCompatActivity implements View.OnClickListener, ProcessingCallback {
    private static final String LOG_TAG = "ClusterAndRouteActivity";
    private DatePickerDialog mDatePickerDialog;
    private TmsWASFragment networkFragment;
    private boolean processing = false;
    private SimpleDateFormat mSdf;
    private String mOldDateStr;

    private int mCourierNumber;
    private TextView mTextCourierNumber;
    private TextView mTextCourierDate;
    private Button mBtnClusterAndRoute;
    private View.OnTouchListener mTouchListner;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_and_route);

        mTextCourierNumber = findViewById(R.id.text_courier_number);
        mTextCourierDate = findViewById(R.id.text_courier_date2);
        mBtnClusterAndRoute = findViewById(R.id.btn_process_cluster_and_route);

        mTouchListner = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundColor(0xFFE91E63);
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.setBackgroundColor(0xFF42A5F5);
                }
                return false;
            }
        };
        mBtnClusterAndRoute.setOnTouchListener(mTouchListner);
        mBtnClusterAndRoute.setOnClickListener(this);
        mTextCourierDate.setOnClickListener(this);

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

        mSdf = new SimpleDateFormat("yyyy-MM-dd");

        mDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                mTextCourierDate.setText(mSdf.format(newDate.getTime()));
            }
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));

        networkFragment = TmsWASFragment.getInstance(getSupportFragmentManager(), "https://tmsproto-py.herokuapp.com/");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_process_cluster_and_route:
                Log.i(LOG_TAG, "button clicked. processing="+Boolean.toString(processing)+", networkFragment="+Boolean.toString(networkFragment!=null));
                if (!processing && networkFragment != null) {
                    Log.i(LOG_TAG, "Process Clustering & Routing");
                    processing = true;
                    networkFragment.startProcess();
                }
                break;
            case R.id.text_courier_date2:
                mDatePickerDialog.show();
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
        Log.i(LOG_TAG, "onProgressUpdate(" +
                        Integer.toString(progressCode) + "," +
                        Integer.toString(percentComplete) + ")");
        switch(progressCode) {
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
    public void finishProcessing() {
        Log.i(LOG_TAG, "finishProcessing()");
        if (networkFragment != null) {
            networkFragment.cancelProcess();
        }
        processing = false;
    }
}