package com.lge.pickitup;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;


import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
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
import java.util.List;

public class MapViewActivity extends AppCompatActivity
        implements MapView.OpenAPIKeyAuthenticationResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener, View.OnClickListener, MapView.CurrentLocationEventListener {

    private FirebaseDatabaseConnector mFbConnector;
    private static final String LOG_TAG = "MapViewActivity";
    private static MapView mMapView;
    public static final String DAUM_MAPS_ANDROID_APP_API_KEY = "8be996dd99057764a9876591b3270e31";
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private AlertDialog.Builder mChangeOrderDialog;
    private String mSelectedDate;
    private static String mSelectedCourierName;
    private String mSelectedSectionID;
    private ImageView mTrackingModeBtn;
    private TextView mTvCountInfo;
    private TmsItemAdapter mArrayAdapter;

    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private static HashMap<MarkerItem, ArrayList<TmsParcelItem>> mMarkerHash = new HashMap();
    private static HashMap<MarkerItem, ArrayList<MapPOIItem>> mMapPOItemHash = new HashMap();
    private ArrayList<String> mArrayKeys = new ArrayList<String>();
    private static ArrayList<TmsParcelItem> mArrayValues = new ArrayList<TmsParcelItem>();
    private static ArrayList<TmsParcelItem> mArrayParcelListValues = new ArrayList<TmsParcelItem>();
    private static ArrayList<Integer> mArrayValuesOrder = new ArrayList<>();
    private static ArrayList<TmsCourierItem> mCourierArrayValues = new ArrayList<TmsCourierItem>();

    private static String mSort = "id";
    private ScrollView mScrollView;


    private static float mInitLatitude = 0;
    private static float mInitLongitude = 0;
    private TmsParcelItem mCompleteTarget;
    private MapPOIItem mCompleteMarker;
    private MapPOIItem mSelectedMarkerToChangeOrder = null;

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
        if (Utils.isCourierAuth()) {
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
        mScrollView = (ScrollView) findViewById(R.id.scroll_view);
        mTvCountInfo = (TextView) findViewById(R.id.tv_countinfo);
        mTrackingModeBtn = (ImageView) findViewById(R.id.ib_tracking);
        mTrackingModeBtn.setOnClickListener(this);
        mTrackingModeBtn.setZ(10);
        mScrollView.setZ(20);
        //mBtnDeliveryinfo = (Button) findViewById(R.id.btn_deliveryinfo);
        //mBtnDeliveryinfo.setOnClickListener(this);

        mScrollView.setOnClickListener(this);

        GlobalRes = getResources();

        mArrayAdapter = new TmsItemAdapter(this, R.layout.parcel_listview_row, mArrayParcelListValues);
        ListView listView = findViewById(R.id.db_list_view);
        listView.setAdapter(mArrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                TmsParcelItem item = (TmsParcelItem) adapterView.getItemAtPosition(position);
                String LatitudeStr = item.consigneeLatitude;
                String LongitudeStr = item.consigneeLongitude;

                Log.d(LOG_TAG, "Address = " + item.consigneeAddr + ", Id = " + item.id);
                Log.d(LOG_TAG, "lat = " + LatitudeStr + ", long = " + LongitudeStr);

                if (TextUtils.isEmpty(LatitudeStr) || TextUtils.isEmpty(LongitudeStr)) {
                    Toast.makeText(MapViewActivity.this, R.string.need_manual_input, Toast.LENGTH_SHORT).show();
                    return;
                }
                Utils.startKakaoMapActivity(MapViewActivity.this, Double.valueOf(LatitudeStr), Double.valueOf(LongitudeStr));
            }
        });

    }
    @Override
    public void onClick(View view) {
        MapPOIItem poiitem;
        TmsParcelItem parcelItem;
        switch (view.getId()) {
            /* likepaul block
            case R.id.parcel_data:
                MapPOIItem mapPOIItem = (MapPOIItem)view.getTag(R.id.parcel_data);
                Utils.startKakaoMapActivity(MapViewActivity.this, mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude, mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude);
                break;
             */
            case R.id.btn_deliveryinfo:
                TmsParcelItem item = (TmsParcelItem)view.getTag(R.id.btn_deliveryinfo);
                goToUploadImageActivity(item, Utils.NO_NEED_RESULT);
                break;
            case R.id.btn_changeorder:
                poiitem = (MapPOIItem)view.getTag(R.id.btn_changeorder);
                parcelItem = (TmsParcelItem) poiitem.getUserObject();
                processChangeOrderDialog(parcelItem, poiitem);
                break;
            case R.id.btn_complete:
                poiitem = (MapPOIItem)view.getTag(R.id.btn_complete);
                parcelItem = (TmsParcelItem) poiitem.getUserObject();
                processListBtnClick(parcelItem, poiitem);
                break;
            case R.id.ib_tracking:
                toggleTrackingMode();
                break;

        }
    }

    protected class TmsItemAdapter extends ArrayAdapter<TmsParcelItem> {

        public TmsItemAdapter(Context context, int resource, List<TmsParcelItem> list) {
            super(context, resource, list);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.parcel_listview_row, null);
            }

            final TmsParcelItem item = getItem(position);
            boolean isDeliverd = item.status.equals(TmsParcelItem.STATUS_DELIVERED);
            boolean isValidAddress = !(item.consigneeLatitude.equals("0") || item.consigneeLongitude.equals("0"));

            if (item != null) {
                TextView addrText = v.findViewById(R.id.listAddr);
                TextView providerText = v.findViewById(R.id.listItemTextProvider);
                TextView customerText = v.findViewById(R.id.listItemTextCustomer);
                TextView deliveryNote = v.findViewById(R.id.listItemTextDeliveryMemo);
                TextView remark = v.findViewById(R.id.listItemTextRemark);
                Button btn_changeorder = v.findViewById(R.id.btn_changeorder);
                Button btn_complete = v.findViewById(R.id.btn_complete);
                Button btn_deliveryinfo = v.findViewById(R.id.btn_deliveryinfo);
                ImageView statusIcon = v.findViewById(R.id.status_icon);

                ArrayList<MapPOIItem> arrPoiitems = mMapPOItemHash.get(new MarkerItem(item.consigneeLatitude, item.consigneeLongitude));

                btn_complete.setOnClickListener(MapViewActivity.this);
                btn_complete.setTag(R.id.btn_complete, arrPoiitems.get(position));
                btn_changeorder.setOnClickListener(MapViewActivity.this);
                btn_changeorder.setTag(R.id.btn_changeorder, arrPoiitems.get(position));
                btn_deliveryinfo.setOnClickListener(MapViewActivity.this);
                btn_deliveryinfo.setTag(R.id.btn_deliveryinfo, item);

                if (addrText != null) {
                    String addrTextValue = "";

                    if (!mSelectedCourierName.equals(getString(R.string.all_couriers))) {
                        addrTextValue = mArrayValuesOrder.get(position) + " : ";
                    }
                    addrText.setText(addrTextValue + item.consigneeAddr);
                    if (isDeliverd) {
                        addrText.setTextColor(0xFF68C166);
                        statusIcon.setVisibility(View.VISIBLE);
                        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_delivered_v2));
                        btn_complete.setVisibility(View.GONE);
                        btn_changeorder.setVisibility(View.GONE);
                        btn_deliveryinfo.setVisibility(View.VISIBLE);
                        btn_deliveryinfo.setBackgroundColor(0xFF68C166);
                    } else {
                        if (isValidAddress) {
                            addrText.setTextColor(0xFF4F4F4F);
                        } else {
                            addrText.setTextColor(0xFFC12F2F);
                        }
                        statusIcon.setVisibility(View.INVISIBLE);
                        btn_complete.setVisibility(View.VISIBLE);
                        if (mSelectedCourierName.equals(getString(R.string.all_couriers))) {
                            btn_changeorder.setVisibility(View.INVISIBLE);
                        } else {
                            btn_changeorder.setVisibility(View.VISIBLE);
                        }
                        btn_complete.setBackgroundColor(0xFF42A5F5);
                        btn_deliveryinfo.setVisibility(View.GONE);
                    }
                }
                if (providerText != null) {
                    providerText.setText("업체명" + " : " + item.consignorName);
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
            return v;
        }
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
        if (item.sectorId == -1) {
            pinSeletedResourceId = (int) mSelectedMarkerList.get(0);
        } else {
            pinSeletedResourceId = (int) mSelectedMarkerList.get((Integer.valueOf(item.sectorId) - 1)%10);
        }

        Bitmap bmp = BitmapFactory.decodeResource(GlobalRes, pinSeletedResourceId, bmOptions).copy(Bitmap.Config.ARGB_8888, true);
        return bmp;
    }

    private void addMarkerHash(TmsParcelItem item) {
        String strLatitude = item.consigneeLatitude;
        String strLongitude = item.consigneeLongitude;
        if (strLatitude.isEmpty())
            strLatitude = "0";

        if (strLongitude.isEmpty())
            strLongitude = "0";

        MarkerItem markeritem = new MarkerItem(strLatitude, strLongitude);

        ArrayList<TmsParcelItem> list_parcelitem;
        if (mMarkerHash.containsKey(markeritem)) {
            list_parcelitem = mMarkerHash.get(markeritem);
        } else {
            list_parcelitem = new ArrayList<>();
        }
        list_parcelitem.add(item);
        mMarkerHash.put(markeritem, list_parcelitem);
    }

    protected void addMarker() {
        mMapView.removeAllPOIItems();
        if (Utils.isRootAuth()) {
            addCourierMarker();
        }
        int num = 1;

        for (TmsParcelItem item : mArrayValues) {
            String strLatitude = item.consigneeLatitude;
            String strLongitude = item.consigneeLongitude;

            Log.i(LOG_TAG, "addr = " + item.consigneeAddr);
            Log.i(LOG_TAG, "lat = " + item.consigneeLatitude);
            Log.i(LOG_TAG, "lon = " + item.consigneeLongitude);
            Log.i(LOG_TAG, "status = " + item.status);
            Log.i(LOG_TAG, "orderInRoute = " + item.orderInRoute);

            if (strLatitude.isEmpty())
                strLatitude = "0";

            if (strLongitude.isEmpty())
                strLongitude = "0";

            if ((mInitLatitude == 0 || mInitLongitude == 0) && !(strLatitude.equals("0") || strLongitude.equals("0"))) {
                mInitLatitude = Float.valueOf(item.consigneeLatitude);
                mInitLongitude = Float.valueOf(item.consigneeLongitude);
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

            if (!mSelectedCourierName.equals(GlobalRes.getString(R.string.all_couriers))) {
                int textVal = num;
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.BLACK); // Text Color

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
                if (!isDeliverd) {
                    Canvas canvas = new Canvas(pin);
                    canvas.drawText(String.valueOf(textVal), posX, 37, paint); // 63
                }

                Canvas canvas2 = new Canvas(seleted_pin);
                canvas2.drawText(String.valueOf(textVal), posX, 37, paint); // 63

            }
            if (mSelectedCourierName.equals(GlobalRes.getString(R.string.all_couriers))) {
                marker.setTag(-1);
            } else {
                marker.setTag(num);
            }

            MarkerItem markeritem = new MarkerItem(strLatitude, strLongitude);

            ArrayList<TmsParcelItem> list_parcelitem;
            list_parcelitem = mMarkerHash.get(markeritem);
            if (list_parcelitem.size() > 1) {
                int count = list_parcelitem.size();
                pin = drawCountPaint(count, pin);
                seleted_pin = drawCountPaint(count, seleted_pin);
            }

            ArrayList<MapPOIItem> list_poiitem;
            if (mMapPOItemHash.containsKey(markeritem)) {
                list_poiitem = mMapPOItemHash.get(markeritem);
            } else {
                list_poiitem = new ArrayList<>();
            }

            marker.setCustomImageBitmap(pin);
            marker.setCustomSelectedImageBitmap(seleted_pin);
            marker.setCustomImageAutoscale(false);

            list_poiitem.add(marker);
            mMapPOItemHash.put(markeritem, list_poiitem);
            if (list_poiitem.size() == 1) {
                mMapView.addPOIItem(marker);
            } else {
                if (!item.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                    boolean hasNotDeliveryStatus = false;
                    for (int i=0; i < list_poiitem.size() -1 ; i++) {
                        TmsParcelItem parcelItem = (TmsParcelItem) list_poiitem.get(i).getUserObject();
                        if (!parcelItem.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                            hasNotDeliveryStatus = true;
                            break;
                        }
                    }
                    if (!hasNotDeliveryStatus) {
                        mMapView.addPOIItem(marker);
                    }
                }
            }
            num++;
        }
    }

    private Bitmap drawCountPaint(int count, Bitmap pin) {
        Paint paint_circle = new Paint();
        Paint paint_text = new Paint();
        paint_circle.setStyle(Paint.Style.FILL_AND_STROKE);
        paint_circle.setColor(Color.RED); // Text Color
        paint_text.setStyle(Paint.Style.FILL);
        paint_text.setColor(Color.BLACK); // Text Color
        paint_text.setTextSize(100);
        Canvas canvas = new Canvas(pin);
        canvas.drawCircle(10, 10, 10, paint_circle);
        //canvas.drawText(String.valueOf(count), 0, 0, paint_text); // 63
        return pin;
    }


    private void getFirebaseList() {
        mFbConnector.getCourierListFromFirebaseDatabaseWithListener(mSelectedDate, mCourierValueEventListener);
    }

    private ValueEventListener mCourierValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            mCourierDatabaseHash.clear();
            mCourierArrayValues.clear();
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                TmsCourierItem item = postSnapshot.getValue(TmsCourierItem.class);
                if (Utils.mCurrentUserItem.hasChild(item.name)) {
                    mCourierDatabaseHash.put(item.name, item);
                    mCourierArrayValues.add(item);
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
        if (Utils.isConsignorAuth()) {
            firebaseQuery = mDatabaseRef.child(mFbConnector.PARCEL_REF_NAME).child(pathString).orderByChild(TmsParcelItem.KEY_CONSIGNOR_NAME).equalTo(Utils.mCurrentUser.getDisplayName());
        } else {
            if (select.equals(getString(R.string.all_couriers))) {
                firebaseQuery = mDatabaseRef.child(mFbConnector.PARCEL_REF_NAME).child(pathString).orderByChild(orderBy);
            } else {
                firebaseQuery = mDatabaseRef.child(mFbConnector.PARCEL_REF_NAME).child(pathString).orderByChild(orderBy).equalTo(select);
            }
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

            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                String key = postSnapshot.getKey();
                TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);
                String courier = value.courierName;
                String brand = value.consignorName;
                if (Utils.isRootAuth() || Utils.mCurrentUserItem.brand.equals(brand) || Utils.mCurrentUserItem.hasChild(courier)) {
                    mParcelDatabaseHash.put(key, value);
                    mArrayKeys.add(key);
                    mArrayValues.add(value);
                    addMarkerHash(value);
                }
            }

            String courierName = mSelectedCourierName;
            if (!courierName.equals(R.string.all_couriers) ) {
                TmsCourierItem courierItem = mCourierDatabaseHash.get(courierName);
                if(courierItem != null) {
                    TmsParcelItem parcelItem = mParcelDatabaseHash.get(String.valueOf(courierItem.startparcelid));
                    if (parcelItem != null) {
                        mArrayValues.clear();
                        mArrayValues.add(parcelItem);
                        makeToastToInformWrongAddress(parcelItem, mArrayValues.size());
                        while(parcelItem.nextParcel != -1) {
                            parcelItem = mParcelDatabaseHash.get(String.valueOf(parcelItem.nextParcel));
                            mArrayValues.add(parcelItem);
                            makeToastToInformWrongAddress(parcelItem, mArrayValues.size());
                        }
                    }
                }
            }
            addMarker();
            if (mSelectedMarkerToChangeOrder != null) {
                showInfoListview(mSelectedMarkerToChangeOrder);
                mSelectedMarkerToChangeOrder = null;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w(LOG_TAG, "getParcelListFromFirebaseDatabase, ValueEventListener.onCancelled", databaseError.toException());
        }
    };

    private void makeToastToInformWrongAddress(TmsParcelItem item, int order) {
        if (item.consigneeLatitude.isEmpty() || item.consigneeLatitude.equals("0")
                || item.consigneeLongitude.isEmpty() || item.consigneeLongitude.equals("0"))
            Toast.makeText(MapViewActivity.this, order + "번: " + item.consigneeName +"고객건은 잘못된 주소이므로 지도에 표시되지 않습니다.", Toast.LENGTH_SHORT).show();
    }

    private void addCourierMarker() {
        for (TmsCourierItem courierItem : mCourierArrayValues) {
            addCourierMarker(courierItem);
        }
    }

    private void addCourierMarker(TmsCourierItem courierItem) {
        if (!Utils.isRootAuth()) {
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

    public void onMapViewInitialized(MapView mapView) {
        Log.i(LOG_TAG, "MapView had loaded. Now, MapView APIs could be called safely");
        Log.i(LOG_TAG, String.format("onMapViewInitialized %f, %f", mInitLatitude, mInitLongitude));
        if (Utils.mCurrent != null) {
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(Utils.mCurrent.getLatitude(), Utils.mCurrent.getLongitude()), 7, true);
        } else {
            if (mInitLatitude== 0 || mInitLongitude==0) {
                mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(37.560128,126.995802), 9, true);
            } else {
                mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(mInitLatitude,mInitLongitude), 7, true);
            }
        }

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapCenterPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapCenterPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onMapViewCenterPointMoved (%f,%f)", mapPointGeo.latitude, mapPointGeo.longitude));
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {
    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        mScrollView.setVisibility(View.GONE);
        mTvCountInfo.setVisibility(View.GONE);
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
        if (mapPOIItem.getUserObject() instanceof  TmsParcelItem) {
            mScrollView.setVisibility(View.VISIBLE);
            showInfoListview(mapPOIItem);
        }
    }

    private void showInfoListview(MapPOIItem mapPOIItem) {
        final TmsParcelItem item = (TmsParcelItem) mapPOIItem.getUserObject();

        MarkerItem markeritem = new MarkerItem(item.consigneeLatitude, item.consigneeLongitude);
        final ArrayList<TmsParcelItem> sameGeoMarkerItems = mMarkerHash.get(markeritem);
        final ArrayList<MapPOIItem> sameGeoPOIItems = mMapPOItemHash.get(markeritem);
        mArrayParcelListValues.clear();
        mArrayValuesOrder.clear();
        if (sameGeoMarkerItems != null ) {
            if (sameGeoMarkerItems.size() > 1) {
                int deliveryed_count = 0;
                for (MapPOIItem poiItem : sameGeoPOIItems) {
                    TmsParcelItem parcelItem = (TmsParcelItem)poiItem.getUserObject();
                    if (parcelItem.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                        deliveryed_count++;
                    }
                    mArrayParcelListValues.add(parcelItem);
                    mArrayValuesOrder.add(poiItem.getTag());
                }
                mTvCountInfo.setText("선택된 주문 총 " + mArrayParcelListValues.size()  + "개 중 " + deliveryed_count + "개 배송완료" );
                mTvCountInfo.setVisibility(View.VISIBLE);
            } else {
                mArrayParcelListValues.add(item);
                mArrayValuesOrder.add(sameGeoPOIItems.get(0).getTag());
            }
        }
        mArrayAdapter.notifyDataSetChanged();
    }

    private void processChangeOrderDialog(final TmsParcelItem item, final MapPOIItem mapPOIItem) {

        TmsCourierItem courierItem = mCourierDatabaseHash.get(mSelectedCourierName);
        if (courierItem.startparcelid == -1) {
            Toast.makeText(MapViewActivity.this, getString(R.string.need_to_upload_bylastRelease), Toast.LENGTH_LONG).show();
            return;
        }

        final EditText edittext = new EditText(this);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        final int prevOrder = mapPOIItem.getTag();
        final int sizeofParcels = mArrayValues.size();
        mChangeOrderDialog = new AlertDialog.Builder(this)
                .setTitle(prevOrder +"번 순서변경")
                .setMessage(item.consigneeName + ": " + item.consigneeAddr + "\n\n변경되길 원하는 순서를 입력하세요.\n" + " (1 ~ " + sizeofParcels + ")")
                .setView(edittext)
                .setPositiveButton(R.string.dialog_title_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        String edittextvalue = edittext.getText().toString();
                        if (edittextvalue.isEmpty()) {
                            return;
                        }
                        int newOrder = Integer.valueOf(edittextvalue);
                        if (prevOrder == newOrder) {
                            Toast.makeText(MapViewActivity.this, "기존과 동일한 순서정보를 입력하셨습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newOrder < 1 || newOrder > sizeofParcels) {
                            Toast.makeText(MapViewActivity.this, "유효한 범위의 숫자를 입력하세요" + " (1 ~ " + sizeofParcels + ")", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DatabaseReference.CompletionListener listener = new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                mSelectedMarkerToChangeOrder = mapPOIItem;
                                getFirebaseList();
                            }
                        };
                        mFbConnector.proceedChangeOrder(mArrayValues, item, prevOrder, newOrder, mSelectedCourierName, mSelectedDate, listener, mCourierDatabaseHash);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });


        mChangeOrderDialog.show();
    }

    private void processListBtnClick(final TmsParcelItem item, final MapPOIItem mapPOIItem) {

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
                        Utils.makeComplete(mFbConnector, mCompleteTarget, mSelectedDate, filePath, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                getFirebaseList();
                                showInfoListview(mCompleteMarker);
                            }
                        });
                        mMapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(Double.valueOf(mCompleteTarget.consigneeLatitude),Double.valueOf(mCompleteTarget.consigneeLongitude)), 7, true);
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
