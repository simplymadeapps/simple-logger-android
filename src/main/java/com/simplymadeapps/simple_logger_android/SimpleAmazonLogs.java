package com.simplymadeapps.simple_logger_android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.snatik.storage.Storage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SimpleAmazonLogs {

    protected static SimpleAmazonLogs instance;

    protected static Context context;

    protected static int daysToKeepInStorage;

    protected static SharedPreferences preferences;
    protected static SharedPreferences.Editor editor;

    protected static final String KEEP_IN_STORAGE_KEY = "com.simplymadeapps.simple_logger_android_storage_duration";

    protected static String access_token = "";
    protected static String secret_token = "";
    protected static String bucket = "";
    protected static Regions region = null;
    protected static long last_clear_old_logs_checked = 0;
    final static long HRS_24_IN_MS = 86400000l;

    protected static Storage storage;
    protected static String storagePath;

    protected static SimpleDateFormat date_format_log = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    protected static SimpleDateFormat date_format_title = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * @param application the application object used for storing records
     */
    public static void init(Application application) {
        if(instance == null) {
            instance = new SimpleAmazonLogs(application);
        }
    }

    // Constructor
    protected SimpleAmazonLogs(Application application) {
        this.storage = SimpleAmazonLogsHelper.newStorageInstance(application.getApplicationContext());
        this.context = application.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.editor = preferences.edit();
        this.daysToKeepInStorage = preferences.getInt(KEEP_IN_STORAGE_KEY, 7);
        this.storagePath = storage.getInternalFilesDirectory()+File.separator+"SIO-Logs";
    }

    public static void addLog(String log) {
        if(haveNotCheckedForOldLogsInLast24Hrs()) { // Only check to clear old logs if we haven't in the last 24 hrs
            clearOldLogs();  // Because adding a log is called frequently, we will use it to check for week old logs and delete them
        }

        Date date = new Date(System.currentTimeMillis());
        String textFileName = date_format_title.format(date);
        String logDateTag = date_format_log.format(date);
        storage.createDirectory(storagePath, false);
        createTextFileIfItDoesntExist(textFileName);
        storage.appendFile(getFilePath(textFileName), logDateTag+" - "+log+"");
    }

    protected static void createTextFileIfItDoesntExist(String fileName) {
        boolean exists = storage.isFileExist(getFilePath(fileName));
        if(!exists) {
            storage.createFile(getFilePath(fileName), "");
        }
    }

    protected static String getFilePath(String fileName) {
        return storagePath+File.separator+fileName;
    }

    protected static boolean haveNotCheckedForOldLogsInLast24Hrs() {
        return last_clear_old_logs_checked < System.currentTimeMillis()-HRS_24_IN_MS;
    }

    public static void setStorageDuration(int days) {
        editor.putInt(KEEP_IN_STORAGE_KEY, days);
        editor.commit();
        daysToKeepInStorage = days;
    }

    public static void deleteAllLogs() {
        storage.deleteDirectory(storagePath);
    }

    public static String getLogsFromDaysAgo(int daysAgo) {
        long decrement = daysAgo*HRS_24_IN_MS;
        String current_day_title = date_format_title.format(new Date(System.currentTimeMillis()-decrement));
        boolean exists = storage.isFileExist(getFilePath(current_day_title));
        if(exists) {
            return storage.readTextFile(getFilePath(current_day_title));
        }
        else {
            // There are no logs for the current day
            return "";
        }
    }

    /**
     * @param amazon_access_token your access token for Amazon S3
     * @param amazon_secret_token your secret token for Amazon S3
     * @param amazon_bucket the name of your bucket for Amazon S3
     * @param amazon_region the region your Amazon S3 is located
     */
    public static void setAmazonCredentials(String amazon_access_token, String amazon_secret_token, String amazon_bucket, Regions amazon_region) {
        access_token = amazon_access_token;
        secret_token = amazon_secret_token;
        bucket = amazon_bucket;
        region = amazon_region;
    }

    // Clear logs from storage that are older than the threshold
    protected static void clearOldLogs() {
        long decrement = daysToKeepInStorage*HRS_24_IN_MS;
        last_clear_old_logs_checked = System.currentTimeMillis();
        List<File> files = storage.getFiles(getFilePath(""));
        if(files != null) {
            for(File file : files) {
                if(file.lastModified() < System.currentTimeMillis()-decrement) {
                    file.delete();
                }
            }
        }
    }

    protected static boolean verifyAmazonCredentialsHaveBeenAdded() {
        return access_token.isEmpty() || secret_token.isEmpty() || bucket.isEmpty() || region == null;
    }

    /**
     * @param directory the directory you wish to upload to on Amazon S3 - for example, 'app-logs/user-200/'
     * @param callback the callback for upload completion
     */
    public static void uploadLogsToAmazon(final String directory, final SimpleAmazonLogCallback callback) {
        // Make sure they have setup credentials
        if(verifyAmazonCredentialsHaveBeenAdded()) {
            callback.onFailure(new Exception("You must call setAmazonCredentials() before uploading to Amazon"), 0, 0);
        }
        else {
            // Create an S3 client
            AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(access_token,secret_token));

            // Set the region of your S3 bucket
            s3.setRegion(Region.getRegion(region));

            final List<File> files_to_upload = storage.getFiles(storagePath);
            final int TOTAL_LOGS_TO_UPLOAD = files_to_upload.size();
            successful_calls = 0;
            unsuccessful_calls = 0;

            for(File file : files_to_upload) {
                // Upload to amazon
                TransferUtility transferUtility = new TransferUtility(s3, context);
                TransferObserver observer = TransferHelper.getTransferObserver(transferUtility, directory, bucket, file.getName(), file);

                // Monitor the upload so we can callback when completed
                observer.setTransferListener(getTransferListener(TOTAL_LOGS_TO_UPLOAD, file, callback));
            }

            // There was nothing to upload - let's just return success
            if(TOTAL_LOGS_TO_UPLOAD == 0) {
                callback.onSuccess(0);
            }
        }
    }

    protected static int successful_calls = 0;
    protected static int unsuccessful_calls = 0;

    // This is our transfer listener for when we upload the file
    protected static TransferListener getTransferListener(final int TOTAL_LOGS_TO_UPLOAD, final File file, final SimpleAmazonLogCallback callback) {
        return new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    // Iterate the number of uploads that have completed
                    successful_calls = successful_calls + 1;

                    String current_day_title = date_format_title.format(new Date(System.currentTimeMillis()));
                    if(!current_day_title.equalsIgnoreCase(file.getName())) {
                        // We can delete any text file that isn't from today
                        file.delete();
                    }
                }

                if (state.equals(TransferState.CANCELED) || state.equals(TransferState.FAILED)) {
                    // Failed upload
                    unsuccessful_calls = unsuccessful_calls + 1;
                }

                // Wait until all of the logs have uploaded successfully before giving the onSuccess callback
                if(successful_calls + unsuccessful_calls == TOTAL_LOGS_TO_UPLOAD) {
                    if(unsuccessful_calls == 0) {
                        // All calls were success, we can return success
                        callback.onSuccess(successful_calls);
                    }
                    else {
                        // There was at least one failed call, we can return failure
                        callback.onFailure(new Exception("At least one file failed to upload or was canceled"), successful_calls, unsuccessful_calls);
                    }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                // We do not track progress, do nothing
            }

            @Override
            public void onError(int id, Exception ex) {
                // Failed upload
                ex.printStackTrace();
                unsuccessful_calls = unsuccessful_calls + 1;
                if(successful_calls + unsuccessful_calls == TOTAL_LOGS_TO_UPLOAD) {
                    callback.onFailure(ex, successful_calls, unsuccessful_calls);
                }
            }
        };
    }
}
