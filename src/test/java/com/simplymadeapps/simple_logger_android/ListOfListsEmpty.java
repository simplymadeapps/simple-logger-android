package com.simplymadeapps.simple_logger_android;

import android.content.SharedPreferences;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.xebia.android.freezer.QueryBuilder;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Created by stephenruda on 8/8/17.
 */

public class ListOfListsEmpty {

    @Test
    public void testListOfListOfLogsNone() {
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
        List<RecordedLog> logs = new ArrayList<>();
        doReturn(logs).when(rlqb).asList();

        List<List<RecordedLog>> list_of_lists = SimpleAmazonLogs.getListOfListOfLogsToUpload();
        Assert.assertEquals(list_of_lists.size(), 0);
    }
}
