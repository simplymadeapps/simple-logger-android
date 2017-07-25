package com.simplymadeapps.simple_logger_android;

import android.content.SharedPreferences;

import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.simplymadeapps.simple_logger_android.SimpleAmazonLogs.PREF_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Gson.class })
public class TestLogs {

    @Test
    public void testRecordedLogAWeekOld() {
        long system_time = System.currentTimeMillis();
        RecordedLog log = new RecordedLog("text","date",system_time-604803000); // This time is 1 week and 3 seconds old
        Assert.assertTrue(log.isLogAWeekOld());
    }

    @Test
    public void testRecordedLogNotAWeekOld() {
        long system_time = System.currentTimeMillis();
        RecordedLog log = new RecordedLog("text","date",system_time-604700000); // This time is 6 days old
        Assert.assertFalse(log.isLogAWeekOld());
    }

    @Test
    public void testAddLog() {
        SimpleAmazonLogs calling_class = spy(new SimpleAmazonLogs());

        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        Gson gson = PowerMockito.mock(Gson.class);

        doReturn(new ArrayList<RecordedLog>()).when(calling_class).getLogs();
        doReturn(editor).when(editor).putString(PREF_KEY, "json");
        doReturn(true).when(editor).commit();
        doReturn("json").when(gson).toJson(any(List.class));

        calling_class.gson = gson;
        calling_class.editor = editor;
        calling_class.addLog("testing logs");

        verify(gson, times(1)).toJson(any(List.class));
        verify(editor, times(1)).putString(PREF_KEY, "json");
        verify(editor, times(1)).commit();
    }

    @Test
    public void getLogsEmpty() {
        SimpleAmazonLogs calling_class = spy(new SimpleAmazonLogs());

        SharedPreferences prefs = mock(SharedPreferences.class);
        Gson gson = PowerMockito.mock(Gson.class);

        doReturn("").when(prefs).getString(PREF_KEY, "");

        calling_class.preferences = prefs;
        calling_class.gson = gson;

        Assert.assertEquals(0, calling_class.getLogs().size());
    }

    @Test
    public void getLogsMultiple() {
        SimpleAmazonLogs calling_class = spy(new SimpleAmazonLogs());

        SharedPreferences prefs = mock(SharedPreferences.class);
        Gson gson = PowerMockito.mock(Gson.class);

        doReturn("not_empty").when(prefs).getString(PREF_KEY, "");
        doReturn(null).when(gson).fromJson(eq("not_empty"), any(Type.class));
        doReturn(new ArrayList<RecordedLog>()).when(calling_class).pruneLogs(null);

        calling_class.preferences = prefs;
        calling_class.gson = gson;

        Assert.assertEquals(0, calling_class.getLogs().size());
    }
}
