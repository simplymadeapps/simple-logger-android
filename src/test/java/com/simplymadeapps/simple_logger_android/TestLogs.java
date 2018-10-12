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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PreferenceManager.class, TransferHelper.class, TransferObserver.class, Room.class, AmazonWebServiceClient.class })
public class TestLogs {

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    RecordedLogDao dao;

    @Before
    public void setup() {
        RecordedLogDatabase database = mock(RecordedLogDatabase.class);
        dao = mock(RecordedLogDao.class);
        doReturn(dao).when(database).recordedLogDao();

        preferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        SimpleAmazonLogs.daysToKeepInStorage = 7;
        SimpleAmazonLogs.database = database;
        SimpleAmazonLogs.editor = editor;
        SimpleAmazonLogs.preferences = preferences;
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

        mockStatic(Room.class);
        mockStatic(PreferenceManager.class);

        doReturn(mock(Context.class)).when(application).getApplicationContext();
        when(PreferenceManager.getDefaultSharedPreferences(any(Context.class))).thenReturn(preferences);
        doReturn(editor).when(preferences).edit();
        doReturn(7).when(preferences).getInt(SimpleAmazonLogs.KEEP_IN_STORAGE_KEY, 7);
        RoomDatabase.Builder<RecordedLogDatabase> builder = mock(RoomDatabase.Builder.class);
        when(Room.databaseBuilder(application, RecordedLogDatabase.class, "recorded_log_database")).thenReturn(builder);
        RecordedLogDatabase database = mock(RecordedLogDatabase.class);
        doReturn(database).when(builder).build();

        SimpleAmazonLogs.init(application);

        Assert.assertNotNull(SimpleAmazonLogs.instance);
        Assert.assertEquals(SimpleAmazonLogs.database, database);
        Assert.assertEquals(SimpleAmazonLogs.preferences, preferences);
        Assert.assertEquals(SimpleAmazonLogs.editor, editor);
        Assert.assertEquals(SimpleAmazonLogs.daysToKeepInStorage, 7);

        // Test init when it is not null
        SimpleAmazonLogs.init(application);
    }

    @Test
    public void test_addLog_withClear() {
        SimpleAmazonLogs.last_clear_old_logs_checked = 0;
        Assert.assertEquals(SimpleAmazonLogs.last_clear_old_logs_checked, 0);
        SimpleAmazonLogs.addLog("Test");
        waitForThread(100);
        verify(dao, times(1)).deleteLogsOlderThanTime(anyLong());
        verify(dao, times(1)).insert(any(RecordedLog.class));
        Assert.assertNotSame(SimpleAmazonLogs.last_clear_old_logs_checked, 0);
    }

    @Test
    public void test_addLog_withoutClear() {
        SimpleAmazonLogs.last_clear_old_logs_checked = Long.MAX_VALUE;
        Assert.assertEquals(SimpleAmazonLogs.last_clear_old_logs_checked, Long.MAX_VALUE);
        SimpleAmazonLogs.addLog("Test");
        waitForThread(100);
        verify(dao, times(0)).deleteLogsOlderThanTime(anyLong());
        verify(dao, times(1)).insert(any(RecordedLog.class));
        Assert.assertNotSame(SimpleAmazonLogs.last_clear_old_logs_checked, Long.MAX_VALUE);
    }

    @Test
    public void test_getAllLogs() {
        RecordedLogCallbacks callback = mock(RecordedLogCallbacks.class);
        SimpleAmazonLogs.getAllLogs(callback);
        waitForThread(100);
        verify(dao, times(1)).getAll();
        verify(callback, times(1)).onLogsReady(any(List.class));
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
        waitForThread(100);
        verify(dao, times(1)).deleteAll();
    }

    @Test
    public void test_setAmazonCredentials() {
        SimpleAmazonLogs.setAmazonCredentials("1","2","3",null);
        Assert.assertEquals(SimpleAmazonLogs.access_token, "1");
        Assert.assertEquals(SimpleAmazonLogs.bucket, "3");
        Assert.assertEquals(SimpleAmazonLogs.secret_token, "2");
    }

    @Test
    public void test_getLogsFromSpecificDay() {
        Assert.assertEquals(SimpleAmazonLogs.getLogsFromSpecificDay(1).size(), 0);
        waitForThread(100);
        verify(dao, times(1)).getLogsWithinTimeRange(anyLong(), anyLong());
    }

    @Test
    public void test_createLogTextFile() throws FileNotFoundException {
        List<RecordedLog> logs = new ArrayList<>();
        RecordedLog log1 = new RecordedLog();
        log1.setLog("Test Input Log");
        log1.setDate(0);
        logs.add(log1);
        File file = SimpleAmazonLogs.createLogTextFile(logs);
        Assert.assertNotNull(file);

        Scanner scanner = new Scanner(file);
        String scannerLine = scanner.nextLine();
        boolean hasText = scannerLine.contains("Test Input Log");
        Assert.assertTrue(hasText);

        List<RecordedLog> logs2 = new ArrayList<>();
        logs2.add(null);
        File file2 = SimpleAmazonLogs.createLogTextFile(logs2);
        Assert.assertNull(file2);
    }

    @Test
    public void test_getListOfListOfLogsToUpload() {
        List<RecordedLog> logs = new ArrayList<>();
        logs.add(new RecordedLog());
        doReturn(logs).when(dao).getLogsWithinTimeRange(anyLong(), anyLong());
        List<List<RecordedLog>> list_of_lists = SimpleAmazonLogs.getListOfListOfLogsToUpload();
        waitForThread(100);
        Assert.assertEquals(list_of_lists.size(), 7);
        Assert.assertEquals(list_of_lists.get(0).size(), 1);
        Assert.assertEquals(list_of_lists.get(1).size(), 1);
        Assert.assertEquals(list_of_lists.get(2).size(), 1);
        Assert.assertEquals(list_of_lists.get(3).size(), 1);
        Assert.assertEquals(list_of_lists.get(4).size(), 1);
        Assert.assertEquals(list_of_lists.get(5).size(), 1);
        Assert.assertEquals(list_of_lists.get(6).size(), 1);
        verify(dao, times(7)).getLogsWithinTimeRange(anyLong(), anyLong());
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

        SystemClock.sleep(1000);
    }

    @Test
    public void test_zero_logs() {
        SimpleAmazonLogs.access_token = "1";
        SimpleAmazonLogs.secret_token = "2";
        SimpleAmazonLogs.bucket = "3";
        SimpleAmazonLogs.region = Regions.US_EAST_1;
        SimpleAmazonLogs.daysToKeepInStorage = 0;

        SimpleAmazonLogs.uploadLogsToAmazon("directory", new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total) {
                Assert.assertEquals(total, 0);
            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {

            }
        });

        waitForThread(1000);

        SystemClock.sleep(1000);
    }

    @Test
    public void test_uploadLogsToAmazonSuccess() {
        SimpleAmazonLogs.access_token = "1";
        SimpleAmazonLogs.secret_token = "2";
        SimpleAmazonLogs.bucket = "3";
        SimpleAmazonLogs.region = Regions.US_EAST_1;
        SimpleAmazonLogs.daysToKeepInStorage = 1;

        List<RecordedLog> logs = new ArrayList<>();
        logs.add(new RecordedLog());
        doReturn(logs).when(dao).getLogsWithinTimeRange(anyLong(), anyLong());

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

        waitForThread(1000);

        verify(observer, times(1)).setTransferListener(any(TransferListener.class));
    }

    @Test
    public void test_listenerOnProgress() {
        SimpleAmazonLogs.getTransferListener(0, null, null, null).onProgressChanged(0,0,0); // Do nothing as this is an empty method
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

        SimpleAmazonLogs.getTransferListener(3, null, file, callback).onError(0, ex);

        Assert.assertEquals(SimpleAmazonLogs.unsuccessful_calls, 1);
        Assert.assertEquals(SimpleAmazonLogs.successful_calls, 0);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 1;

        SimpleAmazonLogs.getTransferListener(2, null, file, callback).onError(0, ex);

        verify(file, times(2)).delete();
        verify(ex, times(2)).printStackTrace();

        SystemClock.sleep(1000);
    }

    @Test
    public void test_listenerOnStateChangeCompleted() {
        TransferState ts = TransferState.COMPLETED;

        File file = mock(File.class);
        doReturn(true).when(file).delete();

        SimpleAmazonLogCallback callback = new SimpleAmazonLogCallback() {
            @Override
            public void onSuccess(int total_uploaded) {
                Assert.assertEquals(total_uploaded, 2);
            }

            @Override
            public void onFailure(Exception exception, int successful_uploads, int unsuccessful_uploads) {

            }
        };

        List<RecordedLog> list_of_logs1 = new ArrayList<>();
        RecordedLog log1 = new RecordedLog();
        log1.setDate(System.currentTimeMillis());
        list_of_logs1.add(log1);

        List<RecordedLog> list_of_logs2 = new ArrayList<>();
        RecordedLog log2 = new RecordedLog();
        log2.setDate(System.currentTimeMillis()-SimpleAmazonLogs.HRS_24_IN_MS);
        list_of_logs2.add(log2);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 0;

        SimpleAmazonLogs.getTransferListener(2, list_of_logs1, file, callback).onStateChanged(0, ts);
        waitForThread(100);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 1;

        SimpleAmazonLogs.getTransferListener(2, list_of_logs2, file, callback).onStateChanged(0, ts);
        waitForThread(100);

        SimpleAmazonLogs.unsuccessful_calls = 0;
        SimpleAmazonLogs.successful_calls = 2;

        verify(file, times(2)).delete();
        verify(dao, times(1)).delete(any(List.class));

        SystemClock.sleep(1000);
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

        SimpleAmazonLogs.getTransferListener(2, null, file, callback).onStateChanged(0, ts1);

        SimpleAmazonLogs.unsuccessful_calls = 1;
        SimpleAmazonLogs.successful_calls = 0;

        SimpleAmazonLogs.getTransferListener(2, null, file, callback).onStateChanged(0, ts2);

        SimpleAmazonLogs.unsuccessful_calls = 2;
        SimpleAmazonLogs.successful_calls = 0;

        verify(file, times(2)).delete();

        SystemClock.sleep(1000);
    }
}
