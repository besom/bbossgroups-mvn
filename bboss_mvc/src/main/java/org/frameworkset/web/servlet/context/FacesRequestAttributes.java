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

import java.lang.reflect.Method;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.frameworkset.util.Assert;
import org.frameworkset.util.ReflectionUtils;
import org.frameworkset.web.util.WebUtils;

import com.frameworkset.util.StringUtil;

/**
 * <p>Title: FacesRequestAttributes.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2010-10-1
 * @author biaoping.yin
 * @version 1.0
 */
public class FacesRequestAttributes implements RequestAttributes {

	/**
	 * We'll create a lot of these objects, so we don't want a new logger every time.
	 */
	protected final static Logger logger = Logger.getLogger(FacesRequestAttributes.class);
//	private static final Log logger = LogFactory.getLog(FacesRequestAttributes.class);

	private final FacesContext facesContext;


	/**
	 * Create a new FacesRequestAttributes adapter for the given FacesContext.
	 * @param facesContext the current FacesContext
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public FacesRequestAttributes(FacesContext facesContext) {
		Assert.notNull(facesContext, "FacesContext must not be null");
		this.facesContext = facesContext;
	}


	/**
	 * Return the JSF FacesContext that this adapter operates on.
	 */
	protected FacesContext getFacesContext() {
		return this.facesContext;
	}

	/**
	 * Return the JSF ExternalContext that this adapter operates on.
	 * @see javax.faces.context.FacesContext#getExternalContext()
	 */
	protected ExternalContext getExternalContext() {
		return getFacesContext().getExternalContext();
	}

	/**
	 * Return the JSF attribute Map for the specified scope
	 * @param scope constant indicating request or session scope
	 * @return the Map representation of the attributes in the specified scope
	 * @see #SCOPE_REQUEST
	 * @see #SCOPE_SESSION
	 */
	protected Map getAttributeMap(int scope) {
		if (scope == SCOPE_REQUEST) {
			return getExternalContext().getRequestMap();
		}
		else {
			return getExternalContext().getSessionMap();
		}
	}


	public Object getAttribute(String name, int scope) {
		return getAttributeMap(scope).get(name);
	}

	public void setAttribute(String name, Object value, int scope) {
		getAttributeMap(scope).put(name, value);
	}

	public void removeAttribute(String name, int scope) {
		getAttributeMap(scope).remove(name);
	}

	public String[] getAttributeNames(int scope) {
		return StringUtil.toStringArray(getAttributeMap(scope).entrySet());
	}

	public void registerDestructionCallback(String name, Runnable callback, int scope) {
//		if (logger.isWarnEnabled())
		{
			logger.warn("Could not register destruction callback [" + callback + "] for attribute '" + name +
					"' because FacesRequestAttributes does not support such callbacks");
		}
	}

	public String getSessionId() {
		Object session = getExternalContext().getSession(true);
		try {
			// Both HttpSession and PortletSession have a getId() method.
			Method getIdMethod = session.getClass().getMethod("getId", new Class[0]);
			return ReflectionUtils.invokeMethod(getIdMethod, session).toString();
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Session object [" + session + "] does not have a getId() method");
		}
	}

	public Object getSessionMutex() {
		// Enforce presence of a session first to allow listeners
		// to create the mutex attribute, if any.
		Object session = getExternalContext().getSession(true);
		Object mutex = getExternalContext().getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = session;
		}
		return mutex;
	}

}
