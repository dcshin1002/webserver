package com.lge.pickitup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ParcelListActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "ParcelListActivity";
    final int MY_PERMISSIONS = 998;
    final Calendar myCalendar = Calendar.getInstance();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabaseConnector mFbConnector;
    private TmsItemAdapter mArrayAdapter;
    private TmsParcelItem mCompleteTarget;
    private TextView mTvAccountName;
    private TextView mTvSignOutText;
    private ImageView mIvConnStatus;
    private HashMap<String, TmsParcelItem> mParcelDatabaseHash = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierDatabaseHash = new HashMap<>();
    private HashMap<String, LinkedList<TmsParcelItem>> mHashParcels = new HashMap<>();
    private HashMap<String, TmsCourierItem> mHashCouriers = new HashMap<>();
    private ArrayList<TmsParcelItem> mParcelArrayValues = new ArrayList<TmsParcelItem>();
    private ArrayList<TmsCourierItem> mCourierArrayValues = new ArrayList<TmsCourierItem>();
    private TextView mTextCourierName;
    private TextView mTextCourierDate;
    private TextView mTextCount;
    private ListView mListView;
    private CheckBox mAllCheckbox;
    private TextView mTextFilter;
    private EditText mEditTextFilter;
    private Button mBtnResetdb;
    private Button mBtnAssign;
    private Button mBtnChangeView;

    private DatePickerDialog mDatePickerDialog;
    private AlertDialog.Builder mCourierPickerDialog;
    private AlertDialog.Builder mDeliveryCompleteDialog;
    private SimpleDateFormat mSdf;
    private View.OnTouchListener mTouchListner;
    private String mOldDateStr;
    private ArrayList<String> mCourierArray = new ArrayList<>(); // To list up courier name list, It is set null when date is changed
    private int mFilterTypeId = 0;

    private DatabaseReference.CompletionListener mParcelListRemove = new DatabaseReference.CompletionListener() {
        @Override
        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
            String selectedDate = mTextCourierDate.getText().toString();
            Log.i(LOG_TAG, "mParcelListRemove is called");
            Toast.makeText(ParcelListActivity.this, selectedDate + " 날짜의 정보가 초기화 되었습니다. csv 파일을 다시 올려주세요", Toast.LENGTH_LONG).show();
        }
    };
    private ValueEventListener mCourierValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            mCourierDatabaseHash.clear();
            mCourierArrayValues.clear();
            Log.d(LOG_TAG, "CourierList size : " + dataSnapshot.getChildrenCount());
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                TmsCourierItem item = postSnapshot.getValue(TmsCourierItem.class);
                if (Utils.mCurrentUserItem.hasChild(item.name)) {
                    mCourierDatabaseHash.put(item.name, item);
                    mCourierArrayValues.add(item);
                    mHashCouriers.put(item.name, item);
                }
                mHashParcels.put(item.name, new LinkedList<TmsParcelItem>());
            }
            mHashParcels.put("", new LinkedList<TmsParcelItem>());
            Log.d(LOG_TAG, "mCourierValueEventListener, size = " + mCourierDatabaseHash.size());
            String courierName = mTextCourierName.getText().toString();
            String selectedDate = mTextCourierDate.getText().toString();
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
            Query firebaseQuery;
            if (Utils.isConsignorAuth()) {
                firebaseQuery = databaseRef.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(selectedDate).orderByChild(TmsParcelItem.KEY_CONSIGNOR_NAME).equalTo(Utils.mCurrentUserItem.brand);
            } else {
                if (courierName.equals(getString(R.string.all_couriers))) {
                    firebaseQuery = databaseRef.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(selectedDate).orderByChild(TmsParcelItem.KEY_ID);
                } else {
                    firebaseQuery = databaseRef.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(selectedDate).orderByChild(TmsParcelItem.KEY_COURIER_NAME).equalTo(courierName);
                }
            }
            firebaseQuery.addValueEventListener(mParcelListEventListener);

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    ValueEventListener mParcelListEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            mParcelDatabaseHash.clear();
            mParcelArrayValues.clear();
            Log.i(LOG_TAG, "getParcelListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());

            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                String key = postSnapshot.getKey();
                TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);

                String courier = value.courierName;
                String brand = value.consignorName;
                if (Utils.isRootAuth() || Utils.mCurrentUserItem.brand.equals(brand) || Utils.mCurrentUserItem.hasChild(courier)) {
                    mParcelDatabaseHash.put(key, value);
                    mParcelArrayValues.add(value);
                    mHashParcels.get(value.courierName).add(value);
                }

            }
            String courierName = mTextCourierName.getText().toString();
            if (!courierName.equals(R.string.all_couriers)) {
                TmsCourierItem courierItem = mCourierDatabaseHash.get(courierName);
                if (courierItem != null) {
                    TmsParcelItem parcelItem = mParcelDatabaseHash.get(String.valueOf(courierItem.startparcelid));
                    if (parcelItem != null) {
                        mParcelArrayValues.clear();
                        mHashParcels.get(courierName).clear();
                        while (parcelItem != null) {
                            mParcelArrayValues.add(parcelItem);
                            parcelItem.orderInList = mParcelArrayValues.size();
                            mHashParcels.get(courierName).add(parcelItem);
                            if (parcelItem.nextParcel == -1) break;
                            parcelItem = mParcelDatabaseHash.get(String.valueOf(parcelItem.nextParcel));
                        }
                    }
                }
            }
            Collections.sort(mParcelArrayValues);

            if (mCourierArray.isEmpty()) {
                mCourierArray = prepareCourierArray();
            }

            if (mArrayAdapter != null) {
                mArrayAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_list);

        Bundle b = getIntent().getExtras();
        String dateStr;
        final String courierStr;

        if (b != null) {
            dateStr = b.getString(Utils.KEY_DB_DATE);
            if (Utils.isAdminAuth()) {
                courierStr = getString(R.string.all_couriers);
            } else {
                courierStr = b.getString(Utils.KEY_COURIER_NAME);
            }
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
                    Utils.mCurrentUser = user;
                    Utils.mCurrentUserItem = Utils.mHashUserList.get(user.getUid());
                    Log.e(LOG_TAG, "onAuthStateChanged: signed in to UID :" + user.getUid());
                    Log.e(LOG_TAG, "onAuthStateChanged: signed in to email:" + user.getEmail());
                    Log.e(LOG_TAG, "onAuthStateChanged: signed in to display name:" + user.getDisplayName());
                } else {
                    Utils.mCurrentUser = null;
                    // User is signed out
                    Log.d(LOG_TAG, "onAuthStateChanged: signed_out");

                    Intent intent = new Intent(ParcelListActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                updateConnectUI(courierStr);
            }
        };

        initResources();

        mTextCourierDate.setText(dateStr);
        mTextCourierName.setText(courierStr);
        mTextCourierName.setTag(0);

        mFbConnector = new FirebaseDatabaseConnector(this);
        mFbConnector.setParcelHash(this.mParcelDatabaseHash);
        mFbConnector.setCourierHash(this.mCourierDatabaseHash);
        mFbConnector.setParcelValueArray(this.mParcelArrayValues);
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
        if (Utils.mRootUserItem.size() == 0) {
            Utils.getUserListFromFirebase(mAuth, mAuthListener);
        } else {
            mAuth.addAuthStateListener(mAuthListener);
            refreshList(courierStr);
        }
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
            case Utils.SEND_COMPLETED_MESSAGE:
                Log.d(LOG_TAG, "onActivityResult, SEND_COMPLETED_MESSAGE");
                if (data != null) {
                    String sendResult = data.getStringExtra(UploadImageActivity.EXTRA_SEND_RESULT);

                    if (TextUtils.equals(sendResult, "success")) {
                        String filePath = data.getStringExtra(UploadImageActivity.EXTRA_UPLOADED_FILE_PATH);
                        Utils.makeComplete(mFbConnector, mCompleteTarget, mTextCourierDate.getText().toString(), filePath, null);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);

    }

    private void updateConnectUI(String courierName) {
        if (Utils.mCurrentUser != null) {
            mIvConnStatus.setImageDrawable(getDrawable(R.mipmap.activity_connect_account_settings_connected));
            courierName = Utils.mCurrentUser.getDisplayName();
            mTvAccountName.setText(courierName + " (" + Utils.mCurrentUser.getEmail() + ")");
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

        if (Utils.isRootAuth()) {
            mTextCourierName.setOnClickListener(this);
            mBtnResetdb.setVisibility(View.VISIBLE);
        } else if (Utils.isAdminAuth()) {
            mTextCourierName.setOnClickListener(this);
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

        final boolean checkBoxEnabled[] = {false};
        final ImageView mIvCheckBox = findViewById(R.id.checkbox_icon);
        if (Utils.isRootAuth() || Utils.isAdminAuth()) {
            mIvCheckBox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO: icon image should toggled base on status
                    if (!checkBoxEnabled[0]) {
                        checkBoxEnabled[0] = true;
                        mAllCheckbox.setVisibility(View.VISIBLE);
                        mBtnAssign.setVisibility(View.VISIBLE);
                        mArrayAdapter.setCheckboxVisible(true);
                    } else {
                        checkBoxEnabled[0] = false;
                        mAllCheckbox.setVisibility(View.GONE);
                        mBtnAssign.setVisibility(View.GONE);
                        mArrayAdapter.setCheckboxVisible(false);
                    }
                    mArrayAdapter.notifyDataSetChanged();
                }
            });
            //mIvCheckBox.setVisibility(View.VISIBLE);
        }


        mAllCheckbox = findViewById(R.id.all_parcels_checkbox);
        mAllCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (compoundButton.isPressed()) {
                    SparseBooleanArray checkedItems = mListView.getCheckedItemPositions();
                    int count = mArrayAdapter.getCount();

                    for (int i = count - 1; i >= 0; i--) {
                        TmsParcelItem item = (TmsParcelItem) mArrayAdapter.getItem(i);
                        item.setChecked(checked);
                    }
                }
                // TODO: toggle "all checked" if there is any unchecked in list

                if (mArrayAdapter != null) {
                    mArrayAdapter.notifyDataSetChanged();
                }
            }
        });

        mBtnChangeView = findViewById(R.id.btn_change_view);
        mBtnAssign = findViewById(R.id.btn_assign);
        mBtnResetdb = findViewById(R.id.btn_resetdb);
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

        mBtnChangeView.setOnTouchListener(mTouchListner);
        mBtnAssign.setOnTouchListener(mTouchListner);
        mBtnResetdb.setOnTouchListener(mTouchListner);

        mBtnChangeView.setOnClickListener(this);
        mBtnAssign.setOnClickListener(this);
        mBtnResetdb.setOnClickListener(this);
        mTextCourierDate.setOnClickListener(this);


        mSdf = new SimpleDateFormat("yyyy-MM-dd");

        mTextCount = findViewById(R.id.show_item_num);

        mArrayAdapter = new TmsItemAdapter(mParcelArrayValues);
        mListView = findViewById(R.id.db_list_view);
        mListView.setAdapter(mArrayAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                TmsParcelItem item = (TmsParcelItem) adapterView.getItemAtPosition(position);
                goToUploadImageActivity(item, Utils.NO_NEED_RESULT);
            }
        });

        mTextFilter = (TextView) findViewById(R.id.filter_text);
        registerForContextMenu(mTextFilter);

        mEditTextFilter = (EditText) findViewById(R.id.filter_edittext);
        mEditTextFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable edit) {
                String filterText = edit.toString();
                if (filterText.length() > 0) {
                    mListView.setFilterText(filterText);
                } else {
                    mListView.clearTextFilter();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                mCourierArray.clear();
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
        TmsParcelItem item;
        switch (view.getId()) {
            case R.id.btn_resetdb:
                AlertDialog.Builder resetDBAlert = new AlertDialog.Builder(ParcelListActivity.this);
                final String selectedDate = mTextCourierDate.getText().toString();
                resetDBAlert.setTitle(getText(R.string.reset_db_title))
                        .setMessage(String.format(getResources().getString(R.string.reset_db_alert_title), selectedDate))
                        .setPositiveButton(getText(R.string.text_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

                                databaseRef.child(FirebaseDatabaseConnector.PARCEL_REF_NAME).child(selectedDate).removeValue(mParcelListRemove);
                                databaseRef.child(FirebaseDatabaseConnector.COURIER_REF_NAME).child(selectedDate).removeValue();
                                databaseRef.child(FirebaseDatabaseConnector.JOB_STATUS_NAME).child(selectedDate).removeValue();

                            }
                        }).setNegativeButton(getText(R.string.text_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
                break;

            case R.id.btn_assign:
                // select courier from picker dialog
                showAssignCourierPicker();

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
                showSelectCourierPicker();
                break;
            case R.id.btn_deliveryinfo:
                item = (TmsParcelItem) view.getTag(R.id.btn_deliveryinfo);
                goToUploadImageActivity(item, Utils.NO_NEED_RESULT);
                break;
            case R.id.btn_complete:
                item = (TmsParcelItem) view.getTag(R.id.btn_complete);
                processListBtnClick(item);
                break;
            case R.id.btn_changeorder:
                item = (TmsParcelItem) view.getTag(R.id.btn_changeorder);
                int prevOrder = (int) view.getTag(R.id.status_icon);
                processChangeOrderDialog(item, prevOrder);
                break;
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filter_selection_menu, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {
        int filterTypeTobe = -1;
        switch (item.getItemId()) {
            case R.id.filter_address_item:
                filterTypeTobe = 0;
                mTextFilter.setText(item.getTitle());
                break;
            case R.id.filter_name_item:
                filterTypeTobe = 1;
                mTextFilter.setText(item.getTitle());
                break;
            case R.id.filter_number_item:
                filterTypeTobe = 2;
                mTextFilter.setText(item.getTitle());
                break;
        }

        if (mFilterTypeId != filterTypeTobe) {
            mFilterTypeId = filterTypeTobe;

            // clear filter text
            mEditTextFilter.setText("");
        }

        return super.onContextItemSelected(item);
    }


    private void processChangeOrderDialog(final TmsParcelItem item, final int prevOrder) {

        final String selectedCourierName = mTextCourierName.getText().toString();
        final String selectedDate = mTextCourierDate.getText().toString();

        TmsCourierItem courierItem = mCourierDatabaseHash.get(selectedCourierName);
        if (courierItem.startparcelid == -1) {
            Toast.makeText(ParcelListActivity.this, getString(R.string.need_to_upload_bylastRelease), Toast.LENGTH_LONG).show();
            return;
        }

        final EditText edittext = new EditText(this);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        final int sizeofParcels = mParcelArrayValues.size();
        AlertDialog.Builder changeOrderDialog = new AlertDialog.Builder(this)
                .setTitle(prevOrder + "번 순서변경")
                .setMessage(item.consigneeName + ": " + item.consigneeAddr + "\n\n변경되길 원하는 순서를 입력하세요.\n" + " (1 ~ " + sizeofParcels + ")")
                .setView(edittext)
                .setPositiveButton(R.string.dialog_title_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String edittextvalue = edittext.getText().toString();


                        if (edittextvalue.isEmpty()) {
                            return;
                        }
                        final int newOrder = Integer.valueOf(edittextvalue);

                        if (prevOrder == newOrder) {
                            Toast.makeText(ParcelListActivity.this, "기존과 동일한 순서정보를 입력하셨습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newOrder < 1 || newOrder > sizeofParcels) {
                            Toast.makeText(ParcelListActivity.this, "유효한 범위의 숫자를 입력하세요" + " (1 ~ " + sizeofParcels + ")", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        DatabaseReference.CompletionListener listener = new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                getFirebaseList();
                                Toast.makeText(ParcelListActivity.this, item.consigneeName + ": " + item.consigneeAddr + "\n\n" + newOrder + "번으로 변경완료되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        };
                        mFbConnector.proceedChangeOrder(new ArrayList<>(mHashParcels.get(selectedCourierName)), item, prevOrder, newOrder, selectedCourierName, selectedDate, listener, mCourierDatabaseHash);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        changeOrderDialog.show();
    }

    private void resetCourierText() {
        mTextCourierName.setText(getString(R.string.default_courier_name));
    }

    private void showSelectCourierPicker() {
        final String[] items = mCourierArray.toArray(new String[mCourierArray.size()]);
        mCourierPickerDialog = new AlertDialog.Builder(this);
        mCourierPickerDialog.setTitle(getString(R.string.courier_sel_dialog_title));
        int defaultIdx = (int) mTextCourierName.getTag();
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
                    mTextCourierName.setText(items[index].split("\\(")[0].trim());
                    mTextCourierName.setTag(index);
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

    private void showAssignCourierPicker() {
        ArrayList<String> arr_couriers = Utils.makeCourierUserList();
        final String[] items = arr_couriers.toArray(new String[arr_couriers.size()]);
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
                    String assignCourier = items[index];
                    ArrayList<TmsParcelItem> checkedItems = getCheckedParcelItems();
                    Log.d(LOG_TAG, "checkedItems.size : " + checkedItems.size());

                    assignParcelItemToCourier(checkedItems, assignCourier);
                    if (mArrayAdapter != null) {
                        mArrayAdapter.notifyDataSetChanged();
                    }
                }
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

    public void assignParcelItemToCourier(ArrayList<TmsParcelItem> items, String couriername) {
        String date = mTextCourierDate.getText().toString();

        AssignParcelsUtil assignUtil = new AssignParcelsUtil(date, mHashParcels, mHashCouriers);

        TmsCourierItem assignCourier = mCourierDatabaseHash.get(couriername);
        assignUtil.assignCourier(items, assignCourier);
    }


    private ArrayList<TmsParcelItem> getCheckedParcelItems() {
        ArrayList<TmsParcelItem> checkedItems = new ArrayList<>();
        for (int i = 0; i < mArrayAdapter.getCount(); i++) {
            TmsParcelItem item = (TmsParcelItem) mArrayAdapter.getItem(i);
            if (item.getChecked()) {
                checkedItems.add(item);
            }
        }
        return checkedItems;
    }

    private ArrayList<String> prepareCourierArray() {
        ArrayList<TmsCourierItem> courierArrayList = new ArrayList<>();
        ArrayList<String> strArrayList = new ArrayList<>();
        Log.d(LOG_TAG, "prepareCourierArray, size = " + mCourierDatabaseHash.size());
        courierArrayList.addAll(mCourierDatabaseHash.values());
        for (TmsCourierItem item : courierArrayList) {
            int cnt_delivered = 0;
            LinkedList<TmsParcelItem> list_parcels = mHashParcels.get(item.name);
            for (TmsParcelItem parcelItem : list_parcels) {
                if (parcelItem.status.equals(TmsParcelItem.STATUS_DELIVERED))
                    cnt_delivered++;

            }
            strArrayList.add(item.name + " (" + cnt_delivered + "/" + list_parcels.size() + ")");
        }
        Collections.sort(strArrayList);
        strArrayList.add(0, getString(R.string.default_courier_name));
        return strArrayList;
    }

    private void getFirebaseList() {
        String selectedDate = mTextCourierDate.getText().toString();
        mFbConnector.getCourierListFromFirebaseDatabaseWithListener(selectedDate, mCourierValueEventListener);
    }

    private void refreshList(String courierName) {
        getFirebaseList();
    }

    private void goToUploadImageActivity(final TmsParcelItem item, int requestCode) {
        Intent intent = new Intent(ParcelListActivity.this, UploadImageActivity.class);
        intent.putExtra(Utils.SELECTED_ITEM, item);
        intent.putExtra(Utils.SELECTED_DATE, mTextCourierDate.getText().toString());
        if (requestCode != Utils.NO_NEED_RESULT) {
            intent.setAction(Utils.ACTION_MAKE_DELIVERED);
            startActivityForResult(intent, requestCode);
        } else {
            intent.setAction(Utils.ACTION_SHOWINFO);
            startActivity(intent);
        }
    }


    private String getItemString(ArrayList<TmsParcelItem> items) {
        int numTotal = items.size();
        int numComplted = 0;

        for (TmsParcelItem item : items) {
            if (item != null && item.status.equals(TmsParcelItem.STATUS_DELIVERED))
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

    protected class TmsItemAdapter extends BaseAdapter implements Filterable {
        ArrayList<TmsParcelItem> totalItemList = new ArrayList<TmsParcelItem>();
        ArrayList<TmsParcelItem> filteredItemList = totalItemList;
        Filter listFilter;
        boolean isCheckboxVisible = false;

        public void setCheckboxVisible(boolean checkboxVisible) {
            isCheckboxVisible = checkboxVisible;
        }

        public TmsItemAdapter(ArrayList<TmsParcelItem> list) {
            totalItemList = list;
            filteredItemList = list;
        }

        @Override
        public Filter getFilter() {
            if (listFilter == null) {
                listFilter = new TmsItemFilter();
            }

            return listFilter;
        }

        @Override
        public int getCount() {
            return filteredItemList.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return filteredItemList.get(position);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            mTextCourierName.setClickable(true);
            mTextCourierDate.setClickable(true);
            mBtnChangeView.setEnabled(true);
            mTextCount.setText(getItemString(mParcelArrayValues));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            final TmsParcelItem item = (TmsParcelItem) getItem(position);
            boolean isDeliverd = item.status.equals(TmsParcelItem.STATUS_DELIVERED);
            boolean isValidAddress = !(item.consigneeLatitude.equals("0") || item.consigneeLongitude.equals("0") || item.consigneeLatitude.isEmpty() || item.consigneeLongitude.isEmpty());

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.parcel_listview_row, null);
                holder = new ViewHolder();
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.listItemCheck);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (item != null) {
                TextView addrText = convertView.findViewById(R.id.listAddr);
                TextView providerText = convertView.findViewById(R.id.listItemTextProvider);
                TextView customerText = convertView.findViewById(R.id.listItemTextCustomer);
                TextView deliveryNote = convertView.findViewById(R.id.listItemTextDeliveryMemo);
                TextView remark = convertView.findViewById(R.id.listItemTextRemark);
                Button btn_complete = convertView.findViewById(R.id.btn_complete);
                Button btn_deliveryinfo = convertView.findViewById(R.id.btn_deliveryinfo);
                Button btn_changeorder = convertView.findViewById(R.id.btn_changeorder);
                ImageView statusIcon = convertView.findViewById(R.id.status_icon);

                holder.checkBox.setTag(String.valueOf(position));   // to properly track the actual position
                holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        int pos = Integer.parseInt(buttonView.getTag().toString());
                        TmsParcelItem item = (TmsParcelItem) getItem(pos);
                        item.setChecked(isChecked);
                    }
                });
                holder.checkBox.setChecked(item.getChecked());

                LinearLayout checkBoxRegion = convertView.findViewById(R.id.checkbox_layout);
                if (isCheckboxVisible) {
                    holder.checkBox.setVisibility(View.VISIBLE);
                    checkBoxRegion.setVisibility(View.VISIBLE);
                } else {
                    holder.checkBox.setVisibility(View.GONE);
                    checkBoxRegion.setVisibility(View.GONE);
                }

                if (item.status.equals(TmsParcelItem.STATUS_COLLECTED)) {
                    convertView.setBackgroundColor(0xFFD5D5D5);
                } else {
                    convertView.setBackgroundColor(0xFFFFFFFF);
                }

                btn_complete.setOnClickListener(ParcelListActivity.this);
                btn_complete.setTag(R.id.btn_complete, item);
                //btn_deliveryinfo.setOnClickListener(ParcelListActivity.this);
                //btn_deliveryinfo.setTag(R.id.btn_deliveryinfo, item);
                btn_deliveryinfo.setVisibility(View.GONE);
                btn_changeorder.setVisibility(View.GONE);
                //btn_changeorder.setOnClickListener(ParcelListActivity.this);
                //btn_changeorder.setTag(R.id.btn_changeorder, item);
                //btn_changeorder.setTag(R.id.status_icon, position + 1);
                if (addrText != null) {
                    String addrTextValue = "";
                    if (!mTextCourierName.getText().toString().equals(getString(R.string.all_couriers))) {
                        addrTextValue = item.orderInList + " : ";
                    }
                    addrText.setText(addrTextValue + item.consigneeAddr);
                    if (isDeliverd) {
                        addrText.setTextColor(0xFF68C166);
                        statusIcon.setVisibility(View.VISIBLE);
                        statusIcon.setImageDrawable(getDrawable(R.mipmap.tag_delivered_v2));
                        btn_complete.setVisibility(View.GONE);
                        //btn_changeorder.setVisibility(View.GONE);
                        //btn_deliveryinfo.setVisibility(View.VISIBLE);
                        //btn_deliveryinfo.setBackgroundColor(0xFF68C166);
                    } else {
                        if (isValidAddress) {
                            addrText.setTextColor(0xFF4F4F4F);
                        } else {
                            addrText.setTextColor(0xFFC12F2F);
                        }
                        statusIcon.setVisibility(View.GONE);
                        btn_complete.setVisibility(View.VISIBLE);
                        btn_complete.setBackgroundColor(0xFF42A5F5);
                        //if (mTextCourierName.getText().toString().equals(getString(R.string.all_couriers))) {
                        //    btn_changeorder.setVisibility(View.INVISIBLE);
                        //} else {
                        //    btn_changeorder.setVisibility(View.VISIBLE);
                        //}
                        //btn_deliveryinfo.setVisibility(View.GONE);
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
            return convertView;
        }

        public class ViewHolder {
            public CheckBox checkBox;
        }

        private class TmsItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    results.values = totalItemList;
                    results.count = totalItemList.size();
                } else {
                    ArrayList<TmsParcelItem> itemList = new ArrayList<TmsParcelItem>();

                    for (TmsParcelItem item : totalItemList) {
                        if (item.getDesc(mFilterTypeId).toUpperCase().contains(constraint.toString().toUpperCase())) {
                            itemList.add(item);
                        }
                    }

                    results.values = itemList;
                    results.count = itemList.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                // update listview by filtered data list.
                filteredItemList = (ArrayList<TmsParcelItem>) results.values;

                // notify
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }

    }
}

