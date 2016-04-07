/*
 * Copyright 2016 Busy, LLC
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

package com.busybusy.analyticskit_android;

import android.support.annotation.NonNull;

/**
 * Defines the interface for provider plugins to be used with AnalyticsKit-Android.
 *
 * Note: in your provider implementation, make sure the underlying provider SDK calls are
 * executed asynchronously. Otherwise, you will have network operations running on the main thread.
 *
 * @author John Hunt on 3/5/16.
 */
public interface AnalyticsKitProvider
{
	/**
	 * Returns the filter used to restrict events by priority
	 * @return the {@link PriorityFilter} instance the provider is using to determine if an event of a given priority should be logged
	 */
	@NonNull
	PriorityFilter getPriorityFilter();

	/**
	 * Sends the event using provider-specific code
	 * @param event an instantiated event
	 */
	void sendEvent(@NonNull AnalyticsEvent event);

	/**
	 * End the timed event
	 * @param timedEvent the event which has finished
	 */
	void endTimedEvent(@NonNull AnalyticsEvent timedEvent);

	/**
	 * Defines the 'callback' interface providers will use to determine
	 * how to handle events of various priorities.
	 */
	interface PriorityFilter
	{
		/**
		 * Determines if a provider should log an event with a given priority
		 * @param priorityLevel the priority value from an {@link AnalyticsEvent} object
		 *                       (Generally {@link AnalyticsEvent#getPriority()})
		 * @return {@code true} if the event should be logged by the provider.
		 * Returns {@code false} otherwise.
		 */
		boolean shouldLog(int priorityLevel);
	}
}
