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
import com.crashlytics.android.answers.ShareEvent;

import java.util.Map;

/**
 * Implements the behavior of logging a Share event
 *
 * @author John Hunt on 3/11/16.
 */
public class ShareLogger implements LogHandler
{
	private Answers answers;

	public ShareLogger(Answers answers)
	{
		this.answers = answers;
	}

	@Override
	public void logSpecificEvent(@NonNull AnalyticsEvent event)
	{
		ShareEvent ShareEvent = buildAnswersShareEvent(event);

		answers.logShare(ShareEvent);
	}

	/**
	 * Constructs an Answers consumable event from the given {@code AnalyticsEvent}
	 *
	 * @param event the Share event containing data to submit to the Answers framework
	 * @return the instantiated {@code ShareEvent} object
	 */
	@NonNull
	ShareEvent buildAnswersShareEvent(@NonNull AnalyticsEvent event)
	{
		ShareEvent ShareEvent = new ShareEvent();

		Map<String, Object> attributeMap = event.getAttributes();
		if (attributeMap != null)
		{
			for (String key : attributeMap.keySet())
			{
				//noinspection IfCanBeSwitch (switch on String doesn't play nice on older devices)
				if (key.equals(Attributes.Share.METHOD))
				{
					ShareEvent.putMethod(String.valueOf(attributeMap.get(key)));
				}
				else if (key.equals(Attributes.Share.CONTENT_NAME))
				{
					ShareEvent.putContentName(String.valueOf(attributeMap.get(key)));
				}
				else if (key.equals(Attributes.Share.CONTENT_TYPE))
				{
					ShareEvent.putContentType(String.valueOf(attributeMap.get(key)));
				}
				else if (key.equals(Attributes.Share.CONTENT_ID))
				{
					ShareEvent.putContentId(String.valueOf(attributeMap.get(key)));
				}
				else
				{
					ShareEvent.putCustomAttribute(key, attributeMap.get(key).toString());
				}
			}
		}

		return ShareEvent;
	}
}
