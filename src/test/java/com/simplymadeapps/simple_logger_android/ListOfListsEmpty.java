package com.simplymadeapps.simple_logger_android;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyLong;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Created by stephenruda on 8/8/17.
 */

public class ListOfListsEmpty {

    @Test
    public void testListOfListOfLogsNone() {
        RecordedLogDatabase database = mock(RecordedLogDatabase.class);
        RecordedLogDao dao = mock(RecordedLogDao.class);
        doReturn(dao).when(database).recordedLogDao();
        List<RecordedLog> logs = new ArrayList<>();
        doReturn(logs).when(dao).getLogsWithinTimeRange(anyLong(), anyLong());

        SimpleAmazonLogs.daysToKeepInStorage = 7;
        SimpleAmazonLogs.database = database;

        List<List<RecordedLog>> list_of_lists = SimpleAmazonLogs.getListOfListOfLogsToUpload();
        Assert.assertEquals(list_of_lists.size(), 0);
    }
}
