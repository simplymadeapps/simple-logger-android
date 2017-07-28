package com.simplymadeapps.simple_logger_android;

import android.app.Application;
import android.content.Context;
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
import java.util.Date;
import java.util.List;

import fr.xebia.android.freezer.Freezer;

public class SimpleAmazonLogs {

    RecordedLogEntityManager rlem;
    protected Context context;

    // Constructor
    public SimpleAmazonLogs(Application application) {
        Freezer.onCreate(application);
        this.context = application.getApplicationContext();
        rlem = new RecordedLogEntityManager();
    }

    // Takes a string to store as a log.  The time stamp will be added automatically.
    public void addLog(String log, int offset) {
        //rlem.add(new RecordedLog(log, offsetDateString(0), currentTimeString(), System.currentTimeMillis()));
        rlem.add(new RecordedLog(log, offsetDateString(offset), currentTimeString(), System.currentTimeMillis() - (offset*86400000)));
        clearWeekOldLogs();  // Because adding a log is called frequently, we will use it to check for week old logs and delete them
    }

    // Returns the array of records from storage.  If there are no records, it will be an empty list.
    public List<RecordedLog> getAllLogs() {
        return rlem.select().asList();
    }

    // Clear logs from storage that are over a week old
    protected void clearWeekOldLogs() {
        long one_week_ago = System.currentTimeMillis() - 604800000;
        List<RecordedLog> week_old_logs = rlem.select().epoch().lessThan(one_week_ago).asList();
        rlem.delete(week_old_logs);
    }

    // Returns an array of records from a specific day from storage.  If there are no records, it will be an empty list.
    protected List<RecordedLog> getLogsFromSpecificDay(String date_string) {
        return rlem.select().date().equalsTo(date_string).asList();
    }

    // Gets a date with day(s) offset.  If offset = 1 then the date will be yesterday.  If offset = 0 then the date will be today
    // yyyy-MM-dd
    // ex: 2015-06-24
    protected String offsetDateString(int offset) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        int subtract = offset*86400000;
        Date date = new Date(System.currentTimeMillis()-subtract);
        return sdf.format(date);
    }

    // Gets the current time to be added to the log
    // HH:mm:ss
    // ex: 07:36:01 CST
    protected String currentTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss z");
        Date currentTime = new Date(System.currentTimeMillis());
        return sdf.format(currentTime);
    }

    // Will return a text file with all of the logs.  It will return null if there was an error creating the file
    protected File createLogTextFile(List<RecordedLog> logs) {
        try {
            File file = File.createTempFile(System.currentTimeMillis()+"", ".txt");
            FileOutputStream stream = new FileOutputStream(file);
            for(RecordedLog log : logs) {
                String line = log.date + " " + log.time + " - " + log.log + "\n";
                stream.write(line.getBytes());
            }
            stream.close();
            return file;
        }
        catch(Exception e) {
            return null;
        }
    }

    public List<List<RecordedLog>> getListOfListOfLogsToUpload() {
        List<List<RecordedLog>> list_of_lists = new ArrayList<>();
        for(int i = 0; i < 7; i++) {
            List<RecordedLog> logs_from_day = getLogsFromSpecificDay(offsetDateString(i));
            if(logs_from_day.size() > 0) {
                list_of_lists.add(logs_from_day);
            }
        }
        return list_of_lists;
    }

    // Upload current list of logs to Amazon S3
    public void uploadLogsToAmazon(String access_token, String secret, String bucket, String directory, final SimpleAmazonLogCallback callback) {
        // Create an S3 client
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(access_token,secret));

        // Set the region of your S3 bucket
        s3.setRegion(Region.getRegion(Regions.US_EAST_1));

        // We have to do an upload for each day of logs we have.  This could mean there is between 0 and 7 logs that need to be uploaded
        List<List<RecordedLog>> list_of_list_of_logs = getListOfListOfLogsToUpload();

        final int TOTAL_LOGS_TO_UPLOAD = list_of_list_of_logs.size();
        completed_calls = 0;

        Log.d("TOTAL",TOTAL_LOGS_TO_UPLOAD+"");

        for(List<RecordedLog> list_of_logs : list_of_list_of_logs) {
            final File file = createLogTextFile(list_of_logs); // Create a text file from the logs
            String filename = list_of_logs.get(0).date; // Generate a name for the file

            // Upload to amazon
            TransferUtility transferUtility = new TransferUtility(s3, context);
            TransferObserver observer = transferUtility.upload(
                    bucket,     /* The bucket to upload to */
                    directory+filename+".txt",    /* The key for the uploaded object */
                    file        /* The file where the data to upload exists */
            );

            // Monitor the upload so we can callback when completed
            observer.setTransferListener(getTransferListener(TOTAL_LOGS_TO_UPLOAD, filename, file, callback));
        }

        // There was nothing to upload - let's just return success
        if(TOTAL_LOGS_TO_UPLOAD == 0) {
            callback.onSuccess();
        }
    }

    private int completed_calls = 0;

    // This is our transfer listener for when we upload the file
    protected TransferListener getTransferListener(final int TOTAL_LOGS_TO_UPLOAD, final String filename, final File file, final SimpleAmazonLogCallback callback) {
        return new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    // Iterate the number of uploads that have completed
                    completed_calls = completed_calls + 1;
                    Log.d("COMPLETED",completed_calls+"");

                    // We want to delete this temporary file
                    file.delete();

                    // If the uploaded logs are from a previous day (not from today) we can clear those logs.
                    if(!filename.equalsIgnoreCase(offsetDateString(0))) {
                        rlem.delete(getLogsFromSpecificDay(filename));
                    }

                    // Wait until all of the logs have uploaded successfully before giving the onSuccess callback
                    if(completed_calls == TOTAL_LOGS_TO_UPLOAD) {
                        Log.d("ALL UPLOADED","ALL UPLOADED");
                        callback.onSuccess();
                    }
                }

                if (state.equals(TransferState.CANCELED) || state.equals(TransferState.FAILED)) {
                    // Failed upload
                    callback.onFailure("Transfer failed or was canceled");
                    file.delete();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                // We do not track progress, do nothing
            }

            @Override
            public void onError(int id, Exception ex) {
                // Failed upload
                callback.onFailure(ex.getMessage());
                file.delete();
            }
        };
    }
}
