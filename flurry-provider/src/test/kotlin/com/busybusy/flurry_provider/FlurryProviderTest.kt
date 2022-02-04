/*
 * Copyright 2016, 2020 busybusy, Inc.
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
package com.busybusy.flurry_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.flurry.android.FlurryAgent
import com.nhaarman.mockitokotlin2.times
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

/**
 * Tests the [FlurryProvider] class
 *
 * @author John Hunt on 3/21/16.
 */
@RunWith(PowerMockRunner::class)
class FlurryProviderTest {
    private lateinit var provider: FlurryProvider
    private var testEventName: String? = null
    private lateinit var testEventPropertiesMap: Map<String, Any>
    private var logEventNameOnlyCalled = false
    private var logEventNameAndParamsCalled = false
    private var logTimedEventNameOnlyCalled = false
    private var logTimedEventNameAndParamsCalled = false

    @Before
    fun setup() {
        provider = FlurryProvider()
        testEventName = null
        testEventPropertiesMap = mutableMapOf()
        logEventNameOnlyCalled = false
        logEventNameAndParamsCalled = false
        logTimedEventNameOnlyCalled = false
        logTimedEventNameAndParamsCalled = false
    }

    @Test
    fun testSetAndGetPriorityFilter() {
        val filter = PriorityFilter { false }
        val filteringProvider = FlurryProvider(priorityFilter = filter)
        assertThat(filteringProvider.priorityFilter).isEqualTo(filter)
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("Forecast: Event Flurries")
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
        val filteringProvider = FlurryProvider(priorityFilter = filter)
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(filteringProvider.priorityFilter.shouldLog(event.priority)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(filteringProvider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun testStringifyParameters_noParams() {
        val flurryParams = provider.stringifyParameters(null)
        assertThat(flurryParams).isNull()
    }

    @Test
    fun testStringifyParameters_validParams() {
        val eventParams = mutableMapOf<String, Any>("favorite_color" to "Blue", "favorite_number" to 42)
        val flurryParams = provider.stringifyParameters(eventParams)
        assertThat(flurryParams).isNotNull
        assertThat(flurryParams).containsExactly(entry("favorite_color", "Blue"), entry("favorite_number", "42"))
    }

    @Test
    fun testStringifyParameters_willThrow() {
        val attributeMap = HashMap<String, Any>()
        for (count in 0..provider.ATTRIBUTE_LIMIT + 1) {
            attributeMap[count.toString()] = "placeholder"
        }
        var exceptionMessage: String? = ""
        try {
            provider.stringifyParameters(attributeMap)
        } catch (e: IllegalStateException) {
            exceptionMessage = e.message
        }
        assertThat(exceptionMessage).isEqualTo("Flurry events are limited to " + provider.ATTRIBUTE_LIMIT + " attributes")
    }

    @PrepareForTest(FlurryAgent::class)
    @Test
    fun testSendEvent_unTimed_noParams() {
        val event = AnalyticsEvent("Flurry Test Run")
        PowerMockito.mockStatic(FlurryAgent::class.java)
        PowerMockito.`when`(FlurryAgent.logEvent(anyString()))
                .thenAnswer { invocation ->
                    val args = invocation.arguments
                    testEventName = args[0] as String
                    logEventNameOnlyCalled = true
                    null
                }
        provider.sendEvent(event)
        assertThat(testEventName).isEqualTo("Flurry Test Run")
        assertThat(testEventPropertiesMap).isEmpty()
        assertThat(logEventNameOnlyCalled).isEqualTo(true)
        assertThat(logEventNameAndParamsCalled).isEqualTo(false)
        assertThat(logTimedEventNameOnlyCalled).isEqualTo(false)
        assertThat(logTimedEventNameAndParamsCalled).isEqualTo(false)
    }

    @Suppress("UNCHECKED_CAST")
    @PrepareForTest(FlurryAgent::class)
    @Test
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Flurry Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        PowerMockito.mockStatic(FlurryAgent::class.java)
        PowerMockito.`when`(FlurryAgent.logEvent(anyString(), anyMap()))
                .thenAnswer { invocation ->
                    logEventNameAndParamsCalled = true
                    val args = invocation.arguments
                    testEventName = args[0] as String
                    testEventPropertiesMap = args[1] as Map<String, Any>
                    null
                }
        provider.sendEvent(event)
        assertThat(testEventName).isEqualTo("Flurry Event With Params Run")
        assertThat(testEventPropertiesMap).containsExactly(entry("some_param", "yes"), entry("another_param", "yes again"))
        assertThat(logEventNameAndParamsCalled).isEqualTo(true)
        assertThat(logEventNameOnlyCalled).isEqualTo(false)
        assertThat(logTimedEventNameOnlyCalled).isEqualTo(false)
        assertThat(logTimedEventNameAndParamsCalled).isEqualTo(false)
    }

    @PrepareForTest(FlurryAgent::class)
    @Test
    fun testSendEvent_timed_noParams() {
        val event = AnalyticsEvent("Flurry Timed Event")
                .setTimed(true)
        PowerMockito.mockStatic(FlurryAgent::class.java)
        PowerMockito.`when`(FlurryAgent.logEvent(anyString(), anyBoolean()))
                .thenAnswer { invocation ->
                    val args = invocation.arguments
                    testEventName = args[0] as String
                    logTimedEventNameOnlyCalled = true
                    null
                }
        provider.sendEvent(event)
        assertThat(testEventName).isEqualTo("Flurry Timed Event")
        assertThat(testEventPropertiesMap).isEmpty()
        assertThat(logTimedEventNameOnlyCalled).isEqualTo(true)
        assertThat(logEventNameOnlyCalled).isEqualTo(false)
        assertThat(logEventNameAndParamsCalled).isEqualTo(false)
        assertThat(logTimedEventNameAndParamsCalled).isEqualTo(false)
    }

    @Suppress("UNCHECKED_CAST")
    @PrepareForTest(FlurryAgent::class)
    @Test
    fun testSendEvent_timed_withParams() {
        val event = AnalyticsEvent("Flurry Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        PowerMockito.mockStatic(FlurryAgent::class.java)
        PowerMockito.`when`(FlurryAgent.logEvent(anyString(), anyMap(), anyBoolean()))
                .thenAnswer { invocation ->
                    val args = invocation.arguments
                    testEventName = args[0] as String
                    logTimedEventNameAndParamsCalled = true
                    testEventPropertiesMap = args[1] as Map<String, Any>
                    null
                }
        provider.sendEvent(event)
        assertThat(testEventName).isEqualTo("Flurry Timed Event With Parameters")
        assertThat(testEventPropertiesMap).containsExactly(entry("some_param", "yes"), entry("another_param", "yes again"))
        assertThat(logTimedEventNameAndParamsCalled).isEqualTo(true)
        assertThat(logTimedEventNameOnlyCalled).isEqualTo(false)
        assertThat(logEventNameOnlyCalled).isEqualTo(false)
        assertThat(logEventNameAndParamsCalled).isEqualTo(false)
    }

    @PrepareForTest(FlurryAgent::class)
    @Test
    fun testEndTimedEvent() {
        val event = AnalyticsEvent("End timed event")
        PowerMockito.mockStatic(FlurryAgent::class.java)
        provider.endTimedEvent(event)

        // Verify Flurry framework is called
        PowerMockito.verifyStatic(FlurryAgent::class.java, times(1))
        FlurryAgent.endTimedEvent(event.name())
    }
}