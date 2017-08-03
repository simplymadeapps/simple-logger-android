package com.simplymadeapps.simple_logger_android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import fr.xebia.android.freezer.QueryBuilder;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PreferenceManager.class, TransferHelper.class, TransferObserver.class })
public class TestLogs {

    @Before
    public void setup() {
        RecordedLogEntityManager rlem = mock(RecordedLogEntityManager.class);
        SharedPreferences preferences = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        doReturn(editor).when(editor).putInt(SimpleAmazonLogs.KEEP_IN_STORAGE_KEY, 7);
        doReturn(true).when(editor).commit();
        SimpleAmazonLogs.daysToKeepInStorage = 7;
        SimpleAmazonLogs.rlem = rlem;
        SimpleAmazonLogs.editor = editor;
        preferences = preferences;
        RecordedLogQueryBuilder rlqb = mock(RecordedLogQueryBuilder.class);
        QueryBuilder.DateSelector ds = mock(QueryBuilder.DateSelector.class);
        doReturn(rlqb).when(rlem).select();
        doReturn(ds).when(rlqb).recordDate();
        doReturn(rlqb).when(ds).before(any(Date.class));
        doReturn(rlqb).when(ds).between(any(Date.class), any(Date.class));
        List<RecordedLog> logs = new ArrayList<>();
        logs.add(new RecordedLog("Test Input Log 1", Calendar.getInstance().getTime()));
        logs.add(new RecordedLog("Test Input Log 2", Calendar.getInstance().getTime()));
        doReturn(logs).when(rlqb).asList();
    }

    @Test
    public void test_init() {
        Application application = mock(Application.class);

        SharedPreferences preferences = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);

        mockStatic(PreferenceManager.class);
        doReturn(preferences).when(PreferenceManager.class);
        PreferenceManager.getDefaultSharedPreferences(any(Context.class));
        doReturn(editor).when(preferences).edit();
        doReturn(7).when(preferences).getInt(SimpleAmazonLogs.KEEP_IN_STORAGE_KEY, 7);

        SimpleAmazonLogs.init(application);

        Assert.assertNotNull(SimpleAmazonLogs.instance);
        Assert.assertNotNull(SimpleAmazonLogs.rlem);
        Assert.assertNotNull(preferences);
        Assert.assertNotNull(SimpleAmazonLogs.editor);
        Assert.assertEquals(SimpleAmazonLogs.daysToKeepInStorage, 7);
    }

    @Test
    public void test_addLog() {
        SimpleAmazonLogs.addLog("Test");
        verify(SimpleAmazonLogs.rlem, times(1)).add(any(RecordedLog.class));
        verify(SimpleAmazonLogs.rlem, times(1)).delete(any(List.class));
    }

    @Test
    public void test_getAllLogs() {
        Assert.assertEquals(SimpleAmazonLogs.getAllLogs().size(), 2);
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
        verify(SimpleAmazonLogs.rlem, times(1)).deleteAll();
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
        Assert.assertEquals(SimpleAmazonLogs.getLogsFromSpecificDay(1).size(), 2);
    }

    @Test
    public void test_createLogTextFile() throws FileNotFoundException {
        List<RecordedLog> logs = new ArrayList<>();
        logs.add(new RecordedLog("Test Input Log", Calendar.getInstance().getTime()));
        File file = SimpleAmazonLogs.createLogTextFile(logs);
        Assert.assertNotNull(file);

        Scanner scanner = new Scanner(file);
        String scannerLine = scanner.nextLine();
        boolean hasText = scannerLine.contains("Test Input Log");
        Assert.assertTrue(hasText);

        List<RecordedLog> logs2 = new ArrayList<>();
        logs2.add(new RecordedLog(null, null));
        File file2 = SimpleAmazonLogs.createLogTextFile(logs2);
        Assert.assertNull(file2);
    }

    @Test
    public void test_getListOfListOfLogsToUpload() {
        List<List<RecordedLog>> list_of_lists = SimpleAmazonLogs.getListOfListOfLogsToUpload();
        Assert.assertEquals(list_of_lists.size(), 7);
        Assert.assertEquals(list_of_lists.get(0).size(), 2);
        Assert.assertEquals(list_of_lists.get(1).size(), 2);
        Assert.assertEquals(list_of_lists.get(2).size(), 2);
        Assert.assertEquals(list_of_lists.get(3).size(), 2);
        Assert.assertEquals(list_of_lists.get(4).size(), 2);
        Assert.assertEquals(list_of_lists.get(5).size(), 2);
        Assert.assertEquals(list_of_lists.get(6).size(), 2);
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

        SystemClock.sleep(1000);
    }

    @Test
    public void test_uploadLogsToAmazonSuccess() {
        SimpleAmazonLogs.access_token = "1";
        SimpleAmazonLogs.secret_token = "2";
        SimpleAmazonLogs.bucket = "3";
        SimpleAmazonLogs.region = Regions.US_EAST_1;
        SimpleAmazonLogs.daysToKeepInStorage = 1;

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

        verify(observer, times(1)).setTransferListener(any(TransferListener.class));
    }
}
