/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.authentication.web;

import org.openmrs.module.authentication.AuthenticationLogger;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Called each time a http session is created or destroyed
 */
@Component
public class AuthenticationHttpSessionListener implements HttpSessionListener {

	/**
	 * This ensures a new AuthenticationSession is created and initialized with appropriate values
	 * @see AuthenticationSession which will initialize with existing values if an authenticated user is found
	 * @param httpSessionEvent the event passed at session creation
	 */
	@Override
	public void sessionCreated(HttpSessionEvent httpSessionEvent) {
		AuthenticationSession session = new AuthenticationSession(httpSessionEvent.getSession());
		String sessionId = session.getHttpSessionId();
		AuthenticationLogger.logEvent(AuthenticationLogger.SESSION_CREATED, "httpSessionId=" + sessionId);
	}

	/**
	 * This ensures a new AuthenticationSession is created and initialized with appropriate values
	 * If this event coincides with a logged-out user, then ensure all authentication data is destroyed in the session
	 * @param httpSessionEvent the event passed at session creation
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
		AuthenticationSession session = new AuthenticationSession(httpSessionEvent.getSession());
		String sessionId = session.getHttpSessionId();
		AuthenticationLogger.logEvent(AuthenticationLogger.SESSION_DESTROYED, "httpSessionId=" + sessionId);
		if (!session.isUserAuthenticated()) {
			session.destroy();
		}
	}
}