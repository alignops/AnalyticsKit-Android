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

package com.busybusy.answers_provider.loggers;

import com.crashlytics.android.answers.MockAnswers;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Throw the common config and setup down in this class to make life easier.
 *
 * @author Trevor
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, constants = com.busybusy.answers_provider.BuildConfig.class, manifest = Config.NONE)
public abstract class LoggerBaseTest
{
	static String CUSTOM_KEY  = "CustomFieldKey";
	static String CUSTOM_DATA = "CustomData";

	MockAnswers answers;

	@Before
	public void setup()
	{
		this.answers = new MockAnswers();
	}
}
