package com.simplymadeapps.simple_logger_android;

/**
 * Created by stephenruda on 7/27/17.
 */

public interface SimpleAmazonLogCallback {
    void onSuccess(int total_uploaded);
    void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads);
}
