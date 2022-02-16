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

/**
 * Presents an interface for logging Analytics events across multiple analytics providers.
 *
 * @author John Hunt on 3/4/16.
 */
object AnalyticsKit {
    val providers: MutableSet<AnalyticsKitProvider> = mutableSetOf()
    val timedEvents: MutableMap<String, AnalyticsEvent> by lazy { mutableMapOf() }

    /**
     * Returns the AnalyticsKit singleton.
     *
     * @return the singleton instance
     */
    fun getInstance(): AnalyticsKit = this

    /**
     * Registers an `AnalyticsKitProvider` instance to receive future events.
     *
     * @param provider the `AnalyticsKitProvider` to notify on future calls to [AnalyticsKit.logEvent].
     * @return the `AnalyticsKit` instance so multiple calls to `registerProvider(AnalyticsKitProvider)` can be chained.
     */
    fun registerProvider(provider: AnalyticsKitProvider): AnalyticsKit {
        providers.add(provider)
        return this
    }

    /**
     * Sends the given event to all registered analytics providers (OR just to select providers if the event has been set to restrict the providers).
     *
     * @param event the event to capture with analytics tools
     */
    @Throws(IllegalStateException::class)
    fun logEvent(event: AnalyticsEvent) {
        if (event.isTimed()) {
            timedEvents[event.name()] = event
        }
        // No else needed: no need to worry about hanging on to this event

        for (provider in providers) {
            if (provider.getPriorityFilter().shouldLog(event.getPriority())) {
                provider.sendEvent(event)
            }
            // No else needed: the provider doesn't care about logging events of the specified priority
        }
    }

    /**
     * Marks the end of a timed event.
     *
     * @param eventName the unique name of the event that has finished
     */
    @Throws(java.lang.IllegalStateException::class)
    fun endTimedEvent(eventName: String) {
        val timedEvent = timedEvents.remove(eventName)
        when {
            timedEvent != null -> for (provider in providers) {
                if (provider.getPriorityFilter().shouldLog(timedEvent.getPriority())) {
                    provider.endTimedEvent(timedEvent)
                }
                // No else needed: the provider doesn't care about logging events of the specified priority
            }
            else -> error("Attempted ending an event that was never started (or was previously ended): $eventName")
        }
    }

    /**
     * Marks the end of a timed event.
     *
     * @param event the event that has finished
     */
    fun endTimedEvent(event: AnalyticsEvent) = endTimedEvent(event.name())
}
