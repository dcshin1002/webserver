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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import net.daum.mf.map.api.MapLayout;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class MapViewActivity extends AppCompatActivity
        implements MapView.OpenAPIKeyAuthenticationResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener {

    public static final String DAUM_MAPS_ANDROID_APP_API_KEY = "8be996dd99057764a9876591b3270e31";
    private static final String LOG_TAG = "MapViewActivity";
    private static final int SEND_COMPLETED_MESSAGE = 1;
    private static MapView mMapView;
    private static ArrayList<TmsParcelItem> mArrayValues = new ArrayList<TmsParcelItem>();
    private static ArrayList<TmsCourierItem> mCourierArrayValues = new ArrayList<TmsCourierItem>();
    private static String mSort = "id";
    private static float mInitLatitude = 0;
    private static float mInitLongitude = 0;
    private static Bitmap bluepin;
    private static Bitmap redpin;
    private static Bitmap greenpin;
    private static Resources GlobalRes;
    private static ArrayList mSectorMarkerList = new ArrayList(Arrays.asList(
            R.drawable.marker_bluepin,
            R.drawable.marker_orangepin,
            R.drawable.marker_yeondoopin,
            R.drawable.marker_purplepin,
            R.drawable.marker_redpin,
            R.drawable.marker_greenpin,
            R.drawable.marker_greypin,
            R.drawable.marker_pinkpin,
            R.drawable.marker_peachpin,
            R.drawable.marker_skybluepin,
            R.drawable.marker_bluepin,
            R.drawable.marker_orangepin,
            R.drawable.marker_yeondoopin,
            R.drawable.marker_purplepin,
            R.drawable.marker_redpin,
            R.drawable.marker_greenpin,
            R.drawable.marker_greypin,
            R.drawable.marker_pinkpin,
            R.drawable.marker_peachpin,
            R.drawable.marker_skybluepin));
    private static ArrayList mCourierLocationMarkerList = new ArrayList(Arrays.asList(
            R.drawable.truck_blue,
            R.drawable.truck_orange,
            R.drawable.truck_yeondoo,
            R.drawable.truck_purple,
            R.drawable.truck_red,
            R.drawable.truck_green,
            R.drawable.truck_grey,
            R.drawable.truck_pink,
            R.drawable.truck_peach,
            R.drawable.truck_skyblue,
            R.drawable.truck_blue,
            R.drawable.truck_orange,
            R.drawable.truck_yeondoo,
            R.drawable.truck_purple,
            R.drawable.truck_red,
            R.drawable.truck_green,
            R.drawable.truck_grey,
            R.drawable.truck_pink,
            R.drawable.truck_peach,
            R.drawable.truck_skyblue));
    private FirebaseDatabaseConnector mFbConnector;
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private String mSelectedDate;
    private String mSelectedCourierName;
    private String mSelectedSectionID;
    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private ArrayList<String> mArrayKeys = new ArrayList<String>();
    private RelativeLayout mLayout_parcel_data;
    private TmsParcelItem mCompleteTarget;
    private MapPOIItem mCompleteMarker;
    private ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Log.d(LOG_TAG, "mapview CourierList size : " + dataSnapshot.getChildrenCount());
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                TmsCourierItem item = postSnapshot.getValue(TmsCourierItem.class);
                Log.i(LOG_TAG, item.id + "," + item.name + "," + item.latitude + "," + item.longitude);
                addCourierMarker(item);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    protected static void drawDeliveredStatus(Bitmap bitmap) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#5D5D5D")); // Text Color
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(65);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawText("V", 25, 70, paint); // 63
    }

    protected static void addCourierLocationMarker() {

    }

    private static Bitmap getBitmapPinByParcelItem(TmsParcelItem item) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inScaled = false;
        int pinResourceId;
        if (item.sectorId == -1) {
            pinResourceId = (int) mSectorMarkerList.get(0);
        } else {
            pinResourceId = (int) mSectorMarkerList.get(Integer.valueOf(item.sectorId) - 1);
        }
        Bitmap bmp = BitmapFactory.decodeResource(GlobalRes, pinResourceId, bmOptions).copy(Bitmap.Config.ARGB_8888, true);
        if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
            drawDeliveredStatus(bmp);
        }
        return bmp;
    }

    private static Bitmap getBitmapSeletedPinByParcelItem(TmsParcelItem item) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inScaled = false;
        int pinSeletedResourceId = R.drawable.marker_yellowpin;
        Bitmap bmp = BitmapFactory.decodeResource(GlobalRes, pinSeletedResourceId, bmOptions).copy(Bitmap.Config.ARGB_8888, true);
        if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
            drawDeliveredStatus(bmp);
        }
        return bmp;
    }

    protected static void addMarker() {
        Log.i(LOG_TAG, "mArrayValues.size = " + mArrayValues.size());

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


            if (item.orderInRoute != -1) {
                int textVal = item.orderInRoute;
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.BLACK); // Text Color
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                int posX = 37; // 1 digit
                if (textVal >= 10) posX = 21; // 2 digit
                if (textVal >= 100) { // 3 digit
                    posX = 18;
                    paint.setTextSize(35);
                } else {
                    paint.setTextSize(50);
                }
                Canvas canvas = new Canvas(pin);
                canvas.drawText(String.valueOf(textVal), posX, 57, paint); // 63

                Canvas canvas2 = new Canvas(seleted_pin);
                canvas2.drawText(String.valueOf(textVal), posX, 57, paint); // 63

            }
            marker.setCustomImageBitmap(pin);
            marker.setCustomSelectedImageBitmap(seleted_pin);
            marker.setCustomImageAutoscale(false);

            mMapView.addPOIItem(marker);
        }
    }

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
        addCurrentLocationMarker();
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

    private void addCurrentLocationMarker() {
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
        mLayout_parcel_data = (RelativeLayout) findViewById(R.id.parcel_data);
        GlobalRes = getResources();
    }

    private void getFirebaseList() {
        if (mSelectedCourierName != null) {
            mFbConnector.getParcelListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_COURIER_NAME, mSelectedCourierName);
        } else if (mSelectedSectionID != null) {
            mFbConnector.getParcelListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_SECTOR_ID, mSelectedSectionID);
        }
        mFbConnector.getCourierListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_ID);
        if (Utils.isAdminAuth()) {
            mFbConnector.getCourierListFromFirebaseDatabaseWithListener(mSelectedDate, TmsCourierItem.KEY_ID, mValueEventListener);
        }
    }

    private void addCourierMarker(TmsCourierItem courierItem) {
        if (!courierItem.latitude.isEmpty() && !courierItem.longitude.isEmpty()) {
            MapPOIItem marker = new MapPOIItem();
            marker.setItemName(courierItem.name);
            marker.setUserObject(courierItem);
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(courierItem.latitude), Double.parseDouble(courierItem.longitude)));
            marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
            int resId = (int) mCourierLocationMarkerList.get((Integer.parseInt(courierItem.id) - 1) % 20);
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
        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(Utils.mCurrent.getLatitude(), Utils.mCurrent.getLongitude()), 7, true);
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

    @Override
    public void onPOIItemSelected(MapView mapView, final MapPOIItem mapPOIItem) {
        Log.i(LOG_TAG, "onPOIItemSelected");
        if (mapPOIItem.getItemName().equals(getString(R.string.current_location))) {
            return;
        }

        if (mapPOIItem.getUserObject() instanceof TmsParcelItem) {
            mLayout_parcel_data.setVisibility(View.VISIBLE);
            final TmsParcelItem item = (TmsParcelItem) mapPOIItem.getUserObject();
            if (item != null) {
                boolean isDeliverd = item.status.equals(TmsParcelItem.STATUS_DELIVERED);
                TextView addrText = findViewById(R.id.listAddr);
                TextView customerText = findViewById(R.id.listItemTextCustomer);
                TextView deliveryNote = findViewById(R.id.listItemTextDeliveryMemo);
                TextView remark = findViewById(R.id.listItemTextRemark);
                Button btn_complete = findViewById(R.id.btn_complete);
                ImageView statusIcon = findViewById(R.id.status_icon);

                btn_complete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        processLitBtnClick(item, mapPOIItem);
                    }
                });

                if (addrText != null) {
                    String addrTextValue = "";
                    if (item.orderInRoute != -1) {
                        addrTextValue += item.orderInRoute + " : ";
                    }
                    addrText.setText(addrTextValue + item.consigneeAddr);
                    if (isDeliverd) {
                        updateStatusToComplete();
                    } else {
                        addrText.setTextColor(0xFF4F4F4F);
                        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_in_transit_v2));
                        btn_complete.setVisibility(View.VISIBLE);
                    }
                }
                if (customerText != null) {
                    customerText.setText(getString(R.string.customer) + " : " + item.consigneeName + " (" + item.consigneeContact + ")");
                }
                if (deliveryNote != null) {
                    deliveryNote.setText(getString(R.string.delivery_note) + " : " + item.deliveryNote);
                }
                if (remark != null) {
                    remark.setText(getString(R.string.remark) + " : " + item.remark);
                }

            }
        }

    }

    private void updateStatusToComplete() {
        TextView addrText = findViewById(R.id.listAddr);
        Button btn_complete = findViewById(R.id.btn_complete);
        ImageView statusIcon = findViewById(R.id.status_icon);
        addrText.setTextColor(0xFF68c166);
        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_delivered_v2));
        btn_complete.setVisibility(View.INVISIBLE);

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
                        Intent intent = new Intent(MapViewActivity.this, UploadImageActivity.class);
                        intent.putExtra(Utils.SELECTED_ITEM, item);
                        intent.putExtra(Utils.SELECTED_DATE, mSelectedDate);
                        startActivityForResult(intent, SEND_COMPLETED_MESSAGE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            Log.d(LOG_TAG, "onActivityResult, result code is not RESULT_OK");
            return;
        }

        switch (requestCode) {
            case SEND_COMPLETED_MESSAGE:
                Log.d(LOG_TAG, "onActivityResult, SEND_COMPLETED_MESSAGE");
                if (data != null) {
                    String sendResult = data.getStringExtra(UploadImageActivity.EXTRA_SEND_RESULT);
                    if (TextUtils.equals(sendResult, "success")) {
                        String filePath = data.getStringExtra(UploadImageActivity.EXTRA_UPLOADED_FILE_PATH);
                        Utils.makeComplete(mFbConnector, mCompleteTarget, mSelectedDate, filePath);

                        mMapView.removePOIItem(mCompleteMarker);

                        Bitmap bm = getBitmapPinByParcelItem(mCompleteTarget);
                        Bitmap seleted_bm = getBitmapSeletedPinByParcelItem(mCompleteTarget);
                        drawDeliveredStatus(bm);
                        drawDeliveredStatus(seleted_bm);
                        mCompleteMarker.setCustomImageBitmap(bm);
                        mCompleteMarker.setCustomSelectedImageBitmap(seleted_bm);
                        mMapView.addPOIItem(mCompleteMarker);
                        updateStatusToComplete();
                    } else {

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
        Utils.startKakaoMapActivity(getApplication(), mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude, mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude);
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

}
