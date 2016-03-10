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

import java.util.HashMap;

/**
 * Provides an implementation of the {@link AnalyticsKitProvider} interface that facilitates testing.
 * @author John Hunt on 3/8/16.
 */
public class MockProvider implements AnalyticsKitProvider
{
	final int type;
	HashMap<String, AnalyticsEvent> sentEvents;
	HashMap<String, Long> eventTimes;

	public static final String EVENT_DURATION = "event_duration";


	public MockProvider(int type)
	{
		this.type = type;
		this.sentEvents = new HashMap<>();
		this.eventTimes = new HashMap<>();
	}

	@Override
	public int getType()
	{
		return this.type;
	}

	@Override
	public void sendEvent(@NonNull AnalyticsEvent event)
	{
		sentEvents.put(event.name(), event);

		if (event.isTimed())
		{
			long startTime = System.currentTimeMillis();
			this.eventTimes.put(event.name(), startTime);
		}
	}

	@Override
	public void endTimedEvent(@NonNull AnalyticsEvent timedEvent)
	{
		long endTime = System.currentTimeMillis();
		Long startTime = this.eventTimes.remove(timedEvent.name());

		if (startTime != null)
		{
			timedEvent.putAttribute(EVENT_DURATION, endTime - startTime);
		}
	}
}
