package com.lge.pickitup;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class AddressFacade {
    private static final String LOG_TAG = "AddressFacade";
    String mFileName;
    String mDateStr;
    Context mContext;
    List<TmsParcelItem> mParcelList = new ArrayList<>();
    private HashMap<Integer, TmsParcelItem> mPreviousParcelItemInSector = new HashMap<>();
    private HashMap<String, TmsCourierItem> mCourierHash = new HashMap<>();
    private HashMap<String, TmsParcelItem> mLastParcelItemInSectorOnDB = new HashMap<>();
    private FirebaseDatabaseConnector mFbConnector;

    public AddressFacade(Context mContext) {
        this.mContext = mContext;
    }

    protected void init(String fileName) {
        mFileName = fileName;
        mFbConnector = new FirebaseDatabaseConnector(mContext);
        mDateStr = getDateFromFileName(mFileName);
        mFbConnector.getCourierListFromFirebaseDatabaseWithListener(mDateStr, mCourierValueEventListener);
    }

    ValueEventListener mCourierValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            mCourierHash.clear();
            int maxCourierIdInDB = 0;
            Log.d(LOG_TAG, "getCourierListFromFirebaseDatabase : size " + dataSnapshot.getChildrenCount());
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                String key = postSnapshot.getKey();
                TmsCourierItem value = postSnapshot.getValue(TmsCourierItem.class);
                mCourierHash.put(value.name, value);
                int id = Integer.valueOf(key);
                if (id > maxCourierIdInDB)
                    maxCourierIdInDB = id;
            }
            initFile(mFileName, maxCourierIdInDB);
        }
        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    String getDateFromFileName(String fileName) {
        String[] dateStr = fileName.split("[_]|[.]");
        String result = dateStr[1].substring(0, 4) + "-" + dateStr[1].substring(4, 6) + "-" + dateStr[1].substring(6);
        return result;
    }
    ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            long initValueOfParcelId = 0;
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                TmsParcelItem value = postSnapshot.getValue(TmsParcelItem.class);
                if (value.nextParcel == -1) {
                    mLastParcelItemInSectorOnDB.put(value.courierName, value);
                }
                long id = Long.valueOf(value.id);
                if (id > initValueOfParcelId)
                    initValueOfParcelId = id;
            }

            Log.i(LOG_TAG, "initValueOfParcelId is " + initValueOfParcelId);
            // Get longitude and latitude from address through Daum Kakao API
            AddressTranslate addressTranslate = new AddressTranslate();
            addressTranslate.execute(String.valueOf(initValueOfParcelId));

            String[] date_piece = mDateStr.split("-");
            String setUrl = Utils.SERVER_URL + "/route";
            String getUrl = Utils.SERVER_URL + "/job";
            for (String piece : date_piece) {
                setUrl += "/" + piece;
                getUrl += "/" + piece;
            }
            Log.d(LOG_TAG, "processing url = " + setUrl + ", " + getUrl);
            ProcessingBackTask processingBackTask = new ProcessingBackTask();
            processingBackTask.execute(setUrl, getUrl);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    void initFile(String filename, int startIdx) {
        mParcelList.clear();

        File file = new File("/sdcard/address/" + filename);
        try {
            FileInputStream fis = new FileInputStream(file);

            InputStreamReader is = new InputStreamReader(fis, "EUC-KR");
            CSVReader reader = new CSVReader(is);
            String[] record = null;
            boolean firstLine = true;
            while ((record = reader.readNext()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Add it if the courier is new one
                if (record.length > 10) {
                    String courierName = record[10];
                    if (!mCourierHash.containsKey(courierName)) {
                        startIdx++;
                        TmsCourierItem item = new TmsCourierItem(String.valueOf(startIdx), courierName);
                        mCourierHash.put(record[10], item);
                    }
                }
                addRecordToParcelList(mParcelList, mDateStr, record);
            }

            // make valueeventlistner
            mFbConnector.getParcelListFromFirebaseDatabase(mDateStr, mValueEventListener);

            boolean deleted = file.delete();
            if (deleted)
                Log.i(LOG_TAG, file.getName() + " file is deleted");
            else
                Log.i(LOG_TAG, file.getName() + " file is not deleted");

        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "FileNotFoundException has been raised");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addRecordToParcelList(List<TmsParcelItem> list, String dateRecord, String[] record) {
        TmsParcelItem item = new TmsParcelItem(dateRecord, record);
        item.sectorId = Integer.valueOf(mCourierHash.get(item.courierName).id);

        list.add(item);
    }

    private void initParcelId(int startIdx) {
        for (int i = 0; i < mParcelList.size(); i++) {
            TmsParcelItem item = mParcelList.get(i);
            item.id = startIdx + i + 1;

            TmsParcelItem lastItemInHash = mPreviousParcelItemInSector.get(item.sectorId);
            if (lastItemInHash != null) {
                lastItemInHash.nextParcel = item.id;
            } else {
                if (mCourierHash.get(item.courierName).startparcelid == -1) {
                    mCourierHash.get(item.courierName).startparcelid = item.id;
                } else {
                    TmsParcelItem tailItem = mLastParcelItemInSectorOnDB.get(item.courierName);
                    tailItem.nextParcel = item.id;

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(mFbConnector.PARCEL_REF_NAME);
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/" + mDateStr + "/" + tailItem.id, tailItem.toMap());
                    ref.updateChildren(childUpdates);
                }

            }
            mPreviousParcelItemInSector.put(item.sectorId, item);
        }
    }

    private List<TmsCourierItem> buildTmsCouriers() {
        List<TmsCourierItem> result = new ArrayList<>();

        for (String key : mCourierHash.keySet()) {
            TmsCourierItem item = mCourierHash.get(key);
            result.add(item);
        }
        return result;
    }

    private void goToParcelList() {

        mContext.startActivity(new Intent(mContext, ParcelListActivity.class)
                .putExtra(Utils.KEY_DB_DATE, mDateStr)
                .putExtra(Utils.KEY_COURIER_NAME, mContext.getString(R.string.all_couriers)));
    }

    class AddressTranslate extends AsyncTask<String, Void, String> {
        ProgressDialog asyncDialog = new ProgressDialog(mContext);
        int startIdx;
        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            asyncDialog.setMessage(mContext.getString(R.string.translate_address));
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... s) {
            startIdx = Integer.valueOf(s[0]);
            asyncDialog.setMax(mParcelList.size());
            asyncDialog.setProgress(0);

            for (int i = 0; i < mParcelList.size(); i++) {
                makeAddressWithKakao(mParcelList.get(i));
                asyncDialog.setProgress(i + 1);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            super.onPostExecute(o);
            asyncDialog.dismiss();
            initParcelId(startIdx);
            List<TmsCourierItem> couriers = buildTmsCouriers();

            for (TmsCourierItem item : couriers) {
                Log.d(LOG_TAG, "id-" + item.id + ", name-" + item.name);
            }

            mFbConnector.postParcelListToFirebaseDatabase(mDateStr, (ArrayList<TmsParcelItem>) mParcelList);
            mFbConnector.postCourierListToFirbaseDatabase(mDateStr, (ArrayList<TmsCourierItem>) couriers);
            mFbConnector.postJobStatusFromFirebaseDatabase(mDateStr);
            goToParcelList();
        }

        private void makeAddressWithKakao(TmsParcelItem item) {
            String address = item.consigneeAddr;
            String[] code = address.split("[(,]|[0-9]+호|[0-9]+동");

            try {
                URL url = new URL("https://dapi.kakao.com/v2/local/search/address.json?query="
                        + URLEncoder.encode(code[0], "UTF-8"));
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(10000);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestProperty("Authorization", "KakaoAK a9a4f76e68df45d99954e267b0337b44");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setUseCaches(false);
                httpURLConnection.connect();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                JSONObject json_addr = new JSONObject(sb.toString());
                JSONArray addr_arr = json_addr.getJSONArray("documents");
                String latitude_y = "0";
                String longitude_x = "0";
                for (int i = 0; i < addr_arr.length(); i++) {
                    latitude_y = addr_arr.getJSONObject(i).getString("y");  // Latitude
                    longitude_x = addr_arr.getJSONObject(i).getString("x"); // Longitude

                    break; // Extract first address facade
                }

                Log.d(LOG_TAG, "Address to covert : " + item.consigneeAddr);
                Log.d(LOG_TAG, "latitude = " + latitude_y + ", longitude = " + longitude_x);

                // Record coverted loc, lat to TmsParcelItem
                item.consigneeLatitude = latitude_y;
                item.consigneeLongitude = longitude_x;
                item.setStatus(TmsParcelItem.STATUS_GEOCODED);

                bufferedReader.close();
                httpURLConnection.disconnect();

            } catch (Exception e) {
                Log.e(LOG_TAG, "makeAddressWithKakao error =" + e.toString());
            }
        }
    }

    class ProcessingBackTask extends AsyncTask<String, Void, String> {
//        ProgressDialog processingDialog = new ProgressDialog(mContext);

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
//            processingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            processingDialog.setMessage(mContext.getString(R.string.route_parcels));
//            processingDialog.show();
            super.onPreExecute();
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected String doInBackground(String... urls) {
            // TODO - represent progress bar to be more user friendly UX?
//            processingDialog.setMax(mCourierNum);
//            processingDialog.setProgress(0);

            String result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                try {
                    URL setUrl = new URL(urls[0]);
                    String jobId = processUrl(setUrl, "jobid");
                    Log.i(TAG, "Returned jobid : " + jobId);
                    if (jobId != null) {
                        result = jobId;
                    } else {
                        throw new IOException("No response received.");
                    }

//                    URL queryUrl = new URL(urls[1] + "/" + jobId);
//                    while (true) {
//                        Thread.sleep(5000);
//                        String status = processUrl(queryUrl, "status");
//                        if (status.equals("finished")) {
//                            break;
//                        }
//                    }
//                    processingDialog.setProgress(mCourierNum);
                } catch (Exception e) {
                    result = e.getMessage();
                }
            }
            String msg = "doInBackground([" + urls[0] + ", " + urls[1] + "]) -> ";
            if (result != null) msg += result;
            Log.i(TAG, msg);
            return result;
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
//            processingDialog.dismiss();

            String msg = "onPostExecute(";
            if (result != null) msg += result;
            msg += ")";
            Log.i(TAG, msg);
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(String result) {
            String msg = "onCancelled(";
            if (result != null) msg += result;
            msg += ")";
            Log.i(TAG, msg);
        }

        private String getStringFromInputStream(InputStream is) {
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return sb.toString();
        }

        private String parseJSON(JSONObject json, String field) throws JSONException {
            return json.getString(field);
        }

        private String processUrl(URL url, String jsonField) throws Exception {
            Log.i(TAG, "processUrl(" + url.toString() + ")");
            InputStream stream = null;
            HttpsURLConnection connection = null;
            JSONObject json = null;
            String result = null;
            try {
                // call API by using HTTPURLConnection
                connection = (HttpsURLConnection) url.openConnection();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);
                // Open communications link (network traffic occurs here).
                connection.connect();
//                int responseCode = connection.getResponseCode();
//                if (responseCode != HttpsURLConnection.HTTP_OK) {
//                    throw new IOException("HTTP error code: " + responseCode);
//                }

                // Retrieve the response body as an InputStream.
                stream = new BufferedInputStream(connection.getInputStream());

                // parse JSON
                json = new JSONObject(getStringFromInputStream(stream));
                result = parseJSON(json, jsonField);
                Log.i(TAG, result);
            } catch (MalformedURLException e) {
                System.err.println("Malformed URL");
                e.printStackTrace();
//            } catch (JSONException e) {
//                System.err.println("JSON parsing error");
//                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("URL Connection failed");
                e.printStackTrace();
            } finally {
                // Close Stream and disconnect HTTPS connection.
                if (stream != null) {
                    stream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }
    }
}
