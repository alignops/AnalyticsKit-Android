/*
 * Copyright 2016 - 2022 busybusy, Inc.
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
package com.busybusy.google_analytics_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.ContentViewEvent
import com.busybusy.analyticskit_android.ErrorEvent
import com.google.android.gms.analytics.Tracker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mockito
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates.notNull

/**
 * Tests the [GoogleAnalyticsProvider] class
 *
 * @author John Hunt on 3/21/16.
 */
class GoogleAnalyticsProviderTest {
    private lateinit var tracker: Tracker
    private lateinit var provider: GoogleAnalyticsProvider
    private lateinit var testEventPropertiesMap: Map<String, String>
    private var sendCalled: Boolean by notNull()

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        // Mock behavior for when Tracker.send(Map<String, String>) is called
        tracker = Mockito.mock(Tracker::class.java)
        provider = GoogleAnalyticsProvider(tracker)
        sendCalled = false
        testEventPropertiesMap = mutableMapOf()
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            testEventPropertiesMap = args[0] as Map<String, String>
            sendCalled = true
            null
        }.`when`(tracker).send(anyMap())
    }

    @Test
    fun testSetAndGetPriorityFilter() {
        val filter = PriorityFilter { false }
        val filteringProvider = GoogleAnalyticsProvider(tracker, filter)
        assertThat(filteringProvider.getPriorityFilter()).isEqualTo(filter)
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(provider.getPriorityFilter().shouldLog(event.priorityLevel)).isEqualTo(true)
        event.setPriority(-9)
                .send()
        assertThat(provider.getPriorityFilter().shouldLog(event.priorityLevel)).isEqualTo(true)
    }

    @Test
    fun test_priorityFiltering_custom() {
        val filter = PriorityFilter { priorityLevel -> priorityLevel < 10 }
        val filteringProvider = GoogleAnalyticsProvider(tracker, filter)
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(filteringProvider.getPriorityFilter().shouldLog(event.priorityLevel)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(filteringProvider.getPriorityFilter().shouldLog(event.priorityLevel)).isEqualTo(true)
    }

    @Test
    fun testStringifyAttributesMap_noParams() {
        val stringAttributes = provider.stringifyAttributesMap(null)
        assertThat(stringAttributes).isNull()
    }

    @Test
    fun testStringifyAttributesMap_validParams() {
        val eventParams = HashMap<String, Any>()
        eventParams["favorite_color"] = "Blue"
        eventParams["favorite_number"] = 42
        val stringAttributes = provider.stringifyAttributesMap(eventParams)
        assertThat(stringAttributes).isNotNull
        assertThat(stringAttributes!!.containsKey("favorite_color"))
        assertThat(stringAttributes["favorite_color"]).isEqualTo("Blue")
        assertThat(stringAttributes.containsKey("favorite_number"))
        assertThat(stringAttributes["favorite_number"]).isEqualTo("42")
    }

    @Test
    fun testSendEvent_unTimed_noParams() {
        val event = AnalyticsEvent("Google Analytics Test Run")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(3)
        assertThat(testEventPropertiesMap).containsExactly(
                entry("&ea", "Google Analytics Test Run"),
                entry("&ec", "User Event"),
                entry("&t", "event")
        )
    }

    @Test
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Google Analytics Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(5)
        assertThat(testEventPropertiesMap).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf(
                        "&ea" to "Google Analytics Event With Params Run",
                        "&ec" to "User Event",
                        "&t" to "event",
                        "some_param" to "yes",
                        "another_param" to "yes again"
                )
        )
    }

    @Test
    fun testLogContentViewEvent() {
        val event = ContentViewEvent("Test page 7")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(2)
        assertThat(testEventPropertiesMap).containsExactly(
                entry("&t", "screenview"),
                entry("contentName", "Test page 7")
        )
    }

    @Test
    fun testLogErrorEvent() {
        val event = ErrorEvent()
                .setMessage("something bad happened")
                .setException(EmptyStackException())
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(5)
        assertThat(testEventPropertiesMap).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf(
                        "&exd" to "something bad happened",
                        "&t" to "exception",
                        "&exf" to "0",
                        "error_message" to "something bad happened",
                        "exception_object" to "java.util.EmptyStackException"
                )
        )
    }

    @Test
    fun testSendEvent_timed_noParams() {
        val event = AnalyticsEvent("Google Analytics Timed Event")
                .setTimed(true)
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
    }

    @Test
    fun testSendEvent_timed_withParams() {
        val event = AnalyticsEvent("Google Analytics Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
    }

    @Test
    fun testEndTimedEvent_Valid() {
        val event = AnalyticsEvent("Google Analytics Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
        try {
            Thread.sleep(50)
        } catch (e: InterruptedException) {
            // don't do anything, this is just a test that needs some delay
        }
        provider.endTimedEvent(event)

        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(6)
        val timeString = testEventPropertiesMap["&utt"]
        assertThat(testEventPropertiesMap).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf(
                        "&utl" to "Google Analytics Timed Event With Parameters",
                        "&utc" to "Timed Events",
                        "&t" to "timing",
                        "&utt" to timeString,
                        "some_param" to "yes",
                        "another_param" to "yes again"
                )
        )

        val elapsedTime = java.lang.Long.valueOf(timeString!!)
        assertThat(elapsedTime).isGreaterThanOrEqualTo(50)
    }

    @Test
    fun test_endTimedEvent_WillThrow() {
        var didThrow = false
        val event = AnalyticsEvent("Google Analytics Timed Event With Parameters")
                .setTimed(true)
        try {
            provider.endTimedEvent(event) // attempting to end a timed event that was not started should throw an exception
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }
}
