package com.simplymadeapps.simple_logger_android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import androidx.room.Room;

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

    protected static RecordedLogDatabase database;

    /**
     * @param application the application object used for storing records
     * @return the entry point for adding, reading, and uploading logs
     */
    public static void init(Application application) {
        if(instance == null) {
            instance = new SimpleAmazonLogs(application);
        }
    }

    // Constructor
    protected SimpleAmazonLogs(Application application) {
        this.database = Room.databaseBuilder(application, RecordedLogDatabase.class, "recorded_log_database").build();
        this.context = application.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.editor = preferences.edit();
        this.daysToKeepInStorage = preferences.getInt(KEEP_IN_STORAGE_KEY, 7);
    }

    /**
     * @param log takes a string and stores it as a log.  The time stamp will be added automatically.
     */
    public static void addLog(final String log) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                if(haveNotCheckedForOldLogsInLast24Hrs()) { // Only check to clear old logs if we haven't in the last 24 hrs
                    clearOldLogs();  // Because adding a log is called frequently, we will use it to check for week old logs and delete them
                }

                RecordedLog recordedLog = new RecordedLog();
                recordedLog.setLog(log);
                recordedLog.setDate(System.currentTimeMillis());
                database.recordedLogDao().insert(recordedLog);
            }
        };
        thread.start();
    }

    /**
     * @return returns true if we have already checked for old logs today, false if not
     */
    protected static boolean haveNotCheckedForOldLogsInLast24Hrs() {
        return last_clear_old_logs_checked < System.currentTimeMillis()-HRS_24_IN_MS;
    }

    /**
     * @return returns all currently stored logs
     */
    public static void getAllLogs(final RecordedLogCallbacks callback) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                callback.onLogsReady(database.recordedLogDao().getAll());
            }
        };
        thread.start();
    }

    /**
     * @param days sets how long we should keep logs stored locally - by default, logs are deleted after 7 days
     */
    public static void setStorageDuration(int days) {
        editor.putInt(KEEP_IN_STORAGE_KEY, days);
        editor.commit();
        daysToKeepInStorage = days;
    }

    /**
     * Deletes all stored records
     */
    public static void deleteAllLogs() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                database.recordedLogDao().deleteAll();
            }
        };
        thread.start();
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
        // no need for it is own thread as this always gets called from the addLogs thread
        database.recordedLogDao().deleteLogsOlderThanTime(System.currentTimeMillis()-decrement);
        last_clear_old_logs_checked = System.currentTimeMillis();
    }

    // Will return a text file with all of the logs.  It will return null if there was an error creating the file
    protected static File createLogTextFile(List<RecordedLog> logs) {
        try {
            File file = File.createTempFile(System.currentTimeMillis()+"", ".txt");
            FileOutputStream stream = new FileOutputStream(file);
            for(RecordedLog log : logs) {
                String line = log.getReadableDate() + " - " + log.getLog() + "\n";
                stream.write(line.getBytes());
            }
            stream.close();
            return file;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Get the logs from a specific day
    // The range will end up being as follows:
    // 06/25 12:00:00 <= date < 06/26 12:00:00
    // So it should give all logs perfectly for that date
    protected static List<RecordedLog> getLogsFromSpecificDay(int daysAgo) {
        // get the date/time for right now
        Calendar date = new GregorianCalendar();

        // reset hour, minutes, seconds and millis so it is midnight
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        // This will set the date to daysAgo at midnight
        date.add(Calendar.DAY_OF_MONTH, (daysAgo*-1)+1);
        long upperBound = date.getTimeInMillis();
        Log.d("UPPER BOUND",upperBound+"");

        // This will set the date to daysAgo+1 at midnight
        date.add(Calendar.DAY_OF_MONTH, -1);
        long lowerBound = date.getTimeInMillis();
        Log.d("LOWER BOUND",lowerBound+"");

        return database.recordedLogDao().getLogsWithinTimeRange(lowerBound, upperBound);
    }

    // Get list of list of logs to be uploaded - one list of logs per day
    protected static List<List<RecordedLog>> getListOfListOfLogsToUpload() {
        List<List<RecordedLog>> list_of_lists = new ArrayList<>();
        for(int i = 0; i < daysToKeepInStorage; i++) {
            List<RecordedLog> logs_from_day = getLogsFromSpecificDay(i);
            if(logs_from_day.size() > 0) {
                list_of_lists.add(logs_from_day);
            }
        }
        return list_of_lists;
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
            Thread thread = new Thread() {
                @Override
                public void run() {
                    // Create an S3 client
                    AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(access_token,secret_token));

                    // Set the region of your S3 bucket
                    s3.setRegion(Region.getRegion(region));

                    // We have to do an upload for each day of logs we have.  This could mean there is between 0 and 7 logs that need to be uploaded
                    List<List<RecordedLog>> list_of_list_of_logs = getListOfListOfLogsToUpload();
                    System.out.println(list_of_list_of_logs.size());

                    final int TOTAL_LOGS_TO_UPLOAD = list_of_list_of_logs.size();
                    successful_calls = 0;
                    unsuccessful_calls = 0;

                    for(List<RecordedLog> list_of_logs : list_of_list_of_logs) {
                        final File file = createLogTextFile(list_of_logs); // Create a text file from the logs
                        String filename = list_of_logs.get(0).getTextFileTitle(); // Generate a name for the file
                        System.out.println(filename);

                        // Upload to amazon
                        TransferUtility transferUtility = new TransferUtility(s3, context);
                        TransferObserver observer = TransferHelper.getTransferObserver(transferUtility, directory, bucket, filename, file);

                        // Monitor the upload so we can callback when completed
                        observer.setTransferListener(getTransferListener(TOTAL_LOGS_TO_UPLOAD, list_of_logs, file, callback));
                    }

                    // There was nothing to upload - let's just return success
                    if(TOTAL_LOGS_TO_UPLOAD == 0) {
                        callback.onSuccess(0);
                    }
                }
            };
            thread.start();
        }
    }

    public static void deleteListOfLogs(final List<RecordedLog> list_of_logs) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                database.recordedLogDao().delete(list_of_logs);
            }
        };
        thread.start();
    }

    protected static int successful_calls = 0;
    protected static int unsuccessful_calls = 0;

    // This is our transfer listener for when we upload the file
    protected static TransferListener getTransferListener(final int TOTAL_LOGS_TO_UPLOAD, final List<RecordedLog> list_of_logs, final File file, final SimpleAmazonLogCallback callback) {
        return new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    // We want to delete this temporary file
                    file.delete();

                    // Iterate the number of uploads that have completed
                    successful_calls = successful_calls + 1;

                    // This just gets me a dummy log for today's date
                    RecordedLog recordedLog = new RecordedLog();
                    recordedLog.setDate(System.currentTimeMillis());

                    // If the uploaded logs are from a previous day (not from today) we can clear those logs.
                    if(!list_of_logs.get(0).getTextFileTitle().equalsIgnoreCase(recordedLog.getTextFileTitle())) {
                        deleteListOfLogs(list_of_logs);
                    }
                }

                if (state.equals(TransferState.CANCELED) || state.equals(TransferState.FAILED)) {
                    // We want to delete this temporary file
                    file.delete();

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
                file.delete();
                ex.printStackTrace();
                unsuccessful_calls = unsuccessful_calls + 1;
                if(successful_calls + unsuccessful_calls == TOTAL_LOGS_TO_UPLOAD) {
                    callback.onFailure(ex, successful_calls, unsuccessful_calls);
                }
            }
        };
    }
}
