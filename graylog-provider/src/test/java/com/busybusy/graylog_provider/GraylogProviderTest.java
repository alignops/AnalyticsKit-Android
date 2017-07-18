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
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.busybusy.analyticskit_android.CommonEvents;
import com.busybusy.analyticskit_android.ContentViewEvent;
import com.busybusy.analyticskit_android.ErrorEvent;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.EmptyStackException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * Tests the {@link GraylogProvider} class.
 *
 * @author John Hunt on 6/28/17.
 */
public class GraylogProviderTest
{
    GraylogProvider provider;
    MockWebServer mockServer = new MockWebServer();
    OkHttpClient  httpClient = new OkHttpClient.Builder().build();
    GraylogResponseListener callbackListener;

    int     testEventHashCode;
    boolean logEventCalled;
    String  loggedEventName;

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

                logEventCalled = true;
                loggedEventName = response.eventName();
                testEventHashCode = response.eventHashCode();
                lock.countDown();
            }
        };

        mockServer.enqueue(new MockResponse().setResponseCode(202).setStatus("Accepted"));
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
    }

    @Test
    public void testSetAndGetPriorityFilter()
    {
        AnalyticsKitProvider.PriorityFilter filter = new AnalyticsKitProvider.PriorityFilter()
        {
            @Override
            public boolean shouldLog(int priorityLevel)
            {
                return false;
            }
        };

        provider.setPriorityFilter(filter);

        assertThat(provider.getPriorityFilter()).isEqualTo(filter);
    }

    @Test
    public void test_priorityFiltering_default()
    {
        AnalyticsEvent event = new AnalyticsEvent("A Test Event")
                .setPriority(10)
                .send();
        assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();

        event.setPriority(-9)
             .send();
        assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
    }

    @Test
    public void test_priorityFiltering_custom()
    {
        provider.setPriorityFilter(new AnalyticsKitProvider.PriorityFilter()
        {
            @Override
            public boolean shouldLog(int priorityLevel)
            {
                return priorityLevel < 10;
            }
        });

        AnalyticsEvent event = new AnalyticsEvent("A Test Event")
                .setPriority(10)
                .send();

        assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isFalse();

        event.setPriority(9)
             .send();

        assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
    }

    @Test
    public void testSendEvent_unTimed_noParams() throws InterruptedException
    {
        AnalyticsEvent event = new AnalyticsEvent("Graylog Test Run No Params");
        provider.sendEvent(event);

        lock.await(50L, TimeUnit.MILLISECONDS);

        assertThat(logEventCalled).isTrue();
        assertThat(testEventHashCode).isEqualTo(event.hashCode());
        assertThat(loggedEventName).isEqualTo("Graylog Test Run No Params");
    }

    @Test
    public void testSendEvent_unTimed_withParams() throws InterruptedException
    {
        AnalyticsEvent event = new AnalyticsEvent("Graylog Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again");

        provider.sendEvent(event);

        lock.await(50L, TimeUnit.MILLISECONDS);

        assertThat(loggedEventName).isEqualTo("Graylog Event With Params Run");
        assertThat(logEventCalled).isTrue();
        assertThat(testEventHashCode).isEqualTo(event.hashCode());
    }

    @Test
    public void testLogContentViewEvent() throws InterruptedException
    {
        ContentViewEvent event = new ContentViewEvent("Test page 7");
        provider.sendEvent(event);

        lock.await(50L, TimeUnit.MILLISECONDS);

        assertThat(loggedEventName).isEqualTo(CommonEvents.CONTENT_VIEW);
        assertThat(logEventCalled).isTrue();
        assertThat(testEventHashCode).isEqualTo(event.hashCode());
    }

    @Test
    public void testLogErrorEvent() throws InterruptedException
    {
        ErrorEvent event = new ErrorEvent()
                .setMessage("something bad happened")
                .setException(new EmptyStackException());
        provider.sendEvent(event);

        lock.await(50L, TimeUnit.MILLISECONDS);

        assertThat(loggedEventName).isEqualTo(CommonEvents.ERROR);
        assertThat(logEventCalled).isTrue();
        assertThat(testEventHashCode).isEqualTo(event.hashCode());
    }

    @Test
    public void testSendEvent_timed_noParams()
    {
        AnalyticsEvent event = new AnalyticsEvent("Graylog Timed Event")
                .setTimed(true);

        provider.sendEvent(event);
        assertFalse(logEventCalled);
    }

    @Test
    public void testSendEvent_timed_withParams()
    {
        AnalyticsEvent event = new AnalyticsEvent("Graylog Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again");

        provider.sendEvent(event);
        assertFalse(logEventCalled);
    }

    @Test
    public void testEndTimedEvent_Valid() throws InterruptedException
    {
        AnalyticsEvent event = new AnalyticsEvent("Graylog Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again");

        provider.sendEvent(event);
        assertFalse(logEventCalled);

        try
        {
            Thread.sleep(50);
        }
        catch (InterruptedException e)
        {
            // don't do anything, this is just a test that needs some delay
        }

        provider.endTimedEvent(event);

        lock.await(50L, TimeUnit.MILLISECONDS);


        assertThat(loggedEventName).isEqualTo("Graylog Timed Event With Parameters");
        assertThat(logEventCalled).isTrue();
        assertThat(testEventHashCode).isEqualTo(event.hashCode());
    }

    @Test
    public void test_endTimedEvent_WillThrow()
    {
        boolean didThrow = false;
        AnalyticsEvent event = new AnalyticsEvent("Graylog Timed Event With Parameters")
                .setTimed(true);

        try
        {
            provider.endTimedEvent(event); // attempting to end a timed event that was not started should throw an exception
        }
        catch (IllegalStateException e)
        {
            didThrow = true;
        }

        assertThat(didThrow).isTrue();
    }
}
