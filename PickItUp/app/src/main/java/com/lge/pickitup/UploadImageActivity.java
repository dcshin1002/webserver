package com.lge.pickitup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadImageActivity extends AppCompatActivity {
    private static final String LOG_TAG = "UploadImageActivity";

    private Button mBtnCapture;
    private Button mBtnPickImgFromGallery;
    private Button mBtnUploadToServer;
    private Button mBtnSendMsg;
    private ImageView mIvPreviewImage;

    private static final int CAPTURE_CAMERA_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_REQUEST_CODE = 2;
    private static final int FROM_CAMERA = 1;
    private static final int FROM_ALBUM = 2;

    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;
    private ProgressDialog mProgressDialog;


    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 997;
    private int mFlag;
    private String mCurrentPhotoPath;
    private Uri mPhotoUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_and_upload);

        initResources();

        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();

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
            mIvPreviewImage.setClickable(true);
        }
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
                    Log.d(LOG_TAG, "onRequestPermissionResul, Permission are granted");
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

        if (resultCode != RESULT_OK) {
            Log.d(LOG_TAG, "onActivityResult, result code is not RESULT_OK");
            return;
        }

        switch (requestCode) {
            case CAPTURE_CAMERA_REQUEST_CODE :
                // Return after take picture
                Log.d(LOG_TAG, "onActivityResult, requestCode is CAPTURE_CAMERA_REQUEST_CODE");
                galleryAddPic();
                setPic();
                break;

            case PICK_IMAGE_REQUEST_CODE :
                // Picking an image from gallery album
                if (data.getData() != null) {
                    try {
                        mPhotoUri = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mPhotoUri);
                        mIvPreviewImage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    private void initResources() {
        mBtnCapture = findViewById(R.id.btnCameraCapture);
        mBtnPickImgFromGallery = findViewById(R.id.btnPickImageFromGallery);
        mBtnUploadToServer = findViewById(R.id.btnUploadImageToServer);
        mBtnSendMsg = findViewById(R.id.btnSendMessage);
        mIvPreviewImage = findViewById(R.id.ivImagePreview);

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

        mBtnUploadToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImageToServer();
            }
        });

    }
    
    private void makeDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UploadImageActivity.this);
        alertDialogBuilder.setTitle("사진선택")
                .setMessage("사진을 선택할 방법을 선택해주세요.")
                .setPositiveButton("카메라 촬영", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(LOG_TAG, "\"Camera capture\" is selected");
                        mFlag = FROM_CAMERA;
                        dispatchTakePicureIntent();
                    }
                }).setNeutralButton("앨범에서 선택", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(LOG_TAG, "\"Pick image from gallery\" is selected");
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

    public void dispatchTakePicureIntent(){
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
        Log.d(LOG_TAG, "storageDir = " + storageDir);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e(LOG_TAG, "currentPhotoPath = " + mCurrentPhotoPath);
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mIvPreviewImage.getWidth();
        int targetH = mIvPreviewImage.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mIvPreviewImage.setImageBitmap(bitmap);
    }

    private void uploadImageToServer() {
        final String currentUid = mAuth.getUid();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = currentUid + "_" + timeStamp;

        StorageReference storageRef = mStorage.getReferenceFromUrl("gs://smart-router-17060.appspot.com/")
                .child(Utils.getTodayDateStr() + "/")
                .child("Images/" + filename);

        UploadTask uploadTask;
        Uri uri = null;

        if (mFlag == FROM_CAMERA) {
            uri = Uri.fromFile(new File(mCurrentPhotoPath));
        } else if (mFlag == FROM_ALBUM) {
            uri = mPhotoUri;
        } else {
            Log.e(LOG_TAG, "mFlag has some wrong value");
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(getString(R.string.uploading_image));
        mProgressDialog.show();


        uploadTask = storageRef.putFile(uri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(UploadImageActivity.this, "Complete uploading", Toast.LENGTH_LONG).show();;
                mProgressDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Log.d(LOG_TAG, "Uploading progress : " +progress +"%");
                mProgressDialog.setMax((int) (taskSnapshot.getTotalByteCount() / 1024));
                mProgressDialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 1024));
            }
        });
    }
}
