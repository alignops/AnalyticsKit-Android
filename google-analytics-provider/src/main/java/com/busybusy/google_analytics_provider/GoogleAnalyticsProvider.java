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

package com.busybusy.google_analytics_provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.busybusy.analyticskit_android.ContentViewEvent;
import com.busybusy.analyticskit_android.ErrorEvent;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Implements Google Analytics as a provider to use with {@link com.busybusy.analyticskit_android.AnalyticsKit}
 * @author John Hunt on 5/4/16.
 */
public class GoogleAnalyticsProvider implements AnalyticsKitProvider
{
	protected HashMap<String, AnalyticsEvent> timedEvents;
	protected HashMap<String, Long>           eventTimes;
	protected PriorityFilter                  priorityFilter;
	protected Tracker                         tracker;
	
	/**
	 * Initializes a new {@code GoogleAnalyticsProvider} object
	 * @param tracker the initialized {@code Tracker} instance associated with the application
	 */
	public GoogleAnalyticsProvider(@NonNull Tracker tracker)
	{
		this(tracker, new PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return true; // Log all events, regardless of priority
			}
		});
	}

	/**
	 * Initializes a new {@code GoogleAnalyticsProvider} object
	 * @param tracker the initialized {@code Tracker} instance associated with the application
	 * @param priorityFilter the {@code PriorityFilter} to use when evaluating events
	 */
	public GoogleAnalyticsProvider(@NonNull Tracker tracker, @NonNull PriorityFilter priorityFilter)
	{
		this.tracker = tracker;
		this.priorityFilter = priorityFilter;
	}

	/**
	 * @see AnalyticsKitProvider
	 */
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
	public void sendEvent(@NonNull AnalyticsEvent event)
	{
		if (event.isTimed()) // Hang onto it until it is done
		{
			ensureTimeTrackingMaps();

			this.eventTimes.put(event.name(), System.currentTimeMillis());
			timedEvents.put(event.name(), event);
		}
		else // Send the event through the Google Analytics API
		{
			logGoogleAnalyticsEvent(event);
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
			finishedEvent.putAttribute("event_duration", df.format(durationSeconds));

			logGoogleAnalyticsEvent(finishedEvent);
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

	private void logGoogleAnalyticsEvent(AnalyticsEvent event)
	{
		if (event instanceof ContentViewEvent)
		{
			// Set the screen name.
			this.tracker.setScreenName(String.valueOf(event.getAttribute(ContentViewEvent.CONTENT_NAME)));

			HitBuilders.ScreenViewBuilder screenViewBuilder = new HitBuilders.ScreenViewBuilder();

			// Add any custom attributes that are attached to the event
			HashMap<String, String> parameterMap = stringifyParameters(event.getAttributes());
			if (parameterMap != null && parameterMap.size() > 0)
			{
				for (String key : parameterMap.keySet())
				{
					screenViewBuilder.set(key, parameterMap.get(key));
				}
			}

			// Send a screen view.
			this.tracker.send(screenViewBuilder.build());
		}
		else if (event instanceof ErrorEvent)
		{
			ErrorEvent errorEvent = (ErrorEvent) event;
			// Build and send exception.
			this.tracker.send(new HitBuilders.ExceptionBuilder()
					       .setDescription(errorEvent.message())
					       .setFatal(false)
					       .build());

			HitBuilders.ExceptionBuilder exceptionBuilder = new HitBuilders.ExceptionBuilder()
					.setDescription(errorEvent.message())
					.setFatal(false);

			// Add any custom attributes that are attached to the event
			HashMap<String, String> parameterMap = stringifyParameters(event.getAttributes());
			if (parameterMap != null && parameterMap.size() > 0)
			{
				for (String key : parameterMap.keySet())
				{
					exceptionBuilder.set(key, parameterMap.get(key));
				}
			}

			this.tracker.send(exceptionBuilder.build());
		}
		else
		{
			// Build and send an Event.
			HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
					.setCategory("User Event")
					.setAction(event.name());

			// Add any custom attributes that are attached to the event
			HashMap<String, String> parameterMap = stringifyParameters(event.getAttributes());
			if (parameterMap != null && parameterMap.size() > 0)
			{
				for (String key : parameterMap.keySet())
				{
					eventBuilder.set(key, parameterMap.get(key));
				}
			}

			this.tracker.send(eventBuilder.build());
		}
	}

	/**
	 * Converts a {@code HashMap<String, Object>} to {@code HashMap<String, String>}
	 * @param attributeMap the map of attributes attached to the event
	 * @return the String map of parameters. Returns {@code null} if no parameters are attached to the event.
	 */
	@Nullable
	HashMap<String, String> stringifyParameters(HashMap<String, Object> attributeMap)
	{
		HashMap<String, String> googleAnalyticsMap = null;

		// convert the attributes to to <String, String> to appease the GoogleAnalytics API
		if (attributeMap != null && attributeMap.size() > 0)
		{
			googleAnalyticsMap = new HashMap<>();
			for (String key : attributeMap.keySet())
			{
				googleAnalyticsMap.put(key, attributeMap.get(key).toString());
			}
		}

		return googleAnalyticsMap;
	}
}
