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

package com.busybusy.library.analyticskit_android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

/**
 * Defines information that is needed to distribute the event to the registered analytics providers.
 * Created by john on 3/5/16.
 */
public class AnalyticsEvent
{
	final String name;
	HashMap<String, Object> attributes;
	int providersMask = 0;


	public AnalyticsEvent(@NonNull String name)
	{
		this.name = name;
		this.attributes = null;
	}

	/**
	 * Access the event name
	 *
	 * @return the name of the custom event
	 */
	@NonNull
	public String name()
	{
		return this.name;
	}

	/**
	 * Adds an attribute to the event
	 *
	 * @param attributeName the name of the attribute (should be unique)
	 * @param value         the {@link Object} to associate with the name given
	 * @return the {@link AnalyticsEvent} instance
	 */
	@NonNull
	public AnalyticsEvent putAttribute(@NonNull String attributeName, @NonNull Object value)
	{
		// guard clause - make sure the dictionary is initialized
		if (this.attributes == null)
		{
			this.attributes = new HashMap<>();
		}

		this.attributes.put(attributeName, value);
		return this;
	}

	/**
	 * Restricts this event from being sent to all registered analytics providers.
	 * <p/>
	 * For example, if you have five registered providers, but only want to send this event to Google Analytics, call
	 *                      {@code specifyProviders(Providers.GOOGLE_ANALYTICS)}. To send this event to only Google Analytics and Answers,
	 *                      call {@code specifyProviders(Providers.GOOGLE_ANALYTICS | Providers.ANSWERS)}.
	 *
	 * @param providersMask an int value that contains the bitwise OR of the provider(s) to which you DO wish to send the event.
	 *
	 * @return the {@link AnalyticsEvent} instance
	 */
	@NonNull
	public AnalyticsEvent specifyProviders(int providersMask)
	{
		this.providersMask = providersMask;

		return this;
	}

	/**
	 * Specifies a certain provider to which this event will be sent (restricts this event from being sent to all registered analytics providers).
	 * <p/>
	 * For example, if you have five registered providers, but only want to send this event to Google Analytics, call
	 *                      {@code event.addProvider(Providers.GOOGLE_ANALYTICS)}. To send this event to only Google Analytics and Answers,
	 *                      call {@code event.addProvider(Providers.GOOGLE_ANALYTICS).addProvider(Providers.ANSWERS)}.
	 * @param providerMask an int value that contains the type of provider to which you DO wish to send the event.
	 * @return the {@link AnalyticsEvent} instance
	 */
	public AnalyticsEvent addProvider(int providerMask)
	{
		this.providersMask |= providerMask;
		return this;
	}

	/**
	 * Access the attributes of this event
	 *
	 * @return A non-empty map of attributes set on this event.
	 * Returns {@code null} if no attributes have been added to the event.
	 */
	@Nullable
	public HashMap<String, Object> getAttributes()
	{
		return this.attributes;
	}

	/**
	 * Sends the event out to the registered/specified providers.
	 */
	@NonNull
	public AnalyticsEvent send()
	{
		//noinspection ConstantConditions
		if (this.name == null)
		{
			throw new IllegalStateException("event name == null");
		}

		AnalyticsKit.getInstance().logEvent(this);
		return this;
	}

}
