package com.simplymadeapps.simple_logger_android;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface RecordedLogDao {

    @Insert
    void insert(RecordedLog log);

    @Query("SELECT * FROM recorded_log")
    List<RecordedLog> getAll();

    @Query("SELECT * FROM recorded_log WHERE date >= :lowerBound and date < :upperBound")
    List<RecordedLog> getLogsWithinTimeRange(long lowerBound, long upperBound);

    @Query("DELETE FROM recorded_log WHERE date < :time;")
    void deleteLogsOlderThanTime(long time);

    @Delete
    void delete(List<RecordedLog> logs);

    @Query("DELETE FROM recorded_log")
    void deleteAll();

}