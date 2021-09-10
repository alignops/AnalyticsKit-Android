/*
 * Copyright 2018, 2021 busybusy, Inc.
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

package com.busybusy.firebase_provider;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider that facilitates reporting events to Firebase Analytics.
 * We recommend using the {@link FirebaseAnalytics.Param} names for attributes as much as possible due to the restriction on the number of
 * custom parameters in Firebase.
 *
 * @see <a href="https://firebase.google.com/docs/analytics/android/events">Log Events</a>
 * @see <a href="https://support.google.com/firebase/answer/7397304?hl=en&ref_topic=6317489">Custom-parameter reporting</a>
 */
public class FirebaseProvider implements AnalyticsKitProvider
{
	protected       Map<String, AnalyticsEvent> timedEvents;
	protected       Map<String, Long>           eventTimes;
	protected       PriorityFilter              priorityFilter;
	protected final FirebaseAnalytics           firebaseAnalytics;

	/**
	 * Initializes a new {@code FirebaseProvider} object.
	 *
	 * @param firebaseAnalytics the initialized {@code FirebaseAnalytics} instance associated with the application
	 */
	public FirebaseProvider(@NonNull FirebaseAnalytics firebaseAnalytics)
	{
		this(firebaseAnalytics, new PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return true; // Log all events, regardless of priority
			}
		});
	}

	/**
	 * Initializes a new {@code FirebaseProvider} object
	 *
	 * @param firebaseAnalytics the initialized {@code FirebaseAnalytics} instance associated with the application
	 * @param priorityFilter    the {@code PriorityFilter} to use when evaluating events
	 */
	public FirebaseProvider(@NonNull FirebaseAnalytics firebaseAnalytics, @NonNull PriorityFilter priorityFilter)
	{
		this.firebaseAnalytics = firebaseAnalytics;
		this.priorityFilter = priorityFilter;
	}

	/**
	 * Specifies the {@code PriorityFilter} to use when evaluating event priorities.
	 *
	 * @param priorityFilter the filter to use
	 * @return the {@code GoogleAnalyticsProvider} instance (for builder-style convenience)
	 */
	public FirebaseProvider setPriorityFilter(@NonNull PriorityFilter priorityFilter)
	{
		this.priorityFilter = priorityFilter;
		return this;
	}

	@NonNull
	@Override
	public PriorityFilter getPriorityFilter()
	{
		return priorityFilter;
	}

	/**
	 * @see AnalyticsKitProvider
	 */
	@Override
	public void sendEvent(@NonNull AnalyticsEvent event)
	{
		if (event.isTimed()) // Hang onto it until it is done
		{
			ensureTimeTrackingMaps();

			this.eventTimes.put(event.name(), System.currentTimeMillis());
			timedEvents.put(event.name(), event);
		}
		else // Send the event through the Firebase Analytics API
		{
			logFirebaseAnalyticsEvent(event);
		}
	}

	/**
	 * @see AnalyticsKitProvider
	 */
	@Override
	public void endTimedEvent(@NonNull AnalyticsEvent timedEvent) throws IllegalStateException
	{
		ensureTimeTrackingMaps();

		long           endTime       = System.currentTimeMillis();
		Long           startTime     = this.eventTimes.remove(timedEvent.name());
		AnalyticsEvent finishedEvent = this.timedEvents.remove(timedEvent.name());

		if (startTime != null && finishedEvent != null)
		{
			double        durationSeconds = (endTime - startTime) / 1000;
			DecimalFormat df              = new DecimalFormat("#.###");
			finishedEvent.putAttribute(FirebaseAnalytics.Param.VALUE, df.format(durationSeconds));
			logFirebaseAnalyticsEvent(finishedEvent);
		}
		else
		{
			throw new IllegalStateException("Attempted ending an event that was never started (or was previously ended): " + timedEvent.name());
		}
	}

	private void ensureTimeTrackingMaps()
	{
		if (this.eventTimes == null)
		{
			eventTimes = new HashMap<>(); // lazy initialization
		}
		if (this.timedEvents == null)
		{
			timedEvents = new HashMap<>(); // lazy initialization
		}
	}

	private void logFirebaseAnalyticsEvent(@NonNull AnalyticsEvent event)
	{
		Bundle              parameterBundle = null;
		Map<String, Object> attributes      = event.getAttributes();
		if (attributes != null && !attributes.isEmpty())
		{
			parameterBundle = new Bundle();
			for (String key : attributes.keySet())
			{
				parameterBundle.putSerializable(key, getCheckAndCast(attributes, key));
			}
		}

		firebaseAnalytics.logEvent(event.name(), parameterBundle);
	}

	private <ObjectType extends Serializable> ObjectType getCheckAndCast(@NonNull Map<String, Object> map, @NonNull String key)
	{
		Serializable result = (Serializable) map.get(key);
		return (ObjectType) result;
	}
}
