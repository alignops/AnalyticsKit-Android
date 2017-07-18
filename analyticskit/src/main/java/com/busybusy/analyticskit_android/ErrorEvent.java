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

/**
 * Defines information that is needed to distribute an "Error" event to the registered analytics providers.
 *
 * @author John Hunt on 4/6/16.
 */
public class ErrorEvent extends AnalyticsEvent
{
	public final String ERROR_MESSAGE    = "error_message";
	public final String EXCEPTION_OBJECT = "exception_object";
	public final String ERROR_OBJECT     = "error_object";

	/**
	 * Initializes a new {@code ErrorEvent} object.
	 */
	public ErrorEvent()
	{
		super(CommonEvents.ERROR);
	}

	/**
	 * Initializes a new {@code ErrorEvent} object.
	 *
	 * @param eventName the name of the {@code ErrorEvent}
	 */
	public ErrorEvent(@NonNull String eventName)
	{
		super(eventName);
	}

	/**
	 * Sets an error message on the {@code ErrorEvent}.
	 *
	 * @param errorMessage the message to set
	 * @return the {@code ErrorEvent} instance (for builder-style convenience)
	 */
	public ErrorEvent setMessage(@NonNull String errorMessage)
	{
		putAttribute(ERROR_MESSAGE, errorMessage);
		return this;
	}

	/**
	 * Access the error message.
	 *
	 * @return the error message set on this event. Returns {@code null} if the message was not set.
	 */
	@Nullable
	public String message()
	{
		Object value = attributes != null ? attributes.get(ERROR_MESSAGE) : null;
		return value != null ? String.valueOf(value) : null;
	}

	/**
	 * Sets an {@code Exception} object to associate with this event.
	 *
	 * @param exception the Exception object to store
	 * @return the {@code ErrorEvent} instance (for builder-style convenience)
	 */
	public ErrorEvent setException(@NonNull Exception exception)
	{
		putAttribute(EXCEPTION_OBJECT, exception);
		return this;
	}

	/**
	 * Access the {@link Exception} object.
	 *
	 * @return the {@code Exception} set on this event. Returns {@code null} if the exception was not set.
	 */
	@Nullable
	public Exception exception()
	{
		Object value = attributes != null ? attributes.get(EXCEPTION_OBJECT) : null;
		return value != null ? (Exception) value : null;
	}

	/**
	 * Sets an {@code Error} object to associate with this event.
	 *
	 * @param error the Error object to store
	 * @return the {@code ErrorEvent} instance (for builder-style convenience)
	 */
	public ErrorEvent setError(@NonNull Error error)
	{
		putAttribute(ERROR_OBJECT, error);
		return this;
	}

	/**
	 * Access the {@link Error} object.
	 *
	 * @return the {@code Error} set on this event. Returns {@code null} if the error was not set.
	 */
	@Nullable
	public Error error()
	{
		Object value = attributes != null ? attributes.get(ERROR_OBJECT) : null;
		return value != null ? (Error) value : null;
	}
}
