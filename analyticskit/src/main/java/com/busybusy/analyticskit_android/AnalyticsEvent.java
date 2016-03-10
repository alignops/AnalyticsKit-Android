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

package com.busybusy.analyticskit_android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

/**
 * Defines information that is needed to distribute the event to the registered analytics providers.
 * @author John Hunt on 3/5/16.
 */
public class AnalyticsEvent
{
	final String name;
	HashMap<String, Object> attributes;
	boolean timed;
	int providersMask = 0;

	/**
	 * Instantiates a new {@code AnalyticsEvent} object
	 * @param name the name of the event
	 */
	public AnalyticsEvent(@NonNull String name)
	{
		this.name = name;
		this.attributes = null;
		this.timed = false;
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
	 * Access a single attribute of this event
	 * @param name the name the of the attribute to retrieve
	 * @return the value associated with the given attribute name.
	 * Returns {@code null} if the attribute has not been set.
	 */
	@Nullable
	public Object getAttribute(@NonNull String name)
	{
		return this.attributes == null ? null : this.attributes.get(name);
	}

	/**
	 * Indicates if this event is a timed event
	 * @return {@code true} if the event has been set to be a timed event. Returns {@code false} otherwise.
	 */
	public boolean isTimed()
	{
		return this.timed;
	}

	/**
	 * Sets whether this event should capture timing
	 * @param timed {@code true} to set the event to track the time
	 * @return the {@link AnalyticsEvent} instance
	 */
	public AnalyticsEvent setTimed(boolean timed)
	{
		this.timed = timed;
		return this;
	}

	/**
	 * Sends the event out to the registered/specified providers.
	 * This is a convenience method that wraps {@link AnalyticsKit#logEvent(AnalyticsEvent)}
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
