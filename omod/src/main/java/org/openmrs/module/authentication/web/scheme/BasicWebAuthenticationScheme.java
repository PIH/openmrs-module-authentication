/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.authentication.web.scheme;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.AuthenticationScheme;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.UsernamePasswordAuthenticationScheme;
import org.openmrs.module.authentication.AuthenticationLogger;
import org.openmrs.module.authentication.credentials.AuthenticationCredentials;
import org.openmrs.module.authentication.credentials.BasicAuthenticationCredentials;
import org.openmrs.module.authentication.scheme.ConfigurableAuthenticationScheme;
import org.openmrs.module.authentication.web.AuthenticationSession;

import java.util.Properties;

/**
 * This is an implementation of a WebAuthenticationScheme that delegates to a UsernamePasswordAuthenticationScheme,
 * and supports basic authentication with a username and password.
 * This scheme supports configuration parameters that enable implementations to utilize it with their own login pages
 * This includes the ability to configure the `loginPage` that the user should be taken to, as well as the
 * `usernameParam` and `passwordParam` that should be read from the http request submission to authenticate.
 */
public class BasicWebAuthenticationScheme implements WebAuthenticationScheme {

    public static final String LOGIN_PAGE = "loginPage";
    public static final String USERNAME_PARAM = "usernameParam";
    public static final String PASSWORD_PARAM = "passwordParam";

    private String schemeId;
    private String loginPage;
    private String usernameParam;
    private String passwordParam;

    public BasicWebAuthenticationScheme() {
        this.schemeId = getClass().getName();
    }

    /**
     * @return the configured schemeId
     */
    @Override
    public String getSchemeId() {
        return schemeId;
    }

    /**
     * This supports a `loginPage`, `usernameParam`, and `passwordParam` property
     * @see ConfigurableAuthenticationScheme#configure(String, Properties)
     */
    @Override
    public void configure(String schemeId, Properties config) {
        this.schemeId = schemeId;
        loginPage = config.getProperty(LOGIN_PAGE, "/module/authentication/basicLogin.htm");
        usernameParam = config.getProperty(USERNAME_PARAM, "username");
        passwordParam = config.getProperty(PASSWORD_PARAM, "password");
    }

    /**
     * @see WebAuthenticationScheme#getCredentials(AuthenticationSession)
     */
    @Override
    public AuthenticationCredentials getCredentials(AuthenticationSession session) {
        BasicAuthenticationCredentials credentials = null;
        String username = session.getRequestParam(usernameParam);
        String password = session.getRequestParam(passwordParam);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            credentials = new BasicAuthenticationCredentials(schemeId, username, password);
            session.getAuthenticationContext().addCredentials(credentials);
        }
        return credentials;
    }

    /**
     * @see WebAuthenticationScheme#getChallengeUrl(AuthenticationSession)
     */
    @Override
    public String getChallengeUrl(AuthenticationSession session) {
        if (session.getAuthenticationContext().getCredentials(schemeId) == null) {
            return loginPage;
        }
        return null;
    }

    /**
     * @see AuthenticationScheme#authenticate(Credentials)
     */
    @Override
    public Authenticated authenticate(Credentials credentials) throws ContextAuthenticationException {

        // Ensure the credentials provided are of the expected type
        if (!(credentials instanceof BasicAuthenticationCredentials)) {
            throw new ContextAuthenticationException("The credentials provided are invalid.");
        }

        BasicAuthenticationCredentials bac = (BasicAuthenticationCredentials) credentials;
        AuthenticationLogger.addToContext(AuthenticationLogger.USERNAME, bac.getUsername());
        return new UsernamePasswordAuthenticationScheme().authenticate(bac.toUsernamePasswordCredentials());
    }
}
