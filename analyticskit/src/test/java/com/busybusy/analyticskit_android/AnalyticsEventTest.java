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

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests the {@link AnalyticsEvent} class.
 *
 * @author John Hunt on 3/5/16.
 */
public class AnalyticsEventTest
{
	@Test
	public void testBuilder_withAttributes()
	{
		String name       = "Yeah, this should work";
		String animalText = "The quick brown fox jumps over the lazy dog";
		AnalyticsEvent event = new AnalyticsEvent(name)
				.putAttribute("the answer", 42)
				.putAttribute("animal text", animalText);

		assertThat(event.name()).isEqualTo(name);
		assertThat(event.getAttributes()).isNotNull();
		assertThat(event.getAttributes().keySet()).containsOnly("the answer", "animal text");
		assertThat(event.getAttributes().values()).containsOnly(42, animalText);
	}

	@Test
	public void testBuilder_noAttributes()
	{
		String         name  = "Yeah, this should work";
		AnalyticsEvent event = new AnalyticsEvent(name);

		assertThat(event.name()).isEqualTo(name);
		assertThat(event.getAttributes()).isNull();
	}

	@Test
	public void testBuilder_noName_willThrow()
	{
		AnalyticsEvent event = new AnalyticsEvent(null);

		boolean didThrow = false;

		try
		{
			event.send();
		}
		catch (IllegalStateException e)
		{
			didThrow = true;
		}

		assertThat(didThrow).isTrue();
	}

	@Test
	public void testBuilder_specifyPriority()
	{
		String name = "Priority 7 event";
		AnalyticsEvent event = new AnalyticsEvent(name)
				.setPriority(7);

		assertThat(event.name()).isEqualTo(name);
		assertThat(event.getAttributes()).isNull();
		assertThat(event.getPriority()).isEqualTo(7);
	}
}