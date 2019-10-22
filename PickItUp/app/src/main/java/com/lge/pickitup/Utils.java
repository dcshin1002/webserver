package com.lge.pickitup;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    static final String LOG_TAG = "Utils";

    static final String KEY_DB_DATE = "date";
    static final String KEY_COURIER_NAME = "courier_name";
    static final String KEY_COURIER_NUMBER = "courier_number";
    static final String SELECTED_ITEM = "selected_item";
    static final String SELECTED_DATE = "selected_date";
    static final String SERVER_URL = "https://tmsproto-py.herokuapp.com";
    static final String KEY_USERTYPE = "usertype";
    static final String KEY_USERNAME = "username";
    static final String usertype_admin = "admin";
    static final String usertype_courier = "courier";
    static final String usertype_consignor = "consignor";

    static final ArrayList<String> ARR_ADMIN_UIDS = new ArrayList<>();
    static final ArrayList<String> ARR_CONSIGNOR_UIDS = new ArrayList<>();
    static final ArrayList<String> ARR_COURIER_UIDS = new ArrayList<>();

    public static final int NO_NEED_RESULT = 0;
    public static final int SEND_COMPLETED_MESSAGE = 1;
    public static final String ACTION_MAKE_DELIVERED = "makedeliveried";
    public static final String ACTION_SHOWINFO = "showinfo";

    static Location mCurrent;
    static LocationManager mLocationMgr;
    static Context mContext;
    static HashMap<String, String> mUserList = new HashMap<>();
    private static int BIAS_HOUR = 7;
    private static final LocationListener mGPSLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            mLocationMgr.removeUpdates(mGPSLocationListener);
            mLocationMgr.removeUpdates(mNetworkLocationListener);

            mCurrent = location;
            Log.i(LOG_TAG, "current(mGPSLocationListener) : " + mCurrent.getLatitude() + "/" + mCurrent.getLongitude());
            Toast.makeText(mContext, "current(mGPSLocationListener)", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Do nothing
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Do nothing
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Do nothing
        }
    };
    private static final LocationListener mNetworkLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            mLocationMgr.removeUpdates(mGPSLocationListener);
            mLocationMgr.removeUpdates(mNetworkLocationListener);

            mCurrent = location;
            Log.i(LOG_TAG, "current(mNetworkLocationListener) : " + mCurrent.getLatitude() + "/" + mCurrent.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
    static String mCurrentUserId;
    static String mCurrentUserName;

    public static String getKeyHash(final Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            } catch (NoSuchAlgorithmException e) {
                Log.w(LOG_TAG, "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
        return null;
    }

    public static String getTodayDateStr() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        Log.i(LOG_TAG, "getTodayDateStr now is " + date.toString());
        now -= BIAS_HOUR * 3600 * 1000;
        date = new Date(now);
        Log.i(LOG_TAG, "getTodayDateStr now_bias is " + date.toString());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedTime = sdf.format(date);
        return formattedTime;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit.equals("km")) {
            dist = dist * 1.609344;
        } else if (unit.equals("m")) {
            dist = dist * 1609.344;
        }
        return (dist);
    }

    // This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    public static void startKakaoMapActivity(Context ctx, double targetLat, double targetLon) {
        if (mCurrent != null) {
            String url = "daummaps://route?sp=" + mCurrent.getLatitude() + "," + mCurrent.getLongitude() + "&ep=" + targetLat + "," + targetLon + "&by=CAR";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                ctx.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=net.daum.android.map")));
            }
        } else {
            Toast.makeText(ctx,R.string.cannot_know_mylocation, Toast.LENGTH_SHORT).show();
        }

    }

    public static void makeComplete(FirebaseDatabaseConnector mFbConnector, TmsParcelItem item, String date, String path, DatabaseReference.CompletionListener listener) {
        Log.d(LOG_TAG, "uploaded path = " + path);
        item.completeImage = path;
        item.status = TmsParcelItem.STATUS_DELIVERED;
        item.completeTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        mFbConnector.postParcelItemToFirebaseDatabase(date, item, listener);
        //mArrayAdapter.notifyDataSetChanged();
    }

    public static boolean isAdminAuth() {
        return ARR_ADMIN_UIDS.contains(mCurrentUserId);
    }

    public static boolean isConsignorAuth() {
        return ARR_CONSIGNOR_UIDS.contains(mCurrentUserId);
    }

    public static boolean isCourierAuth() {
        return ARR_COURIER_UIDS.contains(mCurrentUserId);
    }


    public static boolean checkConsignorItem(TmsParcelItem item) {
        if (isConsignorAuth()) {
            if (mCurrentUserName.equals(item.consignorName)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static void assignCourier(ArrayList<TmsParcelItem> items, String date, String courierName) {
        for (TmsParcelItem item : items) {
            assignCourier(item, date, courierName);
        }
    }
    public static TmsParcelItem getEndParcelPrevItem(TmsParcelItem parcelItem, String date, String prevCourierName) {
        TmsParcelItem retParcel = null;


        return retParcel;
    }


    public static void assignCourier(final TmsParcelItem parcelItem, final String date, final String prevCourierName, final String newCourierName) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        Query query = ref.child(FirebaseDatabaseConnector.COURIER_REF_NAME).
                child(date).orderByChild(TmsCourierItem.KEY_NAME).equalTo(prevCourierName);
        query.addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    TmsCourierItem courieritem = null;
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(TmsCourierItem.class).name.equals(prevCourierName)) {
                            courieritem = postSnapshot.getValue(TmsCourierItem.class);
                        };
                    }
                    if (courieritem != null) {
                        if (courieritem.startparcelid == parcelItem.id) {
                            courieritem.startparcelid = parcelItem.nextParcel;
                            assignCourier(parcelItem, date, newCourierName);
                        } else if (courieritem.endparcelid == parcelItem.id) {
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                            Query query = ref.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).
                                    child(date).orderByChild(TmsParcelItem.KEY_COURIER_NAME).equalTo(prevCourierName);
                            query.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                        TmsParcelItem item = postSnapshot.getValue(TmsParcelItem.class);
                                        if (item.nextParcel == parcelItem.id ) {
                                            item.nextParcel = -1;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            courieritem.endparcelid = getEndParcelPrevItem(parcelItem, date, prevCourierName).id;

                        } else {
                            // parcelitem.prev.nextparcelid = parcelitem.nextparcelid;


                        }
                    }

                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });

    }

    public static void assignCourier(final TmsParcelItem parcelItem, final String date, final String courierName) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        Query query = ref.child(FirebaseDatabaseConnector.COURIER_REF_NAME).
                child(date).orderByChild(TmsCourierItem.KEY_SECTOR_ID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int sectorid = 0;
                int lastSectorIdOnCourierItem = 0;
                int prevEndParcelId = -1;
                TmsCourierItem selectedCourierItem = null;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    TmsCourierItem value = postSnapshot.getValue(TmsCourierItem.class);
                    lastSectorIdOnCourierItem = value.sectorid;
                    if (value.name.equals(courierName)) {
                        selectedCourierItem = value;
                        sectorid = value.sectorid;
                        prevEndParcelId = value.endparcelid;
                    }
                }
                if (sectorid == 0) {
                    // there is no courier item of selected courier name, so need to make new courier item on DB.
                    TmsCourierItem courierItem;
                    if (lastSectorIdOnCourierItem == 0) {
                        // No Courier item on DB
                        sectorid = 1;
                    } else {
                        sectorid = lastSectorIdOnCourierItem+1;
                    }
                    courierItem = new TmsCourierItem(sectorid, courierName);
                    courierItem.startparcelid = courierItem.endparcelid = parcelItem.id;
                    postCourierItem(date, courierItem);
                } else {
                    // update tail parcel item's next parcel to new parcel item
                    Query query = ref.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(date).orderByChild(TmsParcelItem.KEY_ID).equalTo(prevEndParcelId);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                TmsParcelItem value = snapshot.getValue(TmsParcelItem.class);
                                value.nextParcel = parcelItem.id;
                                postParcelItem(date, value);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                    selectedCourierItem.endparcelid = parcelItem.id;
                    postCourierItem(date, selectedCourierItem);
                }
                parcelItem.nextParcel = -1;
                parcelItem.courierName = courierName;
                parcelItem.sectorId = sectorid;
                parcelItem.status = TmsParcelItem.STATUS_ASSIGNED;
                postParcelItem(date, parcelItem);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }





    private static void postParcelItem(String date, TmsParcelItem item) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(FirebaseDatabaseConnector.PARCEL_REF_NAME);
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;
        postValues = item.toMap();
        childUpdates.put("/" + date + "/" + item.id, postValues);
        ref.updateChildren(childUpdates);
    }

    private static void postCourierItem(String date, TmsCourierItem item) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(FirebaseDatabaseConnector.COURIER_REF_NAME);
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;
        postValues = item.toMap();
        childUpdates.put("/" + date + "/" + item.id, postValues);
        ref.updateChildren(childUpdates);
    }


    public static String[] makeCourierUserList() {
        ArrayList<String> courierlist = new ArrayList<>();
        for (String username : mUserList.keySet()) {
            if (mUserList.get(username).equals(usertype_courier)) {
                courierlist.add(username);
            }
        }
        return courierlist.toArray(new String[courierlist.size()]);
    }




    @SuppressLint("MissingPermission")
    public static void setCurrentLocation() {
        initLocation(mContext);
    }

    @SuppressLint("MissingPermission")
    public static void initLocation(Context context) {
        mContext = context;

        Log.i(LOG_TAG, "Start search location");
        mLocationMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mCurrent = mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (mCurrent != null) {
            Log.i(LOG_TAG, "current (last) : " + mCurrent.getLatitude() + "/" + mCurrent.getLongitude());
            //Toast.makeText(context, "current (last)", Toast.LENGTH_SHORT).show();
        } else {
            List<String> providers = mLocationMgr.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = mLocationMgr.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }

            if (null == bestLocation) {
                Log.i(LOG_TAG, "bestLocation is null");
//                Toast.makeText(context, "bestLocation is null", Toast.LENGTH_SHORT).show();
                mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1f, mGPSLocationListener);
                mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 1f, mNetworkLocationListener);
            } else {
                mCurrent = bestLocation;
                Log.i(LOG_TAG, "current : " + bestLocation.getLatitude() + "/" + bestLocation.getLongitude());
            }
        }
    }
}
