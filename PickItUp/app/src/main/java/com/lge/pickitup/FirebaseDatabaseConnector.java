package com.lge.pickitup;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FirebaseDatabaseConnector {
    private static final String LOG_TAG = "FirebaseConnector";
    private static final String PARCEL_REF_NAME = "parcel_list";
    private static final String COURIER_REF_NAME = "courier_list";
    private static final String REGISTERED_COURIER_REF_NAME = "registered_courier";
    private static final String JOB_STATUS_NAME = "backend_status";
    private Context mContext;
    private DatabaseReference mDatabaseRef;

    private HashMap<String, TmsParcelItem> mParcelHash;
    private HashMap<String, TmsCourierItem> mCourierHash;
    private ArrayList<String> mArrayKeys;
    private ArrayList<TmsParcelItem> mArrayValues;
    private ArrayAdapter<TmsParcelItem> mListAdapter;
    private ArrayList<String> mSectorList;

    public FirebaseDatabaseConnector(Context context) {
        this.mContext = context;
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
    }

    protected void setParcelHash(HashMap<String, TmsParcelItem> map) {
        if (map != null) {
            this.mParcelHash = map;
        } else {
            Log.e(LOG_TAG, "Given map should be not null");
            throw new NullPointerException();
        }
    }

    protected void setCourierHash(HashMap<String, TmsCourierItem> map) {
        if (map != null) {
            this.mCourierHash = map;
        } else {
            Log.e(LOG_TAG, "Given map should be not null");
            throw new NullPointerException();
        }
    }

    protected void setParcelKeyArray(ArrayList<String> keyArray) {
        if (keyArray != null) {
            this.mArrayKeys = keyArray;
        } else {
            Log.e(LOG_TAG, "Given keyArray should be not null");
            throw new NullPointerException();
        }
    }

    protected void setParcelValueArray(ArrayList<TmsParcelItem> valueArray) {
        if (valueArray != null) {
            this.mArrayValues = valueArray;
        } else {
            Log.e(LOG_TAG, "Given valueArray should be not null");
            throw new NullPointerException();
        }
    }

    protected void setListAdapter(ArrayAdapter<TmsParcelItem> listAdapter) {
        if (listAdapter != null) {
            this.mListAdapter = listAdapter;
        } else {
            Log.e(LOG_TAG, "Given valueArray should be not null");
            throw new NullPointerException();
        }
    }
    protected void setSectorList(ArrayList<String> sectorList) {
        if (sectorList != null) {
            this.mSectorList = sectorList;
        } else {
            Log.e(LOG_TAG, "Given valueArray should be not null");
            throw new NullPointerException();
        }
    }

    protected void postParcelListToFirebaseDatabase(String pathString, ArrayList<TmsParcelItem> list) {
        for (TmsParcelItem item : list) {
            postParcelItemToFirebaseDatabase(pathString, item);
        }
    }

    protected void postParcelListToFirebaseDatabase2(String pathString, ArrayList<TmsParcelItem> list) {
        DatabaseReference ref = mDatabaseRef.child(PARCEL_REF_NAME);
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;

        for (TmsParcelItem item : list) {
            postValues = item.toMap();
            childUpdates.put("/" + pathString + "/" + item.id, postValues);
        }
        ref.updateChildren(childUpdates);
    }

    protected void postParcelItemToFirebaseDatabase(String pathString, TmsParcelItem item) {
        DatabaseReference ref = mDatabaseRef.child(PARCEL_REF_NAME);
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;
        postValues = item.toMap();

        childUpdates.put("/" + pathString + "/" + item.id, postValues);
        ref.updateChildren(childUpdates);
    }

    protected void postCourierListToFirbaseDatabase(String pathString, ArrayList<TmsCourierItem> list) {
        for (TmsCourierItem item : list) {
            postCourierItemToFirebaseDatabase(pathString, item);
        }
    }

    protected  void postCourierItemToFirebaseDatabase(String pathString, TmsCourierItem item) {
        DatabaseReference ref = mDatabaseRef.child(COURIER_REF_NAME);
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;
        postValues = item.toMap();

        childUpdates.put("/" + pathString + "/" + item.id, postValues);
        ref.updateChildren(childUpdates);
    }

    protected void getCourierListFromFirebaseDatabase(String pathString, String orderBy) {
        getCourierListFromFirebaseDatabase(pathString, orderBy, null);
    }

    protected void getCourierListFromFirebaseDatabase(String pathString, String orderBy, String select) {
        Query firebaseQuery;

        if (TextUtils.isEmpty(select) || select.equals(mContext.getString(R.string.all_couriers)) || select == null) {
            firebaseQuery = mDatabaseRef.child(COURIER_REF_NAME).child(pathString).orderByChild(orderBy);
        } else {
            firebaseQuery = mDatabaseRef.child(COURIER_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(select);
        }

        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mCourierHash.clear();

                Log.d(LOG_TAG, "getCourierListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    TmsCourierItem value = postSnapshot.getValue(TmsCourierItem.class);

                    mCourierHash.put(key, value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(LOG_TAG,"getCouriersFromFirebase, ValueEventListener.onCancelled", databaseError.toException());
            }
        });
    }

    protected void getRegisteredCourierListFromFirebaseDatabase(String orderBy) {
        Query firebaseQuery;

        firebaseQuery = mDatabaseRef.child(REGISTERED_COURIER_REF_NAME).orderByChild(orderBy);

        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mCourierHash.clear();

                Log.d(LOG_TAG, "getCourierListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    TmsCourierItem value = postSnapshot.getValue(TmsCourierItem.class);

                    mCourierHash.put(key, value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(LOG_TAG,"getCouriersFromFirebase, ValueEventListener.onCancelled", databaseError.toException());
            }
        });
    }


    protected void getParcelListFromFirebaseDatabase(String pathString, String orderBy, String select, ValueEventListener listener) {
        Query firebaseQuery;
        if (TextUtils.isEmpty(select) || select.equals(mContext.getString(R.string.all_couriers)) || select == null) {
            firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(orderBy);
        } else {
            if (orderBy.equals(TmsParcelItem.KEY_SECTOR_ID)) {
                firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(Integer.valueOf(select));
            } else {
                firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(select);
            }
        }

        firebaseQuery.addListenerForSingleValueEvent(listener);
    }

    protected void getSectorListFromFirebaseDatabase(String pathString) {
        Query firebaseQuery;
        firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(TmsParcelItem.KEY_SECTOR_ID);
        ValueEventListener ve = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mSectorList.clear();
                Log.d(LOG_TAG, "getParcelListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
                HashSet<String> sectionSet = new HashSet<String>();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);
                    sectionSet.add(String.valueOf(value.sectorId));
                }
                for (String s : sectionSet) {
                    mSectorList.add(s);
                }
                Log.d(LOG_TAG, "mSectorList size = " + mSectorList.size());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        firebaseQuery.addListenerForSingleValueEvent(ve);
    }

    protected void getParcelListFromFirebaseDatabase(String pathString, String orderBy) {
        getParcelListFromFirebaseDatabase(pathString, orderBy, null);
    }

    protected void getParcelListFromFirebaseDatabase(final String pathString, String orderBy, String select) {
        Query firebaseQuery;
        if (TextUtils.isEmpty(select) || select.equals(mContext.getString(R.string.all_couriers)) || select == null) {
            firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(orderBy);
        } else {
            if (orderBy.equals(TmsParcelItem.KEY_SECTOR_ID)) {
                firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(Integer.valueOf(select));
            }else {
                firebaseQuery = mDatabaseRef.child(PARCEL_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(select);
            }
        }

        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mParcelHash.clear();
                mArrayKeys.clear();
                mArrayValues.clear();

                Log.d(LOG_TAG, "getParcelListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);

                    mParcelHash.put(key, value);
                    mArrayKeys.add(key);
                    mArrayValues.add(value);

                    Log.d(LOG_TAG, "mArrayValues size = " + mArrayValues.size());
                }
                if (mArrayValues.size() > 0) {
                    Collections.sort(mArrayValues);
                    /*
                    getJobStatusFromFirebaseDatabase(pathString, new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            long numElements = dataSnapshot.getChildrenCount();
                            if (numElements > 0 ) {
                                TmsStatusItem jobStatus = dataSnapshot.getValue(TmsStatusItem.class);
                                if (jobStatus.route_job.equals("finished")) {
                                    int nearIdx = getNearIdx(mArrayValues);
                                    int newOrderInRoute = 1;
                                    for (int i=0; i < mArrayValues.size(); i++) {
                                        int idx = (nearIdx + i) % mArrayValues.size();
                                        mArrayValues.get(idx).orderInRoute = newOrderInRoute++;
                                    }
                                    Collections.sort(mArrayValues);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    */

                }

                if (mListAdapter != null) {
                    mListAdapter.notifyDataSetChanged();
                }
                if (mContext instanceof MapViewActivity) {
                    MapViewActivity.addMarker();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(LOG_TAG,"getParcelListFromFirebaseDatabase, ValueEventListener.onCancelled", databaseError.toException());
            }
        });
    }

    protected void getJobStatusFromFirebaseDatabase(String pathString, ValueEventListener listener) {
        Query firebaseQuery;
        firebaseQuery = mDatabaseRef.child(JOB_STATUS_NAME).child(pathString);
        firebaseQuery.addListenerForSingleValueEvent(listener);
    }
    protected void postJobStatusFromFirebaseDatabase(String pathString) {
        Map<String, Object> initStatus = new HashMap<>();
        initStatus.put("route_job", "init");
        mDatabaseRef.child(JOB_STATUS_NAME).child(pathString).updateChildren(initStatus);
    }

    private int getNearIdx(ArrayList<TmsParcelItem> mArrayValues) {
        int nearIdx = 0;
        double minDist = 100000000;

        for (int i=0; i < mArrayValues.size(); i++ ){
            TmsParcelItem item = mArrayValues.get(i);
            if (!item.consigneeLatitude.isEmpty() && !item.consigneeLongitude.isEmpty()) {
                double dist = Utils.distance( Utils.mCurrent.getLatitude(),
                                              Utils.mCurrent.getLongitude(),
                                              Double.valueOf(item.consigneeLatitude),
                                              Double.valueOf(item.consigneeLongitude), "km");
                if (dist < minDist) {
                    nearIdx = i;
                    minDist = dist;
                }
            }
         }
        return nearIdx;
    }

    private String getNumString(ArrayList<TmsParcelItem> items) {
        int numTotal = items.size();
        int numComplted = 0;

        for (TmsParcelItem item : items) {
            if (item.status.equals(TmsParcelItem.STATUS_DELIVERED))
                numComplted++;
        }

        String result = String.valueOf(numTotal) + "개중 " + String.valueOf(numComplted) + "개 배송 완료됨";
        return result;
    }
}
