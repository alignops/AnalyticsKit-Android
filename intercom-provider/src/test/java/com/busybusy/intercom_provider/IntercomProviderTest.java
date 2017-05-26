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

package com.busybusy.intercom_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.busybusy.analyticskit_android.CommonEvents;
import com.busybusy.analyticskit_android.ContentViewEvent;
import com.busybusy.analyticskit_android.ErrorEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.EmptyStackException;
import java.util.Map;

import io.intercom.android.sdk.Intercom;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link IntercomProvider} class.
 *
 * @author John Hunt on 5/19/17.
 */
@RunWith(PowerMockRunner.class)
public class IntercomProviderTest
{
	IntercomProvider provider;
	Intercom         mockedIntercom;

	String              testEventName;
	Map<String, Object> testEventPropertiesMap;
	boolean             logEventCalled;

	@Before
	public void setup()
	{
		mockedIntercom = mock(Intercom.class);

		doAnswer(new Answer<Void>()
		{
			public Void answer(InvocationOnMock invocation)
			{
				Object[] args = invocation.getArguments();
				testEventName = (String) args[0];
				//noinspection unchecked
				testEventPropertiesMap = (Map<String, Object>) args[1];
				logEventCalled = true;
				return null;
			}
		}).when(mockedIntercom).logEvent(anyString(), anyMapOf(String.class, Object.class));

		provider = new IntercomProvider(mockedIntercom);

		logEventCalled = false;
		testEventPropertiesMap = null;
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
		AnalyticsEvent event = new AnalyticsEvent("Intercom event")
				.setPriority(10);
		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();

		event.setPriority(-9);
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

		AnalyticsEvent event = new AnalyticsEvent("Intercom event")
				.setPriority(10);

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isFalse();

		event.setPriority(9);

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testSendEvent_unTimed_noParams()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		AnalyticsEvent event = new AnalyticsEvent("Intercom Test Run");
		provider.sendEvent(event);

		assertThat(logEventCalled).isTrue();
		assertThat(testEventName).isEqualTo("Intercom Test Run");
		assertThat(testEventPropertiesMap).isNull();
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testSendEvent_unTimed_withParams()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		AnalyticsEvent event = new AnalyticsEvent("Intercom Event With Params Run")
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);

		assertThat(logEventCalled).isTrue();
		assertThat(testEventName).isEqualTo("Intercom Event With Params Run");
		assertThat(testEventPropertiesMap.keySet()).containsExactlyInAnyOrder("some_param", "another_param");
		assertThat(testEventPropertiesMap.values()).containsExactlyInAnyOrder("yes", "yes again");
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testLogContentViewEvent()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		ContentViewEvent event = new ContentViewEvent("Test page 7");
		provider.sendEvent(event);

		assertThat(logEventCalled).isTrue();
		assertThat(testEventName).isEqualTo(CommonEvents.CONTENT_VIEW);
		assertThat(testEventPropertiesMap.keySet()).containsExactly(ContentViewEvent.CONTENT_NAME);
		assertThat(testEventPropertiesMap.values()).containsExactly("Test page 7");
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testLogErrorEvent()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		EmptyStackException myException = new EmptyStackException();

		ErrorEvent event = new ErrorEvent()
				.setMessage("something bad happened")
				.setException(myException);
		provider.sendEvent(event);

		assertThat(logEventCalled).isTrue();
		assertThat(testEventName).isEqualTo(CommonEvents.ERROR);
		assertThat(testEventPropertiesMap.keySet()).containsExactlyInAnyOrder("error_message", "exception_object");
		assertThat(testEventPropertiesMap.values()).containsExactlyInAnyOrder("something bad happened", myException);
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testSendEvent_timed_noParams()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		AnalyticsEvent event = new AnalyticsEvent("Intercom Timed Event")
				.setTimed(true);

		provider.sendEvent(event);
		assertThat(logEventCalled).isFalse();
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testSendEvent_timed_withParams()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		AnalyticsEvent event = new AnalyticsEvent("Intercom Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);
		assertThat(logEventCalled).isFalse();
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void testEndTimedEvent_Valid()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		AnalyticsEvent event = new AnalyticsEvent("Intercom Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);
		assertThat(logEventCalled).isFalse();

		try
		{
			Thread.sleep(50);
		}
		catch (InterruptedException e)
		{
			// don't do anything, this is just a test that needs some delay
		}

		provider.endTimedEvent(event);

		String timeString = String.valueOf(testEventPropertiesMap.get(provider.DURATION));
		double elapsed    = Double.valueOf(timeString);
		assertThat(elapsed).isGreaterThanOrEqualTo(0.05d);

		// Verify Intercom framework is called
		assertThat(logEventCalled).isTrue();
		assertThat(testEventPropertiesMap.keySet()).containsExactlyInAnyOrder(provider.DURATION, "some_param", "another_param");
		assertThat(testEventPropertiesMap.values()).containsExactlyInAnyOrder(timeString, "yes", "yes again");
	}

	@PrepareForTest({Intercom.class})
	@Test
	public void test_endTimedEvent_WillThrow()
	{
		PowerMockito.mockStatic(Intercom.class);
		PowerMockito.when(Intercom.client()).thenReturn(mockedIntercom);

		boolean didThrow = false;
		AnalyticsEvent event = new AnalyticsEvent("Intercom Timed Event With Parameters")
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