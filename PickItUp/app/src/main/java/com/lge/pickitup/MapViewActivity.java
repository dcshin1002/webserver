package com.lge.pickitup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;


import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapLayout;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class MapViewActivity extends AppCompatActivity
        implements MapView.OpenAPIKeyAuthenticationResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener, View.OnClickListener, MapView.CurrentLocationEventListener {

    private FirebaseDatabaseConnector mFbConnector;
    private static final String LOG_TAG = "MapViewActivity";
    private static MapView mMapView;
    public static final String DAUM_MAPS_ANDROID_APP_API_KEY = "8be996dd99057764a9876591b3270e31";
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private String mSelectedDate;
    private static String mSelectedCourierName;
    private String mSelectedSectionID;
    private ImageView mTrackingModeBtn;
    private Button mBtnDeliveryinfo;

    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private static HashMap<MarkerItem, ArrayList<TmsParcelItem>> mMarkerHash = new HashMap();
    private static HashMap<MarkerItem, ArrayList<MapPOIItem>> mMapPOItemHash = new HashMap();
    private ArrayList<String> mArrayKeys = new ArrayList<String>();
    private static ArrayList<TmsParcelItem> mArrayValues = new ArrayList<TmsParcelItem>();
    private static ArrayList<TmsCourierItem> mCourierArrayValues = new ArrayList<TmsCourierItem>();

    private static String mSort = "id";
    private LinearLayout mLayout_parcel_data;


    private static float mInitLatitude = 0;
    private static float mInitLongitude = 0;
    private TmsParcelItem mCompleteTarget;
    private MapPOIItem mCompleteMarker;

    private static Bitmap bluepin;
    private static Bitmap redpin;
    private static Bitmap greenpin;
    private static Resources GlobalRes;
    private static ArrayList mSectorMarkerList = new ArrayList(Arrays.asList(
            R.drawable.location_map_pin_blue_normal,
            R.drawable.location_map_pin_green_normal,
            R.drawable.location_map_pin_indigo_normal,
            R.drawable.location_map_pin_light_green_normal,
            R.drawable.location_map_pin_mint_normal,
            R.drawable.location_map_pin_orange_normal,
            R.drawable.location_map_pin_pink_normal,
            R.drawable.location_map_pin_red_normal,
            R.drawable.location_map_pin_skyplue_normal,
            R.drawable.location_map_pin_yellow_normal
            ));

    private static ArrayList mSelectedMarkerList = new ArrayList(Arrays.asList(
            R.drawable.location_map_pin_blue_selected,
            R.drawable.location_map_pin_green_selected,
            R.drawable.location_map_pin_indigo_selected,
            R.drawable.location_map_pin_light_green_selected,
            R.drawable.location_map_pin_mint_selected,
            R.drawable.location_map_pin_orange_selected,
            R.drawable.location_map_pin_pink_selected,
            R.drawable.location_map_pin_red_selected,
            R.drawable.location_map_pin_skyplue_selected,
            R.drawable.location_map_pin_yellow_selected
    ));

    private static ArrayList mDeliveredMarkerList = new ArrayList(Arrays.asList(
            R.drawable.location_map_pin_blue_check,
            R.drawable.location_map_pin_green_check,
            R.drawable.location_map_pin_indigo_check,
            R.drawable.location_map_pin_light_green_check,
            R.drawable.location_map_pin_mint_check,
            R.drawable.location_map_pin_orange_check,
            R.drawable.location_map_pin_pink_check,
            R.drawable.location_map_pin_red_check,
            R.drawable.location_map_pin_skyplue_check,
            R.drawable.location_map_pin_yellow_check
            ));

    private static ArrayList mCourierLocationMarkerList = new ArrayList(Arrays.asList(
            R.drawable.truck_blue,
            R.drawable.truck_green,
            R.drawable.truck_indigo,
            R.drawable.truck_light_green,
            R.drawable.truck_mint,
            R.drawable.truck_orange,
            R.drawable.truck_pink,
            R.drawable.truck_red,
            R.drawable.truck_skyblue,
            R.drawable.truck_yellow
            ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapview);
        initResources();
        MapLayout mapLayout = new MapLayout(this);
        mMapView = mapLayout.getMapView();
        mMapView.setDaumMapApiKey(DAUM_MAPS_ANDROID_APP_API_KEY);
        mMapView.setOpenAPIKeyAuthenticationResultListener(this);
        mMapView.setMapViewEventListener(this);
        mMapView.setPOIItemEventListener(this);
        mMapView.setMapType(MapView.MapType.Standard);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapLayout);
        //addCurrentLocationMarker();
        if (!Utils.isAdminAuth()) {
            turnOnTrackingMode();
            mMapView.setCurrentLocationEventListener(this);
            mMapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.location_map_pin_pink, new MapPOIItem.ImageOffset(28, 28));
        } else {
            turnOffTrackingMode();
            mTrackingModeBtn.setVisibility(View.GONE);
        }

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mSelectedDate = b.getString(TmsParcelItem.KEY_DATE);
            mSelectedCourierName = b.getString(TmsParcelItem.KEY_COURIER_NAME);
            mSelectedSectionID = b.getString(TmsParcelItem.KEY_SECTOR_ID);
        }
        mFbConnector = new FirebaseDatabaseConnector(this);
        mFbConnector.setParcelHash(this.mParcelDatabaseHash);
        mFbConnector.setCourierHash(this.mCourierDatabaseHash);
        mFbConnector.setParcelKeyArray(this.mArrayKeys);
        mFbConnector.setParcelValueArray(this.mArrayValues);
        mFbConnector.setCourierValueArray(this.mCourierArrayValues);
        getFirebaseList();

    }
    private void turnOnTrackingMode() {
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mTrackingModeBtn.setColorFilter(Color.RED);
        mMapView.setShowCurrentLocationMarker(true);
    }
    private void turnOffTrackingMode() {
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mTrackingModeBtn.setColorFilter(Color.WHITE);
        mMapView.setShowCurrentLocationMarker(false);
    }
    private boolean isTrackingModeOff() {
        return (mMapView.getCurrentLocationTrackingMode() == MapView.CurrentLocationTrackingMode.TrackingModeOff);
    }
    private void toggleTrackingMode() {
        if (isTrackingModeOff()) {
            turnOnTrackingMode();
        } else {
            turnOffTrackingMode();
        }
    }

    private void addCurrentLocationMarker() {
        if (Utils.mCurrent == null) {
            return;
        }
        Log.i(LOG_TAG, "addCurrentLocationMarker");
        Log.i(LOG_TAG, "mCurrent.getLatitude()=" + Utils.mCurrent.getLatitude());
        Log.i(LOG_TAG, "mCurrent.getLongitude()=" + Utils.mCurrent.getLongitude());
        MapPOIItem marker = new MapPOIItem();

        marker.setItemName(getString(R.string.current_location));
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Utils.mCurrent.getLatitude(), Utils.mCurrent.getLongitude()));
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
        marker.setCustomImageResourceId(R.drawable.truck);
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);
        marker.setCustomSelectedImageResourceId(R.drawable.truck_yellow);
        mMapView.addPOIItem(marker);
    }

    protected void initResources() {
        mLayout_parcel_data = (LinearLayout) findViewById(R.id.parcel_data);
        mTrackingModeBtn = (ImageView) findViewById(R.id.ib_tracking);
        mTrackingModeBtn.setOnClickListener(this);
        mTrackingModeBtn.setZ(10);
        mLayout_parcel_data.setZ(20);
        mBtnDeliveryinfo = (Button) findViewById(R.id.btn_deliveryinfo);
        mBtnDeliveryinfo.setOnClickListener(this);

        mLayout_parcel_data.setOnClickListener(this);

        GlobalRes = getResources();
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.parcel_data:
                MapPOIItem mapPOIItem = (MapPOIItem)view.getTag(R.id.parcel_data);
                Utils.startKakaoMapActivity(MapViewActivity.this, mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude, mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude);
                break;
            case R.id.btn_deliveryinfo:
                TmsParcelItem item = (TmsParcelItem)view.getTag(R.id.parcel_data);
                goToUploadImageActivity(item, Utils.NO_NEED_RESULT);
                break;
            case R.id.ib_tracking:
                toggleTrackingMode();
                break;

        }
    }

    protected static void drawDeliveredStatus(Bitmap bitmap) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#5D5D5D")); // Text Color
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(65);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawText("V", 25, 70, paint); // 63
    }

    private static Bitmap getBitmapPinByParcelItem(TmsParcelItem item) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inScaled = false;
        int pinResourceId;

        if (item.sectorId == -1) {
            if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                pinResourceId = (int) mDeliveredMarkerList.get(0);
            } else {
                pinResourceId = (int) mSectorMarkerList.get(0);
            }
        } else {
            if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                pinResourceId = (int) mDeliveredMarkerList.get((Integer.valueOf(item.sectorId) - 1)%10);
            } else {
                pinResourceId = (int) mSectorMarkerList.get((Integer.valueOf(item.sectorId) - 1)%10);
            }
        }
        Bitmap bmp = BitmapFactory.decodeResource(GlobalRes, pinResourceId, bmOptions).copy(Bitmap.Config.ARGB_8888, true);
        return bmp;
    }

    private static Bitmap getBitmapSeletedPinByParcelItem(TmsParcelItem item) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inScaled = false;
        int pinSeletedResourceId;
        pinSeletedResourceId = (int) mSelectedMarkerList.get((Integer.valueOf(item.sectorId) - 1)%10);

        Bitmap bmp = BitmapFactory.decodeResource(GlobalRes, pinSeletedResourceId, bmOptions).copy(Bitmap.Config.ARGB_8888, true);
        return bmp;
    }

    protected void addMarker() {
        Log.i(LOG_TAG, "mArrayValues.size = " + mArrayValues.size());
        int num = 1;
        for (TmsParcelItem item : mArrayValues) {
            String strLatitude = item.consigneeLatitude;
            String strLongitude = item.consigneeLongitude;
            if (mInitLatitude == 0 || mInitLongitude == 0) {
                mInitLatitude = Float.valueOf(item.consigneeLatitude);
                mInitLongitude = Float.valueOf(item.consigneeLongitude);
            }
            Log.i(LOG_TAG, "addr = " + item.consigneeAddr);
            Log.i(LOG_TAG, "lat = " + item.consigneeLatitude);
            Log.i(LOG_TAG, "lon = " + item.consigneeLongitude);
            Log.i(LOG_TAG, "status = " + item.status);
            Log.i(LOG_TAG, "orderInRoute = " + item.orderInRoute);

            if (strLatitude == null || strLatitude.length() == 0
                    || strLongitude == null || strLongitude.length() == 0) {
                continue;
            }
            MapPOIItem marker = new MapPOIItem();

            marker.setItemName(item.consigneeAddr);
            marker.setUserObject(item);
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(strLatitude)
                    , Double.parseDouble(strLongitude)));

            marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.

            Bitmap pin = getBitmapPinByParcelItem(item);
            Bitmap seleted_pin = getBitmapSeletedPinByParcelItem(item);
            boolean isDeliverd = item.status.equals(TmsParcelItem.STATUS_DELIVERED);
            //if (!isDeliverd &&  item.orderInRoute != -1 && !mSelectedCourierName.equals(GlobalRes.getString(R.string.all_couriers))) {
            if (!isDeliverd &&  !mSelectedCourierName.equals(GlobalRes.getString(R.string.all_couriers))) {
                int textVal = num;
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.BLACK); // Text Color
//                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                int posX; // 1 digit
                if (textVal < 10) {
                    posX = 17;
                    paint.setTextSize(33);
                } else if (textVal >= 10) {
                    posX = 11; // 2 digit
                    paint.setTextSize(30);
                } else if (textVal >= 100) { // 3 digit
                    posX = 7;
                    paint.setTextSize(18);
                } else {
                    posX = 3;
                    paint.setTextSize(15);
                }
                Canvas canvas = new Canvas(pin);
                canvas.drawText(String.valueOf(textVal), posX, 37, paint); // 63

                Canvas canvas2 = new Canvas(seleted_pin);
                canvas2.drawText(String.valueOf(textVal), posX, 37, paint); // 63

            }
            if (mSelectedCourierName.equals(GlobalRes.getString(R.string.all_couriers))) {
                marker.setTag(-1);
            } else {
                marker.setTag(num);
            }

            marker.setCustomImageBitmap(pin);
            marker.setCustomSelectedImageBitmap(seleted_pin);
            marker.setCustomImageAutoscale(false);


            MarkerItem markeritem = new MarkerItem(strLatitude, strLongitude);

            ArrayList<TmsParcelItem> list_parcelitem;
            if (mMarkerHash.containsKey(markeritem)) {
                list_parcelitem = mMarkerHash.get(markeritem);
            } else {
                list_parcelitem = new ArrayList<>();
            }
            list_parcelitem.add(item);

            mMarkerHash.put(markeritem, list_parcelitem);


            ArrayList<MapPOIItem> list_poiitem;
            if (mMapPOItemHash.containsKey(markeritem)) {
                list_poiitem = mMapPOItemHash.get(markeritem);
            } else {
                list_poiitem = new ArrayList<>();
            }
            list_poiitem.add(marker);
            mMapPOItemHash.put(markeritem, list_poiitem);

            mMapView.addPOIItem(marker);
            num++;
        }
    }

    private void getFirebaseList() {
        mFbConnector.getCourierListFromFirebaseDatabaseWithListener(mSelectedDate, mCourierValueEventListener);
    }

    private ValueEventListener mCourierValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Log.d(LOG_TAG, "mapview CourierList size : " + dataSnapshot.getChildrenCount());
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                TmsCourierItem item = postSnapshot.getValue(TmsCourierItem.class);
                mCourierDatabaseHash.put(item.name, item);
                mCourierArrayValues.add(item);
                if (Utils.isAdminAuth()) {
                    addCourierMarker(item);
                }
            }
            if (mSelectedCourierName != null) {
                getParcelListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_COURIER_NAME, mSelectedCourierName);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    protected void getParcelListFromFirebaseDatabase(final String pathString, String orderBy, String select) {
        DatabaseReference mDatabaseRef;
        Query firebaseQuery;
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        if (select.equals(getString(R.string.all_couriers))) {
            firebaseQuery = mDatabaseRef.child(mFbConnector.PARCEL_REF_NAME).child(pathString).orderByChild(orderBy);
        } else {
            firebaseQuery = mDatabaseRef.child(mFbConnector.PARCEL_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(select);
        }
        firebaseQuery.addListenerForSingleValueEvent(mParcelValueEventListener);
    }



    private ValueEventListener mParcelValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            mParcelDatabaseHash.clear();
            mArrayKeys.clear();
            mArrayValues.clear();
            mMarkerHash.clear();
            mMapPOItemHash.clear();
            boolean isRouted = true;

            Log.d(LOG_TAG, "getParcelListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                String key = postSnapshot.getKey();
                TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);

                if (value.orderInRoute == -1) {
                    isRouted = false;
                }
                value.orderInRoute = -1;

                mParcelDatabaseHash.put(key, value);
                mArrayKeys.add(key);
                mArrayValues.add(value);

                Log.d(LOG_TAG, "mArrayValues size = " + mArrayValues.size());
            }

            String courierName = mSelectedCourierName;
            if (!courierName.equals(R.string.all_couriers) ) {
                TmsCourierItem courierItem = mCourierDatabaseHash.get(courierName);
                if(courierItem != null) {
                    TmsParcelItem parcelItem = mParcelDatabaseHash.get(String.valueOf(courierItem.startparcelid));
                    if (parcelItem != null) {
                        mArrayValues.clear();
                        mArrayValues.add(parcelItem);
                        while(parcelItem.nextParcel != -1) {
                            parcelItem = mParcelDatabaseHash.get(String.valueOf(parcelItem.nextParcel));
                            mArrayValues.add(parcelItem);
                        }
                    }
                }
            }
            addMarker();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w(LOG_TAG, "getParcelListFromFirebaseDatabase, ValueEventListener.onCancelled", databaseError.toException());
        }
    };

    private void addCourierMarker(TmsCourierItem courierItem) {
        if (!Utils.isAdminAuth()) {
            return;
        }
        if (!courierItem.latitude.isEmpty() && !courierItem.longitude.isEmpty()) {
            MapPOIItem marker = new MapPOIItem();
            marker.setItemName(courierItem.name);
            marker.setUserObject(courierItem);
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(courierItem.latitude), Double.parseDouble(courierItem.longitude)));
            marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
            int resId =(int) mCourierLocationMarkerList.get((Integer.parseInt(courierItem.id)-1)%10);
            marker.setCustomImageResourceId(resId);
            marker.setCustomSelectedImageResourceId(R.drawable.truck_yellow);
            marker.setCustomImageAutoscale(true);
            mMapView.addPOIItem(marker);
        }
    }

    @Override
    public void onDaumMapOpenAPIKeyAuthenticationResult(MapView mapView, int resultCode, String resultMessage) {
        Log.i(LOG_TAG, String.format("Open API Key Authentication Result : code=%d, message=%s", resultCode, resultMessage));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    // net.daum.mf.map.api.MapView.MapViewEventListener

    public void onMapViewInitialized(MapView mapView) {
        Log.i(LOG_TAG, "MapView had loaded. Now, MapView APIs could be called safely");
        Log.i(LOG_TAG, String.format("onMapViewInitialized %f, %f", mInitLatitude, mInitLongitude));
        //mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        //mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(37.537229,127.005515), 7, true);
        //mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(mInitLatitude,mInitLongitude), 6, true);
        if (Utils.mCurrent != null) {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(Utils.mCurrent.getLatitude(), Utils.mCurrent.getLongitude()), 7, true);
        } else {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(mInitLatitude,mInitLongitude), 7, true);
        }

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapCenterPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapCenterPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onMapViewCenterPointMoved (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();

        /*
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("DaumMapLibrarySample");
        alertDialog.setMessage(String.format("Double-Tap on (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
        alertDialog.setPositiveButton("OK", null);
        alertDialog.show();
        */
    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        /*
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("DaumMapLibrarySample");
        alertDialog.setMessage(String.format("Long-Press on (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
        alertDialog.setPositiveButton("OK", null);
        alertDialog.show();*/
    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        mLayout_parcel_data.setVisibility(View.GONE);
        Log.i(LOG_TAG, String.format("MapView onMapViewSingleTapped (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onMapViewDragStarted (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onMapViewDragEnded (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onMapViewMoveFinished (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int zoomLevel) {
        Log.i(LOG_TAG, String.format("MapView onMapViewZoomLevelChanged (%d)", zoomLevel));
    }

    private void showSameGeoParcelDialog(ArrayList<TmsParcelItem> sameGeoMarkerItems) {

    }

    private void updateInfoUI(final TmsParcelItem parcelItem, final MapPOIItem mapPOIItem){


        mLayout_parcel_data.setVisibility(View.VISIBLE);
        mLayout_parcel_data.setTag(R.id.parcel_data, mapPOIItem);
        mBtnDeliveryinfo.setTag(R.id.parcel_data, mapPOIItem.getUserObject());

        if (parcelItem != null) {
            boolean isDeliverd = parcelItem.status.equals(TmsParcelItem.STATUS_DELIVERED);
            TextView addrText = findViewById(R.id.listAddr);
            TextView providerText = findViewById(R.id.listItemTextProvider);
            TextView customerText = findViewById(R.id.listItemTextCustomer);
            TextView deliveryNote = findViewById(R.id.listItemTextDeliveryMemo);
            TextView remark = findViewById(R.id.listItemTextRemark);
            Button btn_complete = findViewById(R.id.btn_complete);
            Button btn_deliveryinfo = findViewById(R.id.btn_deliveryinfo);
            ImageView statusIcon = findViewById(R.id.status_icon);

            btn_complete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    processLitBtnClick(parcelItem, mapPOIItem);
                }
            });

            if (addrText != null) {
                String addrTextValue = "";
                int deliveryOrder = mapPOIItem.getTag();
                //if (deliveryOrder != 0) {
                if (deliveryOrder != -1) {
                    addrTextValue += deliveryOrder + " : ";
                }
                //}
                addrText.setText(addrTextValue + parcelItem.consigneeAddr);
                if (isDeliverd) {
                    updateStatusToComplete(addrText, statusIcon, btn_complete, btn_deliveryinfo);
                } else {
                    updateStatusToNotDelivery(addrText, statusIcon, btn_complete, btn_deliveryinfo);
                }
            }
            if (providerText != null) {
                providerText.setText("업체명" + " : " + parcelItem.consignorName);
            }
            if (customerText != null) {
                customerText.setText(getString(R.string.customer) + " : " + parcelItem.consigneeName + " (" + parcelItem.consigneeContact + ")");
            }
            if (deliveryNote != null) {
                deliveryNote.setText(getString(R.string.delivery_note) + " : " + parcelItem.deliveryNote);
            }
            if (remark != null) {
                remark.setText(getString(R.string.remark) + " : " + parcelItem.remark);
            }
        }
    }


    @Override
    public void onPOIItemSelected(MapView mapView, final MapPOIItem mapPOIItem) {
        Log.i(LOG_TAG, "onPOIItemSelected");
        if (mapPOIItem.getItemName().equals(getString(R.string.current_location))) {
            return;
        }

        if (mapPOIItem.getUserObject() instanceof  TmsParcelItem) {
            final TmsParcelItem item = (TmsParcelItem) mapPOIItem.getUserObject();

            MarkerItem markeritem = new MarkerItem(item.consigneeLatitude, item.consigneeLongitude);
            final ArrayList<TmsParcelItem> sameGeoMarkerItems = mMarkerHash.get(markeritem);
            final ArrayList<MapPOIItem> sameGeoPOIItems = mMapPOItemHash.get(markeritem);

            Log.i(LOG_TAG, "sameGeoPOIItems size is " + sameGeoPOIItems.size());

            if (sameGeoMarkerItems.size() > 1) {

                final ArrayList<String> ListItems = new ArrayList<>();
                for (MapPOIItem poiitem : sameGeoPOIItems) {
                    TmsParcelItem parcelItem = (TmsParcelItem) poiitem.getUserObject();
                    String strOrder;
                    if (poiitem.getTag() != -1) {
                        strOrder = poiitem.getTag()  + " : ";
                    } else {
                        strOrder = "";
                    }
                    ListItems.add(strOrder + parcelItem.consigneeName + " : " + parcelItem.consigneeAddr);
                }
                final CharSequence[] items =  ListItems.toArray(new String[ ListItems.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("주문을 선택하세요");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int pos) {
                        updateInfoUI(sameGeoMarkerItems.get(pos), sameGeoPOIItems.get(pos));
                    }
                });
                builder.show();
            } else {
                updateInfoUI(item, mapPOIItem);
            }
        }

    }


    private void updateStatusToNotDelivery() {
        TextView addrText = findViewById(R.id.listAddr);
        ImageView statusIcon = findViewById(R.id.status_icon);
        Button btn_complete = findViewById(R.id.btn_complete);
        Button btn_deliveryinfo = findViewById(R.id.btn_deliveryinfo);
        updateStatusToNotDelivery(addrText, statusIcon, btn_complete, btn_deliveryinfo);
    }

    private void updateStatusToNotDelivery(TextView addrText, ImageView statusIcon, Button btn_complete, Button btn_deliveryinfo) {
        addrText.setTextColor(0xFF4F4F4F);
        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_in_transit_v2));
        statusIcon.setVisibility(View.INVISIBLE);
        btn_complete.setVisibility(View.VISIBLE);
        btn_deliveryinfo.setVisibility(View.GONE);
    }

    private void updateStatusToComplete() {
        TextView addrText = findViewById(R.id.listAddr);
        ImageView statusIcon = findViewById(R.id.status_icon);
        Button btn_complete = findViewById(R.id.btn_complete);
        Button btn_deliveryinfo = findViewById(R.id.btn_deliveryinfo);
        updateStatusToComplete(addrText, statusIcon, btn_complete, btn_deliveryinfo);
    }

    private void updateStatusToComplete(TextView addrText, ImageView statusIcon, Button btn_complete, Button btn_deliveryinfo) {
        addrText.setTextColor(0xFF68c166);
        statusIcon.setVisibility(View.VISIBLE);
        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_delivered_v2));
        btn_complete.setVisibility(View.GONE);
        btn_deliveryinfo.setVisibility(View.VISIBLE);
    }

    private void processLitBtnClick(final TmsParcelItem item, final MapPOIItem mapPOIItem) {

        Log.d(LOG_TAG, "Selected item\'s status will be chaanged to \"deliverd\"");

        mDeliveryCompleteDialog = new AlertDialog.Builder(this)
                .setTitle(getText(R.string.query_delivery_complete_title))
                .setMessage(getText(R.string.query_delivery_complete_message))
                .setPositiveButton(R.string.complete_with_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCompleteTarget = item;
                        mCompleteMarker = mapPOIItem;
                        goToUploadImageActivity(item, Utils.SEND_COMPLETED_MESSAGE);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });


        mDeliveryCompleteDialog.show();
    }

    private void goToUploadImageActivity(final TmsParcelItem item, int requestCode) {
        Intent intent = new Intent(MapViewActivity.this, UploadImageActivity.class);
        intent.putExtra(Utils.SELECTED_ITEM, item);
        intent.putExtra(Utils.SELECTED_DATE, mSelectedDate);
        if (requestCode != Utils.NO_NEED_RESULT) {
            intent.setAction(Utils.ACTION_MAKE_DELIVERED);
            startActivityForResult(intent, requestCode);
        } else {
            intent.setAction(Utils.ACTION_SHOWINFO);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            Log.d(LOG_TAG, "onActivityResult, result code is not RESULT_OK");
            return;
        }

        switch (requestCode) {
            case Utils.SEND_COMPLETED_MESSAGE:
                Log.d(LOG_TAG, "onActivityResult, SEND_COMPLETED_MESSAGE");
                if (data != null) {
                    String sendResult = data.getStringExtra(UploadImageActivity.EXTRA_SEND_RESULT);
                    if (TextUtils.equals(sendResult, "success")) {
                        String filePath = data.getStringExtra(UploadImageActivity.EXTRA_UPLOADED_FILE_PATH);
                        Utils.makeComplete(mFbConnector, mCompleteTarget, mSelectedDate, filePath);
                        mMapView.removePOIItem(mCompleteMarker);
                        Bitmap bm = getBitmapPinByParcelItem(mCompleteTarget);
                        Bitmap seleted_bm = getBitmapSeletedPinByParcelItem(mCompleteTarget);
                        mCompleteMarker.setCustomImageBitmap(bm);
                        mCompleteMarker.setCustomSelectedImageBitmap(seleted_bm);
                        mMapView.addPOIItem(mCompleteMarker);
                        updateStatusToComplete();
                        mMapView.setMapCenterPointAndZoomLevel(
                                MapPoint.mapPointWithGeoCoord(mCompleteMarker.getMapPoint().getMapPointGeoCoord().latitude, mCompleteMarker.getMapPoint().getMapPointGeoCoord().longitude),
                                7,
                                true);
                    }
                }
                break;
        }
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
        Toast.makeText(this, "onCalloutBalloonOfPOIItemTouched", Toast.LENGTH_SHORT);
    }


    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        if (mapPOIItem.getItemName().equals(getString(R.string.current_location))) {
            return;
        }
        if (mapPOIItem.getUserObject() instanceof TmsCourierItem) {
            return;
        }
        Utils.startKakaoMapActivity(MapViewActivity.this, mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude, mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude);
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {

    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }
}
