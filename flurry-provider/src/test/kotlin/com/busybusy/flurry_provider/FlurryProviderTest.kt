/*
 * Copyright 2016 - 2023 busybusy, Inc.
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test
import org.mockito.Mockito.mockStatic

/**
 * Tests the [FlurryProvider] class
 *
 * @author John Hunt on 3/21/16.
 */
class FlurryProviderTest {
    private val provider = FlurryProvider()

    @Test
    fun testSetAndGetPriorityFilter() {
        val filter = PriorityFilter { false }
        val filteringProvider = FlurryProvider(priorityFilter = filter)
        assertThat(filteringProvider.getPriorityFilter()).isEqualTo(filter)
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
        event.setPriority(-9)
                .send()
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun test_priorityFiltering_custom() {
        val filter = PriorityFilter { priorityLevel -> priorityLevel < 10 }
        val filteringProvider = FlurryProvider(priorityFilter = filter)
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(filteringProvider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(filteringProvider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
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
        val attributeMap = mutableMapOf<String, Any>()
        for (count in 0..ATTRIBUTE_LIMIT + 1) {
            attributeMap[count.toString()] = "placeholder"
        }
        var exceptionMessage: String? = ""
        try {
            provider.stringifyParameters(attributeMap)
        } catch (e: IllegalStateException) {
            exceptionMessage = e.message
        }
        assertThat(exceptionMessage).isEqualTo("Flurry events are limited to $ATTRIBUTE_LIMIT attributes")
    }

    @Test
    fun testSendEvent_unTimed_noParams() {
        val event = AnalyticsEvent("Flurry Test Run")
        mockStatic(FlurryAgent::class.java).use {
            provider.sendEvent(event)
            it.verify { FlurryAgent.logEvent("Flurry Test Run") }
        }
    }

    @Test
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Flurry Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        mockStatic(FlurryAgent::class.java).use {
            provider.sendEvent(event)
            it.verify {
                FlurryAgent.logEvent(
                    "Flurry Event With Params Run",
                    mapOf("some_param" to "yes", "another_param" to "yes again"),
                )
            }
        }
    }

    @Test
    fun testSendEvent_timed_noParams() {
        val event = AnalyticsEvent("Flurry Timed Event")
                .setTimed(true)
        mockStatic(FlurryAgent::class.java).use {
            provider.sendEvent(event)
            it.verify { FlurryAgent.logEvent("Flurry Timed Event", true) }
        }
    }

    @Test
    fun testSendEvent_timed_withParams() {
        val event = AnalyticsEvent("Flurry Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        mockStatic(FlurryAgent::class.java).use {
            provider.sendEvent(event)
            it.verify {
                FlurryAgent.logEvent(
                    "Flurry Timed Event With Parameters",
                    mapOf("some_param" to "yes", "another_param" to "yes again"),
                    true,
                )
            }
        }
    }

    @Test
    fun testEndTimedEvent() {
        val event = AnalyticsEvent("End timed event")
        mockStatic(FlurryAgent::class.java).use {
            provider.endTimedEvent(event)
            it.verify { FlurryAgent.endTimedEvent("End timed event") }
        }
    }
}
