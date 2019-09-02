package com.lge.pickitup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ParcelListActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "ParcelListActivity";
    private static final int SEND_COMPLETED_MESSAGE = 1;
    final int MY_PERMISSIONS = 998;
    final Calendar myCalendar = Calendar.getInstance();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mCurrentUser;
    private FirebaseDatabaseConnector mFbConnector;
    private TmsItemAdapter mArrayAdapter;
    private TmsParcelItem mCompleteTarget;
    private TextView mTvAccountName;
    private TextView mTvSignOutText;
    private ImageView mIvConnStatus;
    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private ArrayList<String> mArrayKeys = new ArrayList<String>();
    private ArrayList<TmsParcelItem> mArrayValues = new ArrayList<TmsParcelItem>();
    private ArrayList<TmsCourierItem> mCourierArrayValues = new ArrayList<TmsCourierItem>();
    private TextView mTextCourierName;
    private TextView mTextCourierDate;
    private TextView mTextCount;
    private Button mBtnUpdateList;
    private Button mBtnChangeView;
    private DatePickerDialog mDatePickerDialog;
    private AlertDialog.Builder mCourierPickerDialog;
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private SimpleDateFormat mSdf;
    private View.OnTouchListener mTouchListner;
    private String mOldDateStr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_list);

        Bundle b = getIntent().getExtras();
        String dateStr;
        String courierStr;

        if (b != null) {
            dateStr = b.getString(Utils.KEY_DB_DATE);
            courierStr = b.getString(Utils.KEY_COURIER_NAME);
        } else {
            dateStr = Utils.getTodayDateStr();
            courierStr = getString(R.string.all_couriers);
        }
        mOldDateStr = dateStr;

        //Get FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        //Make AuthStateListener to know oAuth's auth state
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    mCurrentUser = user;
                    Log.d(LOG_TAG, "onAuthStateChanged: signed in to UID :" + user.getUid());
                    Log.d(LOG_TAG, "onAuthStateChanged: signed in to email:" + user.getEmail());
                    Log.d(LOG_TAG, "onAuthStateChanged: signed in to display name:" + user.getDisplayName());
                } else {
                    mCurrentUser = null;
                    // User is signed out
                    Log.d(LOG_TAG, "onAuthStateChanged: signed_out");

                    Intent intent = new Intent(ParcelListActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                updateConnectUI();
            }
        };

        initResources();

        mTextCourierDate.setText(dateStr);
        mTextCourierName.setText(courierStr);

        mFbConnector = new FirebaseDatabaseConnector(this);
        mFbConnector.setParcelHash(this.mParcelDatabaseHash);
        mFbConnector.setCourierHash(this.mCourierDatabaseHash);
        mFbConnector.setParcelKeyArray(this.mArrayKeys);
        mFbConnector.setParcelValueArray(this.mArrayValues);
        mFbConnector.setListAdapter(this.mArrayAdapter);
        mFbConnector.setCourierValueArray(this.mCourierArrayValues);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS);
        } else {
            Utils.initLocation(this);
        }

        refreshList(courierStr);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(mTextCourierName.getText().toString());
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
                        Utils.makeComplete(mFbConnector, mCompleteTarget, mTextCourierDate.getText().toString(), filePath);
                    } else {

                    }
                }

                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    Utils.initLocation(this);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    private void updateConnectUI() {
        if (mCurrentUser != null) {
            mIvConnStatus.setImageDrawable(getDrawable(R.mipmap.activity_connect_account_settings_connected));
            mTvAccountName.setText(mCurrentUser.getDisplayName() + " (" + mCurrentUser.getEmail() + ")");
            mTvAccountName.setBackground(getDrawable(R.drawable.connected_account_border));
            mTvSignOutText.setBackground(getDrawable(R.drawable.active_border2));
            mTvSignOutText.setClickable(true);
        } else {
            mIvConnStatus.setImageDrawable(getDrawable(R.mipmap.activity_connect_account_settings_disconnected));
            mTvAccountName.setText(getText(R.string.disconnected_text));
            mTvAccountName.setBackground(getDrawable(R.drawable.disconnected_account_border));
            mTvSignOutText.setBackground(getDrawable(R.drawable.disconnected_account_border));
            mTvSignOutText.setClickable(false);
        }
    }


    private void initResources() {
        mTvAccountName = findViewById(R.id.conn_account_name);
        mIvConnStatus = findViewById(R.id.conn_image);
        mTvSignOutText = findViewById(R.id.sign_out_text);

        mTvSignOutText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder signOutAlert = new AlertDialog.Builder(ParcelListActivity.this);

                signOutAlert.setTitle(getText(R.string.sign_out_alert_title))
                        .setMessage(getText(R.string.sign_out_alert_message))
                        .setPositiveButton(getText(R.string.text_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mAuth.signOut();
                                dialogInterface.dismiss();
                            }
                        }).setNegativeButton(getText(R.string.text_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();

            }
        });

        mTextCourierName = findViewById(R.id.text_courier_name);
        mTextCourierDate = findViewById(R.id.text_courier_date);


        mBtnUpdateList = findViewById(R.id.btn_update);
        mBtnChangeView = findViewById(R.id.btn_change_view);

        mTouchListner = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundColor(0xFFE91E63);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.setBackgroundColor(0xFF42A5F5);
                }
                return false;
            }
        };

        mBtnUpdateList.setOnTouchListener(mTouchListner);
        mBtnChangeView.setOnTouchListener(mTouchListner);

        mTextCourierDate.setOnClickListener(this);
        if (Arrays.asList(Utils.ADMIN_UIDS).contains(mAuth.getUid())) {
            mTextCourierName.setOnClickListener(this);
        }
        mBtnUpdateList.setOnClickListener(this);
        mBtnChangeView.setOnClickListener(this);

        mSdf = new SimpleDateFormat("yyyy-MM-dd");

        mTextCount = findViewById(R.id.show_item_num);

        mArrayAdapter = new TmsItemAdapter(this, R.layout.parcel_listview_row, mArrayValues);
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

                // Daum API 에서 위경도 받아오지 못한 항목 선택 시 직접 입력 안내 toast 실행
                if (TextUtils.isEmpty(LatitudeStr) || TextUtils.isEmpty(LongitudeStr)) {
                    Toast.makeText(ParcelListActivity.this, R.string.need_manual_input, Toast.LENGTH_SHORT).show();
                    return;
                }
                Utils.startKakaoMapActivity(ParcelListActivity.this, Double.valueOf(LatitudeStr), Double.valueOf(LongitudeStr));
            }
        });

        mDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                String newDateStr = mSdf.format(newDate.getTime());

                if (!newDateStr.equals(mOldDateStr) && Utils.isAdminAuth()) {
                    resetCourierText();
                }
                mTextCourierDate.setText(newDateStr);
                refreshList(mTextCourierName.getText().toString());
            }
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));
    }

    private boolean isCourierNameHasDefaultText() {
        return mTextCourierName.getText().equals(getString(R.string.all_couriers));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_update:
                refreshList(mTextCourierName.getText().toString());
                break;

            case R.id.btn_change_view:
                Intent intent = new Intent(ParcelListActivity.this, MapViewActivity.class);
                intent.putExtra(TmsParcelItem.KEY_DATE, mTextCourierDate.getText().toString());
                intent.putExtra(TmsParcelItem.KEY_COURIER_NAME, mTextCourierName.getText().toString());
                startActivity(intent);
                break;

            case R.id.text_courier_date:
                mDatePickerDialog.show();
                break;

            case R.id.text_courier_name:
                showCourierPicker();
                break;
        }

    }

    private void resetCourierText() {
        mTextCourierName.setText(getString(R.string.default_courier_name));
    }

    private void showCourierPicker() {
        final String[] items = prepareCourierArray();
        mCourierPickerDialog = new AlertDialog.Builder(this);
        mCourierPickerDialog.setTitle(getString(R.string.courier_sel_dialog_title));
        int defaultIdx = 0;
        final List selectedItems = new ArrayList<>();
        selectedItems.add(defaultIdx);

        mCourierPickerDialog.setSingleChoiceItems(items, defaultIdx, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int pos) {
                selectedItems.clear();
                selectedItems.add(pos);
            }
        }).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int pos) {
                Log.d(LOG_TAG, "Select button is pressed");

                if (!selectedItems.isEmpty()) {
                    int index = (int) selectedItems.get(0);
                    mTextCourierName.setText(items[index]);
                }
                refreshList(mTextCourierName.getText().toString());
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int pos) {
                Log.d(LOG_TAG, "Cancel button is pressed");
                dialogInterface.cancel();
            }
        });

        mCourierPickerDialog.show();
    }

    private String[] prepareCourierArray() {
        ArrayList<TmsCourierItem> courierArrayList = new ArrayList<>();
        ArrayList<String> strArrayList = new ArrayList<>();
        Log.d(LOG_TAG, "prepareCourierArray, size = " + mCourierDatabaseHash.size());
        courierArrayList.addAll(mCourierDatabaseHash.values());
        strArrayList.add(getString(R.string.default_courier_name));
        for (TmsCourierItem item : courierArrayList) {
            strArrayList.add(item.name);
        }
        String[] result = strArrayList.toArray(new String[strArrayList.size()]);
        return result;
    }
    ValueEventListener mParcelListEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            mParcelDatabaseHash.clear();
            mArrayKeys.clear();
            mArrayValues.clear();
            boolean isRouted = true;

            Log.d(LOG_TAG, "getParcelListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                String key = postSnapshot.getKey();
                TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);

                if (value.orderInRoute == -1) {
                    isRouted = false;
                }

                mParcelDatabaseHash.put(key, value);
                mArrayKeys.add(key);
                mArrayValues.add(value);

                Log.d(LOG_TAG, "mArrayValues size = " + mArrayValues.size());
            }
            if (!isRouted) {
                for (TmsParcelItem item : mArrayValues) {
                    item.orderInRoute = -1;
                }
            }

            if (mArrayValues.size() > 0) {
                Collections.sort(mArrayValues);
            }
            if (mArrayAdapter != null) {
                mArrayAdapter.notifyDataSetChanged();
            }
        }
        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    private void refreshList(String courierName) {
        String selectedDate = mTextCourierDate.getText().toString();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        mFbConnector.getCourierListFromFirebaseDatabase(selectedDate, TmsParcelItem.KEY_ID);
        if (courierName.equals(getString(R.string.all_couriers))) {
            //mFbConnector.getParcelListFromFirebaseDatabase(selectedDate, TmsParcelItem.KEY_ID);
            Query firebaseQuery = databaseRef.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(selectedDate).orderByChild(TmsParcelItem.KEY_ID);
            firebaseQuery.addValueEventListener(mParcelListEventListener);
        } else {
            //mFbConnector.getParcelListFromFirebaseDatabase(selectedDate, TmsParcelItem.KEY_COURIER_NAME, select);
            Query firebaseQuery = databaseRef.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(selectedDate).orderByChild(TmsParcelItem.KEY_COURIER_NAME).equalTo(courierName);
            firebaseQuery.addValueEventListener(mParcelListEventListener);

        }

        Log.d(LOG_TAG, "ParcelList size = " + mParcelDatabaseHash.size());
        Log.d(LOG_TAG, "CourierList size = " + mCourierDatabaseHash.size());
        Log.d(LOG_TAG, "KeyArray size = " + mArrayKeys.size());
        Log.d(LOG_TAG, "ValueArray size = " + mArrayValues.size());
    }



    private String getItemString(ArrayList<TmsParcelItem> items) {
        int numTotal = items.size();
        int numComplted = 0;

        for (TmsParcelItem item : items) {
            if (item.status.equals(TmsParcelItem.STATUS_DELIVERED))
                numComplted++;
        }

        String result = String.valueOf(numComplted) + "/" + String.valueOf(numTotal) + "개 배송 완료됨";
        return result;
    }

    private void processListBtnClick(final TmsParcelItem item) {

        Log.d(LOG_TAG, "Selected item\'s status will be chaanged to \"deliverd\"");

        mDeliveryCompleteDialog = new AlertDialog.Builder(this)
                .setTitle(getText(R.string.query_delivery_complete_title))
                .setMessage(getText(R.string.query_delivery_complete_message))
                .setPositiveButton(R.string.complete_with_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCompleteTarget = item;

                        Intent intent = new Intent(ParcelListActivity.this, UploadImageActivity.class);
                        intent.putExtra(Utils.SELECTED_ITEM, item);
                        intent.putExtra(Utils.SELECTED_DATE, mTextCourierDate.getText().toString());
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

    protected class TmsItemAdapter extends ArrayAdapter<TmsParcelItem> {

        public TmsItemAdapter(Context context, int resource, List<TmsParcelItem> list) {
            super(context, resource, list);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            mTextCourierName.setClickable(true);
            mTextCourierDate.setClickable(true);
            mBtnChangeView.setEnabled(true);
            mTextCount.setText(getItemString(mArrayValues));
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

            if (item != null) {
                TextView addrText = v.findViewById(R.id.listAddr);
                TextView customerText = v.findViewById(R.id.listItemTextCustomer);
                TextView deliveryNote = v.findViewById(R.id.listItemTextDeliveryMemo);
                TextView remark = v.findViewById(R.id.listItemTextRemark);
                Button btn_complete = v.findViewById(R.id.btn_complete);
                ImageView statusIcon = v.findViewById(R.id.status_icon);

                btn_complete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        processListBtnClick(item);
                    }
                });

                if (addrText != null) {
                    String addrTextValue = "";
                    if ((item.orderInRoute != -1) && !mTextCourierName.getText().toString().equals(getString(R.string.all_couriers))) {
                        addrTextValue = item.orderInRoute + " : ";
                    }
                    addrText.setText(addrTextValue + item.consigneeAddr);
                    if (isDeliverd) {
                        addrText.setTextColor(0xFF68c166);
                        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_delivered_v2));
                        btn_complete.setVisibility(View.INVISIBLE);
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
            return v;
        }
    }
}

