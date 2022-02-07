/*
 * Copyright 2017 - 2022 busybusy, Inc.
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
package com.busybusy.graylog_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.CommonEvents
import com.busybusy.analyticskit_android.ContentViewEvent
import com.busybusy.analyticskit_android.ErrorEvent
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests the [GraylogProvider] class.
 *
 * @author John Hunt on 6/28/17.
 */
class GraylogProviderTest {
    private lateinit var provider: GraylogProvider
    private val mockServer = MockWebServer()
    private val httpClient = OkHttpClient.Builder().build()
    private lateinit var callbackListener: GraylogResponseListener
    private var testEventHashCode = 0
    private var logEventCalled = false
    private var loggedEventName: String? = null
    private lateinit var lock: CountDownLatch

    @Before
    fun setup() {
        lock = CountDownLatch(1)
        callbackListener = object : GraylogResponseListener {
            override fun onGraylogResponse(response: GraylogResponse) {
                logEventCalled = true
                loggedEventName = response.eventName
                testEventHashCode = response.eventHashCode
                lock.countDown()
            }

        }
        mockServer.enqueue(MockResponse().setResponseCode(202).setStatus("Accepted"))
        try {
            mockServer.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        provider = GraylogProvider(
            client = httpClient,
            graylogInputUrl = "http://${mockServer.hostName}:${mockServer.port}",
            graylogHostName = "unit-test-android",
        )
        provider.setCallbackHandler(callbackListener)
        logEventCalled = false
        testEventHashCode = -1
        loggedEventName = null
    }

    @Test
    fun testSetAndGetPriorityFilter() {
        val filter = PriorityFilter { false }
        val filteringProvider = GraylogProvider(
            client = httpClient,
            graylogInputUrl = "http://${mockServer.hostName}:${mockServer.port}",
            graylogHostName = "unit-test-android",
            priorityFilter = filter
        )
        assertThat(filteringProvider.priorityFilter).isEqualTo(filter)
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("A Test Event")
                .setPriority(10)
                .send()
        assertThat(provider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
        event.setPriority(-9)
                .send()
        assertThat(provider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun test_priorityFiltering_custom() {
        val filter = PriorityFilter { priorityLevel -> priorityLevel < 10 }
        val filteringProvider = GraylogProvider(
            client = httpClient,
            graylogInputUrl = "http://${mockServer.hostName}:${mockServer.port}",
            graylogHostName = "unit-test-android",
            priorityFilter = filter
        )
        val event = AnalyticsEvent("A Test Event")
                .setPriority(10)
                .send()
        assertThat(filteringProvider.priorityFilter.shouldLog(event.priority)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(filteringProvider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSendEvent_unTimed_noParams() {
        val event = AnalyticsEvent("Graylog Test Run No Params")
        provider.sendEvent(event)
        lock.await(50L, TimeUnit.MILLISECONDS)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventHashCode).isEqualTo(event.hashCode())
        assertThat(loggedEventName).isEqualTo("Graylog Test Run No Params")
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Graylog Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        lock.await(50L, TimeUnit.MILLISECONDS)
        assertThat(loggedEventName).isEqualTo("Graylog Event With Params Run")
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventHashCode).isEqualTo(event.hashCode())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogContentViewEvent() {
        val event = ContentViewEvent("Test page 7")
        provider.sendEvent(event)
        lock.await(50L, TimeUnit.MILLISECONDS)
        assertThat(loggedEventName).isEqualTo(CommonEvents.CONTENT_VIEW)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventHashCode).isEqualTo(event.hashCode())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogErrorEvent() {
        val event = ErrorEvent()
                .setMessage("something bad happened")
                .setException(EmptyStackException())
        provider.sendEvent(event)
        lock.await(50L, TimeUnit.MILLISECONDS)
        assertThat(loggedEventName).isEqualTo(CommonEvents.ERROR)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventHashCode).isEqualTo(event.hashCode())
    }

    @Test
    fun testSendEvent_timed_noParams() {
        val event = AnalyticsEvent("Graylog Timed Event")
                .setTimed(true)
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(false)
    }

    @Test
    fun testSendEvent_timed_withParams() {
        val event = AnalyticsEvent("Graylog Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(false)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEndTimedEvent_Valid() {
        val event = AnalyticsEvent("Graylog Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(false)
        provider.endTimedEvent(event)
        lock.await(50L, TimeUnit.MILLISECONDS)
        assertThat(loggedEventName).isEqualTo("Graylog Timed Event With Parameters")
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventHashCode).isEqualTo(event.hashCode())
    }

    @Test
    fun test_endTimedEvent_WillThrow() {
        var didThrow = false
        val event = AnalyticsEvent("Graylog Timed Event With Parameters")
                .setTimed(true)
        try {
            provider.endTimedEvent(event) // attempting to end a timed event that was not started should throw an exception
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }
}
