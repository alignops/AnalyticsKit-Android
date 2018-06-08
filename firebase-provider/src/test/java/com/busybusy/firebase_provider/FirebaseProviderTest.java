/*
 * Copyright 2018 busybusy, Inc.
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

package com.busybusy.firebase_provider;

import android.os.Bundle;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.busybusy.analyticskit_android.CommonEvents;
import com.busybusy.analyticskit_android.ContentViewEvent;
import com.busybusy.analyticskit_android.ErrorEvent;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.EmptyStackException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link FirebaseProvider} class
 *
 * @author John Hunt on 3/21/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27, constants = com.busybusy.firebase_provider.BuildConfig.class, manifest = Config.NONE)
public class FirebaseProviderTest
{
	FirebaseProvider  provider;
	FirebaseAnalytics firebaseAnalytics;

	boolean sendCalled;
	String  testEventName;
	Bundle  testBundle;

	@Before
	public void setup()
	{
		// Mock behavior for when FirebaseAnalytics logEvent() is called
		firebaseAnalytics = mock(FirebaseAnalytics.class);
		doAnswer(new Answer<Void>()
		{
			public Void answer(InvocationOnMock invocation)
			{
				Object[] args = invocation.getArguments();
				//noinspection ConstantConditions
				testEventName = (String) args[0];
				testBundle = (Bundle) args[1];
				sendCalled = true;
				return null;
			}
		}).when(firebaseAnalytics).logEvent(anyString(), (Bundle) any());

		provider = new FirebaseProvider(firebaseAnalytics);

		sendCalled = false;
		testEventName = null;
		testBundle = null;
	}

	@Test
	public void testSetAndGetPriorityFilter()
	{
		AnalyticsKitProvider.PriorityFilter filter = new AnalyticsKitProvider.PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return false;
			}
		};

		provider.setPriorityFilter(filter);

		assertThat(provider.getPriorityFilter()).isEqualTo(filter);
	}

	@Test
	public void test_priorityFiltering_default()
	{
		AnalyticsEvent event = new AnalyticsEvent("A Firebase Event")
				.setPriority(10)
				.send();
		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();

		event.setPriority(-9)
		     .send();
		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@Test
	public void test_priorityFiltering_custom()
	{
		provider.setPriorityFilter(new AnalyticsKitProvider.PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return priorityLevel < 10;
			}
		});

		AnalyticsEvent event = new AnalyticsEvent("Priority 10 Event")
				.setPriority(10)
				.send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isFalse();

		event.setPriority(9)
		     .send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@Test
	public void testSendEvent_unTimed_noParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Test Run");
		provider.sendEvent(event);

		assertThat(sendCalled).isTrue();
		assertThat(testEventName).isEqualTo("Firebase Analytics Test Run");
		assertThat(testBundle).isNull();
	}

	@Test
	public void testSendEvent_unTimed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Event With Params Run")
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);

		assertThat(sendCalled).isTrue();
		assertThat(testBundle).isNotNull();
		assertThat(testBundle.containsKey("some_param")).isTrue();
		assertThat(testBundle.get("some_param")).isEqualTo("yes");
		assertThat(testBundle.containsKey("another_param")).isTrue();
		assertThat(testBundle.get("another_param")).isEqualTo("yes again");
	}

	@Test
	public void testSendEvent_unTimed_withTypedParams()
	{
		int[] intArray = {0, 1, 2};
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Event With Typed Params Run")
				.putAttribute("int_param", 1)
				.putAttribute("long_param", 32L)
				.putAttribute("string_param", "a string")
				.putAttribute("double_param", 3.1415926d)
				.putAttribute("float_param", 0.0f)
				.putAttribute("int_array_param", intArray);

		provider.sendEvent(event);

		assertThat(sendCalled).isTrue();
		assertThat(testEventName).isEqualTo("Firebase Analytics Event With Typed Params Run");
		assertThat(testBundle).isNotNull();
		assertThat(testBundle.get("int_param")).isEqualTo(1);
		assertThat(testBundle.get("long_param")).isEqualTo(32L);
		assertThat(testBundle.get("string_param")).isEqualTo("a string");
		assertThat(testBundle.get("double_param")).isEqualTo(3.1415926d);
		assertThat(testBundle.get("float_param")).isEqualTo(0.0f);
		assertThat(testBundle.get("int_array_param")).isEqualTo(intArray);
	}

	@Test
	public void testLogContentViewEvent()
	{
		ContentViewEvent event = new ContentViewEvent("Test page 7");
		provider.sendEvent(event);

		assertThat(sendCalled).isTrue();
		assertThat(testEventName).isEqualTo(CommonEvents.CONTENT_VIEW);
		assertThat(testBundle).isNotNull();
		assertThat(testBundle.get(ContentViewEvent.CONTENT_NAME)).isEqualTo("Test page 7");
	}

	@Test
	public void testLogErrorEvent()
	{
		ErrorEvent event = new ErrorEvent()
				.setMessage("something bad happened")
				.setException(new EmptyStackException());
		provider.sendEvent(event);

		assertThat(sendCalled).isTrue();
		assertThat(testEventName).isEqualTo(CommonEvents.ERROR);
		assertThat(testBundle).isNotNull();
		assertThat(testBundle.get("error_message")).isEqualTo("something bad happened");
		assertThat(testBundle.get("exception_object")).isNotNull();
	}

	@Test
	public void testSendEvent_timed_noParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Timed Event")
				.setTimed(true);

		provider.sendEvent(event);
		assertThat(sendCalled).isFalse();
	}

	@Test
	public void testSendEvent_timed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);
		assertThat(sendCalled).isFalse();
		assertThat(testBundle).isNull();
	}

	@Test
	public void testEndTimedEvent_Valid()
	{
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);
		assertThat(sendCalled).isFalse();

		provider.endTimedEvent(event);

		// Verify Firebase Analytics framework is called
		assertThat(sendCalled).isTrue();
		assertThat(testBundle).isNotNull();
		assertThat(testBundle.size()).isEqualTo(3);

		assertThat(testBundle.get("some_param")).isEqualTo("yes");
		assertThat(testBundle.get("another_param")).isEqualTo("yes again");
		assertThat(testBundle.get(FirebaseAnalytics.Param.VALUE)).isNotNull();
	}

	@Test
	public void test_endTimedEvent_WillThrow()
	{
		boolean didThrow = false;
		AnalyticsEvent event = new AnalyticsEvent("Firebase Analytics Timed Event With Parameters")
				.setTimed(true);

		try
		{
			provider.endTimedEvent(event); // attempting to end a timed event that was not started should throw an exception
		}
		catch (IllegalStateException e)
		{
			didThrow = true;
		}

		assertThat(didThrow).isTrue();
	}
}
