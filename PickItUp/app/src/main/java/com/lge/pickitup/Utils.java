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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

    public static final int NO_NEED_RESULT = 0;
    public static final int SEND_COMPLETED_MESSAGE = 1;
    public static final String ACTION_MAKE_DELIVERED = "makedeliveried";
    public static final String ACTION_SHOWINFO = "showinfo";

    static FirebaseUser mCurrentUser;
    static TmsUserItem mCurrentUserItem;
    static ArrayList<TmsUserItem> mRootUserItem = new ArrayList<>();

    static Location mCurrent;
    static LocationManager mLocationMgr;
    static Context mContext;
    static HashMap<String, String> mUserList = new HashMap<>();
    static HashMap<String, TmsUserItem> mHashUserList = new HashMap<>();
    static HashMap<String, String> mHashUserNameUID = new HashMap<>();
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

    public static boolean isRootAuth() {
        return mRootUserItem.contains(mCurrentUserItem);
    }
    public static boolean isAdminAuth() {
        return !mCurrentUserItem.children.isEmpty();
    }

    public static boolean isConsignorAuth() {
        if (mCurrentUserItem != null) {
            return mCurrentUserItem.usertype.equals(TmsUserItem.usertype_consignor);
        } else {
            return false;
        }
    }

    public static boolean isCourierAuth() {
        if (mCurrentUserItem != null) {
            return mCurrentUserItem.usertype.equals(TmsUserItem.usertype_courier);
        } else {
            return false;
        }
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


    public static ArrayList<String> makeCourierUserList() {
        ArrayList<String> courierlist = new ArrayList<>();
        for (String username : mUserList.keySet()) {
            if (mUserList.get(username).equals(usertype_courier)) {
                courierlist.add(username);
            }
        }
        return courierlist;
    }

    public static void getUserListFromFirebase(final FirebaseAuth auth, final FirebaseAuth.AuthStateListener authStateListener) {
        Query firebaseQuery;
        firebaseQuery = FirebaseDatabase.getInstance().getReference().child(FirebaseDatabaseConnector.USER_REF_NAME).orderByChild(Utils.KEY_USERTYPE);
        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    TmsUserItem useritem = snapshot.getValue(TmsUserItem.class);
                    mHashUserList.put(useritem.uid, useritem);
                    if (useritem.parentId.isEmpty()) {
                        mRootUserItem.add(useritem);
                    }
                    String usertypevalue = snapshot.child(Utils.KEY_USERTYPE).getValue().toString();
                    String usernamevalue = snapshot.child(Utils.KEY_USERNAME).getValue().toString();
                    mUserList.put(usernamevalue, usertypevalue);

                }
                for (String uid : mHashUserList.keySet()) {
                    TmsUserItem item = mHashUserList.get(uid);
                    String parentId = item.parentId;
                    if (!parentId.isEmpty()) {
                        mHashUserList.get(item.parentId).addChild(item);
                    }
                }

                auth.addAuthStateListener(authStateListener);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

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
