package com.simplymadeapps.simple_logger_android;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(Enclosed.class)
public class TransferHelperTests {

    public static class ConstructorTests {

        @Test
        public void testConstructors() {
            // These are empty constructors with no assertations
            TransferHelper th = new TransferHelper();
        }
    }

    public static class GetTransferObserverTests {

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
}
