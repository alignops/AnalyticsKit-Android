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

import androidx.annotation.Nullable
import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.ContentViewEvent
import com.busybusy.analyticskit_android.ErrorEvent
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.HitBuilders.ExceptionBuilder
import com.google.android.gms.analytics.HitBuilders.ScreenViewBuilder
import com.google.android.gms.analytics.HitBuilders.TimingBuilder
import com.google.android.gms.analytics.Tracker

/**
 * Implements Google Analytics as a provider to use with [com.busybusy.analyticskit_android.AnalyticsKit]
 * @property tracker    the initialized `Tracker` instance associated with the application
 * @property priorityFilter the `PriorityFilter` to use when evaluating events, defaults to
 *                          logging all events regardless of priority
 *
 * @author John Hunt on 5/4/16.
 */
class GoogleAnalyticsProvider(
    private val tracker: Tracker,
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
        if (event.isTimed()) { // Hang onto it until it is done
            eventTimes[event.name()] = System.currentTimeMillis()
            timedEvents[event.name()] = event
        } else { // Send the event through the Google Analytics API
            logGoogleAnalyticsEvent(event)
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
            val timingBuilder = TimingBuilder()
            timingBuilder.setLabel(finishedEvent.name())
                .setCategory("Timed Events")
                .setValue(endTime - startTime)
            // add any custom attributes already set on the event
            timingBuilder.setAll(stringifyAttributesMap(finishedEvent.attributes))
            tracker.send(timingBuilder.build())
        } else {
            error("Attempted ending an event that was never started (or was previously ended): ${timedEvent.name()}")
        }
    }

    private fun logGoogleAnalyticsEvent(event: AnalyticsEvent) {
        when (event) {
            is ContentViewEvent -> {
                val screenViewBuilder = ScreenViewBuilder()
                // add any custom attributes already set on the event
                screenViewBuilder.setAll(stringifyAttributesMap(event.attributes))
                synchronized(tracker) { // Set the screen name and send a screen view.
                    tracker.setScreenName(
                        event.getAttribute(ContentViewEvent.CONTENT_NAME).toString()
                    )
                    tracker.send(screenViewBuilder.build())
                }
            }
            is ErrorEvent -> { // Build and send exception.
                val exceptionBuilder = ExceptionBuilder()
                    .setDescription(event.message())
                    .setFatal(false)

                // Add any custom attributes that are attached to the event
                exceptionBuilder.setAll(stringifyAttributesMap(event.attributes))
                tracker.send(exceptionBuilder.build())
            }
            else -> { // Build and send an Event.
                val eventBuilder = HitBuilders.EventBuilder()
                    .setCategory("User Event")
                    .setAction(event.name())

                // Add any custom attributes that are attached to the event
                eventBuilder.setAll(stringifyAttributesMap(event.attributes))
                tracker.send(eventBuilder.build())
            }
        }
    }

    /**
     * Converts a `Map<String, Any>` to a `Map<String, String>` (nullable)
     *
     * @param attributes the map of attributes attached to the event
     * @return the String map of parameters. Returns `null` if no parameters are attached to the event.
     */
    @Nullable
    fun stringifyAttributesMap(attributes: Map<String, Any>?): Map<String, String>? {
        return attributes?.map { (key, value) -> key to value.toString() }
            ?.takeIf { it.isNotEmpty() }
            ?.toMap()
    }
}
