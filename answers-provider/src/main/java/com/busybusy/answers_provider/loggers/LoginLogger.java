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
import com.crashlytics.android.answers.LoginEvent;

import java.util.Map;

/**
 * Implements the behavior of logging a custom event
 *
 * @author John Hunt on 3/11/16.
 */
public class LoginLogger implements LogHandler
{
	private Answers answers;

	public LoginLogger(Answers answers)
	{
		this.answers = answers;
	}

	@Override
	public void logSpecificEvent(@NonNull AnalyticsEvent event)
	{
		LoginEvent loginEvent = buildAnswersLoginEvent(event);

		answers.logLogin(loginEvent);
	}

	/**
	 * Constructs an Answers consumable event from the given {@code AnalyticsEvent}
	 *
	 * @param event the custom event containing data to submit to the Answers framework
	 * @return the instantiated {@code LoginEvent} object
	 */
	@NonNull
	LoginEvent buildAnswersLoginEvent(@NonNull AnalyticsEvent event)
	{
		LoginEvent loginEvent = new LoginEvent();

		Map<String, Object> attributeMap = event.getAttributes();
		if (attributeMap != null)
		{
			for (String key : attributeMap.keySet())
			{
				//noinspection IfCanBeSwitch (switch on String doesn't play nice on older devices)
				if (key.equals(Attributes.LogIn.METHOD))
				{
					loginEvent.putMethod(attributeMap.get(key).toString());
				}
				else if (key.equals(Attributes.LogIn.SUCCESS))
				{
					loginEvent.putSuccess((Boolean) attributeMap.get(key));
				}
				else
				{
					loginEvent.putCustomAttribute(key, attributeMap.get(key).toString());
				}
			}
		}

		return loginEvent;
	}
}
