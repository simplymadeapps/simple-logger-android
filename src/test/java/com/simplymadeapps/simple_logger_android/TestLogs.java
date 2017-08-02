package com.simplymadeapps.simple_logger_android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.List;

import fr.xebia.android.freezer.QueryBuilder;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PreferenceManager.class })
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
        SimpleAmazonLogs.preferences = preferences;
        RecordedLogQueryBuilder rlqb = mock(RecordedLogQueryBuilder.class);
        QueryBuilder.DateSelector ds = mock(QueryBuilder.DateSelector.class);
        doReturn(rlqb).when(rlem).select();
        doReturn(ds).when(rlqb).recordDate();
        doReturn(rlqb).when(ds).before(any(Date.class));
        doReturn(rlqb).when(ds).between(any(Date.class), any(Date.class));
        doReturn(null).when(rlqb).asList();
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
        Assert.assertNotNull(SimpleAmazonLogs.preferences);
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
        Assert.assertNull(SimpleAmazonLogs.getAllLogs());
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
        Assert.assertEquals(SimpleAmazonLogs.access_token, "");
        Assert.assertEquals(SimpleAmazonLogs.bucket, "");
        Assert.assertEquals(SimpleAmazonLogs.secret_token, "");
        Assert.assertNull(SimpleAmazonLogs.region);


        SimpleAmazonLogs.setAmazonCredentials("1","2","3",null);

        Assert.assertEquals(SimpleAmazonLogs.access_token, "1");
        Assert.assertEquals(SimpleAmazonLogs.bucket, "3");
        Assert.assertEquals(SimpleAmazonLogs.secret_token, "2");
    }

    @Test
    public void test_getLogsFromSpecificDay() {
        Assert.assertNull(SimpleAmazonLogs.getLogsFromSpecificDay(1));
    }
}
