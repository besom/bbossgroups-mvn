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
package org.frameworkset.web.servlet.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.frameworkset.spi.BeanNameAware;
import org.frameworkset.util.CollectionUtils;
import org.frameworkset.web.servlet.support.RequestContext;
import org.frameworkset.web.servlet.support.WebApplicationObjectSupport;

/**
 * <p>Title: AbstractView.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2010-9-28
 * @author biaoping.yin
 * @version 1.0
 */
public abstract class AbstractView  extends WebApplicationObjectSupport implements View, BeanNameAware {

	/** Default content type. Overridable as bean property. */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=ISO-8859-1";

	/** Initial size for the temporary output byte array (if any) */
	private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;


	private String beanName;

	private String contentType = DEFAULT_CONTENT_TYPE;

	private String requestContextAttribute;

	/** Map of static attributes, keyed by attribute name (String) */
	private final Map	staticAttributes = new HashMap();


	/**
	 * Set the view's name. Helpful for traceability.
	 * <p>Framework code must call this when constructing views.
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Return the view's name. Should never be <code>null</code>,
	 * if the view was correctly configured.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Set the content type for this view.
	 * Default is "text/html;charset=ISO-8859-1".
	 * <p>May be ignored by subclasses if the view itself is assumed
	 * to set the content type, e.g. in case of JSPs.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the content type for this view.
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * Set the name of the RequestContext attribute for this view.
	 * Default is none.
	 */
	public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * Return the name of the RequestContext attribute, if any.
	 */
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * Set static attributes as a CSV string.
	 * Format is: attname0={value1},attname1={value1}
	 * <p>"Static" attributes are fixed attributes that are specified in
	 * the View instance configuration. "Dynamic" attributes, on the other hand,
	 * are values passed in as part of the model.
	 */
	public void setAttributesCSV(String propString) throws IllegalArgumentException {
		if (propString != null) {
			StringTokenizer st = new StringTokenizer(propString, ",");
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				int eqIdx = tok.indexOf("=");
				if (eqIdx == -1) {
					throw new IllegalArgumentException("Expected = in attributes CSV string '" + propString + "'");
				}
				if (eqIdx >= tok.length() - 2) {
					throw new IllegalArgumentException(
							"At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
				}
				String name = tok.substring(0, eqIdx);
				String value = tok.substring(eqIdx + 1);

				// Delete first and last characters of value: { and }
				value = value.substring(1);
				value = value.substring(0, value.length() - 1);

				addStaticAttribute(name, value);
			}
		}
	}

	/**
	 * Set static attributes for this view from a
	 * <code>java.util.Properties</code> object.
	 * <p>"Static" attributes are fixed attributes that are specified in
	 * the View instance configuration. "Dynamic" attributes, on the other hand,
	 * are values passed in as part of the model.
	 * <p>This is the most convenient way to set static attributes. Note that
	 * static attributes can be overridden by dynamic attributes, if a value
	 * with the same name is included in the model.
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.

	 */
	public void setAttributes(Properties attributes) {
		CollectionUtils.mergePropertiesIntoMap(attributes, this.staticAttributes);
	}

	/**
	 * Set static attributes for this view from a Map. This allows to set
	 * any kind of attribute values, for example bean references.
	 * <p>"Static" attributes are fixed attributes that are specified in
	 * the View instance configuration. "Dynamic" attributes, on the other hand,
	 * are values passed in as part of the model.
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @param attributes Map with name Strings as keys and attribute objects as values
	 */
	public void setAttributesMap(Map attributes) {
		if (attributes != null) {
			Iterator it = attributes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				Object key = entry.getKey();
				if (!(key instanceof String)) {
					throw new IllegalArgumentException(
							"Invalid attribute key [" + key + "]: only Strings allowed");
				}
				addStaticAttribute((String) key, entry.getValue());
			}
		}
	}

	/**
	 * Allow Map access to the static attributes of this view,
	 * with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "attributesMap[myKey]". This is particularly useful for
	 * adding or overriding entries in child view definitions.
	 */
	public Map getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * Add static data to this view, exposed in each view.
	 * <p>"Static" attributes are fixed attributes that are specified in
	 * the View instance configuration. "Dynamic" attributes, on the other hand,
	 * are values passed in as part of the model.
	 * <p>Must be invoked before any calls to <code>render</code>.
	 * @param name the name of the attribute to expose
	 * @param value the attribute value to expose
	 * @see #render
	 */
	public void addStaticAttribute(String name, Object value) {
		this.staticAttributes.put(name, value);
	}

	/**
	 * Return the static attributes for this view. Handy for testing.
	 * <p>Returns an unmodifiable Map, as this is not intended for
	 * manipulating the Map but rather just for checking the contents.
	 * @return the static attributes in this view
	 */
	public Map getStaticAttributes() {
		return Collections.unmodifiableMap(this.staticAttributes);
	}


	/**
	 * Prepares the view given the specified model, merging it with static
	 * attributes and a RequestContext attribute, if necessary.
	 * Delegates to renderMergedOutputModel for the actual rendering.
	 * @see #renderMergedOutputModel
	 */
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view with name '" + this.beanName + "' with model " + model +
				" and static attributes " + this.staticAttributes);
		}

		// Consolidate static and dynamic model attributes.
		Map mergedModel = new HashMap(this.staticAttributes.size() + (model != null ? model.size() : 0));
		mergedModel.putAll(this.staticAttributes);
		if (model != null) {
			mergedModel.putAll(model);
		}

		// Expose RequestContext?
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, mergedModel));
		}

		prepareResponse(request, response);
		renderMergedOutputModel(mergedModel, request, response);
	}

	/**
	 * Create a RequestContext to expose under the specified attribute name.
	 * <p>Default implementation creates a standard RequestContext instance for the
	 * given request and model. Can be overridden in subclasses for custom instances.
	 * @param request current HTTP request
	 * @param model combined output Map (never <code>null</code>),
	 * with dynamic values taking precedence over static attributes
	 * @return the RequestContext instance
	 * @see #setRequestContextAttribute

	 */
	protected RequestContext createRequestContext(HttpServletRequest request, Map model) {
		return new RequestContext(request, getServletContext(), model);
	}

	/**
	 * Prepare the given response for rendering.
	 * <p>The default implementation applies a workaround for an IE bug
	 * when sending download content via HTTPS.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		if (generatesDownloadContent()) {
			response.setHeader("Pragma", "private");
			response.setHeader("Cache-Control", "private, must-revalidate");
		}
	}

	/**
	 * Return whether this view generates download content
	 * (typically binary content like PDF or Excel files).
	 * <p>The default implementation returns <code>false</code>. Subclasses are
	 * encouraged to return <code>true</code> here if they know that they are
	 * generating download content that requires temporary caching on the
	 * client side, typically via the response OutputStream.
	 * @see #prepareResponse
	 * @see javax.servlet.http.HttpServletResponse#getOutputStream()
	 */
	protected boolean generatesDownloadContent() {
		return false;
	}

	/**
	 * Subclasses must implement this method to actually render the view.
	 * <p>The first step will be preparing the request: In the JSP case,
	 * this would mean setting model objects as request attributes.
	 * The second step will be the actual rendering of the view,
	 * for example including the JSP via a RequestDispatcher.
	 * @param model combined output Map (never <code>null</code>),
	 * with dynamic values taking precedence over static attributes
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if rendering failed
	 */
	protected abstract void renderMergedOutputModel(
			Map<String,Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;


	/**
	 * Expose the model objects in the given map as request attributes.
	 * Names will be taken from the model Map.
	 * This method is suitable for all resources reachable by {@link javax.servlet.RequestDispatcher}.
	 * @param model Map of model objects to expose
	 * @param request current HTTP request
	 */
	protected void exposeModelAsRequestAttributes(Map model, HttpServletRequest request) throws Exception {
		Iterator it = model.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			if (!(entry.getKey() instanceof String)) {
				throw new IllegalArgumentException(
						"Invalid key [" + entry.getKey() + "] in model Map: only Strings allowed as model keys");
			}
			String modelName = (String) entry.getKey();
			Object modelValue = entry.getValue();
			if (modelValue != null) {
				request.setAttribute(modelName, modelValue);
				if (logger.isDebugEnabled()) {
					logger.debug("Added model object '" + modelName + "' of type [" + modelValue.getClass().getName() +
							"] to request in view with name '" + getBeanName() + "'");
				}
			}
			else {
				request.removeAttribute(modelName);
				if (logger.isDebugEnabled()) {
					logger.debug("Removed model object '" + modelName +
							"' from request in view with name '" + getBeanName() + "'");
				}
			}
		}
	}

	/**
	 * Create a temporary OutputStream for this view.
	 * <p>This is typically used as IE workaround, for setting the content length header
	 * from the temporary stream before actually writing the content to the HTTP response.
	 */
	protected ByteArrayOutputStream createTemporaryOutputStream() {
		return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
	}

	/**
	 * Write the given temporary OutputStream to the HTTP response.
	 * @param response current HTTP response
	 * @param baos the temporary OutputStream to write
	 * @throws IOException if writing/flushing failed
	 */
	protected void writeToResponse(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
		// Write content type and also length (determined via byte array).
		response.setContentType(getContentType());
		response.setContentLength(baos.size());

		// Flush byte array to servlet output stream.
		ServletOutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.flush();
	}


	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		if (getBeanName() != null) {
			sb.append(": name '").append(getBeanName()).append("'");
		}
		else {
			sb.append(": unnamed");
		}
		return sb.toString();
	}

}
