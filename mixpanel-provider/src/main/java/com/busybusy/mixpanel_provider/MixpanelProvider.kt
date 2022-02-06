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
package com.busybusy.mixpanel_provider

import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsEvent

/**
 * Implements Mixpanel as a provider to use with [com.busybusy.analyticskit_android.AnalyticsKit]
 *
 * @property mixpanelApi the initialized [MixpanelAPI] instance associated with the application.
 *                       Just send `MixpanelAPI.getInstance(context, MIXPANEL_TOKEN)`
 * @property priorityFilter the [PriorityFilter] to use when evaluating if an event should be sent to this provider's platform.
 *                          By default, this provider will log all events regardless of priority.
 *
 * @author John Hunt on 3/16/16.
 */
class MixpanelProvider(
    private val mixpanelApi: MixpanelAPI,
    private val priorityFilter: PriorityFilter = PriorityFilter { true },
) : AnalyticsKitProvider {

    /**
     * Returns the filter used to restrict events by priority.
     *
     * @return the {@link PriorityFilter} instance the provider is using to determine if an event of a given priority should be logged
     */
    override fun getPriorityFilter(): PriorityFilter = priorityFilter

    /**
     * Sends the event using provider-specific code.
     *
     * @param event an instantiated event
     */
    override fun sendEvent(event: AnalyticsEvent) = when {
        event.isTimed() -> mixpanelApi.timeEvent(event.name())
        else -> mixpanelApi.trackMap(event.name(), event.attributes)
    }

    /**
     * End the timed event.
     *
     * @param timedEvent the event which has finished
     */
    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        mixpanelApi.trackMap(timedEvent.name(), timedEvent.attributes)
    }
}
