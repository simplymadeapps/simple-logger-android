package com.simplymadeapps.simple_logger_android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.snatik.storage.Storage;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PreferenceManager.class, TransferHelper.class, TransferObserver.class, AmazonWebServiceClient.class, SimpleAmazonLogsHelper.class })
public class TestLogs {

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    // These values were chosen specifically as they are the lowest values possible while the tests still pass
    static int sleep_time_short = 50;
    static int sleep_time_long = 750;

    @Before
    public void setup() {
        preferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        SimpleAmazonLogs.daysToKeepInStorage = 7;
        SimpleAmazonLogs.editor = editor;
        SimpleAmazonLogs.preferences = preferences;
        SimpleAmazonLogs.storage = mock(Storage.class);
    }

    protected void waitForThread(final long ms) {
        try {
            Thread.sleep(ms); // Wait for thread
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_init() {
        Application application = mock(Application.class);
        Context context = mock(Context.class);
        doReturn(context).when(application).getApplicationContext();

        mockStatic(SimpleAmazonLogsHelper.class);
        mockStatic(PreferenceManager.class);

        Storage storage = mock(Storage.class);
        when(SimpleAmazonLogsHelper.newStorageInstance(context)).thenReturn(storage);

        doReturn(context).when(application).getApplicationContext();
        when(PreferenceManager.getDefaultSharedPreferences(any(Context.class))).thenReturn(preferences);
        doReturn(editor).when(preferences).edit();
        doReturn(7).when(preferences).getInt(SimpleAmazonLogs.KEEP_IN_STORAGE_KEY, 7);

        SimpleAmazonLogs.init(application);

        Assert.assertEquals(context, SimpleAmazonLogs.context);
        Assert.assertNotNull(SimpleAmazonLogs.storagePath);
        Assert.assertNotNull(SimpleAmazonLogs.instance);
        Assert.assertNotNull(SimpleAmazonLogs.storage);
        Assert.assertEquals(SimpleAmazonLogs.preferences, preferences);
        Assert.assertEquals(SimpleAmazonLogs.editor, editor);
        Assert.assertEquals(SimpleAmazonLogs.daysToKeepInStorage, 7);

        // Test init when it is not null
        SimpleAmazonLogs.init(application);
    }

    @Test
    public void test_getTextFileTitle_good() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleAmazonLogs.date_format_title = sdf;
        Date date = new Date(System.currentTimeMillis());

        String expected = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);

        String output = SimpleAmazonLogs.getTextFileTitle(date);

        Assert.assertEquals(expected, output);
        Assert.assertEquals(sdf, SimpleAmazonLogs.date_format_title);
        Assert.assertEquals(output.length(), 10);
    }

    @Test
    public void test_getTextFileTitle_tooLong() {
        SimpleDateFormat sdf = spy(new SimpleDateFormat("yyyy-00MM-dd", Locale.US));
        SimpleAmazonLogs.date_format_title = sdf;
        Date date = new Date(System.currentTimeMillis());

        String expected = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);

        String output = SimpleAmazonLogs.getTextFileTitle(date);

        Assert.assertEquals(expected, output);
        Assert.assertNotSame(sdf, SimpleAmazonLogs.date_format_title);
        Assert.assertEquals(output.length(), 10);
    }

    @Test
    public void test_addLog_withClear() {
        SimpleAmazonLogs.last_clear_old_logs_checked = 0;
        Assert.assertEquals(SimpleAmazonLogs.last_clear_old_logs_checked, 0);
        SimpleAmazonLogs.addLog("Test");
        verify(SimpleAmazonLogs.storage, times(1)).getFiles(anyString());
        verify(SimpleAmazonLogs.storage, times(1)).createDirectory(anyString(), eq(false));
        verify(SimpleAmazonLogs.storage, times(1)).appendFile(anyString(), anyString());
        Assert.assertNotSame(SimpleAmazonLogs.last_clear_old_logs_checked, 0);
    }

    @Test
    public void test_addLog_withoutClear() {
        SimpleAmazonLogs.last_clear_old_logs_checked = Long.MAX_VALUE;
        Assert.assertEquals(SimpleAmazonLogs.last_clear_old_logs_checked, Long.MAX_VALUE);
        SimpleAmazonLogs.addLog("Test");
        verify(SimpleAmazonLogs.storage, times(0)).getFiles(anyString());
        verify(SimpleAmazonLogs.storage, times(1)).createDirectory(anyString(), eq(false));
        verify(SimpleAmazonLogs.storage, times(1)).appendFile(anyString(), anyString());
        Assert.assertNotSame(SimpleAmazonLogs.last_clear_old_logs_checked, Long.MAX_VALUE);
    }

    @Test
    public void test_createTextFileIfItDoesntExist_exists() {
        doReturn(true).when(SimpleAmazonLogs.storage).isFileExist(anyString());
        SimpleAmazonLogs.createTextFileIfItDoesntExist("");
        verify(SimpleAmazonLogs.storage, times(0)).createFile(anyString(), anyString());
    }

    @Test
    public void test_createTextFileIfItDoesntExist_doesntExists() {
        doReturn(false).when(SimpleAmazonLogs.storage).isFileExist(anyString());
        SimpleAmazonLogs.createTextFileIfItDoesntExist("");
        verify(SimpleAmazonLogs.storage, times(1)).createFile(anyString(), anyString());
    }

    @Test
    public void test_haveNotCheckedForOldLogsIn24Hours_true() {
        SimpleAmazonLogs.last_clear_old_logs_checked = 0;
        boolean result = SimpleAmazonLogs.haveNotCheckedForOldLogsInLast24Hrs();
        Assert.assertTrue(result);
    }

    @Test
    public void test_haveNotCheckedForOldLogsIn24Hours_false() {
        SimpleAmazonLogs.last_clear_old_logs_checked = System.currentTimeMillis();
        boolean result = SimpleAmazonLogs.haveNotCheckedForOldLogsInLast24Hrs();
        Assert.assertFalse(result);
    }

    @Test
    public void test_setStorageDuration() {
        SimpleAmazonLogs.setStorageDuration(3);
        Assert.assertEquals(SimpleAmazonLogs.daysToKeepInStorage, 3);
        verify(SimpleAmazonLogs.editor, times(1)).commit();
        verify(SimpleAmazonLogs.editor, times(1)).putInt(SimpleAmazonLogs.KEEP_IN_STORAGE_KEY, 3);
    }

    @Test
    public void test_deleteAllLogs() {
        SimpleAmazonLogs.deleteAllLogs();
        verify(SimpleAmazonLogs.storage, times(1)).deleteDirectory(anyString());
    }

    @Test
    public void test_getLogsFromDaysAgo_exists() {
        doReturn(true).when(SimpleAmazonLogs.storage).isFileExist(anyString());
        doReturn("logs").when(SimpleAmazonLogs.storage).readTextFile(anyString());
        String result = SimpleAmazonLogs.getLogsFromDaysAgo(0);
        Assert.assertEquals(result, "logs");
    }

    @Test
    public void test_getLogsFromDaysAgo_doesNotExists() {
        doReturn(false).when(SimpleAmazonLogs.storage).isFileExist(anyString());
        doReturn("logs").when(SimpleAmazonLogs.storage).readTextFile(anyString());
        String result = SimpleAmazonLogs.getLogsFromDaysAgo(0);
        Assert.assertEquals(result, "");
    }

    @Test
    public void test_setAmazonCredentials() {
        SimpleAmazonLogs.setAmazonCredentials("1","2","3",null);
        Assert.assertEquals(SimpleAmazonLogs.access_token, "1");
        Assert.assertEquals(SimpleAmazonLogs.bucket, "3");
        Assert.assertEquals(SimpleAmazonLogs.secret_token, "2");
    }

    @Test
    public void test_clearOldLogs_nullFiles() {
        doReturn(null).when(SimpleAmazonLogs.storage).getFiles(anyString());
        SimpleAmazonLogs.clearOldLogs();
        // Nothing to assert on
    }

    @Test
    public void test_clearOldLogs() {
        List<File> files = new ArrayList<>();
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        doReturn(0l).when(file1).lastModified();
        doReturn(System.currentTimeMillis()).when(file2).lastModified();
        files.add(file1);
        files.add(file2);
        doReturn(files).when(SimpleAmazonLogs.storage).getFiles(anyString());
        SimpleAmazonLogs.clearOldLogs();
        verify(file1, times(1)).delete();
        verify(file2, times(0)).delete();
    }

    @Test
    public void test_verifyAmazonCredentialsHaveBeenAdded() {
        SimpleAmazonLogs.access_token = "";
        SimpleAmazonLogs.secret_token = "";
        SimpleAmazonLogs.bucket = "";
        SimpleAmazonLogs.region = null;

        boolean b1 = SimpleAmazonLogs.verifyAmazonCredentialsHaveBeenAdded();
        Assert.assertTrue(b1);

        SimpleAmazonLogs.access_token = "1";
        boolean b2 = SimpleAmazonLogs.verifyAmazonCredentialsHaveBeenAdded();
        Assert.assertTrue(b2);

        SimpleAmazonLogs.secret_token = "2";
        boolean b3 = SimpleAmazonLogs.verifyAmazonCredentialsHaveBeenAdded();
        Assert.assertTrue(b3);

        SimpleAmazonLogs.bucket = "3";
        boolean b4 = SimpleAmazonLogs.verifyAmazonCredentialsHaveBeenAdded();
        Assert.assertTrue(b4);

        SimpleAmazonLogs.region = Regions.US_EAST_1;
        boolean b5 = SimpleAmazonLogs.verifyAmazonCredentialsHaveBeenAdded();
        Assert.assertFalse(b5);
    }

    @Test
    public void test_uploadLogsToAmazonFailure() {
        SimpleAmazonLogs.access_token = "";
        SimpleAmazonLogs.secret_token = "";
        SimpleAmazonLogs.bucket = "";
        SimpleAmazonLogs.region = null;

        SimpleAmazonLogs.uploadLogsToAmazon("directory", new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total) {

            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {
                Assert.assertEquals(exception.getMessage(), "You must call setAmazonCredentials() before uploading to Amazon");
                Assert.assertEquals(successful_uploads, 0);
                Assert.assertEquals(unsuccessful_uploads, 0);
            }
        });

        SystemClock.sleep(100);
    }

    @Test
    public void test_zero_logs() {
        SimpleAmazonLogs.access_token = "1";
        SimpleAmazonLogs.secret_token = "2";
        SimpleAmazonLogs.bucket = "3";
        SimpleAmazonLogs.region = Regions.US_EAST_1;
        SimpleAmazonLogs.daysToKeepInStorage = 0;

        List<File> files = new ArrayList<>();
        doReturn(files).when(SimpleAmazonLogs.storage).getFiles(anyString());

        SimpleAmazonLogs.uploadLogsToAmazon("directory", new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total) {
                Assert.assertEquals(total, 0);
            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {

            }
        });

        // This sleeps the entire thread and allows the method time to do database operations
        waitForThread(sleep_time_long);

        // This sleeps the system clock so it does not start the next test for 1 second, giving us time to Assert in onSuccess
        SystemClock.sleep(100);
    }

    @Test
    public void test_uploadLogsToAmazonSuccess() {
        SimpleAmazonLogs.access_token = "1";
        SimpleAmazonLogs.secret_token = "2";
        SimpleAmazonLogs.bucket = "3";
        SimpleAmazonLogs.region = Regions.US_EAST_1;
        SimpleAmazonLogs.daysToKeepInStorage = 1;

        List<File> files = new ArrayList<>();
        files.add(mock(File.class));
        doReturn(files).when(SimpleAmazonLogs.storage).getFiles(anyString());

        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        doReturn("name").when(context).getPackageName();
        SimpleAmazonLogs.context = context;

        TransferObserver observer = mock(TransferObserver.class);
        doNothing().when(observer).setTransferListener(any(TransferListener.class));

        mockStatic(TransferHelper.class);
        doReturn(observer).when(TransferHelper.class);
        TransferHelper.getTransferObserver(any(TransferUtility.class),eq("directory"),eq("3"),anyString(),any(File.class));

        SimpleAmazonLogs.uploadLogsToAmazon("directory", null);

        waitForThread(sleep_time_long);

        verify(observer, times(1)).setTransferListener(any(TransferListener.class));
    }

    @Test
    public void test_listenerOnProgress() {
        SimpleAmazonLogs.getTransferListener(0, null, null).onProgressChanged(0,0,0); // Do nothing as this is an empty method
    }

    @Test
    public void test_listenerOnError() {
        final Exception ex = mock(Exception.class);
        doNothing().when(ex).printStackTrace();

        File file = mock(File.class);
        doReturn(true).when(file).delete();

        SimpleAmazonLogCallback callback = new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total_uploaded) {

            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {
                Assert.assertEquals(ex, exception);
                Assert.assertEquals(1, successful_uploads);
                Assert.assertEquals(1, unsuccessful_uploads);
            }
        };

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 0;

        SimpleAmazonLogs.getTransferListener(3, file, callback).onError(0, ex);

        Assert.assertEquals(SimpleAmazonLogs.unsuccessful_calls, 1);
        Assert.assertEquals(SimpleAmazonLogs.successful_calls, 0);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 1;

        SimpleAmazonLogs.getTransferListener(2, file, callback).onError(0, ex);

        verify(ex, times(2)).printStackTrace();

        SystemClock.sleep(100);
    }

    @Test
    public void test_listenerOnStateChangeCompleted() {
        TransferState ts = TransferState.COMPLETED;

        SimpleAmazonLogCallback callback = new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total_uploaded) {
                Assert.assertEquals(total_uploaded, 2);
            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {

            }
        };

        String today_title = SimpleAmazonLogs.date_format_title.format(new Date(System.currentTimeMillis()));
        File file1 = mock(File.class);
        doReturn(today_title).when(file1).getName();

        File file2 = mock(File.class);
        doReturn("").when(file2).getName();

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 0;

        SimpleAmazonLogs.getTransferListener(2, file1, callback).onStateChanged(0, ts);
        waitForThread(sleep_time_short);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 1;

        SimpleAmazonLogs.getTransferListener(2, file2, callback).onStateChanged(0, ts);
        waitForThread(sleep_time_short);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 2;

        verify(file1, times(0)).delete();
        verify(file2, times(1)).delete();

        SystemClock.sleep(100);
    }

    @Test
    public void test_listenerOnStateChangeCanceledFailed() {
        TransferState ts1 = TransferState.CANCELED;
        TransferState ts2 = TransferState.FAILED;

        File file = mock(File.class);
        doReturn(true).when(file).delete();

        SimpleAmazonLogCallback callback = new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total_uploaded) {

            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {
                Assert.assertEquals("At least one file failed to upload or was canceled", exception.getMessage());
                Assert.assertEquals(unsuccessful_uploads, 2);
                Assert.assertEquals(successful_uploads, 0);
            }
        };

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 0;

        SimpleAmazonLogs.getTransferListener(2, file, callback).onStateChanged(0, ts1);

        SimpleAmazonLogs.unsuccessful_calls = 1;
        SimpleAmazonLogs.successful_calls = 0;

        SimpleAmazonLogs.getTransferListener(2, file, callback).onStateChanged(0, ts2);

        SimpleAmazonLogs.unsuccessful_calls = 2;
        SimpleAmazonLogs.successful_calls = 0;

        SystemClock.sleep(100);
    }

    @Test
    public void test_newStorageInstance() {
        Assert.assertEquals(SimpleAmazonLogsHelper.newStorageInstance(mock(Context.class)).getClass(), Storage.class);
    }
}
