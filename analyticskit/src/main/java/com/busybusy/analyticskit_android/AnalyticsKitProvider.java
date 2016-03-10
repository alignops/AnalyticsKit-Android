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

/**
 * Defines the interface for provider plugins to be used with AnalyticsKit-Android.
 *
 * Note: in your provider implementation, make sure the underlying provider SDK calls are
 * executed asynchronously. Otherwise, you will have network operations running on the main thread.
 *
 * @author John Hunt on 3/5/16.
 */
public interface AnalyticsKitProvider
{
	/**
	 * Returns the type of the provider. This value should be a power of two between 2^0 and 2^30.
	 * Please use values in the range [2^0 , 2^7] for your own custom provider implementations.
	 * Some popular providers have already been defined as constants in {@link Providers}.
	 * @return the specified type mask of the analytics provider.
	 * @see Providers
	 */
	int getType();

	/**
	 * Sends the event using provider-specific code
	 * @param event an instantiated event
	 */
	void sendEvent(@NonNull AnalyticsEvent event);

	/**
	 * End the timed event
	 * @param timedEvent the event which has finished
	 */
	void endTimedEvent(@NonNull AnalyticsEvent timedEvent);
}
