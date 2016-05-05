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

import com.busybusy.analyticskit_android.ContentViewEvent;

/**
 * Defines predefined attribute names used by Answers.
 * This interface is 'scoped' to facilitate readability and ease of use.
 *
 * @author John Hunt on 3/10/16.
 */
public interface Attributes
{
	String EVENT_DURATION = "event_duration";

	String CONTENT_NAME = ContentViewEvent.CONTENT_NAME;
	String CONTENT_TYPE = "contentType";
	String CONTENT_ID   = "contentId";

	String ITEM_NAME  = "itemName";
	String ITEM_TYPE  = "itemType";
	String ITEM_ID    = "itemId";
	String ITEM_PRICE = "itemPrice";

	String SUCCESS    = "success";
	String METHOD     = "method";
	String CURRENCY   = "currency";
	String LEVEL_NAME = "levelName";


	interface Purchase
	{
		String ITEM_PRICE = Attributes.ITEM_PRICE;
		String CURRENCY   = Attributes.CURRENCY;
		String SUCCESS    = Attributes.SUCCESS;
		String ITEM_NAME  = Attributes.ITEM_NAME;
		String ITEM_TYPE  = Attributes.ITEM_TYPE;
		String ITEM_ID    = Attributes.ITEM_ID;
	}

	interface AddToCart
	{
		String ITEM_PRICE = Attributes.ITEM_PRICE;
		String CURRENCY   = Attributes.CURRENCY;
		String ITEM_NAME  = Attributes.ITEM_NAME;
		String ITEM_TYPE  = Attributes.ITEM_TYPE;
		String ITEM_ID    = Attributes.ITEM_ID;
	}

	interface StartCheckout
	{
		String TOTAL_PRICE = "totalPrice";
		String CURRENCY    = Attributes.CURRENCY;
		String ITEM_COUNT  = "itemCount";
	}

	interface ContentView
	{
		String CONTENT_NAME = Attributes.CONTENT_NAME;
		String CONTENT_TYPE = Attributes.CONTENT_TYPE;
		String CONTENT_ID   = Attributes.CONTENT_ID;
	}

	interface Search
	{
		String QUERY = "query";
	}

	interface Share
	{
		String METHOD       = Attributes.METHOD;
		String CONTENT_NAME = Attributes.CONTENT_NAME;
		String CONTENT_TYPE = Attributes.CONTENT_TYPE;
		String CONTENT_ID   = Attributes.CONTENT_ID;
	}

	interface RatedContent
	{
		String RATING       = "rating";
		String CONTENT_NAME = Attributes.CONTENT_NAME;
		String CONTENT_TYPE = Attributes.CONTENT_TYPE;
		String CONTENT_ID   = Attributes.CONTENT_ID;
	}

	interface SignUp
	{
		String METHOD  = Attributes.METHOD;
		String SUCCESS = Attributes.SUCCESS;
	}

	interface LogIn
	{
		String METHOD  = Attributes.METHOD;
		String SUCCESS = Attributes.SUCCESS;
	}

	interface Invite
	{
		String METHOD = Attributes.METHOD;
	}

	interface LevelStart
	{
		String LEVEL_NAME = Attributes.LEVEL_NAME;
	}

	interface LevelEnd
	{
		String LEVEL_NAME = Attributes.LEVEL_NAME;
		String SCORE      = "score";
		String SUCCESS    = Attributes.SUCCESS;
	}
}
