package com.lge.pickitup;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Utils {

    static final String LOG_TAG = "Utils";

    static final String KEY_DB_DATE = "date";
    static final String KEY_COURIER_NAME = "courier_name";
    static final String KEY_COURIER_NUMBER = "courier_number";
    static final String SELECTED_ITEM = "selected_item";
    static final String SELECTED_DATE = "selected_date";
    static final String SERVER_URL = "https://tmsproto-py.herokuapp.com";
    static final String[] ADMIN_UIDS = {
            "NCtx9UD1qSO4HAk1lhDma0eYhSq1",
            "eXVbCp7Ne1ZeeqPpxCygUA63NPu2",
    };
    static Location mCurrent;
    static LocationManager mLocationMgr;
    static Context mContext;
    private static final LocationListener mNetworkLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            mLocationMgr.removeUpdates(mGPSLocationListener);
            mLocationMgr.removeUpdates(mNetworkLocationListener);

            mCurrent = location;
            Log.i(LOG_TAG, "current(mNetworkLocationListener) : " + mCurrent.getLatitude() + "/" + mCurrent.getLongitude());
            Toast.makeText(mContext, "current(mNetworkLocationListener)", Toast.LENGTH_SHORT).show();
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
        String url = "daummaps://route?sp=" + mCurrent.getLatitude() + "," + mCurrent.getLongitude() + "&ep=" + targetLat + "," + targetLon + "&by=CAR";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            ctx.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=net.daum.android.map")));
        }

    }

    public static void makeComplete(FirebaseDatabaseConnector mFbConnector, TmsParcelItem item, String date, String path) {
        Log.d(LOG_TAG, "uploaded path = " + path);
        item.completeImage = path;
        item.status = TmsParcelItem.STATUS_DELIVERED;
        mFbConnector.postParcelItemToFirebaseDatabase(date.toString(), item);
        //mArrayAdapter.notifyDataSetChanged();
        item = null;
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
            Toast.makeText(context, "current (last)", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, "bestLocation is null", Toast.LENGTH_SHORT).show();
                mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1f, mGPSLocationListener);
                mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 1f, mNetworkLocationListener);
            } else {
                mCurrent = bestLocation;
                Log.i(LOG_TAG, "current : " + bestLocation.getLatitude() + "/" + bestLocation.getLongitude());
            }
        }
    }
}
