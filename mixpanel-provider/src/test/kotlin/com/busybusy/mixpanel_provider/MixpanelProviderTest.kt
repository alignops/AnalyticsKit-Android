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
package com.busybusy.mixpanel_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString

/**
 * Tests the [MixpanelProvider] class
 *
 * @author John Hunt on 3/16/16.
 */
class MixpanelProviderTest {
    private val mockMixpanelAPI: MixpanelAPI = mock()
    private val provider = MixpanelProvider(mockMixpanelAPI)

    private lateinit var testEventName: String
    private lateinit var testEventPropertiesMap: Map<String, Any>
    private var trackMapCalled = false
    private var timeEventCalled = false

    private val timedEvent = AnalyticsEvent("Timed Event")
            .setTimed(true)
            .putAttribute("timed_attribute1", "timed_test1")
    private val untimedEvent = AnalyticsEvent("Untimed Event")
            .putAttribute("attribute1", "test1")
            .putAttribute("attribute2", "test2")

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        testEventPropertiesMap = mutableMapOf()
        trackMapCalled = false
        timeEventCalled = false

        // Mock behavior for when MixpanelAPI.trackMap(String, Map<String, Object>) is called
        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testEventPropertiesMap = args[1] as Map<String, Any>
            trackMapCalled = true
            null
        }.`when`(mockMixpanelAPI).trackMap(anyString(), anyMap())

        // Mock behavior for when MixpanelAPI.timeEvent(String) is called
        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            timeEventCalled = true
            null
        }.`when`(mockMixpanelAPI).timeEvent(anyString())
    }

    @Test
    fun testGetAndSetPriorityFilter() {
        val filter = PriorityFilter { false }
        val filteringProvider = MixpanelProvider(mixpanelApi = mockMixpanelAPI, priorityFilter = filter)
        assertThat(filteringProvider.getPriorityFilter()).isEqualTo(filter)
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("Let's test event priorities")
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
        val filteringProvider = MixpanelProvider(mixpanelApi = mockMixpanelAPI, priorityFilter = filter)
        val event = AnalyticsEvent("Let's test event priorities again")
                .setPriority(10)
                .send()
        assertThat(filteringProvider.getPriorityFilter().shouldLog(event.priorityLevel)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(filteringProvider.getPriorityFilter().shouldLog(event.priorityLevel)).isEqualTo(true)
    }

    @Suppress("UsePropertyAccessSyntax")
    @Test
    fun testSendEvent_notTimed() {
        provider.sendEvent(untimedEvent)
        assertThat(trackMapCalled).isEqualTo(true)
        assertThat(timeEventCalled).isEqualTo(false)
        assertThat(testEventName).isNotNull()
        assertThat(testEventName).isEqualTo("Untimed Event")
        assertThat(testEventPropertiesMap).containsAllEntriesOf(mutableMapOf("attribute1" to "test1", "attribute2" to "test2"))
    }

    @Test
    fun testSendEvent_timed() {
        provider.sendEvent(timedEvent)
        assertThat(timeEventCalled).isEqualTo(true)
        assertThat(trackMapCalled).isEqualTo(false)
        assertThat(testEventName).isEqualTo("Timed Event")
        assertThat(testEventPropertiesMap).isEmpty()
    }

    @Test
    fun testEndTimedEvent() {
        provider.endTimedEvent(timedEvent)
        assertThat(timeEventCalled).isEqualTo(false)
        assertThat(trackMapCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo("Timed Event")
        assertThat(testEventPropertiesMap).containsExactly(entry("timed_attribute1", "timed_test1"))
    }
}
