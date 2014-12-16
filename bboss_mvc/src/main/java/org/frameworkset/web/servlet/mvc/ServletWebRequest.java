/*
 *  Copyright 2008 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.frameworkset.web.servlet.mvc;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.frameworkset.web.servlet.context.ServletRequestAttributes;

import com.frameworkset.util.StringUtil;

/**
 * <p>Title: ServletWebRequest.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2010-10-15
 * @author biaoping.yin
 * @version 1.0
 */
public class ServletWebRequest  extends ServletRequestAttributes implements NativeWebRequest {

	private static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String HEADER_LAST_MODIFIED = "Last-Modified";


	private HttpServletResponse response;

	private boolean notModified = false;


	/**
	 * Create a new ServletWebRequest instance for the given request.
	 * @param request current HTTP request
	 */
	public ServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * Create a new ServletWebRequest instance for the given request/response pair.
	 * @param request current HTTP request
	 * @param response current HTTP response (for automatic last-modified handling)
	 */
	public ServletWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request);
		this.response = response;
	}


	/**
	 * Exposes the native {@link HttpServletRequest} that we're wrapping (if any).
	 */
	public final HttpServletResponse getResponse() {
		return this.response;
	}

	public Object getNativeRequest() {
		return getRequest();
	}

	public Object getNativeResponse() {
		return getResponse();
	}


	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	public String[] getParameterValues(String paramName) {
		return getRequest().getParameterValues(paramName);
	}

	public Map getParameterMap() {
		return getRequest().getParameterMap();
	}

	public Locale getLocale() {
		return getRequest().getLocale();
	}

	public String getContextPath() {
		return getRequest().getContextPath();
	}

	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	public boolean isUserInRole(String role) {
		return getRequest().isUserInRole(role);
	}

	public boolean isSecure() {
		return getRequest().isSecure();
	}


	public boolean checkNotModified(long lastModifiedTimestamp) {
		if (lastModifiedTimestamp >= 0 && !this.notModified &&
				(this.response == null || !this.response.containsHeader(HEADER_LAST_MODIFIED))) {
			long ifModifiedSince = getRequest().getDateHeader(HEADER_IF_MODIFIED_SINCE);
			this.notModified = (ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000));
			if (this.response != null) {
				if (this.notModified) {
					this.response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				}
				else {
					this.response.setDateHeader(HEADER_LAST_MODIFIED, lastModifiedTimestamp);
				}
			}
		}
		return this.notModified;
	}

	public boolean isNotModified() {
		return this.notModified;
	}


	public String getDescription(boolean includeClientInfo) {
		HttpServletRequest request = getRequest();
		StringBuffer buffer = new StringBuffer();
		buffer.append("uri=").append(request.getRequestURI());
		if (includeClientInfo) {
			String client = request.getRemoteAddr();
			if (StringUtil.hasLength(client)) {
				buffer.append(";client=").append(client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				buffer.append(";session=").append(session.getId());
			}
			String user = request.getRemoteUser();
			if (StringUtil.hasLength(user)) {
				buffer.append(";user=").append(user);
			}
		}
		return buffer.toString();
	}

	public String toString() {
		return "ServletWebRequest: " + getDescription(true);
	}

}
