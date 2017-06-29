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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author John Hunt on 6/29/17.
 */

public class GraylogProviderFailedCallTest
{
	GraylogProvider provider;
	MockWebServer mockServer = new MockWebServer();
	OkHttpClient  httpClient = new OkHttpClient.Builder().build();
	GraylogResponseListener callbackListener;

	String              testEventName;
	Map<String, Object> testEventPropertiesMap;
	boolean             logEventCalled;
	String loggedEventName;
	int httpResponseCode;
	String httpStatusMessage;

	@Before
	public void setup()
	{
		callbackListener = new GraylogResponseListener()
		{
			@Override
			public void onGraylogResponse(GraylogResponse response)
			{
				testEventPropertiesMap = response.event().getAttributes();
				loggedEventName = response.event().name();
				logEventCalled = true;
				httpResponseCode = response.code();
				httpStatusMessage = response.message();
			}
		};

		mockServer.enqueue(new MockResponse().setResponseCode(420).setStatus("An error occurred communicating with the Graylog server"));
		try
		{
			mockServer.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		provider = new GraylogProvider(httpClient, "http://" + mockServer.getHostName() + ":" + mockServer.getPort(), "unit-test-android");
		provider.setCallbackHandler(callbackListener);

		logEventCalled = false;
		testEventPropertiesMap = null;
		loggedEventName = null;
		httpResponseCode = -1;
		httpStatusMessage = "Not Sent";
	}

	@After
	public void tearDown()
	{
		try
		{
			mockServer.shutdown();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Test
	public void testSendEvent_unTimed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Graylog Event With Params Run")
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);

		assertThat(loggedEventName).isEqualTo("Graylog Event With Params Run");
		assertThat(logEventCalled).isTrue();
		assertThat(testEventPropertiesMap.keySet()).contains("some_param", "another_param");
		assertThat(testEventPropertiesMap.values()).contains("yes", "yes again");

		assertThat(httpResponseCode).isEqualTo(420);
		assertThat(httpStatusMessage).isEqualTo("An error occurred communicating with the Graylog server");
	}
}
