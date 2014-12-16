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
package org.frameworkset.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.frameworkset.util.StringUtil;



/**
 * <p>Title: UrlPathHelper.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2010-9-22
 * @author biaoping.yin
 * @version 1.0
 */
public class UrlPathHelper {
	/**
	 * @deprecated as of Bboss 2.0, in favor of <code>WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE</code>
	 * @see org.frameworkset.web.util.WebUtils#INCLUDE_REQUEST_URI_ATTRIBUTE
	 */
	public static final String INCLUDE_URI_REQUEST_ATTRIBUTE = WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE;

	/**
	 * @deprecated as of Bboss 2.0, in favor of <code>WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE</code>
	 * @see org.frameworkset.web.util.WebUtils#INCLUDE_CONTEXT_PATH_ATTRIBUTE
	 */
	public static final String INCLUDE_CONTEXT_PATH_REQUEST_ATTRIBUTE = WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE;

	/**
	 * @deprecated as of Bboss 2.0, in favor of <code>WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE</code>
	 * @see org.frameworkset.web.util.WebUtils#INCLUDE_SERVLET_PATH_ATTRIBUTE
	 */
	public static final String INCLUDE_SERVLET_PATH_REQUEST_ATTRIBUTE = WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE;


	private final static Logger logger = Logger.getLogger(UrlPathHelper.class);

	private boolean alwaysUseFullPath = false;

	private static boolean urlDecode = true;

	private static String defaultEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;


	/**
	 * Set if URL lookup should always use full path within current servlet
	 * context. Else, the path within the current servlet mapping is used
	 * if applicable (i.e. in the case of a ".../*" servlet mapping in web.xml).
	 * Default is "false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.alwaysUseFullPath = alwaysUseFullPath;
	}

	/**
	 * Set if context path and request URI should be URL-decoded.
	 * Both are returned <i>undecoded</i> by the Servlet API,
	 * in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
	 * <p>Default is "true", as of Bboss 2.5.
	 * @see #getServletPath
	 * @see #getContextPath
	 * @see #getRequestUri
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlDecode = urlDecode;
	}

	/**
	 * Set the default character encoding to use for URL decoding.
	 * Default is ISO-8859-1, according to the Servlet spec.
	 * <p>If the request specifies a character encoding itself, the request
	 * encoding will override this setting. This also allows for generically
	 * overriding the character encoding in a filter that invokes the
	 * <code>ServletRequest.setCharacterEncoding</code> method.
	 * @param defaultEncoding the character encoding to use
	 * @see #determineEncoding
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Return the default character encoding to use for URL decoding.
	 */
	protected static String getDefaultEncoding() {
		return defaultEncoding;
	}


	/**
	 * Return the mapping lookup path for the given request, within the current
	 * servlet mapping if applicable, else within the web application.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 * @param request current HTTP request
	 * @return the lookup path
	 * @see #getPathWithinApplication
	 * @see #getPathWithinServletMapping
	 */
	public String getLookupPathForRequest(HttpServletRequest request) {
		// Always use full path within current servlet context?
		if (this.alwaysUseFullPath) {
			return getPathWithinApplication(request);
		}
		// Else, use path within current servlet mapping if applicable
		String rest = getPathWithinServletMapping(request);
		if (!"".equals(rest)) {
			return rest;
		}
		else {
			return getPathWithinApplication(request);
		}
	}

	/**
	 * Return the path within the servlet mapping for the given request,
	 * i.e. the part of the request's URL beyond the part that called the servlet,
	 * or "" if the whole URL has been used to identify the servlet.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 * <p>E.g.: servlet mapping = "/test/*"; request URI = "/test/a" -> "/a".
	 * <p>E.g.: servlet mapping = "/test"; request URI = "/test" -> "".
	 * <p>E.g.: servlet mapping = "/*.test"; request URI = "/a.test" -> "".
	 * @param request current HTTP request
	 * @return the path within the servlet mapping, or ""
	 */
	public String getPathWithinServletMapping(HttpServletRequest request) {
		String pathWithinApp = getPathWithinApplication(request);
		String servletPath = getServletPath(request);
		if (pathWithinApp.startsWith(servletPath)) {
			// Normal case: URI contains servlet path.
			return pathWithinApp.substring(servletPath.length());
		}
		else {
			// Special case: URI is different from servlet path.
			// Can happen e.g. with index page: URI="/", servletPath="/index.html"
			// Use servlet path in this case, as it indicates the actual target path.
			return servletPath;
		}
	}

	public static String requesturi_cachekey = "org.frameworkset.web.util.requesturi.cachekey";
	/**
	 * Return the path within the web application for the given request.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 * @param request current HTTP request
	 * @return the path within the web application
	 */
	public static String getPathWithinApplication(HttpServletRequest request) {
		String uri = (String)request.getAttribute(requesturi_cachekey);
		if(uri != null)
			return uri;
		String contextPath = getContextPath(request);
		String requestUri = getRequestUri(request);
		if (StringUtil.startsWithIgnoreCase(requestUri, contextPath)) {
			// Normal case: URI contains context path.
			String path = requestUri.substring(contextPath.length());
			uri = (StringUtil.hasText(path) ? path : "/");
		}
		else {
			// Special case: rather unusual.
			uri = requestUri;
		}
		request.setAttribute(requesturi_cachekey, uri);
		return uri;
	}
	


	/**
	 * Return the request URI for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by <code>request.getRequestURI()</code> is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * <p>The URI that the web container resolves <i>should</i> be correct, but some
	 * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
	 * in the URI. This method cuts off such incorrect appendices.
	 * @param request current HTTP request
	 * @return the request URI
	 */
	public static String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * Return the context path for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by <code>request.getContextPath()</code> is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * @param request current HTTP request
	 * @return the context path
	 */
	public static String getContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		if ("/".equals(contextPath)) {
			// Invalid case, but happens for includes on Jetty: silently adapt it.
			contextPath = "";
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * Return the servlet path for the given request, regarding an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by <code>request.getServletPath()</code> is already
	 * decoded by the servlet container, this method will not attempt to decode it.
	 * @param request current HTTP request
	 * @return the servlet path
	 */
	public String getServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		return servletPath;
	}


	/**
	 * Return the request URI for root of the given request. If this is a forwarded request,
	 * correctly resolves to the request URI of the original request.
	 * <p>Relies on the Servlet 2.4 'forward' attributes. These attributes may be set by
	 * other components when running in a Servlet 2.3 environment.
	 */
	public String getOriginatingRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * Return the context path for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by <code>request.getContextPath()</code> is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * <p>Relies on the Servlet 2.4 'forward' attributes. These attributes may be set by
	 * other components when running in a Servlet 2.3 environment.
	 * @param request current HTTP request
	 * @return the context path
	 */
	public String getOriginatingContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * Return the request URI for root of the given request. If this is a forwarded request,
	 * correctly resolves to the request URI of the original request.
	 * <p>Relies on the Servlet 2.4 'forward' attributes. These attributes may be set by
	 * other components when running in a Servlet 2.3 environment.
	 * @param request current HTTP request
	 * @return the query string
	 */
	public String getOriginatingQueryString(HttpServletRequest request) {
		String queryString = (String) request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE);
		if (queryString == null) {
			queryString = request.getQueryString();
		}
		return queryString;
	}


	/**
	 * Decode the supplied URI string and strips any extraneous portion after a ';'.
	 */
	private static String decodeAndCleanUriString(HttpServletRequest request, String uri) {
		uri = decodeRequestString(request, uri);
		int semicolonIndex = uri.indexOf(';');
		return (semicolonIndex != -1 ? uri.substring(0, semicolonIndex) : uri);
	}

	/**
	 * Decode the given source string with a URLDecoder. The encoding will be taken
	 * from the request, falling back to the default "ISO-8859-1".
	 * <p>The default implementation uses <code>URLDecoder.decode(input, enc)</code>.
	 * @param request current HTTP request
	 * @param source the String to decode
	 * @return the decoded String
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see java.net.URLDecoder#decode(String, String)
	 * @see java.net.URLDecoder#decode(String)
	 */
	public static String decodeRequestString(HttpServletRequest request, String source) {
		if (  urlDecode) {
			String enc = determineEncoding(request);
			try {
				return URLDecoder.decode(source, enc);
			}
			catch (UnsupportedEncodingException ex) {
//				if (logger.isWarnEnabled()) 
				{
					logger.warn("Could not decode request string [" + source + "] with encoding '" + enc +
							"': falling back to platform default encoding; exception message: " + ex.getMessage());
				}
				return URLDecoder.decode(source);
			}
		}
		return source;
	}

	/**
	 * Determine the encoding for the given request.
	 * Can be overridden in subclasses.
	 * <p>The default implementation checks the request encoding,
	 * falling back to the default encoding specified for this resolver.
	 * @param request current HTTP request
	 * @return the encoding for the request (never <code>null</code>)
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see #setDefaultEncoding
	 */
	protected static String determineEncoding(HttpServletRequest request) {
		String enc = request.getCharacterEncoding();
		if (enc == null) {
			enc = getDefaultEncoding();
		}
		return enc;
	}

}
