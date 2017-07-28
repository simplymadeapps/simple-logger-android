package com.simplymadeapps.simple_logger_android;

import fr.xebia.android.freezer.annotations.Model;

@Model
public class RecordedLog {

    public String log;
    public String date;
    public String time;
    public long epoch;

    protected RecordedLog() {
        // needed for library
    }

    protected RecordedLog(String log, String date, String time, long epoch) {
        this.log = log;
        this.date = date;
        this.time = time;
        this.epoch = epoch;
    }
}
