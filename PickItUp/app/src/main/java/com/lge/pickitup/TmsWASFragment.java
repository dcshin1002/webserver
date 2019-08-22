package com.lge.pickitup;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class TmsWASFragment extends Fragment {
    private static final String TAG = "TmsWASFragment";

    private static final String URL_KEY = "UrlKey";

    private ProcessingCallback<String> callback;
    private ProcessingTask processingTask;
    private String urlString;

    public static TmsWASFragment getInstance(FragmentManager fragmentManager, String url) {
        TmsWASFragment networkFragment = new TmsWASFragment();
        Bundle args = new Bundle();
        args.putString(URL_KEY, url);
        networkFragment.setArguments(args);
        fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        urlString = getArguments().getString(URL_KEY);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
        callback = (ProcessingCallback<String>) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear reference to host Activity to avoid memory leak.
        callback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelProcess();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution.
     */
    public void startProcess(String date, int clusterNum) {
        cancelProcess();

        String setUrl = urlString + "/set";
        String getUrl = urlString + "/job";
        String[] strArr = date.split("-");
        for (String s : strArr) {
            setUrl += "/";
            setUrl += s.replaceFirst("^0+(?!$)", "");
        }
        // TODO - How to pass cluster number?
//        setUrl += Integer.toString(clusterNum);
        Log.i(TAG, "SetClusters URL : " + setUrl);
        Log.i(TAG, "GetProgress URL : " + getUrl);

        processingTask = new ProcessingTask(callback);
        processingTask.execute(setUrl, getUrl);
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing execution.
     */
    public void cancelProcess() {
        if (processingTask != null) {
            processingTask.cancel(true);
        }
    }

    /**
     * Implementation of AsyncTask designed to fetch data from the network.
     */
    private class ProcessingTask extends AsyncTask<String, Integer, ProcessingTask.Result> {
        private ProcessingCallback<String> callback;

        ProcessingTask(ProcessingCallback<String> callback) {
            setCallback(callback);
        }

        void setCallback(ProcessingCallback<String> callback) {
            this.callback = callback;
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
//        Weather w = new Weather();
//        w.setTemprature(json.getJSONObject("main").getInt("temp"));
//        w.setCity(json.getString("name"));
//        return json.toString();
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
                if (jsonField.equals("jobid")) {
                    publishProgress(ProcessingCallback.Progress.CONNECT_SUCCESS, 5);
                }
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

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            Log.i(TAG, "onPreExecute(), callback=" + Boolean.toString(callback!=null));
            if (callback != null) {
                NetworkInfo networkInfo = callback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    Log.w(TAG, "No connection !!");
                    callback.finishProcessing();
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected Result doInBackground(String... urls) {
            Result result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                try {
                    URL setUrl = new URL(urls[0]);
                    String jobId = processUrl(setUrl, "jobid");
                    Log.i(TAG, "Returned jobid : " + jobId);
                    if (jobId != null) {
                        result = new Result(jobId);
                    } else {
                        throw new IOException("No response received.");
                    }
                    publishProgress(ProcessingCallback.Progress.PROCESS_IN_PROGRESS, 50);

                    URL queryUrl = new URL(urls[1]+"/"+jobId);
                    while (true) {
                        Thread.sleep(5000);
                        String status = processUrl(queryUrl, "status");
                        if (status.equals("finished")) {
                            publishProgress(ProcessingCallback.Progress.PROCESS_SUCCESS, 100);
                            break;
                        } else {
                            publishProgress(ProcessingCallback.Progress.PROCESS_IN_PROGRESS, 75);
                        }
                    }
                } catch (Exception e) {
                    result = new Result(e);
                }
            }
            String msg = "doInBackground([" + urls[0] + ", " + urls[1] + "]) ->";
            if (result != null) msg += result.resultValue;
            Log.i(TAG, msg);
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i(TAG, "onProgressUpdate(" +
                    Integer.toString(values[0]) + "," +
                    Integer.toString(values[1]) + ")");
            callback.onProgressUpdate(values[0], values[1]);
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(Result result) {
            if (result != null && callback != null) {
                if (result.exception != null) {
                    callback.updateFromProcess(result.exception.getMessage());
                } else if (result.resultValue != null) {
                    callback.updateFromProcess(result.resultValue);
                }
                callback.finishProcessing();
            }
            String msg = "onPostExecute(";
            if (result != null) msg += result.resultValue;
            msg += ")";
            Log.i(TAG, msg);
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(Result result) {
            String msg = "onCancelled(";
            if (result != null) msg += result.resultValue;
            msg += ")";
            Log.i(TAG, msg);
        }

        /**
         * Wrapper class that serves as a union of a result value and an exception. When the download
         * task has completed, either the result value or exception can be a non-null value.
         * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
         */
        class Result {
            public String resultValue;
            public Exception exception;

            public Result(String resultValue) {
                this.resultValue = resultValue;
            }

            public Result(Exception exception) {
                this.exception = exception;
            }
        }
    }
}
