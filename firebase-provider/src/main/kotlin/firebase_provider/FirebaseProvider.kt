/*
 * Copyright 2018 - 2022 busybusy, Inc.
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

import com.google.firebase.analytics.FirebaseAnalytics
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsEvent
import android.os.Bundle
import java.io.Serializable
import java.lang.IllegalStateException
import java.text.DecimalFormat

/**
 * Provider that facilitates reporting events to Firebase Analytics.
 * We recommend using the [FirebaseAnalytics.Param] names for attributes as much as possible due to the restriction on the number of
 * custom parameters in Firebase.
 *
 * @see [Log Events](https://firebase.google.com/docs/analytics/android/events)
 * @see [Custom-parameter reporting](https://support.google.com/firebase/answer/7397304?hl=en&ref_topic=6317489)
 *
 * @property firebaseAnalytics the initialized [FirebaseAnalytics] instance associated with the application
 * @property priorityFilter the [PriorityFilter] to use when evaluating if an event should be sent to this provider's platform.
 *                          By default, this provider will log all events regardless of priority.
 */
class FirebaseProvider(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val priorityFilter: PriorityFilter = PriorityFilter { true },
) : AnalyticsKitProvider {

    private val timedEvents: MutableMap<String, AnalyticsEvent> by lazy { mutableMapOf() }
    private val eventTimes: MutableMap<String, Long> by lazy { mutableMapOf() }

    /**
     * Returns the filter used to restrict events by priority.
     *
     * @return the [PriorityFilter] instance the provider is using to determine if an
     *         event of a given priority should be logged
     */
    override fun getPriorityFilter(): PriorityFilter = priorityFilter

    /**
     * Sends the event using provider-specific code.
     *
     * @param event an instantiated event
     */
    override fun sendEvent(event: AnalyticsEvent) {
        if (event.isTimed()) { // Hang onto the event until it is done
            eventTimes[event.name()] = System.currentTimeMillis()
            timedEvents[event.name()] = event
        } else { // Send the event through the Firebase Analytics API
            logFirebaseAnalyticsEvent(event)
        }
    }

    /**
     * End the timed event.
     *
     * @param timedEvent the event which has finished
     * @throws IllegalStateException
     */
    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        val endTime = System.currentTimeMillis()
        val startTime = eventTimes.remove(timedEvent.name())
        val finishedEvent = timedEvents.remove(timedEvent.name())
        if (startTime != null && finishedEvent != null) {
            val durationSeconds = ((endTime - startTime) / 1000).toDouble()
            val df = DecimalFormat("#.###")
            finishedEvent.putAttribute(FirebaseAnalytics.Param.VALUE, df.format(durationSeconds))
            logFirebaseAnalyticsEvent(finishedEvent)
        } else {
            error("Attempted ending an event that was never started (or was previously ended): ${timedEvent.name()}")
        }
    }

    private fun logFirebaseAnalyticsEvent(event: AnalyticsEvent) {
        var parameterBundle: Bundle? = null
        val attributes = event.attributes
        if (attributes != null && attributes.isNotEmpty()) {
            parameterBundle = Bundle()
            for (key in attributes.keys) {
                parameterBundle.putSerializable(key, getCheckAndCast(attributes, key))
            }
        }
        firebaseAnalytics.logEvent(event.name(), parameterBundle)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <ObjectType : Serializable> getCheckAndCast(
        map: Map<String, Any>,
        key: String,
    ): ObjectType {
        val result = map[key] as Serializable
        return result as ObjectType
    }
}
