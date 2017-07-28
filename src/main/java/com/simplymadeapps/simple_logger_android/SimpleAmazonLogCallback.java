package com.simplymadeapps.simple_logger_android;

/**
 * Created by stephenruda on 7/27/17.
 */

public interface SimpleAmazonLogCallback {
    void onSuccess();
    void onFailure(String reason);
}
