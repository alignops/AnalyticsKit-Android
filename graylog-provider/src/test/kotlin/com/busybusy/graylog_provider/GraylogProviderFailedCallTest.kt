/*
 * Copyright 2017, 2020 busybusy, Inc.
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
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author John Hunt on 6/29/17.
 */
class GraylogProviderFailedCallTest {
    private lateinit var provider: GraylogProvider
    private val mockServer = MockWebServer()
    private val httpClient = OkHttpClient.Builder().build()
    private lateinit var callbackListener: GraylogResponseListener
    private var testEventHashCode = 0
    private var logEventCalled = false
    private var loggedEventName: String? = null
    private var httpResponseCode = 0
    private var httpStatusMessage: String? = null
    private lateinit var lock: CountDownLatch

    @Before
    fun setup() {
        lock = CountDownLatch(1)
        callbackListener = GraylogResponseListener { response ->
            testEventHashCode = response.eventHashCode()
            loggedEventName = response.eventName()
            logEventCalled = true
            httpResponseCode = response.code()
            httpStatusMessage = response.message()
            lock.countDown()
        }
        mockServer.enqueue(MockResponse().setResponseCode(420).setStatus("An error occurred communicating with the Graylog server"))
        try {
            mockServer.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        provider = GraylogProvider(httpClient, "http://" + mockServer.hostName + ":" + mockServer.port, "unit-test-android")
        provider.setCallbackHandler(callbackListener)
        logEventCalled = false
        testEventHashCode = -1
        loggedEventName = null
        httpResponseCode = -1
        httpStatusMessage = "Not Sent"
    }

    @After
    fun tearDown() {
        try {
            mockServer.shutdown()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Graylog Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        lock.await(50, TimeUnit.MILLISECONDS)
        assertThat(loggedEventName).isEqualTo("Graylog Event With Params Run")
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventHashCode).isEqualTo(event.hashCode())
        assertThat(httpResponseCode).isEqualTo(420)
        assertThat(httpStatusMessage).isEqualTo("An error occurred communicating with the Graylog server")
    }
}
