package com.simplymadeapps.simple_logger_android;

import java.util.List;

/**
 * Created by stephenruda on 7/27/17.
 */

public interface RecordedLogCallbacks {
    void onLogsReady(List<RecordedLog> logs);
}
