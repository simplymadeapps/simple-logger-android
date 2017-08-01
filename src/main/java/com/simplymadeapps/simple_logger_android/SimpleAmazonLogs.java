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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import fr.xebia.android.freezer.Freezer;

public class SimpleAmazonLogs {

    protected static SimpleAmazonLogs instance;

    protected RecordedLogEntityManager rlem;
    protected Context context;

    protected int daysToKeepInStorage;

    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;

    private static final String KEY_1 = "com.simplymadeapps.simple_logger_android_storage_duration";

    protected String access_token = "";
    protected String secret_token = "";
    protected String amazon_bucket = "";
    protected Regions amazon_region = null;


    /**
     * @param application the application object used for storing records
     * @return the entry point for adding, reading, and uploading logs
     */
    public static SimpleAmazonLogs getInstance(Application application) {
        if(instance == null) {
            instance = new SimpleAmazonLogs(application);
        }
        return instance;
    }

    // Constructor
    protected SimpleAmazonLogs(Application application) {
        Freezer.onCreate(application);
        this.context = application.getApplicationContext();
        this.rlem = new RecordedLogEntityManager();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.editor = preferences.edit();
        this.daysToKeepInStorage = preferences.getInt(KEY_1, 7);
    }

    /**
     * @param log takes a string and stores it as a log.  The time stamp will be added automatically.
     */
    public void addLog(String log) {
        rlem.add(new RecordedLog(log, Calendar.getInstance().getTime()));
        clearOldLogs();  // Because adding a log is called frequently, we will use it to check for week old logs and delete them
    }

    /**
     * @return returns all currently stored logs
     */
    public List<RecordedLog> getAllLogs() {
        return rlem.select().asList();
    }

    /**
     * @param days sets how long we should keep logs stored locally - by default, logs are deleted after 7 days
     */
    public void setStorageDuration(int days) {
        editor.putInt(KEY_1, days);
        editor.commit();
        daysToKeepInStorage = days;
        clearOldLogs();
    }

    /**
     * Deletes all stored records
     */
    public void deleteAllLogs() {
        rlem.deleteAll();
    }

    /**
     * @param access_token your access token for Amazon S3
     * @param secret_token your secret token for Amazon S3
     * @param amazon_bucket the name of your bucket for Amazon S3
     * @param amazon_region the region your Amazon S3 is located
     */
    public void setAmazonCredentials(String access_token, String secret_token, String amazon_bucket, Regions amazon_region) {
        this.access_token = access_token;
        this.secret_token = secret_token;
        this.amazon_bucket = amazon_bucket;
        this.amazon_region = amazon_region;
    }

    // Gets a date object that we will use to compare dates.
    protected Date getPreviousDate(int daysAgo) {
        // Sets up a calendar to the current time
        Calendar today = Calendar.getInstance();
        Calendar calendar = new GregorianCalendar(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        // Subtracts the amount of days we want to go back
        calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return calendar.getTime();
    }

    // Clear logs from storage that are older than the threshold
    protected void clearOldLogs() {
        List<RecordedLog> week_old_logs = rlem.select().recordDate().before(getPreviousDate(daysToKeepInStorage-1)).asList();
        rlem.delete(week_old_logs);
    }

    // Will return a text file with all of the logs.  It will return null if there was an error creating the file
    protected File createLogTextFile(List<RecordedLog> logs) {
        try {
            File file = File.createTempFile(System.currentTimeMillis()+"", ".txt");
            FileOutputStream stream = new FileOutputStream(file);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            for(RecordedLog log : logs) {
                String line = sdf.format(log.recordDate) + " - " + log.log + "\n";
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
    // 06/25 11:59:99 < date < 06/26 12:00:00
    // So it should give all logs perfectly for that date
    protected List<RecordedLog> getLogsFromSpecificDay(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getPreviousDate(daysAgo));
        calendar.add(Calendar.MILLISECOND, -1);
        Date min = calendar.getTime();
        Date max = getPreviousDate(daysAgo-1);
        return rlem.select().recordDate().between(min, max).asList();
    }

    // Get list of list of logs to be uploaded - one list of logs per day
    protected List<List<RecordedLog>> getListOfListOfLogsToUpload() {
        List<List<RecordedLog>> list_of_lists = new ArrayList<>();
        for(int i = 0; i < daysToKeepInStorage; i++) {
            List<RecordedLog> logs_from_day = getLogsFromSpecificDay(i);
            if(logs_from_day.size() > 0) {
                list_of_lists.add(logs_from_day);
            }
        }
        return list_of_lists;
    }

    /**
     * @param directory the directory you wish to upload to on Amazon S3 - for example, 'app-logs/user-200/'
     * @param callback the callback for upload completion
     */
    public void uploadLogsToAmazon(String directory, final SimpleAmazonLogCallback callback) {
        // Make sure they have setup credentials
        if(access_token.isEmpty() || secret_token.isEmpty() || amazon_bucket.isEmpty() || amazon_region == null) {
            callback.onFailure("You must call setAmazonCredentials() before uploading to Amazon");
        }
        else {
            // Create an S3 client
            AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(access_token,secret_token));

            // Set the region of your S3 bucket
            s3.setRegion(Region.getRegion(amazon_region));

            // We have to do an upload for each day of logs we have.  This could mean there is between 0 and 7 logs that need to be uploaded
            List<List<RecordedLog>> list_of_list_of_logs = getListOfListOfLogsToUpload();

            final int TOTAL_LOGS_TO_UPLOAD = list_of_list_of_logs.size();
            successful_calls = 0;
            unsuccessful_calls = 0;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for(List<RecordedLog> list_of_logs : list_of_list_of_logs) {
                final File file = createLogTextFile(list_of_logs); // Create a text file from the logs
                String filename = sdf.format(list_of_logs.get(0).recordDate); // Generate a name for the file

                // Upload to amazon
                TransferUtility transferUtility = new TransferUtility(s3, context);
                TransferObserver observer = transferUtility.upload(
                        amazon_bucket,     /* The bucket to upload to */
                        directory+filename+".txt",    /* The key for the uploaded object */
                        file        /* The file where the data to upload exists */
                );

                // Monitor the upload so we can callback when completed
                observer.setTransferListener(getTransferListener(TOTAL_LOGS_TO_UPLOAD, list_of_logs, file, callback));
            }

            // There was nothing to upload - let's just return success
            if(TOTAL_LOGS_TO_UPLOAD == 0) {
                callback.onSuccess();
            }
        }
    }

    private int successful_calls = 0;
    private int unsuccessful_calls = 0;

    // This is our transfer listener for when we upload the file
    protected TransferListener getTransferListener(final int TOTAL_LOGS_TO_UPLOAD, final List<RecordedLog> list_of_logs, final File file, final SimpleAmazonLogCallback callback) {
        return new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    // We want to delete this temporary file
                    file.delete();

                    // Iterate the number of uploads that have completed
                    successful_calls = successful_calls + 1;

                    // If the uploaded logs are from a previous day (not from today) we can clear those logs.
                    if(list_of_logs.get(0).recordDate.before(getPreviousDate(0))) {
                        rlem.delete(list_of_logs);
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
                        callback.onSuccess();
                    }
                    else {
                        // There was at least one failed call, we can return failure
                        Log.d("STATE",state.toString());
                        callback.onFailure("At least one file failed to upload or was canceled");
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
                    callback.onFailure("At least one file failed to upload or was canceled");
                }
            }
        };
    }
}
