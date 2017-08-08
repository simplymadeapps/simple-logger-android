package com.simplymadeapps.simple_logger_android;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import java.io.File;

/**
 * Created by stephenruda on 8/3/17.
 */

public class TransferHelper {

    protected TransferHelper() {
        // Needed for tests
    }

    protected static TransferObserver getTransferObserver(TransferUtility transferUtility, String directory, String bucket, String filename, File file) {
        TransferObserver observer = transferUtility.upload(
                bucket,     /* The bucket to upload to */
                directory+filename+".txt",    /* The key for the uploaded object */
                file        /* The file where the data to upload exists */
        );
        return observer;
    }
}
