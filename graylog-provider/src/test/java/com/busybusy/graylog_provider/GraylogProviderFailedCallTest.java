/*
 * Copyright 2017 busybusy, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.busybusy.graylog_provider;

import android.support.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author John Hunt on 6/29/17.
 */
public class GraylogProviderFailedCallTest
{
    GraylogProvider provider;
    MockWebServer mockServer = new MockWebServer();
    OkHttpClient  httpClient = new OkHttpClient.Builder().build();
    GraylogResponseListener callbackListener;

    int     testEventHashCode;
    boolean logEventCalled;
    String  loggedEventName;
    int     httpResponseCode;
    String  httpStatusMessage;

    private CountDownLatch lock;

    @Before
    public void setup()
    {
        lock = new CountDownLatch(1);
        callbackListener = new GraylogResponseListener()
        {
            @Override
            public void onGraylogResponse(@NonNull GraylogResponse response)
            {
                testEventHashCode = response.eventHashCode();
                loggedEventName = response.eventName();
                logEventCalled = true;
                httpResponseCode = response.code();
                httpStatusMessage = response.message();
                lock.countDown();
            }
        };

        mockServer.enqueue(new MockResponse().setResponseCode(420).setStatus("An error occurred communicating with the Graylog server"));
        try
        {
            mockServer.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        provider = new GraylogProvider(httpClient, "http://" + mockServer.getHostName() + ":" + mockServer.getPort(), "unit-test-android");
        provider.setCallbackHandler(callbackListener);

        logEventCalled = false;
        testEventHashCode = -1;
        loggedEventName = null;
        httpResponseCode = -1;
        httpStatusMessage = "Not Sent";
    }

    @After
    public void tearDown()
    {
        try
        {
            mockServer.shutdown();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendEvent_unTimed_withParams() throws InterruptedException
    {
        AnalyticsEvent event = new AnalyticsEvent("Graylog Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again");

        provider.sendEvent(event);

        lock.await(50, TimeUnit.MILLISECONDS);

        assertThat(loggedEventName).isEqualTo("Graylog Event With Params Run");
        assertThat(logEventCalled).isTrue();
        assertThat(testEventHashCode).isEqualTo(event.hashCode());

        assertThat(httpResponseCode).isEqualTo(420);
        assertThat(httpStatusMessage).isEqualTo("An error occurred communicating with the Graylog server");
    }
}
