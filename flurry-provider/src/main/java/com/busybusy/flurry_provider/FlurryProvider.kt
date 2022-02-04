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
package com.busybusy.flurry_provider

import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsEvent
import com.flurry.android.FlurryAgent
import java.lang.IllegalStateException

/**
 * Implements Flurry as a provider to use with [com.busybusy.analyticskit_android.AnalyticsKit]
 *
 *
 * **Important**: It is a violation of Flurry’s TOS to record personally identifiable information such as a user’s UDID,
 * email address, and so on using Flurry. If you have a user login that you wish to associate with your session and
 * event data, you should use the SetUserID function. If you do choose to record a user id of any type within a parameter,
 * you must anonymize the data using a hashing function such as MD5 or SHA256 prior to calling the method.
 *
 *
 * Refer to the Flurry documentation here:
 * [Flurry Documentation](https://developer.yahoo.com/flurry/docs/analytics/gettingstarted/events/android/)
 *
 * @author John Hunt on 3/21/16.
 */
class FlurryProvider(
    private var priorityFilter: PriorityFilter = PriorityFilter { true },
) : AnalyticsKitProvider {

    val ATTRIBUTE_LIMIT = 10

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
    @Throws(IllegalStateException::class)
    override fun sendEvent(event: AnalyticsEvent) {
        val eventParams: Map<String, String>? = stringifyParameters(event.attributes)
        if (event.isTimed) {
            // start the Flurry SDK event timer for the event
            when (eventParams) {
                null -> FlurryAgent.logEvent(event.name(), true)
                else -> FlurryAgent.logEvent(event.name(), eventParams, true)
            }
        } else {
            when (eventParams) {
                null -> FlurryAgent.logEvent(event.name())
                else -> FlurryAgent.logEvent(event.name(), eventParams)
            }
        }
    }

    /**
     * End the timed event.
     *
     * @param timedEvent the event which has finished
     */
    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        FlurryAgent.endTimedEvent(timedEvent.name())
    }

    /**
     * Converts a `Map<String, Any>` to `Map<String, String>`
     * @param attributeMap the map of attributes attached to the event
     * @return the String map of parameters. Returns `null` if no parameters are attached to the event.
     */
    @Throws(IllegalStateException::class)
    fun stringifyParameters(attributeMap: Map<String, Any>?): Map<String, String>? {
        var flurryMap: MutableMap<String, String>? = null

        // convert the attributes to to <String, String> to appease the Flurry API
        if (attributeMap != null && attributeMap.isNotEmpty()) {
            check(attributeMap.size <= ATTRIBUTE_LIMIT) { "Flurry events are limited to $ATTRIBUTE_LIMIT attributes" }
            flurryMap = mutableMapOf()
            for (key in attributeMap.keys) {
                flurryMap[key] = attributeMap[key].toString()
            }
        }
        return flurryMap
    }
}
