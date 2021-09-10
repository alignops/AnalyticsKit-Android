/*
 * Copyright 2017 busybusy, Inc.
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

package com.busybusy.intercom_provider;

import android.app.Application;
import androidx.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;

import java.text.DecimalFormat;
import java.util.HashMap;

import io.intercom.android.sdk.Intercom;

/**
 * Implements Intercom as a provider to use with {@link com.busybusy.analyticskit_android.AnalyticsKit}
 *
 * @author John Hunt on 5/19/17.
 */

public class IntercomProvider implements AnalyticsKitProvider
{

	protected Intercom                        intercom;
	protected HashMap<String, AnalyticsEvent> timedEvents;
	protected HashMap<String, Long>           eventTimes;
	protected PriorityFilter                  priorityFilter;

	final int MAX_METADATA_ATTRIBUTES = 5;

	final String DURATION = "event_duration";

	/**
	 * Initializes a new {@code IntercomProvider} object
	 *
	 * @param intercom your already-initialized {@link Intercom} instance.
	 *                 Please call {@link Intercom#initialize(Application, String, String)} prior to setting up your {@link IntercomProvider}.
	 */
	public IntercomProvider(@NonNull Intercom intercom)
	{
		this(intercom, new PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return true; // Log all events, regardless of priority
			}
		});
	}

	/**
	 * Initializes a new {@code IntercomProvider} object
	 *
	 * @param intercom       your already-initialized {@link Intercom} instance.
	 *                       Please call {@link Intercom#initialize(Application, String, String)} prior to setting up your {@link IntercomProvider}.
	 * @param priorityFilter the {@code PriorityFilter} to use when evaluating events
	 */
	public IntercomProvider(@NonNull Intercom intercom, @NonNull PriorityFilter priorityFilter)
	{
		this.intercom = intercom;
		this.priorityFilter = priorityFilter;
	}

	/**
	 * Specifies the {@code PriorityFilter} to use when evaluating event priorities
	 *
	 * @param priorityFilter the filter to use
	 * @return the {@code IntercomProvider} instance (for builder-style convenience)
	 */
	public IntercomProvider setPriorityFilter(@NonNull PriorityFilter priorityFilter)
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
	 * Sends the event using provider-specific code
	 * @param event an instantiated event. <b>Note:</b> When sending timed events, be aware that this provider does not support concurrent timed events with the same name.
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
		else // Send the event through the Intercom SDK
		{
			logIntercomEvent(event);
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
			double        durationSeconds = (endTime - startTime) / 1000d;
			DecimalFormat df              = new DecimalFormat("#.###");
			finishedEvent.putAttribute(DURATION, df.format(durationSeconds));

			logIntercomEvent(finishedEvent);
		}
		else
		{
			throw new IllegalStateException("Attempted ending an event that was never started (or was previously ended): " + timedEvent.name());
		}
	}

	private void logIntercomEvent(@NonNull AnalyticsEvent event) throws IllegalStateException
	{
		// guard clause
		if (event.getAttributes() != null && event.getAttributes().keySet().size() > MAX_METADATA_ATTRIBUTES)
		{
			throw new IllegalStateException("Intercom does not support more than " + MAX_METADATA_ATTRIBUTES +
					                                " metadata fields. See https://docs.intercom.com/the-intercom-platform/track-events-in-intercom.");
		}

		Intercom.client().logEvent(event.name(), event.getAttributes());
	}
}
