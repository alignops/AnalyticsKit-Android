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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines information that is needed to distribute the event to the registered analytics providers.
 *
 * @author John Hunt on 3/5/16.
 */
public class AnalyticsEvent implements Serializable
{
	private static final long serialVersionUID = 8237206047809063471L;

	final String name;
	Map<String, Object> attributes;
	boolean             timed;
	int priorityLevel = 0;

	/**
	 * Instantiates a new {@code AnalyticsEvent} object.
	 *
	 * @param name the name of the event
	 */
	public AnalyticsEvent(@NonNull String name)
	{
		this.name = name;
		this.attributes = null;
		this.timed = false;
	}

	/**
	 * Access the event name.
	 *
	 * @return the name of the custom event
	 */
	@NonNull
	public String name()
	{
		return this.name;
	}

	/**
	 * Adds an attribute to the event.
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
			this.attributes = new LinkedHashMap<>();
		}

		this.attributes.put(attributeName, value);
		return this;
	}

	/**
	 * Gets the priority of this event.
	 *
	 * @return the priority of the event. Returns {@code 0} when {@link #setPriority(int)} has not been called.
	 */
	public int getPriority()
	{
		return priorityLevel;
	}

	/**
	 * Sets the priority of the event. The event defaults to {@code 0} when this method is not called.
	 * <p/>
	 * <b>Note:</b> It is up to the developer to define what priority scheme to use (if any).
	 *
	 * @param priorityLevel the priority the event should use
	 * @return the {@link AnalyticsEvent} instance (for builder-style convenience)
	 */
	public AnalyticsEvent setPriority(int priorityLevel)
	{
		this.priorityLevel = priorityLevel;
		return this;
	}

	/**
	 * Access the attributes of this event.
	 *
	 * @return A non-empty map of attributes set on this event.
	 * Returns {@code null} if no attributes have been added to the event.
	 */
	@Nullable
	public Map<String, Object> getAttributes()
	{
		return this.attributes;
	}

	/**
	 * Access a single attribute of this event.
	 *
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
	 * Indicates if this event is a timed event.
	 *
	 * @return {@code true} if the event has been set to be a timed event. Returns {@code false} otherwise.
	 */
	public boolean isTimed()
	{
		return this.timed;
	}

	/**
	 * Sets whether this event should capture timing.
	 *
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

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (other == null || getClass() != other.getClass())
		{
			return false;
		}

		AnalyticsEvent that = (AnalyticsEvent) other;

		if (timed != that.timed)
		{
			return false;
		}
		if (priorityLevel != that.priorityLevel)
		{
			return false;
		}
		if (name != null ? !name.equals(that.name) : that.name != null)
		{
			return false;
		}
		return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;

	}

	@Override
	public int hashCode()
	{
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
		result = 31 * result + (timed ? 1 : 0);
		result = 31 * result + priorityLevel;
		return result;
	}

	@Override
	public String toString()
	{
		return "AnalyticsEvent{" +
				"name='" + name + '\'' +
				", attributes=" + attributes +
				", timed=" + timed +
				", priorityLevel=" + priorityLevel +
				'}';
	}
}
