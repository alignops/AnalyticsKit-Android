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

package com.busybusy.analyticskit.kissmetrics_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.kissmetrics.sdk.KISSmetricsAPI
import com.kissmetrics.sdk.KISSmetricsAPI.RecordCondition
import java.text.DecimalFormat

/**
 * Provider that facilitates reporting events to KissMetrics.
 * *Note* The KissMetrics SDK supports sending a [RecordCondition]. To send a [RecordCondition] with this provider,
 * simply add it to the event's attribute map with "`record_condition`" as the key. From Kotlin code this can
 * also be accomplished by calling the `recordCondition(condition)` extension function on an [AnalyticsEvent] instance.
 * @see <a href="https://developers.kissmetrics.com/reference#android">KissMetrics Android SDK documentation</a>
 *
 * @constructor Initializes a new [KissMetricsProvider] object.
 * *Please be aware:* asdf
 *
 * @property kissMetrics your already-initialized [KISSmetricsAPI] instance. Please call
 * KISSmetricsAPI.sharedAPI(API_KEY, APPLICATION_CONTEXT) prior to setting up your [KissMetricsProvider].
 * @property priorityFilter the [PriorityFilter] to use when evaluating events
 */
class KissMetricsProvider(private val kissMetrics: KISSmetricsAPI,
                          private val priorityFilter: PriorityFilter = PriorityFilter { true }
) : AnalyticsKitProvider {
    private val timedEvents: MutableMap<String, AnalyticsEvent> = mutableMapOf()
    private val eventTimes: MutableMap<String, Long> = mutableMapOf()

    override fun getPriorityFilter(): PriorityFilter = priorityFilter

    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        val endTime: Long = System.currentTimeMillis()
        val startTime: Long? = this.eventTimes.remove(timedEvent.name())
        val finishedEvent: AnalyticsEvent? = timedEvents.remove(timedEvent.name())

        if (startTime != null && finishedEvent != null) {
            val durationSeconds = (endTime - startTime) / 1000.0
            val df = DecimalFormat("#.###")
            finishedEvent.putAttribute(DURATION, df.format(durationSeconds))
            logKissMetricsEvent(finishedEvent)
        } else {
            error("Attempted ending an event that was never started (or was previously ended): " + timedEvent.name())
        }
    }

    override fun sendEvent(event: AnalyticsEvent) {
        if (event.isTimed) { // Hang onto it until it is done
            eventTimes[event.name()] = System.currentTimeMillis()
            timedEvents[event.name()] = event
        } else {  // Send the event through the Intercom SDK
            logKissMetricsEvent(event)
        }
    }

    private fun logKissMetricsEvent(event: AnalyticsEvent) {
        if (event.attributes != null && event.attributes?.isNotEmpty() == true) {
            val attributes: MutableMap<String, Any> = event.attributes as MutableMap<String, Any>
            if (attributes.containsKey(RECORD_CONDITION)) {
                val condition: RecordCondition = attributes.remove(RECORD_CONDITION) as RecordCondition
                if (attributes.isNotEmpty()) { // other attributes there we need to track
                    val stringProperties = attributes.stringifyAttributes()
                    KISSmetricsAPI.sharedAPI().record(event.name(), stringProperties, condition)
                } else { // just the condition to send
                    KISSmetricsAPI.sharedAPI().record(event.name(), condition)
                }
            } else { // no condition to record, just attributes
                val stringProperties = attributes.stringifyAttributes()
                KISSmetricsAPI.sharedAPI().record(event.name(), stringProperties)
            }
        } else { // record event by name only
            KISSmetricsAPI.sharedAPI().record(event.name())
        }
    }
}

internal const val DURATION = "event_duration"
const val RECORD_CONDITION = "record_condition"

/**
 * Converts an attributes Map to to Map<String, String> to appease the KissMetrics API.
 */
fun Map<String, Any>.stringifyAttributes(): Map<String, String> {
    return if (this.isNotEmpty()) {
        val attributeMap = mutableMapOf<String, String>()
        for (key in this.keys) {
            attributeMap[key] = this[key].toString()
        }
        attributeMap
    } else emptyMap()
}

fun AnalyticsEvent.recordCondition(condition: RecordCondition): AnalyticsEvent {
    this.putAttribute(RECORD_CONDITION, condition)
    return this
}
