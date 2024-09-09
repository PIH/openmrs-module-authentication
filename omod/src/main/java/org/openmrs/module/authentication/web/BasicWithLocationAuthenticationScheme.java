/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.authentication.web;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.openmrs.module.authentication.AuthenticationUtil.getBoolean;

/**
 * This expands on the BasicWebAuthenticationScheme to also handle collecting, validating, and setting sessionLocation
 */
public class BasicWithLocationAuthenticationScheme extends BasicWebAuthenticationScheme {

	public static final String LOCATION_PARAM_NAME = "locationParamName";
	public static final String ONLY_LOCATIONS_WITH_TAG = "onlyLocationsWithTag";
	public static final String LOCATION_REQUIRED = "locationRequired";
	public static final String LOCATION_SESSION_ATTRIBUTE_NAME = "locationSessionAttributeName";
	public static final String LAST_LOCATION_COOKIE_NAME = "lastLocationCookieName";

	private String locationParamName = "sessionLocation";
	private String onlyLocationsWithTag = null;
	private boolean locationRequired = false;
	private String locationSessionAttributeName = "emrContext.sessionLocationId";
	private String lastLocationCookieName = "emr.lastSessionLocation";

	@Override
	public void configure(String schemeId, Map<String, String> config) {
		super.configure(schemeId, config);
		locationParamName = config.getOrDefault(LOCATION_PARAM_NAME, "sessionLocation");
		onlyLocationsWithTag = config.get(ONLY_LOCATIONS_WITH_TAG);
		locationRequired = getBoolean(config.get(LOCATION_REQUIRED), false);
		locationSessionAttributeName = config.getOrDefault(LOCATION_SESSION_ATTRIBUTE_NAME, "emrContext.sessionLocationId");
		lastLocationCookieName = config.getOrDefault(LAST_LOCATION_COOKIE_NAME, "emr.lastSessionLocation");
	}

	@Override
	public void beforeAuthentication(AuthenticationSession session) {
		super.beforeAuthentication(session);
		Location loginLocation = getLoginLocation(session.getHttpRequest());
		if (loginLocation == null && locationRequired) {
			// TODO: Currently do not support setting session location for authentication via header (REST/FHIR/etc)
			if (session.getRequestHeader(AUTHORIZATION_HEADER) == null) {
				throw new ContextAuthenticationException("authentication.error.locationRequired");
			}
		}
	}

	@Override
	public void afterAuthenticationSuccess(AuthenticationSession session) {
		super.afterAuthenticationSuccess(session);
		Location loginLocation = getLoginLocation(session.getHttpRequest());
		if (loginLocation != null) {
			Context.getUserContext().setLocation(loginLocation);
			if (StringUtils.isNotBlank(locationSessionAttributeName)) {
				session.setHttpSessionAttribute(locationSessionAttributeName, loginLocation.getLocationId());
			}
			if (StringUtils.isNotBlank(lastLocationCookieName)) {
				session.setCookieValue(lastLocationCookieName, loginLocation.getLocationId().toString());
			}
		}
	}

	/**
	 * @return the Login Location for the given request.  If present in the request, return this location
	 * If not present in the request, but restricting by tags is configured, and only one location with this tag
	 * is set in the system, return the one tagged location
	 */
	protected Location getLoginLocation(HttpServletRequest request) {
		Location loginLocation = null;
		String locationIdStr = request.getParameter(locationParamName);
		if (StringUtils.isNotBlank(locationIdStr)) {
			loginLocation = getLocation(locationIdStr);
			if (loginLocation == null || !isValidLocation(loginLocation)) {
				throw new IllegalArgumentException("authentication.error.invalidLocation");
			}
		}
		return loginLocation;
	}

	/**
	 * @return a Location for the given lookup, first trying to parse to locationId, then trying to lookup by uuid
	 */
	protected Location getLocation(String lookup) {
		Location l = null;
		if (StringUtils.isNotBlank(lookup)) {
			try {
				l = Context.getLocationService().getLocation(Integer.parseInt(lookup));
			} catch (Exception e) {
				l = Context.getLocationService().getLocationByUuid(lookup);
			}
		}
		return l;
	}

	/**
	 * @param location the location to check
	 * @return true if the passed location is a valid location to set as the login location
	 */
	protected boolean isValidLocation(Location location) {
		return StringUtils.isBlank(onlyLocationsWithTag) || location.hasTag(onlyLocationsWithTag);
	}
}
