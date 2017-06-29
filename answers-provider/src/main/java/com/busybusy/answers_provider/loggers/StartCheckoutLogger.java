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
import com.crashlytics.android.answers.StartCheckoutEvent;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

/**
 * Implements the behavior of logging a StartCheckout event
 *
 * @author John Hunt on 3/11/16.
 */
public class StartCheckoutLogger implements LogHandler
{
	private Answers answers;

	public StartCheckoutLogger(Answers answers)
	{
		this.answers = answers;
	}

	@Override
	public void logSpecificEvent(@NonNull AnalyticsEvent event)
	{
		StartCheckoutEvent StartCheckoutEvent = buildAnswersStartCheckoutEvent(event);

		answers.logStartCheckout(StartCheckoutEvent);
	}

	/**
	 * Constructs an Answers consumable event from the given {@code AnalyticsEvent}
	 *
	 * @param event the StartCheckout event containing data to submit to the Answers framework
	 * @return the instantiated {@code StartCheckoutEvent} object
	 */
	@NonNull
	StartCheckoutEvent buildAnswersStartCheckoutEvent(@NonNull AnalyticsEvent event)
	{
		StartCheckoutEvent StartCheckoutEvent = new StartCheckoutEvent();

		Map<String, Object> attributeMap = event.getAttributes();
		if (attributeMap != null)
		{
			for (String key : attributeMap.keySet())
			{
				//noinspection IfCanBeSwitch (switch on String doesn't play nice on older devices)
				if (key.equals(Attributes.StartCheckout.TOTAL_PRICE))
				{
					StartCheckoutEvent.putTotalPrice((BigDecimal) attributeMap.get(key));
				}
				else if (key.equals(Attributes.StartCheckout.CURRENCY))
				{
					StartCheckoutEvent.putCurrency((Currency) attributeMap.get(key));
				}
				else if (key.equals(Attributes.StartCheckout.ITEM_COUNT))
				{
					StartCheckoutEvent.putItemCount((Integer) attributeMap.get(key));
				}
				else
				{
					StartCheckoutEvent.putCustomAttribute(key, attributeMap.get(key).toString());
				}
			}
		}

		return StartCheckoutEvent;
	}
}
