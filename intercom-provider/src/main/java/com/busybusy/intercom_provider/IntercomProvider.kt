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
package com.busybusy.intercom_provider

import io.intercom.android.sdk.Intercom
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsEvent
import java.text.DecimalFormat

/**
 * Implements Intercom as a provider to use with [com.busybusy.analyticskit_android.AnalyticsKit]
 *
 * @property intercom your already-initialized [Intercom] instance.
 *                    Please call [Intercom.initialize] prior to setting up your IntercomProvider.
 * @property priorityFilter the `PriorityFilter` to use when evaluating events if an event of a given priority should be logged
 * @property quietMode `true` to silently take the first ten items of metadata in the event's attributes per Intercom limits.
 * `                    false` to throw Exceptions when events contain more than the allowed limit of metadata items.
 *
 * @author John Hunt on 5/19/17.
 */
class IntercomProvider(
    private val intercom: Intercom,
    private val priorityFilter: PriorityFilter = PriorityFilter { true },
    private val quietMode: Boolean = false,
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
     * Sends the event using provider-specific code
     * @param event an instantiated event. **Note:** When sending timed events, be aware that this provider does not support concurrent timed events with the same name.
     * @see AnalyticsKitProvider
     */
    override fun sendEvent(event: AnalyticsEvent) {
        when {
            event.isTimed -> {
                // Hang on to the event until it is done
                eventTimes[event.name()] = System.currentTimeMillis()
                timedEvents[event.name()] = event
            }
            else -> logIntercomEvent(event) // Send the event through the Intercom SDK
        }
    }

    /**
     * End the timed event.
     *
     * @param timedEvent the event which has finished
     */
    @Throws(IllegalStateException::class)
    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        val endTime = System.currentTimeMillis()
        val startTime = eventTimes.remove(timedEvent.name())
        val finishedEvent = timedEvents.remove(timedEvent.name())
        if (startTime != null && finishedEvent != null) {
            val durationSeconds = (endTime - startTime) / 1000.0
            val df = DecimalFormat("#.###")
            finishedEvent.putAttribute(DURATION_KEY, df.format(durationSeconds))
            logIntercomEvent(finishedEvent)
        } else {
            error("Attempted ending an event that was never started (or was previously ended): ${timedEvent.name()}")
        }
    }

    @Throws(IllegalStateException::class)
    private fun logIntercomEvent(event: AnalyticsEvent) {
        val sanitizedAttributes: MutableMap<String, Any?>? =
            if (event.attributes != null && event.attributes!!.keys.size > MAX_METADATA_ATTRIBUTES) {
                if (quietMode) {
                    val keepAttributeNames = event.attributes!!.keys.take(MAX_METADATA_ATTRIBUTES)
                    event.attributes!!.filter {
                        keepAttributeNames.contains(it.key)
                    }.toMutableMap()
                } else {
                    error("Intercom does not support more than $MAX_METADATA_ATTRIBUTES" +
                            " metadata fields. See https://www.intercom.com/help/en/articles/175-set-up-event-tracking-in-intercom.")
                }
            } else {
                event.attributes
            }

        Intercom.client().logEvent(event.name(), sanitizedAttributes)
    }
}

internal const val MAX_METADATA_ATTRIBUTES = 10
internal const val DURATION_KEY = "event_duration"
