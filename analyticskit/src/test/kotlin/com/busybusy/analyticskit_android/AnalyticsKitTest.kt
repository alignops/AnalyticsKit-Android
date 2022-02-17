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
package com.busybusy.analyticskit_android

import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests the [AnalyticsKit] class.
 *
 * @author John Hunt on 3/7/16.
 */
class AnalyticsKitTest {
    @Test
    fun testGetInstance() {
        val analyticsKit = AnalyticsKit.getInstance()
        assertThat(analyticsKit).isNotNull
        assertThat(analyticsKit).isEqualTo(AnalyticsKit.getInstance())
    }

    @Test
    fun testRegisterProvider() {
        val provider: AnalyticsKitProvider = object : AnalyticsKitProvider {
            override fun getPriorityFilter(): PriorityFilter = PriorityFilter { true }
            override fun sendEvent(event: AnalyticsEvent) {}  // do nothing
            override fun endTimedEvent(timedEvent: AnalyticsEvent) {} // do nothing
        }
        AnalyticsKit.getInstance().registerProvider(provider)
        assertThat(AnalyticsKit.getInstance().providers).contains(provider)
    }

    @Test
    fun testPriorityFiltering_multiple() {
        val flurryProvider = MockProvider().setPriorityUpperBound(0)
        val customProviderOne = MockProvider().setPriorityUpperBound(3)
        val customProviderTwo = MockProvider().setPriorityUpperBound(5)
        AnalyticsKit.getInstance()
                .registerProvider(flurryProvider)
                .registerProvider(customProviderOne)
                .registerProvider(customProviderTwo)
        val eventName1 = "Custom Providers only"
        val customOneAndTwo = AnalyticsEvent(eventName1)
                .putAttribute("hello", "world")
                .setPriority(2)
                .send()
        assertThat(flurryProvider.sentEvents).isEmpty()
        assertThat(customProviderOne.sentEvents.keys).containsExactly(eventName1)
        assertThat(customProviderOne.sentEvents.values).containsExactly(customOneAndTwo)
        assertThat(customProviderTwo.sentEvents.keys).containsExactly(eventName1)
        assertThat(customProviderTwo.sentEvents.values).containsExactly(customOneAndTwo)
    }

    @Test
    fun testPriorityFiltering_none() {
        val flurryProvider = MockProvider()
        val customProviderOne = MockProvider()
        val customProviderTwo = MockProvider()
        AnalyticsKit.getInstance()
                .registerProvider(flurryProvider)
                .registerProvider(customProviderOne)
                .registerProvider(customProviderTwo)
        val eventName1 = "Flurry and Custom 1 only"
        AnalyticsEvent(eventName1)
                .putAttribute("hello", "world")
                .setPriority(1)
                .send()
        assertThat(flurryProvider.sentEvents).isEmpty()
        assertThat(customProviderOne.sentEvents).isEmpty()
        assertThat(customProviderTwo.sentEvents).isEmpty()
    }

    @Test
    fun testTimedEvent() {
        val flurryProvider = MockProvider()
        AnalyticsKit.getInstance().registerProvider(flurryProvider)
        val eventName1 = "Hello event"
        val flurryEvent = AnalyticsEvent(eventName1)
                .putAttribute("hello", "world")
                .setTimed(true)
                .send()
        assertThat(flurryProvider.sentEvents.keys).containsExactly(eventName1)
        assertThat(flurryProvider.sentEvents.values).containsExactly(flurryEvent)
        try {
            Thread.sleep(150, 0)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        AnalyticsKit.getInstance().endTimedEvent(flurryEvent)
        assertThat(flurryEvent.getAttribute(MockProvider.EVENT_DURATION)).isNotNull
        val duration: Long = flurryEvent.getAttribute(MockProvider.EVENT_DURATION) as Long
        assertThat(duration).isGreaterThanOrEqualTo(150L)
    }

    @Test
    fun testTimedEvent_manyProviders() {
        val flurryProvider = MockProvider()
        val customProviderOne = MockProvider()
        val customProviderTwo = MockProvider()
        AnalyticsKit.getInstance()
                .registerProvider(flurryProvider)
                .registerProvider(customProviderOne)
                .registerProvider(customProviderTwo)
        val eventName1 = "Flurry only event"
        val flurryEvent = AnalyticsEvent(eventName1)
                .putAttribute("hello", "world")
                .setTimed(true)
                .send()
        assertThat(flurryProvider.sentEvents.keys).containsExactly(eventName1)
        assertThat(flurryProvider.sentEvents.values).containsExactly(flurryEvent)
        assertThat(customProviderOne.sentEvents.keys).containsExactly(eventName1)
        assertThat(customProviderOne.sentEvents.values).containsExactly(flurryEvent)
        assertThat(customProviderTwo.sentEvents.keys).containsExactly(eventName1)
        assertThat(customProviderTwo.sentEvents.values).containsExactly(flurryEvent)
        try {
            Thread.sleep(150, 0)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        AnalyticsKit.getInstance().endTimedEvent(flurryEvent)
        assertThat(flurryEvent.getAttribute(MockProvider.EVENT_DURATION)).isNotNull
        val duration = flurryEvent.getAttribute(MockProvider.EVENT_DURATION) as Long
        assertThat(duration).isGreaterThanOrEqualTo(150L)
    }

    @Test
    @Throws(Exception::class)
    fun test_endTimeEvent_willThrow() {
        val flurryProvider = MockProvider()
        AnalyticsKit.getInstance()
                .registerProvider(flurryProvider)
        val eventName1 = "throwEvent"
        val flurryEvent = AnalyticsEvent(eventName1)
                .putAttribute("hello", "world")
                .send()
        var didThrow = false
        try {
            AnalyticsKit.getInstance().endTimedEvent(flurryEvent)
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }

    @Test
    @Throws(Exception::class)
    fun test_endTimeEvent_willThrow_innerCase() {
        val flurryProvider = MockProvider()
        AnalyticsKit.getInstance()
                .registerProvider(flurryProvider)
        AnalyticsEvent("normalEvent")
                .putAttribute("hello", "world")
                .setTimed(true)
                .send()
        val eventName2 = "throwEvent"
        val throwEvent = AnalyticsEvent(eventName2)
                .putAttribute("hello", "world")
                .send()
        var didThrow = false
        try {
            AnalyticsKit.getInstance().endTimedEvent(throwEvent)
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }
}
