/*
 * Copyright 2018, 2020-2021 busybusy, Inc.
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
package com.busybusy.firebase_provider

import android.os.Bundle
import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.CommonEvents
import com.busybusy.analyticskit_android.ContentViewEvent
import com.busybusy.analyticskit_android.ErrorEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

/**
 * Tests the [FirebaseProvider] class
 *
 * @author John Hunt on 3/21/16.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27], manifest=Config.NONE)
class FirebaseProviderTest {
    private val firebaseAnalytics: FirebaseAnalytics = mock()
    private val provider: FirebaseProvider = FirebaseProvider(firebaseAnalytics)
    private var sendCalled = false
    private var testEventName: String? = null
    private var testBundle: Bundle? = null

    @Before
    fun setup() {
        // Mock behavior for when FirebaseAnalytics logEvent() is called
        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testBundle = args[1] as Bundle
            sendCalled = true
            null
        }.whenever(firebaseAnalytics).logEvent(any(), any())

        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testBundle = args[1] as Bundle?
            sendCalled = true
            null
        }.whenever(firebaseAnalytics).logEvent(any(), isNull())

        sendCalled = false
        testEventName = null
        testBundle = null
    }

    @Test
    fun testSetAndGetPriorityFilter() {
        val filter = PriorityFilter { false }
        val filteringProvider = FirebaseProvider(firebaseAnalytics, filter)
        assertThat(filteringProvider.priorityFilter).isEqualTo(filter)
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("A Firebase Event")
                .setPriority(10)
                .send()
        assertThat(provider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
        event.setPriority(-9)
                .send()
        assertThat(provider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun test_priorityFiltering_custom() {
        val filteringProvider = FirebaseProvider(firebaseAnalytics) {
                priorityLevel -> priorityLevel < 10
        }
        val event = AnalyticsEvent("Priority 10 Event")
                .setPriority(10)
                .send()
        assertThat(filteringProvider.priorityFilter.shouldLog(event.priority)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(filteringProvider.priorityFilter.shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun testSendEvent_unTimed_noParams() {
        val event = AnalyticsEvent("Firebase Analytics Test Run")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo("Firebase Analytics Test Run")
        assertThat(testBundle).isNull()
    }

    @Test
    fun testSendEvent_unTimed_withParams() {
        val event = AnalyticsEvent("Firebase Analytics Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testBundle).isNotNull
        assertThat(testBundle!!.containsKey("some_param")).isEqualTo(true)
        assertThat(testBundle!!["some_param"]).isEqualTo("yes")
        assertThat(testBundle!!.containsKey("another_param")).isEqualTo(true)
        assertThat(testBundle!!["another_param"]).isEqualTo("yes again")
    }

    @Test
    fun testSendEvent_unTimed_withTypedParams() {
        val intArray = intArrayOf(0, 1, 2)
        val event = AnalyticsEvent("Firebase Analytics Event With Typed Params Run")
                .putAttribute("int_param", 1)
                .putAttribute("long_param", 32L)
                .putAttribute("string_param", "a string")
                .putAttribute("double_param", 3.1415926)
                .putAttribute("float_param", 0.0f)
                .putAttribute("int_array_param", intArray)
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo("Firebase Analytics Event With Typed Params Run")
        assertThat(testBundle).isNotNull
        assertThat(testBundle!!["int_param"]).isEqualTo(1)
        assertThat(testBundle!!["long_param"]).isEqualTo(32L)
        assertThat(testBundle!!["string_param"]).isEqualTo("a string")
        assertThat(testBundle!!["double_param"]).isEqualTo(3.1415926)
        assertThat(testBundle!!["float_param"]).isEqualTo(0.0f)
        assertThat(testBundle!!["int_array_param"]).isEqualTo(intArray)
    }

    @Test
    fun testLogContentViewEvent() {
        val event = ContentViewEvent("Test page 7")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo(CommonEvents.CONTENT_VIEW)
        assertThat(testBundle).isNotNull
        assertThat(testBundle!![ContentViewEvent.CONTENT_NAME]).isEqualTo("Test page 7")
    }

    @Test
    fun testLogErrorEvent() {
        val event = ErrorEvent()
                .setMessage("something bad happened")
                .setException(EmptyStackException())
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo(CommonEvents.ERROR)
        assertThat(testBundle).isNotNull
        assertThat(testBundle!!["error_message"]).isEqualTo("something bad happened")
        assertThat(testBundle!!["exception_object"]).isNotNull
    }

    @Test
    fun testSendEvent_timed_noParams() {
        val event = AnalyticsEvent("Firebase Analytics Timed Event")
                .setTimed(true)
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
    }

    @Test
    fun testSendEvent_timed_withParams() {
        val event = AnalyticsEvent("Firebase Analytics Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
        assertThat(testBundle).isNull()
    }

    @Test
    fun testEndTimedEvent_Valid() {
        val event = AnalyticsEvent("Firebase Analytics Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
        provider.endTimedEvent(event)

        // Verify Firebase Analytics framework is called
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testBundle).isNotNull
        assertThat(testBundle!!.size()).isEqualTo(3)
        assertThat(testBundle!!["some_param"]).isEqualTo("yes")
        assertThat(testBundle!!["another_param"]).isEqualTo("yes again")
        assertThat(testBundle!![FirebaseAnalytics.Param.VALUE]).isNotNull
    }

    @Test
    fun test_endTimedEvent_WillThrow() {
        var didThrow = false
        val event = AnalyticsEvent("Firebase Analytics Timed Event With Parameters")
                .setTimed(true)
        try {
            provider.endTimedEvent(event) // attempting to end a timed event that was not started should throw an exception
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }
}
