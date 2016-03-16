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
 * Tests the {@link ContentViewEvent} class
 * @author John Hunt on 3/16/16.
 */
public class ContentViewEventTest
{
	@Test
	public void testConstructor()
	{
		AnalyticsEvent event = new ContentViewEvent();

		assertEquals(CommonEvents.CONTENT_VIEW, event.name());
	}
}
