package com.simplymadeapps.simple_logger_android;

import android.content.Context;

import com.snatik.storage.Storage;

public class SimpleAmazonLogsHelper {

    protected static Storage newStorageInstance(Context context) {
        return new Storage(context);
    }
}
