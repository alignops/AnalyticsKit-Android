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

package com.busybusy.library.analyticskit_android;

/**
 * Defines information that is needed to distribute the event
 * Created by john on 3/5/16.
 */
public class AnalyticsEvent
{
    private String name;

    private AnalyticsEvent(Builder builder)
    {
        this.name = builder.name;
    }

	/**
	 * Access the event name
	 * @return the name of the custom event
	 */
    public String name()
    {
        return name;
    }

	/**
	 * Provides convenient builder-style methods for initializing an {@link AnalyticsEvent} object.
	 */
    public static class Builder
    {
        private String name;

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public AnalyticsEvent build() {
            if (name == null) throw new IllegalStateException("name == null");
            return new AnalyticsEvent(this);
        }
    }
}
