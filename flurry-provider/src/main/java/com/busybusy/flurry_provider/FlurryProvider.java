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

package com.busybusy.flurry_provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.flurry.android.FlurryAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements Flurry as a provider to use with {@link com.busybusy.analyticskit_android.AnalyticsKit}
 * <p/>
 * <b>Important</b>: It is a violation of Flurry’s TOS to record personally identifiable information such as a user’s UDID,
 * email address, and so on using Flurry. If you have a user login that you wish to associate with your session and
 * event data, you should use the SetUserID function. If you do choose to record a user id of any type within a parameter,
 * you must anonymize the data using a hashing function such as MD5 or SHA256 prior to calling the method.
 * <p/>
 * Refer to the Flurry documentation here:
 * <a href="https://developer.yahoo.com/flurry/docs/analytics/gettingstarted/events/android/">Flurry Documentation</a>
 *
 * @author John Hunt on 3/21/16.
 */
public class FlurryProvider implements AnalyticsKitProvider
{
	final int ATTRIBUTE_LIMIT = 10;
	protected PriorityFilter priorityFilter;

	/**
	 * Initializes a new {@code FlurryProvider} object
	 */
	public FlurryProvider()
	{
		this(new PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return true; // Log all events, regardless of priority
			}
		});
	}

	/**
	 * Initializes a new {@code FlurryProvider} object
	 * @param priorityFilter the {@code PriorityFilter} to use when evaluating events
	 */
	public FlurryProvider(PriorityFilter priorityFilter)
	{
		this.priorityFilter = priorityFilter;
	}

	/**
	 * Specifies the {@code PriorityFilter} to use when evaluating event priorities
	 * @param priorityFilter the filter to use
	 * @return the {@code FlurryProvider} instance (for builder-style convenience)
	 */
	public FlurryProvider setPriorityFilter(@NonNull PriorityFilter priorityFilter)
	{
		this.priorityFilter = priorityFilter;
		return this;
	}

	@NonNull
	@Override
	public PriorityFilter getPriorityFilter()
	{
		return this.priorityFilter;
	}

	/**
	 * @see AnalyticsKitProvider
	 */
	@Override
	public void sendEvent(@NonNull AnalyticsEvent event) throws IllegalStateException
	{
		Map<String, String> eventParams = stringifyParameters(event.getAttributes());

		if (event.isTimed())
		{
			// start the Flurry SDK event timer for the event
			if (eventParams != null)
			{
				FlurryAgent.logEvent(event.name(), eventParams, true);
			}
			else
			{
				FlurryAgent.logEvent(event.name(), true);
			}
		}
		else
		{
			if (eventParams != null)
			{
				FlurryAgent.logEvent(event.name(), eventParams);
			}
			else
			{
				FlurryAgent.logEvent(event.name());
			}
		}
	}

	/**
	 * @see AnalyticsKitProvider
	 */
	@Override
	public void endTimedEvent(@NonNull AnalyticsEvent timedEvent)
	{
		FlurryAgent.endTimedEvent(timedEvent.name());
	}

	/**
	 * Converts a {@code HashMap<String, Object>} to {@code HashMap<String, String>}
	 * @param attributeMap the map of attributes attached to the event
	 * @return the String map of parameters. Returns {@code null} if no parameters are attached to the event.
	 */
	@Nullable
	HashMap<String, String> stringifyParameters(Map<String, Object> attributeMap) throws IllegalStateException
	{
		HashMap<String, String> flurryMap = null;

		// convert the attributes to to <String, String> to appease the Flurry API
		if (attributeMap != null && attributeMap.size() > 0)
		{
			if (attributeMap.size() > ATTRIBUTE_LIMIT)
			{
				throw new IllegalStateException("Flurry events are limited to " + ATTRIBUTE_LIMIT + " attributes");
			}

			flurryMap = new HashMap<>();
			for (String key : attributeMap.keySet())
			{
				flurryMap.put(key, attributeMap.get(key).toString());
			}
		}

		return flurryMap;
	}
}
