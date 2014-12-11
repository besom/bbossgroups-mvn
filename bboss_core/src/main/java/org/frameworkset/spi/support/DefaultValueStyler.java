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

package org.frameworkset.spi.support;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.frameworkset.util.ClassUtils;
import org.frameworkset.util.ObjectUtils;



/**
 * <p>Title: DefaultValueStyler.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2007</p>
 * @Date 2010-10-12 下午11:15:21
 * @author biaoping.yin
 * @version 1.0
 */
public class DefaultValueStyler  implements ValueStyler {

	private static final String EMPTY = "[empty]";
	private static final String NULL = "[null]";
	private static final String COLLECTION = "collection";
	private static final String SET = "set";
	private static final String LIST = "list";
	private static final String MAP = "map";
	private static final String ARRAY = "array";


	public String style(Object value) {
		if (value == null) {
			return NULL;
		}
		else if (value instanceof String) {
			return "\'" + value + "\'";
		}
		else if (value instanceof Class) {
			return ClassUtils.getShortName((Class) value);
		}
		else if (value instanceof Method) {
			Method method = (Method) value;
			return method.getName() + "@" + ClassUtils.getShortName(method.getDeclaringClass());
		}
		else if (value instanceof Map) {
			return style((Map) value);
		}
		else if (value instanceof Map.Entry) {
			return style((Map.Entry) value);
		}
		else if (value instanceof Collection) {
			return style((Collection) value);
		}
		else if (value.getClass().isArray()) {
			return styleArray(ObjectUtils.toObjectArray(value));
		}
		else {
			return String.valueOf(value);
		}
	}

	private String style(Map value) {
		StringBuffer buffer = new StringBuffer(value.size() * 8 + 16);
		buffer.append(MAP + "[");
		for (Iterator it = value.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			buffer.append(style(entry));
			if (it.hasNext()) {
				buffer.append(',').append(' ');
			}
		}
		if (value.isEmpty()) {
			buffer.append(EMPTY);
		}
		buffer.append("]");
		return buffer.toString();
	}

	private String style(Map.Entry value) {
		return style(value.getKey()) + " -> " + style(value.getValue());
	}

	private String style(Collection value) {
		StringBuffer buffer = new StringBuffer(value.size() * 8 + 16);
		buffer.append(getCollectionTypeString(value)).append('[');
		for (Iterator i = value.iterator(); i.hasNext();) {
			buffer.append(style(i.next()));
			if (i.hasNext()) {
				buffer.append(',').append(' ');
			}
		}
		if (value.isEmpty()) {
			buffer.append(EMPTY);
		}
		buffer.append("]");
		return buffer.toString();
	}

	private String getCollectionTypeString(Collection value) {
		if (value instanceof List) {
			return LIST;
		}
		else if (value instanceof Set) {
			return SET;
		}
		else {
			return COLLECTION;
		}
	}

	private String styleArray(Object[] array) {
		StringBuffer buffer = new StringBuffer(array.length * 8 + 16);
		buffer.append(ARRAY + "<" + ClassUtils.getShortName(array.getClass().getComponentType()) + ">[");
		for (int i = 0; i < array.length - 1; i++) {
			buffer.append(style(array[i]));
			buffer.append(',').append(' ');
		}
		if (array.length > 0) {
			buffer.append(style(array[array.length - 1]));
		}
		else {
			buffer.append(EMPTY);
		}
		buffer.append("]");
		return buffer.toString();
	}

}
