package com.simplymadeapps.simple_logger_android;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import junit.framework.Assert;

import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Created by stephenruda on 8/4/17.
 */

public class TestTransferHelper {

    @Test
    public void test_transferHelper() {
        TransferUtility utility = mock(TransferUtility.class);
        String bucket = "bucket";
        String directory = "dir";
        String filename = "file";
        File file = mock(File.class);

        TransferObserver obs = mock(TransferObserver.class);
        doReturn(obs).when(utility).upload(
                bucket,     /* The bucket to upload to */
                directory+filename+".txt",    /* The key for the uploaded object */
                file);

        TransferObserver observer = TransferHelper.getTransferObserver(utility,directory,bucket,filename,file);
        Assert.assertEquals(obs, observer);
    }
}
