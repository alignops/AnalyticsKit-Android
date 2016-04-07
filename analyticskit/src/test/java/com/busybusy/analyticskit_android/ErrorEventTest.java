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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ErrorEvent} class
 * @author John Hunt on 4/6/16.
 */
public class ErrorEventTest
{
	@Test
	public void testConstructor()
	{
		AnalyticsEvent event = new ErrorEvent();
		assertEquals(CommonEvents.ERROR, event.name());

		event = new ErrorEvent("Custom Error Name");
		assertEquals(event.name(), "Custom Error Name");
	}

	@Test
	public void testSetAndGetMessage()
	{
		String message = "Something went wrong";
		ErrorEvent event = new ErrorEvent()
				.setMessage(message);

		assertEquals(message, event.message());
	}

	@Test
	public void testSetAndGetException()
	{
		//noinspection ThrowableInstanceNeverThrown
		Exception exception = new StringIndexOutOfBoundsException();
		ErrorEvent event = new ErrorEvent()
				.setException(exception);

		assertEquals(exception, event.exception());
	}

	@Test
	public void testSetAndGetError()
	{
		//noinspection ThrowableInstanceNeverThrown
		Error error = new UnknownError();
		ErrorEvent event = new ErrorEvent()
				.setError(error);

		assertEquals(error, event.error());
	}
}
