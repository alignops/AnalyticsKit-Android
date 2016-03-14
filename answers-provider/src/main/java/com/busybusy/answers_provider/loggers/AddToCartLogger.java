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
import com.crashlytics.android.answers.AddToCartEvent;
import com.crashlytics.android.answers.Answers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;

/**
 * Implements the behavior of logging an AddToCart event
 * @author John Hunt on 3/11/16.
 */
public class AddToCartLogger implements LogHandler
{
	private Answers answers;

	public AddToCartLogger(Answers answers)
	{
		this.answers = answers;
	}

	@Override
	public void logSpecificEvent(@NonNull AnalyticsEvent event)
	{
		AddToCartEvent addToCartEvent = buildAnswersAddToCartEvent(event);

		answers.logAddToCart(addToCartEvent);
	}

	/**
	 * Constructs an Answers consumable event from the given {@code AnalyticsEvent}
	 * @param event the AddToCart event containing data to submit to the Answers framework
	 * @return the instantiated {@code AddToCartEvent} object
	 */
	@NonNull
	AddToCartEvent buildAnswersAddToCartEvent(@NonNull AnalyticsEvent event)
	{
		AddToCartEvent AddToCartEvent = new AddToCartEvent();

		HashMap<String, Object> attributeMap = event.getAttributes();
		if (attributeMap != null)
		{
			for (String key : attributeMap.keySet())
			{
				//noinspection IfCanBeSwitch (switch on String doesn't play nice on older devices)
				if (key.equals(Attributes.AddToCart.ITEM_PRICE))
				{
					AddToCartEvent.putItemPrice((BigDecimal) attributeMap.get(key));
				}
				else if (key.equals(Attributes.AddToCart.CURRENCY))
				{
					AddToCartEvent.putCurrency((Currency) attributeMap.get(key));
				}
				else if (key.equals(Attributes.AddToCart.ITEM_NAME))
				{
					AddToCartEvent.putItemName(String.valueOf(attributeMap.get(key)));
				}
				else if (key.equals(Attributes.AddToCart.ITEM_TYPE))
				{
					AddToCartEvent.putItemType(String.valueOf(attributeMap.get(key)));
				}
				else if (key.equals(Attributes.AddToCart.ITEM_ID))
				{
					AddToCartEvent.putItemId(String.valueOf(attributeMap.get(key)));
				}
				else
				{
					AddToCartEvent.putCustomAttribute(key, attributeMap.get(key).toString());
				}
			}
		}

		return AddToCartEvent;
	}
}
