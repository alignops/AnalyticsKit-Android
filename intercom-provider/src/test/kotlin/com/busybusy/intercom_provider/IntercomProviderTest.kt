/*
 * Copyright 2020 busybusy, Inc.
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
package com.busybusy.intercom_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.CommonEvents
import com.busybusy.analyticskit_android.ContentViewEvent
import com.busybusy.analyticskit_android.ErrorEvent
import com.nhaarman.mockitokotlin2.*
import io.intercom.android.sdk.Intercom
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

/**
 * Tests the [IntercomProvider] class.
 *
 * @author John Hunt on 5/19/17.
 */
@RunWith(PowerMockRunner::class)
class IntercomProviderTest {
    private lateinit var provider: IntercomProvider
    private lateinit var mockedIntercom: Intercom
    private lateinit var testEventName: String
    private var testEventPropertiesMap: Map<String, Any>? = null
    private var logEventCalled = false

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        mockedIntercom = mock()
        provider = IntercomProvider(mockedIntercom)
        PowerMockito.mockStatic(Intercom::class.java)
        PowerMockito.`when`(Intercom.client()).thenReturn(mockedIntercom)
        logEventCalled = false
        testEventPropertiesMap = null

        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testEventPropertiesMap = args[1] as Map<String, Any>
            logEventCalled = true
            null
        }.`when`(mockedIntercom).logEvent(any(), any())

        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testEventPropertiesMap = args[1] as Map<String, Any>?
            logEventCalled = true
            null
        }.`when`(mockedIntercom).logEvent(any(), isNull())
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testSetAndGetPriorityFilter() {
        val filter = PriorityFilter { false }
        provider.setPriorityFilter(filter)
        assertThat(provider.getPriorityFilter()).isEqualTo(filter)
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("Intercom event")
                .setPriority(10)
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
        event.priority = -9
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun test_priorityFiltering_custom() {
        provider.setPriorityFilter { priorityLevel -> priorityLevel < 10 }
        val event = AnalyticsEvent("Intercom event")
                .setPriority(10)
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(false)
        event.priority = 9
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testSendEvent_unTimed_noParams() {
        whenever(Intercom.client()).thenReturn(mockedIntercom)
        val event = AnalyticsEvent("Intercom Test Run")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo("Intercom Test Run")
        assertThat(testEventPropertiesMap).isNull()
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Intercom Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo("Intercom Event With Params Run")
        assertThat(testEventPropertiesMap!!.keys).containsExactlyInAnyOrder("some_param", "another_param")
        assertThat(testEventPropertiesMap!!.values).containsExactlyInAnyOrder("yes", "yes again")
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testLogContentViewEvent() {
        val event = ContentViewEvent("Test page 7")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo(CommonEvents.CONTENT_VIEW)
        assertThat(testEventPropertiesMap!!.keys).containsExactly(ContentViewEvent.CONTENT_NAME)
        assertThat(testEventPropertiesMap!!.values).containsExactly("Test page 7")
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testLogErrorEvent() {
        val myException = EmptyStackException()
        val event = ErrorEvent()
                .setMessage("something bad happened")
                .setException(myException)
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo(CommonEvents.ERROR)
        assertThat(testEventPropertiesMap!!.keys).containsExactlyInAnyOrder("error_message", "exception_object")
        assertThat(testEventPropertiesMap!!.values).containsExactlyInAnyOrder("something bad happened", myException)
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testSendEvent_timed_noParams() {
        val event = AnalyticsEvent("Intercom Timed Event")
                .setTimed(true)
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(false)
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testSendEvent_timed_withParams() {
        val event = AnalyticsEvent("Intercom Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(false)
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun testEndTimedEvent_Valid() {
        val event = AnalyticsEvent("Intercom Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(logEventCalled).isEqualTo(false)
        try {
            Thread.sleep(50)
        } catch (e: InterruptedException) {
            // don't do anything, this is just a test that needs some delay
        }
        provider.endTimedEvent(event)
        val timeString = testEventPropertiesMap!![provider.DURATION].toString()
        val elapsed = java.lang.Double.valueOf(timeString)
        assertThat(elapsed).isGreaterThanOrEqualTo(0.05)

        // Verify Intercom framework is called
        assertThat(logEventCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap!!.keys).containsExactlyInAnyOrder(provider.DURATION, "some_param", "another_param")
        assertThat(testEventPropertiesMap!!.values).containsExactlyInAnyOrder(timeString, "yes", "yes again")
    }

    @PrepareForTest(Intercom::class)
    @Test
    fun test_endTimedEvent_WillThrow() {
        var didThrow = false
        val event = AnalyticsEvent("Intercom Timed Event With Parameters")
                .setTimed(true)
        try {
            provider.endTimedEvent(event) // attempting to end a timed event that was not started should throw an exception
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }
}
