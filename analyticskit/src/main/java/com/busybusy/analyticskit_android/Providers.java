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

/**
 * Defines the bit mask values used to identify analytics providers.
 * @author John Hunt on 3/7/16.
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public interface Providers
{
	int ANSWERS          = 0x40000000; // 2^30
	int MIXPANEL         = 0x20000000; // 2^29
	int GOOGLE_ANALYTICS = 0x10000000; // 2^28
	int ADJUST_IO        = 0x08000000; // 2^27
	int APSALAR          = 0x04000000; // 2^26
	int FLURRY           = 0x02000000; // 2^25
	int LOCALYTICS       = 0x01000000; // 2^24
	int NEW_RELIC        = 0x00800000; // 2^23
	int APMETRIX         = 0x00400000; // 2^22
	int UPSIGHT          = 0x00200000; // 2^21
	int KISSMETRICS      = 0x00100000; // 2^20
}
