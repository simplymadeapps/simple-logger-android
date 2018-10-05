package com.simplymadeapps.simple_logger_android;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {RecordedLog.class}, version = 1)
public abstract class RecordedLogDatabase extends RoomDatabase {
    public abstract RecordedLogDao recordedLogDao();
}