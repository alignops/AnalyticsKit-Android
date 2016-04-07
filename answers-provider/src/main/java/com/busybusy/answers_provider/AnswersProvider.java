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

package com.busybusy.answers_provider;

import android.support.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.busybusy.answers_provider.loggers.AddToCartLogger;
import com.busybusy.answers_provider.loggers.ContentViewLogger;
import com.busybusy.answers_provider.loggers.CustomEventLogger;
import com.busybusy.answers_provider.loggers.InviteLogger;
import com.busybusy.answers_provider.loggers.LevelEndLogger;
import com.busybusy.answers_provider.loggers.LevelStartLogger;
import com.busybusy.answers_provider.loggers.LoginLogger;
import com.busybusy.answers_provider.loggers.PurchaseLogger;
import com.busybusy.answers_provider.loggers.RatedContentLogger;
import com.busybusy.answers_provider.loggers.SearchLogger;
import com.busybusy.answers_provider.loggers.ShareLogger;
import com.busybusy.answers_provider.loggers.SignUpLogger;
import com.busybusy.answers_provider.loggers.StartCheckoutLogger;
import com.crashlytics.android.answers.Answers;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;


/**
 * Implements Answers as a provider to use with {@link com.busybusy.analyticskit_android.AnalyticsKit}
 *
 * @author John Hunt on 3/10/16.
 */
public class AnswersProvider implements AnalyticsKitProvider
{
	protected Answers                         answers;
	protected HashMap<String, AnalyticsEvent> timedEvents;
	protected HashMap<String, Long>           eventTimes;
	protected PriorityFilter                  priorityFilter;

	Map<String, LogHandler> loggersMap = new HashMap<>();

	/**
	 * Initializes a new {@code AnswersProvider} object
	 * @param answers just send {@code Answers.getInstance()}
	 */
	public AnswersProvider(@NonNull Answers answers)
	{
		this(answers, new PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return true; // Log all events, regardless of priority
			}
		});
	}

	/**
	 * Initializes a new {@code AnswersProvider} object
	 * @param answers just send {@code Answers.getInstance()}
	 * @param priorityFilter the {@code PriorityFilter} to use when evaluating events
	 */
	public AnswersProvider(@NonNull Answers answers, @NonNull PriorityFilter priorityFilter)
	{
		this.answers = answers;
		this.priorityFilter = priorityFilter;
		initializeLoggersMap();
	}

	private void initializeLoggersMap()
	{
		loggersMap.put(PredefinedEvents.PURCHASE, new PurchaseLogger(answers));
		loggersMap.put(PredefinedEvents.ADD_TO_CART, new AddToCartLogger(answers));
		loggersMap.put(PredefinedEvents.START_CHECKOUT, new StartCheckoutLogger(answers));
		loggersMap.put(PredefinedEvents.CONTENT_VIEW, new ContentViewLogger(answers));
		loggersMap.put(PredefinedEvents.SEARCH, new SearchLogger(answers));
		loggersMap.put(PredefinedEvents.SHARE, new ShareLogger(answers));
		loggersMap.put(PredefinedEvents.RATED_CONTENT, new RatedContentLogger(answers));
		loggersMap.put(PredefinedEvents.SIGN_UP, new SignUpLogger(answers));
		loggersMap.put(PredefinedEvents.LOG_IN, new LoginLogger(answers));
		loggersMap.put(PredefinedEvents.INVITE, new InviteLogger(answers));
		loggersMap.put(PredefinedEvents.LEVEL_START, new LevelStartLogger(answers));
		loggersMap.put(PredefinedEvents.LEVEL_END, new LevelEndLogger(answers));
		loggersMap.put(PredefinedEvents.CUSTOM, new CustomEventLogger(answers));
	}

	/**
	 * Specifies the {@code PriorityFilter} to use when evaluating event priorities
	 * @param priorityFilter the filter to use
	 * @return the {@code AnswersProvider} instance (for builder-style convenience)
	 */
	public AnswersProvider setPriorityFilter(@NonNull PriorityFilter priorityFilter)
	{
		this.priorityFilter = priorityFilter;
		return this;
	}

	/**
	 * @see AnalyticsKitProvider
	 */
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
		else // Send the event through the Answers API
		{
			logAnswersEvent(event);
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
			double        durationSeconds = (endTime - startTime) / 1000;
			DecimalFormat df              = new DecimalFormat("#.###");
			finishedEvent.putAttribute(Attributes.EVENT_DURATION, df.format(durationSeconds));

			logAnswersEvent(finishedEvent);
		}
		else
		{
			throw new IllegalStateException("Attempted ending an event that was never started (or was previously ended): " + timedEvent.name());
		}
	}

	private void logAnswersEvent(AnalyticsEvent event)
	{
		// determine if this is a predefined event or a custom event
		LogHandler logHandler = this.loggersMap.get(event.name());
		if (logHandler == null)
		{
			// the event is not one of the pre-defined events - use the custom event handler
			logHandler = this.loggersMap.get(PredefinedEvents.CUSTOM);
		}

		// send event to the predefined event handler
		logHandler.logSpecificEvent(event);
	}
}
