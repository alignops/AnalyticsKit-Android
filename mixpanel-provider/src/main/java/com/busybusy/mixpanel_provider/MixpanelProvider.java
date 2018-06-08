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

package com.busybusy.mixpanel_provider;

import android.support.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

/**
 * Implements Mixpanel as a provider to use with {@link com.busybusy.analyticskit_android.AnalyticsKit}
 *
 * @author John Hunt on 3/16/16.
 */
public class MixpanelProvider implements AnalyticsKitProvider
{
	protected MixpanelAPI mixpanelApi;
	protected PriorityFilter priorityFilter;

	/**
	 * Initializes a new {@code MixpanelProvider} object
	 * @param mixpanelApiInstance just send {@code MixpanelAPI.getInstance(context, MIXPANEL_TOKEN)}
	 */
	public MixpanelProvider(@NonNull MixpanelAPI mixpanelApiInstance)
	{
		this(mixpanelApiInstance, new PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return true; // Log all events, regardless of priority
			}
		});
	}

	/**
	 * Initializes a new {@code MixpanelProvider} object
	 * @param mixpanelApiInstance just send {@code MixpanelAPI.getInstance(context, MIXPANEL_TOKEN)}
	 * @param priorityFilter the {@code PriorityFilter} to use when evaluating events
	 */
	public MixpanelProvider(@NonNull MixpanelAPI mixpanelApiInstance, @NonNull PriorityFilter priorityFilter)
	{
		this.mixpanelApi = mixpanelApiInstance;
		this.priorityFilter = priorityFilter;
	}

	/**
	 * Specifies the {@code PriorityFilter} to use when evaluating event priorities
	 * @param priorityFilter the filter to use
	 * @return the {@code MixpanelProvider} instance (for builder-style convenience)
	 */
	@NonNull
	public MixpanelProvider setPriorityFilter(@NonNull PriorityFilter priorityFilter)
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

	@Override
	public void sendEvent(@NonNull AnalyticsEvent event)
	{
		if (event.isTimed())
		{
			// start the Mixpanel SDK event timer for the event
			this.mixpanelApi.timeEvent(event.name());
		}
		else
		{
			this.mixpanelApi.trackMap(event.name(), event.getAttributes());
		}
	}

	@Override
	public void endTimedEvent(@NonNull AnalyticsEvent timedEvent)
	{
		this.mixpanelApi.trackMap(timedEvent.name(), timedEvent.getAttributes());
	}
}
