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

package com.busybusy.answers_provider.loggers;

import android.support.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.answers_provider.LogHandler;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.HashMap;

/**
 * Implements the behavior of logging a custom event
 * @author John Hunt on 3/11/16.
 */
public class CustomEventLogger implements LogHandler
{
	private Answers answers;

	public CustomEventLogger(Answers answers)
	{
		this.answers = answers;
	}

	/**
	 * @see LogHandler
	 */
	@Override
	public void logSpecificEvent(@NonNull AnalyticsEvent event)
	{
		CustomEvent customEvent = buildCustomAnswersEvent(event);
		answers.logCustom(customEvent);
	}

	/**
	 * Constructs an Answers consumable event from the given {@code AnalyticsEvent}
	 * @param event the custom event containing data to submit to the Answers framework
	 * @return the instantiated {@code CustomEvent} object
	 */
	CustomEvent buildCustomAnswersEvent(@NonNull AnalyticsEvent event)
	{
		CustomEvent customEvent = new CustomEvent(event.name());
		HashMap<String, Object> attributeMap = event.getAttributes();

		// convert the attributes to to <String, String> to appease the Answers API
		if (attributeMap != null)
		{
			for (String key : attributeMap.keySet())
			{
				customEvent.putCustomAttribute(key, attributeMap.get(key).toString());
			}
		}

		return customEvent;
	}
}
