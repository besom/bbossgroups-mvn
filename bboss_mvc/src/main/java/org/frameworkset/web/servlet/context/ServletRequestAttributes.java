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
package org.frameworkset.web.servlet.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.jsp.PageContext;

import org.frameworkset.util.Assert;
import org.frameworkset.web.util.WebUtils;

import com.frameworkset.util.StringUtil;

/**
 * <p>Title: ServletRequestAttributes.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2010-10-1
 * @author biaoping.yin
 * @version 1.0
 */
public class ServletRequestAttributes  extends AbstractRequestAttributes {

	/**
	 * Constant identifying the {@link String} prefixed to the name of a
	 * destruction callback when it is stored in a {@link HttpSession}.
	 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			ServletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";


	private final HttpServletRequest request;

	private volatile HttpSession session;
	
	private volatile PageContext pageContext;
	
	private HttpServletResponse response;

	/**
	 * @return the response
	 */
	public HttpServletResponse getResponse() {
		return response;
	}

	/**
	 * @return the pageContext
	 */
	public PageContext getPageContext() {
		return pageContext;
	}


	private final Map sessionAttributesToUpdate = new HashMap();


	/**
	 * Create a new ServletRequestAttributes instance for the given request.
	 * @param request current HTTP request
	 */
	public ServletRequestAttributes(HttpServletRequest request,HttpServletResponse response,PageContext pageContext) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
		this.pageContext = pageContext;
		this.response = response;
	}
	
	public ServletRequestAttributes(HttpServletRequest request) {
		
		this(request,null,null);
	}


	/**
	 * Exposes the native {@link HttpServletRequest} that we're wrapping.
	 */
	public final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Exposes the {@link HttpSession} that we're wrapping.
	 * @param allowCreate whether to allow creation of a new session if none exists yet
	 */
	public final HttpSession getSession(boolean allowCreate) {
		if (isRequestActive()) {
			return this.request.getSession(allowCreate);
		}
		else {
			// Access through stored session reference, if any...
			if (this.session == null && allowCreate) {
				throw new IllegalStateException(
						"No session found and request already completed - cannot create new session!");
			}
			return this.session;
		}
	}
	
	/**
	 * Exposes the {@link HttpSession} that we're wrapping.
	 * @param allowCreate whether to allow creation of a new session if none exists yet
	 */
	public final HttpSession getSession() {
		return getSession(true) ;
	}


	public Object getAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attribute - request is not active anymore!");
			}
			return this.request.getAttribute(name);
		}
		else {
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					Object value = session.getAttribute(name);
					if (value != null) {
						synchronized (this.sessionAttributesToUpdate) {
							this.sessionAttributesToUpdate.put(name, value);
						}
					}
					return value;
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			return null;
		}
	}

	public void setAttribute(String name, Object value, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot set request attribute - request is not active anymore!");
			}
			this.request.setAttribute(name, value);
		}
		else {
			HttpSession session = getSession(true);
			synchronized (this.sessionAttributesToUpdate) {
				this.sessionAttributesToUpdate.remove(name);
			}
			session.setAttribute(name, value);
		}
	}

	public void removeAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (isRequestActive()) {
				this.request.removeAttribute(name);
				removeRequestDestructionCallback(name);
			}
		}
		else {
			HttpSession session = getSession(false);
			if (session != null) {
				synchronized (this.sessionAttributesToUpdate) {
					this.sessionAttributesToUpdate.remove(name);
				}
				try {
					session.removeAttribute(name);
					// Remove any registered destruction callback as well.
					session.removeAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
		}
	}

	public String[] getAttributeNames(int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attributes - request is not active anymore!");
			}
			return StringUtil.toStringArray(this.request.getAttributeNames());
		}
		else {
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					return StringUtil.toStringArray(session.getAttributeNames());
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			return new String[0];
		}
	}

	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (scope == SCOPE_REQUEST) {
			registerRequestDestructionCallback(name, callback);
		}
		else {
			registerSessionDestructionCallback(name, callback);
		}
	}

	public String getSessionId() {
		return getSession(true).getId();
	}

	public Object getSessionMutex() {
		return WebUtils.getSessionMutex(getSession(true));
	}


//	/**
//	 * Update all accessed session attributes through <code>session.setAttribute</code>
//	 * calls, explicitly indicating to the container that they might have been modified.
//	 */
//	protected void updateAccessedSessionAttributes() {
//		// Store session reference for access after request completion.
//		this.session = this.request.getSession(false);
//		// Update all affected session attributes.
//		synchronized (this.sessionAttributesToUpdate) {
//			if (this.session != null) {
//				try {
//					for (Iterator it = this.sessionAttributesToUpdate.entrySet().iterator(); it.hasNext();) {
//						Map.Entry entry = (Map.Entry) it.next();
//						String name = (String) entry.getKey();
//						Object newValue = entry.getValue();
//						Object oldValue = this.session.getAttribute(name);
//						if (oldValue == newValue) {
//							this.session.setAttribute(name, newValue);
//						}
//					}
//				}
//				catch (IllegalStateException ex) {
//					// Session invalidated - shouldn't usually happen.
//				}
//			}
//			this.sessionAttributesToUpdate.clear();
//		}
//	}
	
	/**
	 * Update all accessed session attributes through <code>session.setAttribute</code>
	 * calls, explicitly indicating to the container that they might have been modified.
	 */
	protected void updateAccessedSessionAttributes() {
//		// Store session reference for access after request completion.
//		this.session = this.request.getSession(false);
//		// Update all affected session attributes.
//		synchronized (this.sessionAttributesToUpdate) {
//			if (this.session != null) {
//				try {
//					for (Iterator it = this.sessionAttributesToUpdate.entrySet().iterator(); it.hasNext();) {
//						Map.Entry entry = (Map.Entry) it.next();
//						String name = (String) entry.getKey();
//						Object newValue = entry.getValue();
//						Object oldValue = this.session.getAttribute(name);
//						if (oldValue == newValue) {
//							this.session.setAttribute(name, newValue);
//						}
//					}
//				}
//				catch (IllegalStateException ex) {
//					// Session invalidated - shouldn't usually happen.
//				}
//			}
//			this.sessionAttributesToUpdate.clear();
//		}
	}

	/**
	 * Register the given callback as to be executed after session termination.
	 * @param name the name of the attribute to register the callback for
	 * @param callback the callback to be executed for destruction
	 */
	private void registerSessionDestructionCallback(String name, Runnable callback) {
		HttpSession session = getSession(true);
		session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name,
				new DestructionCallbackBindingListener(callback));
	}


	public String toString() {
		return this.request.toString();
	}


	/**
	 * Adapter that implements the Servlet 2.3 HttpSessionBindingListener
	 * interface, wrapping a session destruction callback.
	 */
	private static class DestructionCallbackBindingListener implements HttpSessionBindingListener, Serializable {

		private final Runnable destructionCallback;

		public DestructionCallbackBindingListener(Runnable destructionCallback) {
			this.destructionCallback = destructionCallback;
		}

		public void valueBound(HttpSessionBindingEvent event) {
		}

		public void valueUnbound(HttpSessionBindingEvent event) {
			this.destructionCallback.run();
		}
	}

}
