package com.lge.pickitup;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONObject;

public class AddressFacade {
    private static final String LOG_TAG = "AddressFacade";

    private FirebaseDatabaseConnector mFbConnector;

    String mFileName;
    String mDateStr;
    Context mContext;

    List<TmsParcelItem> mParcelList = new ArrayList<>();
    HashMap<String, String> mCourierHash = new HashMap<>();

    public AddressFacade(Context mContext) {
        this.mContext = mContext;
    }

    protected void init (String fileName) {
        mFileName = fileName;
        mFbConnector = new FirebaseDatabaseConnector(mContext);
        mDateStr = getDateFromFileName(mFileName);
        initFile(fileName);
    }

    String getDateFromFileName(String fileName) {
        String[] dateStr = fileName.split("[_]|[.]");
        String result = dateStr[1].substring(0, 4) + "-" + dateStr[1].substring(4, 6) + "-" + dateStr[1].substring(6);
        return result;
    }

    void initFile(String filename) {
        mParcelList.clear();
        mCourierHash.clear();

        File file = new File("/sdcard/address/" + filename);
        try {
            FileInputStream fis = new FileInputStream(file);

            InputStreamReader is = new InputStreamReader(fis, "EUC-KR");
            CSVReader reader = new CSVReader(is);
            String[] record = null;
            while ((record = reader.readNext()) != null) {
                addRecordToParcelList(mParcelList, record);

                // Add it if the courier is new one
                if (!mCourierHash.containsKey(record[11])) {
                    mCourierHash.put(record[11], "");
                }
            }

            // Get longitude and latitude from address through Daum Kakao API
            AddressTranslate addressTranslate = new AddressTranslate();
            addressTranslate.execute();

        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "FileNotFoundException has been raised");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addRecordToParcelList(List<TmsParcelItem> list, String[] record) {
        String trackingNum = record[0];
        String packageType = record[1];
        String date = record[2];
        String consignorName = record[3];
        String consignorContact = record[4];
        String consigneeName = record[5];
        String consigneeAddr = record[6];
        String consigneeContact = record[7];
        String remark = record[8];
        String deliveryNote = record[9];
        String regionalCode = record[10];
        String courierName = record[11];
        String courierContact = record[12];

        TmsParcelItem item = new TmsParcelItem(TmsParcelItem.UNSET, trackingNum, packageType, date,
                consignorName, consignorContact, consigneeName, consigneeAddr, consigneeContact,
                remark, deliveryNote);
        item.regionalCode = regionalCode;
        item.courierName = courierName;
        item.courierContact = courierContact;

        list.add(item);
    }

    class AddressTranslate extends AsyncTask<String, Void, String> {
        ProgressDialog asyncDialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            asyncDialog.setMessage(mContext.getString(R.string.translate_address));
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... s) {
            asyncDialog.setMax(mParcelList.size());
            asyncDialog.setProgress(0);

            for(int i = 0; i < mParcelList.size(); i++) {
                makeAddressWithKakao(mParcelList.get(i));
                asyncDialog.setProgress(i+1);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            super.onPostExecute(o);
            asyncDialog.dismiss();
            initParcelId();
            List<TmsCourierItem> couriers = buildTmsCouriers(new ArrayList<String>(mCourierHash.keySet()));

            for (TmsCourierItem item : couriers) {
                Log.d(LOG_TAG, "id-" + item.id + ", name-" + item.name);
            }

            mFbConnector.postParcelListToFirebaseDatabase(mDateStr, (ArrayList<TmsParcelItem>) mParcelList);
            mFbConnector.postCourierListToFirbaseDatabase(mDateStr, (ArrayList<TmsCourierItem>) couriers);

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
                String latitude_y = null;
                String longitude_x = null;
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

    private void initParcelId() {
        for (int i = 0; i < mParcelList.size(); i++) {
            TmsParcelItem  item = mParcelList.get(i);
            item.id = String.valueOf(i+1);
        }
    }

    private List<TmsCourierItem> buildTmsCouriers(List<String> list) {
        List<TmsCourierItem> result = new ArrayList<>();
        for (int i =0; i < list.size(); i++) {
            TmsCourierItem item = new TmsCourierItem(String.valueOf(i+1), list.get(i));
            result.add(item);
        }
        return result;
    }

    private void goToParcelList() {
        mContext.startActivity(new Intent(mContext, ParcelListActivity.class)
                .putExtra(Utils.KEY_DB_DATE, mDateStr)
                .putExtra(Utils.KEY_COURIER_NAME, mContext.getString(R.string.all_couriers)));
    }
}
