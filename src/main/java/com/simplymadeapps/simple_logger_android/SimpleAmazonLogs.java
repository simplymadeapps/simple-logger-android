package com.simplymadeapps.simple_logger_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class SimpleAmazonLogs {

    protected final static String PREF_KEY = "com.simplymadeapps.simple_logger_android.logs";
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;
    protected Gson gson;

    // Constructor
    // context - Used to access the storage
    public SimpleAmazonLogs(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
        gson = new Gson();
    }

    // Empty constructor helpful for testing
    protected SimpleAmazonLogs() {
        // empty constructor
    }

    // Takes a string to store as a log.  The time stamp will be added automatically
    public void addLog(String log) {
        List<RecordedLog> current_logs = getLogs(); // Get the current list of records to add to
        RecordedLog rl = new RecordedLog(log, currentTimeString(), System.currentTimeMillis()); // Create a new log object to add, with the current date and time
        current_logs.add(rl);
        String records_json = gson.toJson(current_logs); // Convert logs to json for storage

        // Commit the logs to storage as json
        editor.putString(PREF_KEY, records_json);
        editor.commit();
    }

    // Returns the array of records from storage.  If there are no records, it will be an empty list.  The logs will be automatically pruned.
    public List<RecordedLog> getLogs() {
        String records_json = preferences.getString(PREF_KEY, ""); // Pull logs from storage as json
        Type type = new TypeToken<List<RecordedLog>>(){}.getType();
        if(records_json.isEmpty()) {
            // There were no records
            return new ArrayList<>();
        }
        else {
            // There are records
            List<RecordedLog> logs = gson.fromJson(records_json, type);
            return pruneLogs(logs); // Return the list of pruned logs
        }
    }

    // Remove any logs that are 7 days old and then return the list
    public List<RecordedLog> pruneLogs(List<RecordedLog> logs) {
        for(Iterator<RecordedLog> iterator = logs.iterator(); iterator.hasNext();) {
            RecordedLog current_log = iterator.next();
            if(current_log.isLogAWeekOld()) {
                iterator.remove(); // Remove this old log
            }
            else {
                break; // Because this log was not a week old and they are in order - we can break the loop
            }
        }

        return logs;
    }

    // Gets the current date/time to be added to the log
    // yyyy-MM-dd HH:mm:ss
    // ex: 2015-06-25 07:36:01 CST
    private String currentTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        Date currentDateTime = Calendar.getInstance().getTime();
        return sdf.format(currentDateTime);
    }
}
