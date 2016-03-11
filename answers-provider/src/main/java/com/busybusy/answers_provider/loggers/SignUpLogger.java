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
import com.busybusy.answers_provider.Attributes;
import com.busybusy.answers_provider.LogHandler;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SignUpEvent;

import java.util.HashMap;

/**
 * Implements the behavior of logging a SignUp event
 * @author John Hunt on 3/11/16.
 */
public class SignUpLogger implements LogHandler
{
	@Override
	public void logSpecificEvent(@NonNull AnalyticsEvent event)
	{
		SignUpEvent SignUpEvent = buildAnswersSignUpEvent(event);

		// Add the time if this was a timed event
		Object durationObject = event.getAttribute(Attributes.EVENT_DURATION);
		if (event.isTimed() && durationObject != null)
		{
			String duration = (String) durationObject;
			SignUpEvent.putCustomAttribute(Attributes.EVENT_DURATION, duration);
		}

		Answers.getInstance().logSignUp(SignUpEvent);
	}

	/**
	 * Constructs an Answers consumable event from the given {@code AnalyticsEvent}
	 * @param event the custom event containing data to submit to the Answers framework
	 * @return the instantiated {@code SignUpEvent} object
	 */
	@NonNull
	private SignUpEvent buildAnswersSignUpEvent(@NonNull AnalyticsEvent event)
	{
		SignUpEvent SignUpEvent = new SignUpEvent();

		HashMap<String, Object> attributeMap = event.getAttributes();
		if (attributeMap != null)
		{
			for (String key : attributeMap.keySet())
			{
				//noinspection IfCanBeSwitch (switch on String doesn't play nice on older devices)
				if (key.equals(Attributes.SignUp.METHOD))
				{
					SignUpEvent.putMethod(attributeMap.get(key).toString());
				}
				else if (key.equals(Attributes.SignUp.SUCCESS))
				{
					SignUpEvent.putSuccess((Boolean) attributeMap.get(key));
				}
				else
				{
					SignUpEvent.putCustomAttribute(key, attributeMap.get(key).toString());
				}
			}
		}

		return SignUpEvent;
	}
}
