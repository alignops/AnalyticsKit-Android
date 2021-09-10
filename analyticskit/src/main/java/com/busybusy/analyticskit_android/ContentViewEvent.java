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

import androidx.annotation.NonNull;

/**
 * Defines information that is needed to distribute a "Content View" event to the registered analytics providers.
 *
 * @author John Hunt on 3/16/16.
 */
public class ContentViewEvent extends AnalyticsEvent
{
	public static final String CONTENT_NAME = "contentName";

	/**
	 * Instantiates a new {@code ContentViewEvent} object.
	 *
	 * @param contentName The name/title of the content that is viewed
	 */
	public ContentViewEvent(@NonNull String contentName)
	{
		super(CommonEvents.CONTENT_VIEW);
		this.putAttribute(CONTENT_NAME, contentName);
	}
}
