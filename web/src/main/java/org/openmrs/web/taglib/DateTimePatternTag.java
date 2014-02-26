/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.web.taglib;

import java.text.SimpleDateFormat;

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;

/**
 * Returns a string like dd/mm/yyyy hh:mm for the current user
 * 
 * @since 1.9
 */
public class DateTimePatternTag extends TagSupport {
	
	private static final long serialVersionUID = 1L;
	
	private final Log log = LogFactory.getLog(getClass());
	
	/**
	 * This is to tell the user whether the string to be returned is the localized pattern or not,
	 * in use as the jquery datetimepicker widget format
	 */
	private String localize = null;
	
	/**
	 * Does the actual working of printing the datetime pattern
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
	 */
	@Override
	public int doStartTag() {
		
		SimpleDateFormat dateTimeFormat = Context.getDateTimeFormat();
		
		try {
			String pattern = dateTimeFormat.toLocalizedPattern().toLowerCase();
			
			if ((localize != null) && "false".equals(localize)) {
				pattern = dateTimeFormat.toPattern().toLowerCase();
			}
			
			pageContext.getOut().write(pattern);
		}
		catch (Exception e) {
			log.error("error getting datetime pattern", e);
		}
		
		return SKIP_BODY;
	}
	
	public String getLocalize() {
		return localize;
	}
	
	public void setLocalize(String localize) {
		this.localize = localize;
	}
}
