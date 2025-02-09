package com.lge.pickitup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UploadImageActivity extends AppCompatActivity {
    static final String EXTRA_SEND_RESULT = "send_result";
    static final String EXTRA_UPLOADED_FILE_PATH = "uploaded_file_path";
    private static final String LOG_TAG = "UploadImageActivity";
    private static final boolean enableLog = true;
    // Which option to upload a file to Firebase Stroage
    private static final String UPLOAD_BY_BYTES = "uploadByBytes";
    private static final String UPLOAD_BY_FILE = "uploadByFile";
    private static final String UPLOAD_BY_STREAM = "uploadByStream";
    // Select option
    private static final String mUploadMethod = UPLOAD_BY_BYTES;
    private static final int CAPTURE_CAMERA_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_REQUEST_CODE = 2;
    private static final int MMS_REQUEST_IMG_SEND = 3;
    private static final int KAKAOTALK_REQUEST_IMG_SEND = 4;
    private static final int FROM_CAMERA = 1;
    private static final int FROM_ALBUM = 2;
    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 997;
    private Button mBtnCapture;
    private Button mBtnPickImgFromGallery;
    private Button mBtnSendMsg;
    private Button mBtnAssignCourier;
    private ImageView mIvPreviewImage;
    private TextView mEtMessageText;
    private EditText mEtMessageContent;
    private TextView mTvDeliveryTime;
    private TextView mTvChooseCourier;
    private TableLayout mTableDetail;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    private ProgressDialog mProgressDialog;
    private int mFlag;
    private String mCurrentPhotoPath;
    private Uri mPhotoUri;
    private TmsParcelItem mSelectedParcelItem;
    private String mSelectedDate;
    private String mAction;
    private String mStorageURL = "gs://smart-router-17060.appspot.com/";
    private final long ONE_MEGABYTE = 1024 * 1024;
    private int KEY_IMVSTATUS = 1;
    private int IMGVIEW_INIT = 0;
    private int IMGVIEW_CAPTURED = 1;
    private String mMessagePrefix;
    private String[] mMessageTypes;
    private int mMessageTypeId = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_and_upload);

        Intent intent = getIntent();
        mAction = intent.getAction();
        Log.i(LOG_TAG, "mAction is " + mAction);
        mSelectedParcelItem = intent.getParcelableExtra(Utils.SELECTED_ITEM);
        mSelectedDate = intent.getStringExtra(Utils.SELECTED_DATE);


        initResources();

        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReferenceFromUrl(mStorageURL);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted

            mBtnCapture.setEnabled(false);
            mBtnPickImgFromGallery.setEnabled(false);
            mIvPreviewImage.setClickable(false);

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            mBtnCapture.setEnabled(true);
            mBtnPickImgFromGallery.setEnabled(true);
            if (isShowInfoAction()) {
                showCompleteImage();
                setShowDeliveryInfoUI();
                if (mSelectedParcelItem.status.equals(TmsParcelItem.STATUS_DELIVERED)) {
                    mIvPreviewImage.setImageDrawable(getResources().getDrawable(R.drawable.img_downloading, UploadImageActivity.this.getTheme()));
                    mIvPreviewImage.setClickable(false);
                } else {
                    mIvPreviewImage.setVisibility(View.GONE);
                }
                mEtMessageText.setVisibility(View.GONE);
            } else {
                mIvPreviewImage.setImageDrawable(getResources().getDrawable(R.drawable.capture_image_icon, UploadImageActivity.this.getTheme()));
                mIvPreviewImage.setTag(R.id.ivImagePreview, IMGVIEW_INIT);
            }
        }
    }


    private void setParcelInfoText() {

        ArrayList<Pair> detail = new ArrayList<Pair>();
        LayoutInflater inflator = getLayoutInflater();

        detail.add(new Pair("고객명", mSelectedParcelItem.consignorName));
        detail.add(new Pair("고객주소", mSelectedParcelItem.consigneeAddr));
        detail.add(new Pair("배송메모", mSelectedParcelItem.deliveryNote));
        detail.add(new Pair("특이사항", mSelectedParcelItem.remark));
        detail.add(new Pair("업체명", mSelectedParcelItem.consignorName));
        detail.add(new Pair("배송기사", mSelectedParcelItem.courierName));
        detail.add(new Pair("지역코드", mSelectedParcelItem.regionalCode));

        mTableDetail.removeAllViews();
        for (int i = 0; i < detail.size(); i++) {
            View layout_row = inflator.inflate(R.layout.detailview_row, mTableDetail, false);
            TextView col1 = layout_row.findViewById(R.id.txt_title);
            final TextView col2 = layout_row.findViewById(R.id.txt_contents);
            col1.setText(detail.get(i).first.toString());
            col2.setText(detail.get(i).second.toString());

            if (col1.getText().toString().equals("고객주소")
                    && (mSelectedParcelItem.consigneeLatitude.equals("0") || mSelectedParcelItem.consigneeLongitude.equals("0"))) {
                col2.setTextColor(0xFFC12F2F);
                col2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        processChangeAddrDialog(col2.getText().toString());
                    }
                });
            }

            mTableDetail.addView(layout_row);
        }
        return;
    }

    private void processChangeAddrDialog(final String wrongAddr) {

        final EditText edittext = new EditText(this);

        edittext.setPadding(50,edittext.getPaddingTop(),50,edittext.getPaddingBottom());

        edittext.setText(wrongAddr);
        AlertDialog.Builder changeOrderDialog = new AlertDialog.Builder(this)
                .setTitle("주소 변경")
                .setMessage("잘못된 주소입니다.\n올바른 주소를 입력해 주세요.")
                .setView(edittext)
                .setPositiveButton(R.string.dialog_title_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String edittextvalue = edittext.getText().toString();
                        AddressTranslate addressTranslate = new AddressTranslate();
                        addressTranslate.execute(edittextvalue);

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

    class AddressTranslate extends AsyncTask<String, Void, String> {
        final String result_valid = "valid";
        final String result_invalid = "invalid";
        @Override
        protected String doInBackground(String... strings) {
            String newAddr = strings[0];
            String prevAddr = mSelectedParcelItem.consigneeAddr;
            mSelectedParcelItem.consigneeAddr = newAddr;
            Utils.makeAddressWithKakao(mSelectedParcelItem);
            if (mSelectedParcelItem.consigneeLongitude.equals("0") || mSelectedParcelItem.consigneeLatitude.equals("0")) {
                mSelectedParcelItem.consigneeAddr = prevAddr;
                return result_invalid;
            } else {
                return result_valid;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s.equals(result_valid)) {
                Utils.postParcelItemToFirebaseDatabase(mSelectedDate, mSelectedParcelItem);
                setShowDeliveryInfoUI();
            } else {
                Toast.makeText(UploadImageActivity.this, "잘못된 주소입니다.\n 주소를 다시 입력해 주세요", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setShowDeliveryInfoUI() {
        mEtMessageContent.setVisibility(View.GONE);
        mBtnCapture.setVisibility(View.GONE);
        mBtnPickImgFromGallery.setVisibility(View.GONE);
        mBtnSendMsg.setVisibility(View.GONE);

        mTvDeliveryTime.setVisibility(View.VISIBLE);
        setParcelInfoText();
    }

    private void showCompleteImage() {
        if (!mSelectedParcelItem.completeImage.isEmpty()) {
            StorageReference ref = mStorageRef.child(mSelectedParcelItem.completeImage);
            Log.i(LOG_TAG, "showCompleteImage refpath is " + ref);
            ref.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Log.d(LOG_TAG, "onSeuccess to download data : length = " + bytes.length);
                    Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    mIvPreviewImage.setImageBitmap(bm);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private boolean isMakeDeliveredAction() {
        return (mAction == Utils.ACTION_MAKE_DELIVERED);
    }

    private boolean isShowInfoAction() {
        return (mAction == Utils.ACTION_SHOWINFO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (enableLog) {
                        Log.d(LOG_TAG, "onRequestPermissionResul, Permission are granted");
                    }
                    mBtnCapture.setEnabled(true);
                    mBtnPickImgFromGallery.setEnabled(true);
                    mIvPreviewImage.setClickable(true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mBtnCapture.setEnabled(false);
                    mBtnPickImgFromGallery.setEnabled(false);
                    mIvPreviewImage.setClickable(false);
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if (resultCode != RESULT_OK) {
//            if (enableLog) {
//                Log.d(LOG_TAG, "onActivityResult, result code is not RESULT_OK");
//            }
//            return;
//        }

        switch (requestCode) {
            case CAPTURE_CAMERA_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    // Return after take picture
                    if (enableLog) {
                        Log.d(LOG_TAG, "onActivityResult, requestCode is CAPTURE_CAMERA_REQUEST_CODE");
                    }
                    galleryAddPic();
                    setResizedTakenPic();
                }
                break;

            case PICK_IMAGE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    // Picking an image from gallery album
                    if (data.getData() != null) {
                        mPhotoUri = data.getData();
                        setResizedGalleryPic();
                    }
                }
                break;

            case MMS_REQUEST_IMG_SEND:
                if (enableLog) {
                    Log.d(LOG_TAG, "onActivityResult, MMS_REQUEST_IMG_SEND");
                }
                uploadImageToServer();
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.message_selection_menu, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {
        mEtMessageText.setText(item.getTitle());
        int messageTypeTobe = 0;
        switch (item.getItemId()) {
            case R.id.message_normal_door_front:
                messageTypeTobe = 0;
                break;
            case R.id.message_return_address_unknown:
                messageTypeTobe = 1;
                break;
            case R.id.message_return_cannot_enter:
                messageTypeTobe = 2;
                break;
            case R.id.message_return_broken_product:
                messageTypeTobe = 3;
                break;
            case R.id.message_special_security_office:
            case R.id.message_special_delivery_box:
            case R.id.message_special_public_door_front:
                messageTypeTobe = 4;
                break;
            case R.id.message_special_etc:
                messageTypeTobe = 5;
                break;
            case R.id.message_special_delayed:
                messageTypeTobe = 6;
                break;
        }

        if (mMessageTypeId != messageTypeTobe) {
            mMessageTypeId = messageTypeTobe;

            // reload message contents
            mEtMessageContent.setText(mMessagePrefix + mMessageTypes[mMessageTypeId]);
        }

        return super.onContextItemSelected(item);
    }


    private void initResources() {
        mBtnCapture = findViewById(R.id.btnCameraCapture);
        mBtnPickImgFromGallery = findViewById(R.id.btnPickImageFromGallery);
        mBtnSendMsg = findViewById(R.id.btnSendMessage);
        mIvPreviewImage = findViewById(R.id.ivImagePreview);
        mEtMessageContent = findViewById(R.id.etMessageContent);
        mTvDeliveryTime = findViewById(R.id.tvDeliveryTime);
        mTvChooseCourier = findViewById(R.id.tv_choosecourier);
        mBtnAssignCourier = findViewById(R.id.btn_assign_courier);
        mTableDetail = findViewById(R.id.table_detail);

        mEtMessageText = findViewById(R.id.etMessageText);
        registerForContextMenu(mEtMessageText);

        mMessagePrefix = "안녕하세요. " + mSelectedParcelItem.consignorName + " 입니다. ";
        mMessageTypes = getResources().getStringArray(R.array.sms_messages);
        mEtMessageContent.setText(mMessagePrefix + mMessageTypes[mMessageTypeId]);
        mIvPreviewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeDialog();
            }
        });

        mBtnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFlag = FROM_CAMERA;
                dispatchTakePicureIntent();
            }
        });

        mBtnPickImgFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFlag = FROM_ALBUM;
                selectImgFromAlabum();
            }
        });

        mBtnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder diag_bulder = new AlertDialog.Builder(UploadImageActivity.this)
                        .setTitle(R.string.send_complete_message_title)
                        .setMessage(R.string.send_complete_message_body)
                        .setPositiveButton(R.string.send_mms_message, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendMmsMessage();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                if (mIvPreviewImage.getTag(R.id.ivImagePreview).equals(IMGVIEW_CAPTURED)) {
                    diag_bulder.show();
                } else {
                    Toast.makeText(UploadImageActivity.this, "사진을 찍거나 선택 후에\n메세지 전송과 배송완료 처리가 가능합니다.", Toast.LENGTH_LONG).show();
                }
            }
        });

        mTvChooseCourier.setTag(0); // set default index of courierDialog to 0
        mTvChooseCourier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> arr_couriers = Utils.makeCourierUserList();
                final String[] items = arr_couriers.toArray(new String[arr_couriers.size()]);
                AlertDialog.Builder courierDialog = new AlertDialog.Builder(UploadImageActivity.this);
                courierDialog.setTitle(getString(R.string.courier_sel_dialog_title));

                int defaultIdx = (int) mTvChooseCourier.getTag();
                final List selectedItems = new ArrayList<>();
                selectedItems.add(defaultIdx);

                courierDialog.setSingleChoiceItems(items, defaultIdx, new DialogInterface.OnClickListener() {
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
                            mTvChooseCourier.setText(items[index]);
                            mTvChooseCourier.setTag(index); // set selected index to mTvChooseCourier's tag
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int pos) {
                        Log.d(LOG_TAG, "Cancel button is pressed");
                        dialogInterface.cancel();
                    }
                });
                courierDialog.show();
            }
        });

        mBtnAssignCourier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String selectedCourier = mTvChooseCourier.getText().toString();
                AlertDialog.Builder askAssignDialog = new AlertDialog.Builder(UploadImageActivity.this);
                askAssignDialog.setTitle("배송기사 배정");
                askAssignDialog.setMessage("해당 배송건을 " + selectedCourier + " 기사에게 배정하시겠습니까?");
                askAssignDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                askAssignDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                askAssignDialog.show();

            }
        });
        mBtnAssignCourier.setVisibility(View.GONE);
        mTvChooseCourier.setVisibility(View.GONE);
    }

    private void makeDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UploadImageActivity.this);
        alertDialogBuilder.setTitle("사진선택")
                .setMessage("사진을 선택할 방법을 선택해주세요.")
                .setPositiveButton("카메라 촬영", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (enableLog) {
                            Log.d(LOG_TAG, "\"Camera capture\" is selected");
                        }
                        mFlag = FROM_CAMERA;
                        dispatchTakePicureIntent();
                    }
                }).setNeutralButton("앨범에서 선택", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (enableLog) {
                    Log.d(LOG_TAG, "\"Pick image from gallery\" is selected");
                }
                mFlag = FROM_ALBUM;
                selectImgFromAlabum();
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).show();
    }

    private void selectImgFromAlabum() {
        // Pick an image from gallery album
        // Open gallery album
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }

    public void dispatchTakePicureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.lge.pickitup.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAPTURE_CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (enableLog) {
            Log.d(LOG_TAG, "storageDir = " + storageDir);
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        if (enableLog) {
            Log.e(LOG_TAG, "currentPhotoPath = " + mCurrentPhotoPath);
        }
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setResizedTakenPic() {
        // Get the dimensions of the View
        int targetW = mIvPreviewImage.getWidth();
        int targetH = mIvPreviewImage.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        try {
            bitmap = rotate(bitmap, mCurrentPhotoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIvPreviewImage.setImageBitmap(bitmap);
        mIvPreviewImage.setTag(R.id.ivImagePreview, IMGVIEW_CAPTURED);
    }


    private void setResizedGalleryPic() {
        // Get the dimensions of the View
        int targetW = mIvPreviewImage.getWidth();
        int targetH = mIvPreviewImage.getHeight();

        if (enableLog) {
            Log.d(LOG_TAG, "targetW = " + targetW + ", targetH = " + targetH);
        }

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        InputStream is = null;
        Bitmap bitmap = null;

        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int photoW = bitmap.getWidth();
        int photoH = bitmap.getHeight();

        if (enableLog) {
            Log.d(LOG_TAG, "photoW = " + photoW + ", photoH = " + photoH);
        }

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        try {
            is = getContentResolver().openInputStream(mPhotoUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        bitmap = BitmapFactory.decodeStream(is, null, bmOptions);
        try {
            bitmap = rotate(bitmap, getRealPathFromURI(mPhotoUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIvPreviewImage.setImageBitmap(bitmap);
        mIvPreviewImage.setTag(R.id.ivImagePreview, IMGVIEW_CAPTURED);
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public Bitmap rotate(Bitmap bitmap, String path) throws IOException {
        ExifInterface exif = new ExifInterface(path);
        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int degrees = exifOrientationToDegrees(exifOrientation);

        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);
            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            } catch (OutOfMemoryError ex) {
                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
            }
        }
        return bitmap;
    }


    private void uploadImageToServer() {
        final String currentUid = mAuth.getUid();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = currentUid + "_" + timeStamp;

        StorageReference ref = mStorageRef.child(mSelectedDate + "/").child("Images/" + filename + ".jpg");

        final String firebaseStoragePath = mSelectedDate + "/Images/" + filename + ".jpg";

        UploadTask uploadTask = null;
        Uri uri = null;

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(getString(R.string.uploading_image));
        mProgressDialog.show();


        if (mUploadMethod.equals(UPLOAD_BY_FILE)) {
            if (mFlag == FROM_CAMERA) {
                uri = Uri.fromFile(new File(mCurrentPhotoPath));
            } else if (mFlag == FROM_ALBUM) {
                uri = mPhotoUri;
            } else {
                Log.e(LOG_TAG, "mFlag has some wrong value");
            }

            uploadTask = ref.putFile(uri);
        } else if (mUploadMethod.equals(UPLOAD_BY_BYTES)) {
            mIvPreviewImage.setDrawingCacheEnabled(true);
            mIvPreviewImage.buildDrawingCache();
            Bitmap bm = ((BitmapDrawable) mIvPreviewImage.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] outputData = baos.toByteArray();
            uploadTask = ref.putBytes(outputData);
        } else if (mUploadMethod.equals(UPLOAD_BY_STREAM)) {
            InputStream is = null;
            if (mFlag == FROM_CAMERA) {
                try {
                    is = new FileInputStream(new File(mCurrentPhotoPath));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (mFlag == FROM_ALBUM) {
                try {
                    is = getContentResolver().openInputStream(mPhotoUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            uploadTask = ref.putStream(is);
        } else {
            Log.e(LOG_TAG, "Unknown uploading methodology");
        }

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(UploadImageActivity.this, getString(R.string.success_to_upload), Toast.LENGTH_LONG).show();
                ;
                mProgressDialog.dismiss();

                // Make result intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SEND_RESULT, "success");
                resultIntent.putExtra(EXTRA_UPLOADED_FILE_PATH, firebaseStoragePath);
                setResult(RESULT_OK, resultIntent);
                UploadImageActivity.this.finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadImageActivity.this, getString(R.string.fail_to_upload), Toast.LENGTH_LONG).show();
                ;
                mProgressDialog.dismiss();

                // Make result intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SEND_RESULT, "fail");
                resultIntent.putExtra(EXTRA_UPLOADED_FILE_PATH, "");
                setResult(RESULT_OK, resultIntent);
                UploadImageActivity.this.finish();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Log.d(LOG_TAG, "Uploading progress : " + progress + "%");
                mProgressDialog.setMax((int) (taskSnapshot.getTotalByteCount() / 1024));
                mProgressDialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 1024));
            }
        });
    }

    private void sendMmsMessage() {
        try {
            Uri imageUri = null;

            if (mFlag == FROM_CAMERA) {
                if (Build.VERSION.SDK_INT < 24) {
                    imageUri = Uri.fromFile(new File(mCurrentPhotoPath));
                } else {
                    imageUri = FileProvider.getUriForFile(UploadImageActivity.this,
                            "com.lge.pickitup.fileprovider", new File(mCurrentPhotoPath));
                }
            } else if (mFlag == FROM_ALBUM) {
                imageUri = mPhotoUri;
            } else {
                Log.e(LOG_TAG, "mFlag has some wrong value");
            }

            // Make intent to send a MMS message including message
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra("address", mSelectedParcelItem.consigneeContact);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.message_title));
            sendIntent.putExtra(Intent.EXTRA_TEXT, mEtMessageContent.getText().toString());
            sendIntent.setType("image/*");
            sendIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(/*Intent.createChooser(sendIntent, "Send")*/ sendIntent, MMS_REQUEST_IMG_SEND);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(UploadImageActivity.this, R.string.alert_no_sms_activity, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendKakaoTaskMessage() {
        Uri imageUri = null;

        if (mFlag == FROM_CAMERA) {
            if (Build.VERSION.SDK_INT < 24) {
                imageUri = Uri.fromFile(new File(mCurrentPhotoPath));
            } else {
                imageUri = FileProvider.getUriForFile(UploadImageActivity.this,
                        "com.lge.pickitup.fileprovider", new File(mCurrentPhotoPath));
            }
        } else if (mFlag == FROM_ALBUM) {
            imageUri = mPhotoUri;
        } else {
            Log.e(LOG_TAG, "mFlag has some wrong value");
        }

        try {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("image/*");
            sendIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            sendIntent.setPackage("com.kakako.talk");
            startActivityForResult(sendIntent, KAKAOTALK_REQUEST_IMG_SEND);
        } catch (ActivityNotFoundException e) {
            Uri uriMarket = Uri.parse("market://deatils?id=com.kakao.talk");
            Intent intent = new Intent(Intent.ACTION_VIEW, uriMarket);
            startActivity(intent);
        }
    }
}
