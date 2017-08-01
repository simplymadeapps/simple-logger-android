package com.simplymadeapps.simple_logger_android;

import java.util.Date;

import fr.xebia.android.freezer.annotations.Model;

@Model
public class RecordedLog {

    public String log;
    public Date recordDate;

    protected RecordedLog() {
        // needed for library
    }

    protected RecordedLog(String log, Date date) {
        this.log = log;
        this.recordDate = date;
    }
}
