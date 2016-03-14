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

package com.crashlytics.android.answers;

/**
 * Extended version of answers to ensure that methods get called correctly by our wrapping layers.
 *
 * @author Trevor
 */
public class MockAnswers extends Answers
{

	public MockAnswers()
	{
	}

	public boolean     logCustomCalled = false;
	public CustomEvent customEvent     = null;

	@Override
	public void logCustom(CustomEvent event)
	{
		logCustomCalled = true;
		customEvent = event;
	}

	public boolean       logPurchaseCalled = false;
	public PurchaseEvent purchaseEvent     = null;

	@Override
	public void logPurchase(PurchaseEvent event)
	{
		logPurchaseCalled = true;
		purchaseEvent = event;
	}

	public boolean    logLoginCalled = false;
	public LoginEvent loginEvent     = null;

	@Override
	public void logLogin(LoginEvent event)
	{
		logLoginCalled = true;
		loginEvent = event;
	}

	public boolean    logShareCalled = false;
	public ShareEvent shareEvent     = null;

	@Override
	public void logShare(ShareEvent event)
	{
		logShareCalled = true;
		shareEvent = event;
	}

	public boolean     logInviteCalled = false;
	public InviteEvent inviteEvent     = null;

	@Override
	public void logInvite(InviteEvent event)
	{
		logInviteCalled = true;
		inviteEvent = event;
	}

	public boolean     logSignUpCalled = false;
	public SignUpEvent signUpEvent     = null;

	@Override
	public void logSignUp(SignUpEvent event)
	{
		logSignUpCalled = true;
		signUpEvent = event;
	}

	public boolean         logLevelStartCalled = false;
	public LevelStartEvent levelStartEvent     = null;

	@Override
	public void logLevelStart(LevelStartEvent event)
	{
		logLevelStartCalled = true;
		levelStartEvent = event;
	}

	public boolean       logLevelEndCalled = false;
	public LevelEndEvent levelEndEvent     = null;

	@Override
	public void logLevelEnd(LevelEndEvent event)
	{
		logLevelEndCalled = true;
		levelEndEvent = event;
	}

	public boolean        logAddToCartCalled = false;
	public AddToCartEvent addToCartEvent     = null;

	@Override
	public void logAddToCart(AddToCartEvent event)
	{
		logAddToCartCalled = true;
		addToCartEvent = event;
	}

	public boolean            logStartCheckoutCalled = false;
	public StartCheckoutEvent startCheckoutEvent     = null;

	@Override
	public void logStartCheckout(StartCheckoutEvent event)
	{
		logStartCheckoutCalled = true;
		startCheckoutEvent = event;
	}

	public boolean     logRatingCalled = false;
	public RatingEvent ratingEvent     = null;

	@Override
	public void logRating(RatingEvent event)
	{
		logRatingCalled = true;
		ratingEvent = event;
	}

	public boolean          logContentViewCalled = false;
	public ContentViewEvent contentViewEvent     = null;

	@Override
	public void logContentView(ContentViewEvent event)
	{
		logContentViewCalled = true;
		contentViewEvent = event;
	}

	public boolean     logSearchCalled = false;
	public SearchEvent searchEvent     = null;

	@Override
	public void logSearch(SearchEvent event)
	{
		logSearchCalled = true;
		searchEvent = event;
	}
}
