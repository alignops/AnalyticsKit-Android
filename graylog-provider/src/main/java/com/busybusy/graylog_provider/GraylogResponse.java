/*
 * Copyright 2017 busybusy, Inc.
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

package com.busybusy.graylog_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;

/**
 * POJO containing the HTTP response code, HTTP response message, and the event
 *
 * @author John Hunt on 6/28/17.
 */

public class GraylogResponse
{
	final int            code;
	final String         message;
	final AnalyticsEvent event;

	/**
	 * Initializes a new {@link GraylogResponse} object.
	 *
	 * @param code    the HTTP response code
	 * @param message the HTTP response message
	 * @param event   the {@link AnalyticsEvent} that was sent via HTTP to your Graylog input
	 */
	public GraylogResponse(int code, String message, AnalyticsEvent event)
	{
		this.code = code;
		this.message = message;
		this.event = event;
	}

	/**
	 * Retrieves the HTTP response code.
	 *
	 * @return The HTTP response code. Returns {@code 420} if there was some problem communicating with the Graylog server.
	 */
	public int code()
	{
		return code;
	}

	/**
	 * Retreives the HTTP response message
	 *
	 * @return the message.
	 */
	public String message()
	{
		return message;
	}

	/**
	 * Retrieves the {@link AnalyticsEvent} that was sent to the Graylog server.
	 *
	 * @return the event
	 */
	public AnalyticsEvent event()
	{
		return event;
	}
}
