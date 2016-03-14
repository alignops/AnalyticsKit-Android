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

package com.busybusy.answers_provider;

/**
 * Defines constants used to match {@link com.busybusy.analyticskit_android.AnalyticsEvent} objects up
 * with the appropriate Answers event objects.
 *
 * @author John Hunt on 3/10/16.
 */
public interface PredefinedEvents
{
	String PURCHASE       = "Purchase";
	String ADD_TO_CART    = "Add to Cart";
	String START_CHECKOUT = "Start Checkout";
	String CONTENT_VIEW   = "Content View";
	String SEARCH         = "Search";
	String SHARE          = "Share";
	String RATED_CONTENT  = "Rated Content";
	String SIGN_UP        = "Sign Up";
	String LOG_IN         = "Log In";
	String INVITE         = "Invite";
	String LEVEL_START    = "Level Start";
	String LEVEL_END      = "Level End";
	String CUSTOM         = "Custom";
}
