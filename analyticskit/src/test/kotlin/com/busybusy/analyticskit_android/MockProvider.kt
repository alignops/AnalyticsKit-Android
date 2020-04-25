/*
 * Copyright 2016, 2020 busybusy, Inc.
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

/**
 * Provides an implementation of the [AnalyticsKitProvider] interface that facilitates testing.
 *
 * @author John Hunt on 3/8/16.
 */
class MockProvider : AnalyticsKitProvider {
    var sentEvents: MutableMap<String, AnalyticsEvent> = mutableMapOf()
    var eventTimes: MutableMap<String, Long> = mutableMapOf()
    var priorityLevel = 0
    var myPriorityFilter: PriorityFilter = PriorityFilter { priorityLevel -> priorityLevel <= this@MockProvider.priorityLevel }

    fun setPriorityUpperBound(priorityLevel: Int): MockProvider {
        this.priorityLevel = priorityLevel
        return this
    }

    override fun getPriorityFilter(): PriorityFilter = myPriorityFilter

    override fun sendEvent(event: AnalyticsEvent) {
        sentEvents[event.name()] = event
        if (event.isTimed) {
            val startTime = System.currentTimeMillis()
            eventTimes[event.name()] = startTime
        }
    }

    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        val endTime = System.currentTimeMillis()
        val startTime = eventTimes.remove(timedEvent.name())
        if (startTime != null) {
            timedEvent.putAttribute(EVENT_DURATION, endTime - startTime)
        }
    }

    companion object {
        const val EVENT_DURATION = "event_duration"
    }
}
