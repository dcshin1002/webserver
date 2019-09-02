package com.lge.pickitup;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;


public class CourierLocationUploadService extends Service {
    private static String LOG_TAG = "CourierLocationUploadService";
    private String mCourierName = "";
    private String mTodayDateStr = "";

    private TmsCourierItem mTmsCourierItem;
    private FirebaseDatabaseConnector mFbConnector;
    private ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                mTmsCourierItem = postSnapshot.getValue(TmsCourierItem.class);
            }
            if (mTmsCourierItem == null) {
                Log.i(LOG_TAG, "mTmsCourierItem is null");
            } else {
                Log.i(LOG_TAG, "mTmsCourierItem.name is " + mTmsCourierItem.name);
            }

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    public CourierLocationUploadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(LOG_TAG, "onCreate() is called");
        Thread locationUploadThread = new Thread(new LocationUploader());
        locationUploadThread.start();
        mFbConnector = new FirebaseDatabaseConnector(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.e(LOG_TAG, "onStartCommand");

        mCourierName = intent.getStringExtra(Utils.KEY_COURIER_NAME).toString();
        mTodayDateStr = intent.getStringExtra(Utils.KEY_DB_DATE).toString();
        Query firebaseQuery = FirebaseDatabase.getInstance().getReference().child(FirebaseDatabaseConnector.COURIER_REF_NAME)
                              .child(mTodayDateStr).orderByChild(TmsCourierItem.KEY_NAME).equalTo(mCourierName);
        firebaseQuery.addValueEventListener(mValueEventListener);
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(LOG_TAG, "onBind(Intent intent) is called");
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private class LocationUploader implements Runnable {

        private Handler handler = new Handler();

        @Override
        public void run() {
            Log.e(LOG_TAG, "handler run is called");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.e(LOG_TAG, "Runnable run is called");
                    Log.e(LOG_TAG, "likepaul make log");
                    Timer timer = new Timer();
                    TimerTask TT = new TimerTask() {
                        @Override
                        public void run() {
                            Utils.setCurrentLocation();
                            if (mTmsCourierItem != null) {
                                mTmsCourierItem.latitude = String.valueOf(Utils.mCurrent.getLatitude());
                                mTmsCourierItem.longitude = String.valueOf(Utils.mCurrent.getLongitude());
                                Log.e(LOG_TAG, "likepaul :" + Utils.mCurrent.getLatitude() + " / " + Utils.mCurrent.getLongitude());
                                mFbConnector.postCourierItemToFirebaseDatabase(mTodayDateStr, mTmsCourierItem);
                            }

                        }
                    };
                    timer.schedule(TT, 0, 600000);
                }
            });
        }

    }
}

