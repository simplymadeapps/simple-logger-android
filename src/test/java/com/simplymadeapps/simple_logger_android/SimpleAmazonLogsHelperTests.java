package com.simplymadeapps.simple_logger_android;

import android.content.Context;

import com.snatik.storage.Storage;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(Enclosed.class)
public class SimpleAmazonLogsHelperTests {

    public static class ConstructorTests {

        @Test
        public void test_helperConstructor() {
            SimpleAmazonLogsHelper helpers = new SimpleAmazonLogsHelper();
            Assert.assertEquals(helpers.getClass(), SimpleAmazonLogsHelper.class);
        }
    }

    public static class NewStorageInstanceTests {

        @Test
        public void test_newStorageInstance() {
            Assert.assertEquals(SimpleAmazonLogsHelper.newStorageInstance(mock(Context.class)).getClass(), Storage.class);
        }
    }
}
