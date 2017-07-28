package com.simplymadeapps.simple_logger_android;

import com.google.gson.Gson;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Gson.class })
public class TestLogs {

//    @Test
//    public void testRecordedLogAWeekOld() {
//        long system_time = System.currentTimeMillis();
//        RecordedLog log = new RecordedLog("text","date",system_time-604803000); // This time is 1 week and 3 seconds old
//        Assert.assertTrue(log.isLogAWeekOld());
//    }
//
//    @Test
//    public void testRecordedLogNotAWeekOld() {
//        long system_time = System.currentTimeMillis();
//        RecordedLog log = new RecordedLog("text","date",system_time-604700000); // This time is 6 days old
//        Assert.assertFalse(log.isLogAWeekOld());
//    }
//
//    @Test
//    public void testAddLog() {
//        SimpleAmazonLogs calling_class = spy(new SimpleAmazonLogs());
//
//        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
//        Gson gson = PowerMockito.mock(Gson.class);
//
//        doReturn(new ArrayList<RecordedLog>()).when(calling_class).getLogs();
//        doReturn(editor).when(editor).putString(PREF_KEY, "json");
//        doReturn(true).when(editor).commit();
//        doReturn("json").when(gson).toJson(any(List.class));
//
//        calling_class.gson = gson;
//        calling_class.editor = editor;
//        calling_class.addLog("testing logs");
//
//        verify(gson, times(1)).toJson(any(List.class));
//        verify(editor, times(1)).putString(PREF_KEY, "json");
//        verify(editor, times(1)).commit();
//    }
//
//    @Test
//    public void getLogsEmpty() {
//        SimpleAmazonLogs calling_class = spy(new SimpleAmazonLogs());
//
//        SharedPreferences prefs = mock(SharedPreferences.class);
//        Gson gson = PowerMockito.mock(Gson.class);
//
//        doReturn("").when(prefs).getString(PREF_KEY, "");
//
//        calling_class.preferences = prefs;
//        calling_class.gson = gson;
//
//        Assert.assertEquals(0, calling_class.getLogs().size());
//    }
//
//    @Test
//    public void getLogsMultiple() {
//        SimpleAmazonLogs calling_class = spy(new SimpleAmazonLogs());
//
//        SharedPreferences prefs = mock(SharedPreferences.class);
//        Gson gson = PowerMockito.mock(Gson.class);
//
//        doReturn("not_empty").when(prefs).getString(PREF_KEY, "");
//        doReturn(null).when(gson).fromJson(eq("not_empty"), any(Type.class));
//        doReturn(new ArrayList<RecordedLog>()).when(calling_class).pruneLogs(null);
//
//        calling_class.preferences = prefs;
//        calling_class.gson = gson;
//
//        Assert.assertEquals(0, calling_class.getLogs().size());
//    }
//
//    @Test
//    public void getDateString() {
//        SimpleAmazonLogs calling_class = new SimpleAmazonLogs();
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
//        Date currentDateTime = Calendar.getInstance().getTime();
//        String match = sdf.format(currentDateTime);
//
//        Assert.assertEquals(match, calling_class.currentTimeString());
//    }
//
//    @Test
//    public void testPruneLogs() {
//        SimpleAmazonLogs calling_class = new SimpleAmazonLogs();
//
//        List<RecordedLog> logs = new ArrayList<>();
//        logs.add(new RecordedLog("string0","date",System.currentTimeMillis() - 700000000)); // This log is over a week old
//        logs.add(new RecordedLog("string1","date",System.currentTimeMillis() - 604950000)); // This log is over a week old
//        logs.add(new RecordedLog("string2","date",System.currentTimeMillis() - 604900000)); // This log is over a week old
//        logs.add(new RecordedLog("string3","date",System.currentTimeMillis() - 304900000)); // This log is not a week old
//        logs.add(new RecordedLog("string4","date",System.currentTimeMillis() - 104900000)); // This log is not a week old
//        logs.add(new RecordedLog("string5","date",System.currentTimeMillis())); // This log is not a week old
//
//        List<RecordedLog> final_list = calling_class.pruneLogs(logs);
//        Assert.assertEquals(3, final_list.size());
//        Assert.assertEquals("string3", final_list.get(0).log);
//        Assert.assertEquals("string4", final_list.get(1).log);
//        Assert.assertEquals("string5", final_list.get(2).log);
//    }
}
