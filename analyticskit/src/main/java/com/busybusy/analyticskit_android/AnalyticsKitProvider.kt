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

import androidx.annotation.NonNull

/**
 * Defines the interface for provider plugins to be used with AnalyticsKit-Android.
 *
 *
 * Note: in your provider implementation, make sure the underlying provider SDK calls are
 * executed asynchronously. Otherwise, you will have network operations running on the main thread.
 *
 * @author John Hunt on 3/5/16.
 */
interface AnalyticsKitProvider {
    /**
     * Returns the filter used to restrict events by priority.
     *
     * @return the [PriorityFilter] instance the provider is using to determine if an event of a given priority should be logged
     */
    @NonNull
    fun getPriorityFilter() : PriorityFilter

    /**
     * Sends the event using provider-specific code.
     *
     * @param event an instantiated event
     */
    fun sendEvent(event: AnalyticsEvent)

    /**
     * End the timed event.
     *
     * @param timedEvent the event which has finished
     */
    fun endTimedEvent(timedEvent: AnalyticsEvent)

    /**
     * Defines the 'callback' interface providers will use to determine
     * how to handle events of various priorities.
     */
    fun interface PriorityFilter {
        /**
         * Determines if a provider should log an event with a given priority
         *
         * @param priorityLevel the priority value from an [AnalyticsEvent] object
         * (Generally [AnalyticsEvent.getPriority])
         * @return `true` if the event should be logged by the provider.
         * Returns `false` otherwise.
         */
        fun shouldLog(priorityLevel: Int): Boolean
    }
}
