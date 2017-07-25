package com.simplymadeapps.simple_logger_android;

public class RecordedLog {

    private String log;
    private String date;
    private long epoch;

    protected RecordedLog(String log, String date, long epoch) {
        this.log = log;
        this.date = date;
        this.epoch = epoch;
    }

    // Returns true if this log is older than a week
    protected boolean isLogAWeekOld() {
        long one_week_ago = System.currentTimeMillis() - 604800000;
        return one_week_ago > epoch;
    }
}
