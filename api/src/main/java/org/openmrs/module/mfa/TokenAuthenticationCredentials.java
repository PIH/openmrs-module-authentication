/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.mfa;

/**
 * Credentials supporting token-based authentication.
 * This is expected to be a second authentication factor, and thus does not need to contain a username
 */
public class TokenAuthenticationCredentials implements AuthenticatorCredentials {

    private String authenticatorName;
    private MfaUser mfaUser;
    private String token;

    public TokenAuthenticationCredentials(String authenticatorName, MfaUser mfaUser, String token) {
        this.authenticatorName = authenticatorName;
        this.mfaUser = mfaUser;
        this.token = token;
    }

    @Override
    public String getAuthenticatorName() {
        return authenticatorName;
    }

    @Override
    public String getAuthenticationScheme() {
        return getClass().getName();
    }

    @Override
    public String getClientName() {
        return mfaUser.getUser().getUsername();
    }

    public MfaUser getMfaUser() {
        return mfaUser;
    }

    public String getToken() {
        return token;
    }
}
