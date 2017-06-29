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

/**
 * Defines the contract for callbacks fired by the {@link GraylogProvider} instance.
 *
 * @author John Hunt on 6/28/17.
 */

public interface GraylogResponseListener
{
	/**
	 * This method gets called after an {@link com.busybusy.analyticskit_android.AnalyticsEvent} is sent to a Graylog server.
	 *
	 * @param response the Response object describing a result of an HTTP call to a Graylog server and the event that was sent
	 */
	void onGraylogResponse(final GraylogResponse response);
}
