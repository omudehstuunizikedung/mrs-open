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
package org.openmrs;

/**
 * DrugFrequency
 * 
 * @since 1.10
 */
public class DrugFrequency extends BaseOpenmrsMetadata {
	
	private Integer orderFrequencyId;
	
	private Double frequencyPerDay;
	
	private String uuid;
	
	private Concept concept;
	
	/**
	 * Get the orderFrequencyId
	 * 
	 * @since 1.10
	 */
	public Integer getOrderFrequencyId() {
		return orderFrequencyId;
	}
	
	/**
	 * Sets the orderFrequencyId
	 * 
	 * @param orderFrequencyId
	 */
	public void setOrderFrequencyId(Integer orderFrequencyId) {
		this.orderFrequencyId = orderFrequencyId;
	}
	
	/**
	 * Get the frequencyPerDay
	 * 
	 * @since 1.10
	 */
	public Double getFrequencyPerDay() {
		return frequencyPerDay;
	}
	
	public void setFrequencyPerDay(Double frequencyPerDay) {
		this.frequencyPerDay = frequencyPerDay;
	}
	
	/**
	 * Get the uuid
	 * 
	 * @since 1.10
	 */
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * @since 1.5
	 * @see org.openmrs.OpenmrsObject#getId()
	 */
	public Integer getId() {
		return getOrderFrequencyId();
	}
	
	/**
	 * @since 1.5
	 * @see org.openmrs.OpenmrsObject#setId(java.lang.Integer)
	 */
	public void setId(Integer id) {
		setOrderFrequencyId(id);
	}
	
	/**
	 * Get the concept for the drugFrequency
	 * 
	 * @since 1.10
	 */
	public Concept getConcept() {
		return concept;
	}
	
	/**
	 * Sets the concept for the drugFrequency
	 * 
	 * @since 1.10
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
}
