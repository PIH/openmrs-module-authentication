package org.openmrs.module.authentication.web;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CookieClearingFilterTest extends BaseWebAuthenticationTest {
	
	CookieClearingFilter filter;
	
	MockHttpServletRequest request;
	
	MockHttpServletResponse response;
	
	MockHttpSession session;
	
	MockFilterChain chain;
	
	@Override
	@BeforeEach
	public void setup() {
		super.setup();
		
		filter = new CookieClearingFilter();
		session = new MockHttpSession();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		chain = new MockFilterChain();
	}
	
	@Test
	void shouldClearCookiesIfSessionEnded() throws Exception {
		// arrange
		createChainThatInvalidatesSession();
		clearJSessionIdOnLogout();
		createSessionWithId("1234");
		
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length, "Expected only a single cookie");
		assertTrue(cookies[0].getMaxAge() <= 0, "Expected Max-Age to be zero or a negative number");
	}
	
	@Test
	void shouldNotClearCookiesIfSessionNotInvalidated() throws Exception {
		// arrange
		clearJSessionIdOnLogout();
		createSessionWithId("1234");
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		Cookie[] cookies = response.getCookies();
		assertEquals(0, cookies.length, "Expected no cookies in response");
	}
	
	@Test
	void shouldNotClearCookiesIfNewSessionCreatedButNotInvalidated() throws Exception {
		// arrange
		createChainThatInvalidatesSession();
		clearJSessionIdOnLogout();
		createSessionWithId("1234", true);
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length, "Expected no cookies in response");
	}
	
	@Test
	void shouldNotClearCookiesIfNewSessionCreatedAndInvalidatedInOneRequest() throws Exception {
		// arrange
		clearJSessionIdOnLogout();
		createSessionWithId("1234", true);
		
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length, "Expected the new session cookie in response");
		assertTrue(cookies[0].getMaxAge() > 0, "Expected Max-Age to be set to some future value");
	}
	
	@Test
	void shouldClearAllConfiguredCookies() throws Exception {
		// arrange
		createChainThatInvalidatesSession();
		AuthenticationConfig.setProperty("authentication.cookies.clearOnLogout", Boolean.TRUE.toString());
		AuthenticationConfig.setProperty("authentication.cookies.toClear", "JSESSIONID,AnotherCookie");
		createSessionWithId("1234");
		// add our non-session cookie
		{
			Cookie myOtherCookie = new Cookie("AnotherCookie", UUID.randomUUID().toString());
			Cookie[] requestCookies = request.getCookies();
			Cookie[] cookies = new Cookie[requestCookies.length + 1];
			System.arraycopy(requestCookies, 0, cookies, 0, requestCookies.length);
			cookies[requestCookies.length] = myOtherCookie;
			request.setCookies(cookies);
		}
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		Cookie[] cookies = response.getCookies();
		assertEquals(2, cookies.length, "Expected two cookies");
		for (Cookie cookie : cookies) {
			assertTrue(cookie.getMaxAge() <= 0, "Expected Max-Age to be less than or equal to 0 for cookie " + cookie.getName());
		}
	}
	
	@Test
	void shouldClearAllConfiguredCookiesIgnoringWhitespace() throws Exception {
		// arrange
		createChainThatInvalidatesSession();
		AuthenticationConfig.setProperty("authentication.cookies.clearOnLogout", Boolean.TRUE.toString());
		AuthenticationConfig.setProperty("authentication.cookies.toClear", " JSESSIONID \t,     AnotherCookie     ");
		createSessionWithId("1234");
		// add our non-session cookie
		{
			Cookie myOtherCookie = new Cookie("AnotherCookie", UUID.randomUUID().toString());
			Cookie[] requestCookies = request.getCookies();
			Cookie[] cookies = new Cookie[requestCookies.length + 1];
			System.arraycopy(requestCookies, 0, cookies, 0, requestCookies.length);
			cookies[requestCookies.length] = myOtherCookie;
			request.setCookies(cookies);
		}
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		Cookie[] cookies = response.getCookies();
		assertEquals(2, cookies.length, "Expected two cookies");
		for (Cookie cookie : cookies) {
			assertTrue(cookie.getMaxAge() <= 0, "Expected Max-Age to be less than or equal to 0 for cookie " + cookie.getName());
		}
	}
	
	@Test
	void shouldDoNothingIfNotEnabled() throws Exception {
		// arrange
		request = mock(MockHttpServletRequest.class);
		response = mock(MockHttpServletResponse.class);
		
		// act
		filter.doFilter(request, response, chain);
		
		// assert
		verify(response, atLeastOnce()).isCommitted();
		verifyNoMoreInteractions(request, response);
	}
	
	void clearJSessionIdOnLogout() {
		AuthenticationConfig.setProperty("authentication.cookies.clearOnLogout", Boolean.TRUE.toString());
		AuthenticationConfig.setProperty("authentication.cookies.toClear", "JSESSIONID");
	}
	
	void createChainThatInvalidatesSession() {
		chain = new MockFilterChain(new SessionInvalidationServlet());
	}
	
	void createSessionWithId(String id) {
		createSessionWithId(id, false);
	}
	
	void createSessionWithId(String id, boolean isNew) {
		session = new MockHttpSession(null, id);
		session.setNew(isNew);
		request.setSession(session);
		
		if (!isNew) {
			request.setRequestedSessionId(id);
			Cookie sessionCookie = new Cookie("JSESSIONID", "1234");
			sessionCookie.setMaxAge(60 * 60 * 2);
			request.setCookies(sessionCookie);
		} else {
			Cookie sessionCookie = new Cookie("JSESSIONID", "1234");
			sessionCookie.setMaxAge(60 * 60 * 2);
			response.addCookie(sessionCookie);
		}
	}
	
	private static final class SessionInvalidationServlet extends GenericServlet {
		
		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			if (req instanceof HttpServletRequest) {
				((HttpServletRequest) req).getSession().invalidate();
			}
		}
	}

}
