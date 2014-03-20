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
package org.openmrs.reporting;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.api.PatientSetService;
import org.openmrs.api.PatientSetService.GroupMethod;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.report.EvaluationContext;
import org.openmrs.util.OpenmrsUtil;

/**
 * @deprecated see reportingcompatibility module
 */
@Deprecated
public class DrugOrderFilter extends CachingPatientFilter {
	
	private static final long serialVersionUID = 1L;
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private List<Drug> drugList;
	
	private List<Concept> drugSets;
	
	private PatientSetService.GroupMethod anyOrAll;
	
	private Integer withinLastDays;
	
	private Integer withinLastMonths;
	
	private Integer untilDaysAgo;
	
	private Integer untilMonthsAgo;
	
	private Date sinceDate;
	
	private Date untilDate;
	
	public DrugOrderFilter() {
		super.setType("Patient Filter");
		super.setSubType("Drug Order Filter");
	}
	
	@Override
	public String getCacheKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append(".");
		sb.append(getAnyOrAll()).append(".");
		sb.append(
		    OpenmrsUtil.fromDateHelper(null, withinLastDays, withinLastMonths, untilDaysAgo, untilMonthsAgo, sinceDate,
		        untilDate)).append(".");
		sb.append(
		    OpenmrsUtil.toDateHelper(null, withinLastDays, withinLastMonths, untilDaysAgo, untilMonthsAgo, sinceDate,
		        untilDate)).append(".");
		if (getDrugListToUse() != null)
			for (Drug d : getDrugListToUse())
				sb.append(d.getDrugId()).append(",");
		return sb.toString();
	}
	
	public String getDescription() {
		MessageSourceService mss = Context.getMessageSourceService();
		Locale locale = Context.getLocale();
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Context.getLocale());
		StringBuilder ret = new StringBuilder();
		boolean currentlyCase = getWithinLastDays() != null && getWithinLastDays() == 0
		        && (getWithinLastMonths() == null || getWithinLastMonths() == 0);
		if (currentlyCase)
			ret.append(mss.getMessage("reporting.patientCurrently")).append(" ");
		else
			ret.append(mss.getMessage("reporting.patients")).append(" ");
		if (getDrugListToUse() == null || getDrugListToUse().isEmpty()) {
			if (getAnyOrAll() == GroupMethod.NONE)
				ret.append(currentlyCase ? mss.getMessage("reporting.takingNoDrugs") : mss
				        .getMessage("reporting.whoNeverTakeDrug"));
			else
				ret.append(currentlyCase ? mss.getMessage("reporting.takingAnyDrugs") : mss
				        .getMessage("reporting.everTakingAnyDrugs"));
		} else {
			if (getDrugListToUse().size() == 1) {
				if (getAnyOrAll() == GroupMethod.NONE)
					ret.append(mss.getMessage("reporting.notTaking")).append(" ");
				else
					ret.append(mss.getMessage("reporting.taking").toLowerCase()).append(" ");
				ret.append(getDrugListToUse().get(0).getName());
			} else {
				ret.append(mss.getMessage("reporting.taking").toLowerCase()).append(" ").append(getAnyOrAll()).append(" ")
				        .append(mss.getMessage("reporting.of")).append(" [");
				for (Iterator<Drug> i = getDrugListToUse().iterator(); i.hasNext();) {
					ret.append(i.next().getName());
					if (i.hasNext())
						ret.append(" , ");
				}
				ret.append("]");
			}
		}
		
		Integer within_last_days = getWithinLastDays();
		Integer within_last_months = getWithinLastMonths();
		if (!currentlyCase)
			if (within_last_days != null || within_last_months != null) {
				if (within_last_months != null)
					ret.append(" ").append(
					    mss.getMessage("reporting.WithinTheLastMonth(s)", new Object[] { within_last_months }, locale));
				
				if (within_last_days != null)
					ret.append(" ").append(
					    mss.getMessage("reporting.WithinTheLastDay(s)", new Object[] { within_last_days }, locale));
			}
		if (getSinceDate() != null)
			ret.append(" ").append(mss.getMessage("reporting.since", new Object[] { df.format(getSinceDate()) }, locale));
		if (getUntilDate() != null)
			ret.append(" ").append(mss.getMessage("reporting.until", new Object[] { df.format(getUntilDate()) }, locale));
		return ret.toString();
	}
	
	@Override
	public Cohort filterImpl(EvaluationContext context) {
		List<Integer> drugIds = new ArrayList<Integer>();
		if (getDrugListToUse() != null)
			for (Drug d : getDrugListToUse())
				drugIds.add(d.getDrugId());
		log.debug("filtering with these ids " + drugIds);
		Collection<Integer> patientIds = context == null ? null : context.getBaseCohort().getMemberIds();
		return Context.getPatientSetService().getPatientsHavingDrugOrder(
		    patientIds,
		    drugIds,
		    getAnyOrAll(),
		    OpenmrsUtil.fromDateHelper(null, getWithinLastDays(), getWithinLastMonths(), getUntilDaysAgo(),
		        getUntilMonthsAgo(), getSinceDate(), getUntilDate()),
		    OpenmrsUtil.toDateHelper(null, getWithinLastDays(), getWithinLastMonths(), getUntilDaysAgo(),
		        getUntilMonthsAgo(), getSinceDate(), getUntilDate()));
	}
	
	public boolean isReadyToRun() {
		return true;
	}
	
	public List<Drug> getDrugListToUse() {
		List<Drug> drug_list = getDrugList();
		List<Concept> drug_sets = getDrugSets();
		if (drug_list == null && drug_sets == null)
			return null;
		List<Drug> ret = new ArrayList<Drug>();
		if (drug_list != null)
			ret.addAll(drug_list);
		if (drug_sets != null) {
			Set<Concept> generics = new HashSet<Concept>();
			for (Concept drugSet : drug_sets) {
				List<Concept> list = Context.getConceptService().getConceptsByConceptSet(drugSet);
				generics.addAll(list);
			}
			for (Concept generic : generics) {
				ret.addAll(Context.getConceptService().getDrugsByConcept(generic));
			}
		}
		return ret;
	}
	
	// getters and setters
	
	public PatientSetService.GroupMethod getAnyOrAll() {
		return anyOrAll;
	}
	
	public void setAnyOrAll(PatientSetService.GroupMethod anyOrAll) {
		this.anyOrAll = anyOrAll;
	}
	
	public List<Drug> getDrugList() {
		return drugList;
	}
	
	public void setDrugList(List<Drug> drugList) {
		this.drugList = drugList;
	}
	
	public Date getSinceDate() {
		return sinceDate;
	}
	
	public void setSinceDate(Date sinceDate) {
		this.sinceDate = sinceDate;
	}
	
	public Date getUntilDate() {
		return untilDate;
	}
	
	public void setUntilDate(Date untilDate) {
		this.untilDate = untilDate;
	}
	
	public Integer getUntilDaysAgo() {
		return untilDaysAgo;
	}
	
	public void setUntilDaysAgo(Integer untilDaysAgo) {
		this.untilDaysAgo = untilDaysAgo;
	}
	
	public Integer getUntilMonthsAgo() {
		return untilMonthsAgo;
	}
	
	public void setUntilMonthsAgo(Integer untilMonthsAgo) {
		this.untilMonthsAgo = untilMonthsAgo;
	}
	
	public Integer getWithinLastDays() {
		return withinLastDays;
	}
	
	public void setWithinLastDays(Integer withinLastDays) {
		this.withinLastDays = withinLastDays;
	}
	
	public Integer getWithinLastMonths() {
		return withinLastMonths;
	}
	
	public void setWithinLastMonths(Integer withinLastMonths) {
		this.withinLastMonths = withinLastMonths;
	}
	
	public List<Concept> getDrugSets() {
		return drugSets;
	}
	
	public void setDrugSets(List<Concept> drugSets) {
		this.drugSets = drugSets;
	}
	
}
