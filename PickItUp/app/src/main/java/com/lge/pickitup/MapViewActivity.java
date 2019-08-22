package com.lge.pickitup;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapLayout;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapViewActivity extends AppCompatActivity
    implements MapView.OpenAPIKeyAuthenticationResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener {

    private FirebaseDatabaseConnector mFbConnector;
    private static final String LOG_TAG = "MapViewActivity";
    private static MapView mMapView;
    public static final String DAUM_MAPS_ANDROID_APP_API_KEY = "8be996dd99057764a9876591b3270e31";
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private String selectedDate;
    private String selectedCourierName;

    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private ArrayList<String> mArrayKeys = new ArrayList<String>();
    private static ArrayList<TmsParcelItem> mArrayValues = new ArrayList<TmsParcelItem>();

    private static String mSort = "id";
    private static ArrayList mArrayBluePinImg = new ArrayList();
    private static ArrayList mArrayRedPinImg= new ArrayList();
    private static ArrayList mArrayGreenPinImg= new ArrayList();
    private RelativeLayout mLayout_parcel_data;

    private static float mInitLatitude = 0;
    private static float mInitLongitude = 0;
    private boolean flag_item_selected = false;


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
            selectedDate = b.getString(Utils.KEY_DB_DATE);
            selectedCourierName = b.getString(Utils.KEY_COURIER_NAME);
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
        Resources res = this.getResources();
        for (int i=0; i<= 99; i++) {
            String imagename = "marker_bluepin_" + i;
            int resID = res.getIdentifier(imagename, "drawable", this.getPackageName());
            mArrayBluePinImg.add(resID);
        }
        for (int i=0; i<= 99; i++) {
            String imagename = "marker_redpin_" + i;
            int resID = res.getIdentifier(imagename, "drawable", this.getPackageName());
            mArrayRedPinImg.add(resID);
        }
        for (int i=0; i<= 99; i++) {
            String imagename = "marker_greenpin_" + i;
            int resID = res.getIdentifier(imagename, "drawable", this.getPackageName());
            mArrayGreenPinImg.add(resID);
        }
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
            if (item.orderInRoute == -1) {
                if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                    marker.setCustomImageResourceId(R.drawable.marker_bluepin_0);
                } else if (item.status.equals(TmsParcelItem.STATUS_GEOCODED)) {
                    marker.setCustomImageResourceId(R.drawable.marker_redpin_0);
                }
            } else {
                if (item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                    marker.setCustomImageResourceId((int)mArrayBluePinImg.get(item.orderInRoute));
                } else if (item.status.equals(TmsParcelItem.STATUS_GEOCODED)) {
                    marker.setCustomImageResourceId((int)mArrayRedPinImg.get(item.orderInRoute));
                }
            }
            marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
            marker.setCustomSelectedImageResourceId((int)mArrayGreenPinImg.get(item.orderInRoute));
            mMapView.addPOIItem(marker);
        }
    }

    private void getFirebaseList() {
        mFbConnector.getParcelListFromFirebaseDatabase(selectedDate, TmsParcelItem.KEY_COURIER_NAME, selectedCourierName);
        mFbConnector.getCourierListFromFirebaseDatabase(selectedDate, TmsParcelItem.KEY_ID);

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
        //if (!flag_item_selected)
            //mLayout_parcel_data.setVisibility(View.GONE);
        flag_item_selected = false;
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
        flag_item_selected = true;

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
    private void setMarkertToBlue(final MapPOIItem mapPOIItem) {
        mapPOIItem.setMarkerType(MapPOIItem.MarkerType.BluePin);
    }

    private void processLitBtnClick(final TmsParcelItem item, final MapPOIItem mapPOIItem) {

        Log.d(LOG_TAG, "Selected item\'s status will be chaanged to \"deliverd\"");
        mDeliveryCompleteDialog = new AlertDialog.Builder(this)
                .setTitle(getText(R.string.query_delivery_complete_title))
                .setMessage(getText(R.string.query_delivery_complete_message))
                .setPositiveButton(R.string.complete_with_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Uri smsUri = Uri.parse("sms:" + item.consigneeContact);
                            Intent sendIntent = new Intent(Intent.ACTION_SENDTO, smsUri);
                            sendIntent.putExtra("sms_body", "고객(" + item.consigneeName + ")님께서 요청하신 물품 배송완료되었습니다.");
                            startActivity(sendIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        item.status = TmsParcelItem.STATUS_DELIVERED;
                        mFbConnector.postParcelItemToFirebaseDatabase(selectedDate, item);
                        updateStatusToComplete();
                        setMarkertToBlue(mapPOIItem);
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton(R.string.complete_without_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.status = TmsParcelItem.STATUS_DELIVERED;
                        mFbConnector.postParcelItemToFirebaseDatabase(selectedDate, item);
                        updateStatusToComplete();
                        setMarkertToBlue(mapPOIItem);

                    }
                });

        mDeliveryCompleteDialog.show();
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
