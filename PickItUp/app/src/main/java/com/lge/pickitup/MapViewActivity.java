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
import android.net.Uri;
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

import net.daum.mf.map.api.MapLayout;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class MapViewActivity extends AppCompatActivity
    implements MapView.OpenAPIKeyAuthenticationResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener {

    private FirebaseDatabaseConnector mFbConnector;
    private static final String LOG_TAG = "MapViewActivity";
    private static MapView mMapView;
    public static final String DAUM_MAPS_ANDROID_APP_API_KEY = "8be996dd99057764a9876591b3270e31";
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private String mSelectedDate;
    private String mSelectedCourierName;
    private String mSelectedSectionID;

    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private ArrayList<String> mArrayKeys = new ArrayList<String>();
    private static ArrayList<TmsParcelItem> mArrayValues = new ArrayList<TmsParcelItem>();

    private static String mSort = "id";
    private RelativeLayout mLayout_parcel_data;

    private static float mInitLatitude = 0;
    private static float mInitLongitude = 0;
    private TmsParcelItem mCompleteTarget;
    private MapPOIItem mCompleteMarker;
    private static final int SEND_COMPLETED_MESSAGE = 1;
    private static Bitmap bluepin;
    private static Bitmap redpin;
    private static Bitmap greenpin;
    private static Resources GlobalRes;

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
        getFirebaseList();

    }

    private void addCurrentLocationMarker() {
        Log.i(LOG_TAG, "addCurrentLocationMarker");
        Log.i(LOG_TAG, "mCurrent.getLatitude()=" + Utils.mCurrent.getLatitude() );
        Log.i(LOG_TAG, "mCurrent.getLongitude()=" + Utils.mCurrent.getLongitude() );
        MapPOIItem marker = new MapPOIItem();

        marker.setItemName(getString(R.string.current_location));
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Utils.mCurrent.getLatitude(), Utils.mCurrent.getLongitude()));
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
        marker.setCustomImageResourceId(R.drawable.truck);
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);
        marker.setCustomSelectedImageResourceId(R.drawable.truck_selected);
        mMapView.addPOIItem(marker);
    }

    protected void initResources() {
        mLayout_parcel_data =  (RelativeLayout) findViewById(R.id.parcel_data);
        GlobalRes = getResources();
    }

    protected static void addMarker() {
        Log.i(LOG_TAG,	"mArrayValues.size = " + mArrayValues.size());

        for (TmsParcelItem item : mArrayValues) {
            String strLatitude = item.consigneeLatitude;
            String strLongitude = item.consigneeLongitude;
            if (mInitLatitude == 0 || mInitLongitude == 0) {
                mInitLatitude = Float.valueOf(item.consigneeLatitude);
                mInitLongitude = Float.valueOf(item.consigneeLongitude);
            }
            Log.i(LOG_TAG,	"addr = " + item.consigneeAddr);
            Log.i(LOG_TAG,	"lat = " + item.consigneeLatitude);
            Log.i(LOG_TAG,	"lon = " + item.consigneeLongitude);
            Log.i(LOG_TAG,	"status = " + item.status);
            Log.i(LOG_TAG,	"orderInRoute = " + item.orderInRoute);

            if (   strLatitude  == null  || strLatitude.length()  == 0
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
            if (item.orderInRoute == -1) {
                if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                    marker.setCustomImageResourceId(R.drawable.marker_greenpin);
                } else if (item.status.equals(TmsParcelItem.STATUS_GEOCODED)) {
                    marker.setCustomImageResourceId(R.drawable.marker_redpin);
                } else {
                    marker.setCustomImageResourceId(R.drawable.marker_bluepin);
                }
                marker.setCustomSelectedImageResourceId(R.drawable.marker_yellowpin);
            } else {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inScaled = false;
                bluepin = BitmapFactory.decodeResource(GlobalRes, R.drawable.bluepin,bmOptions).copy(Bitmap.Config.ARGB_8888,true);
                redpin = BitmapFactory.decodeResource(GlobalRes, R.drawable.redpin,bmOptions).copy(Bitmap.Config.ARGB_8888,true);
                greenpin = BitmapFactory.decodeResource(GlobalRes, R.drawable.greenpin,bmOptions).copy(Bitmap.Config.ARGB_8888,true);
                Bitmap pin=greenpin;
                Bitmap seleted_pin=greenpin;
                if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                    pin = bluepin;
                } else if (item.status.equals(TmsParcelItem.STATUS_GEOCODED)) {
                    pin = redpin;
                }
                int textVal = item.orderInRoute;

                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE); // Text Color
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                int posX= 37; // 1 digit
                if (textVal >= 10) posX = 21; // 2 digit
                if (textVal >= 100) { // 3 digit
                    posX = 18;
                    paint.setTextSize(35);
                } else {
                    paint.setTextSize(50);
                }
                Canvas canvas = new Canvas(pin);
                canvas.drawText(String.valueOf(textVal), posX , 57 , paint); // 63

                Canvas canvas2 = new Canvas(seleted_pin);
                canvas2.drawText(String.valueOf(textVal), posX , 57 , paint); // 63

                marker.setCustomImageBitmap(pin);
                marker.setCustomSelectedImageBitmap(seleted_pin);
                marker.setCustomImageAutoscale(false);
            }
            mMapView.addPOIItem(marker);
        }
    }

    private void getFirebaseList() {
        if (mSelectedCourierName != null) {
            mFbConnector.getParcelListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_COURIER_NAME, mSelectedCourierName);
        } else if (mSelectedSectionID != null) {
            mFbConnector.getParcelListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_SECTOR_ID, mSelectedSectionID);
        }
        mFbConnector.getCourierListFromFirebaseDatabase(mSelectedDate, TmsParcelItem.KEY_ID);

    }

    @Override
    public void onDaumMapOpenAPIKeyAuthenticationResult(MapView mapView, int resultCode, String resultMessage) {
        Log.i(LOG_TAG,	String.format("Open API Key Authentication Result : code=%d, message=%s", resultCode, resultMessage));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    // net.daum.mf.map.api.MapView.MapViewEventListener

    public void onMapViewInitialized(MapView mapView) {
        Log.i(LOG_TAG, "MapView had loaded. Now, MapView APIs could be called safely");
        Log.i(LOG_TAG, String.format("onMapViewInitialized %f, %f", mInitLatitude, mInitLongitude));
        //mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        //mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(37.537229,127.005515), 7, true);
        //mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(mInitLatitude,mInitLongitude), 6, true);
        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(Utils.mCurrent.getLatitude(),Utils.mCurrent.getLongitude()), 7, true);
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

        mLayout_parcel_data.setVisibility(View.VISIBLE);

        final TmsParcelItem item = (TmsParcelItem)mapPOIItem.getUserObject();
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
                if ( item.orderInRoute != -1)  {
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
            if(customerText != null) {
                customerText.setText(getString(R.string.customer) + " : " + item.consigneeName + " (" + item.consigneeContact + ")");
            }
            if(deliveryNote != null) {
                deliveryNote.setText(getString(R.string.delivery_note) + " : " + item.deliveryNote);
            }
            if (remark != null) {
                remark.setText(getString(R.string.remark) + " : " + item.remark);
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
            case SEND_COMPLETED_MESSAGE :
                Log.d(LOG_TAG, "onActivityResult, SEND_COMPLETED_MESSAGE");
                if (data != null) {
                    String sendResult = data.getStringExtra(UploadImageActivity.EXTRA_SEND_RESULT);
                    if (TextUtils.equals(sendResult, "success")) {
                        String filePath = data.getStringExtra(UploadImageActivity.EXTRA_UPLOADED_FILE_PATH);
                        Utils.makeComplete(mFbConnector, mCompleteTarget,mSelectedDate,filePath);
                        mMapView.removePOIItem(mCompleteMarker);
                        mCompleteMarker.setCustomImageResourceId(R.drawable.marker_greenpin);
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
