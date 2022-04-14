/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.api.context;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.UserService;
import org.openmrs.api.handler.EncounterVisitHandler;
import org.openmrs.api.handler.ExistingOrNewVisitAssignmentHandler;
import org.openmrs.test.jupiter.BaseContextSensitiveTest;
import org.openmrs.util.LocaleUtility;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Validator;

/**
 * TODO add methods for all context tests
 * 
 * @see Context
 */
public class ContextTest extends BaseContextSensitiveTest {
	
	@Autowired
	private SessionFactory sf;

	private static final Class PERSON_NAME_CLASS = PersonName.class;

	private static final Integer PERSON_NAME_ID_2 = 2;

	private static final Integer PERSON_NAME_ID_9349 = 9349;

	private static final String QUERY_REGION = "test";
	
	/**
	 * Methods in this class might authenticate with a different user, so log that user out after
	 * this whole junit class is done.
	 */
	@AfterAll
	public static void logOutAfterThisTestClass() {
		Context.logout();
	}
	
	/**
	 * @see Context#authenticate(String,String)
	 */
	@Test
	public void authenticate_shouldNotAuthenticateWithNullPassword() {
		assertThrows(ContextAuthenticationException.class, () -> Context.authenticate("some username", null));
	}
	
	/**
	 * @see Context#authenticate(String,String)
	 */
	@Test
	public void authenticate_shouldNotAuthenticateWithNullPasswordAndProperSystemId() {
		assertThrows(ContextAuthenticationException.class, () -> Context.authenticate("1-8", null));
	}
	
	/**
	 * @see Context#authenticate(String,String)
	 */
	@Test
	public void authenticate_shouldNotAuthenticateWithNullPasswordAndProperUsername() {
		assertThrows(ContextAuthenticationException.class, () -> Context.authenticate("admin", null));
	}
	
	/**
	 * @see Context#authenticate(String,String)
	 */
	@Test
	public void authenticate_shouldNotAuthenticateWithNullUsername() {
		assertThrows(ContextAuthenticationException.class, () -> Context.authenticate(null, "some password"));
	}
	
	/**
	 * @see Context#authenticate(String,String)
	 */
	@Test
	public void authenticate_shouldNotAuthenticateWithNullUsernameAndPassword() {
		assertThrows(ContextAuthenticationException.class, () -> Context.authenticate((String) null, (String) null));
	}
	
	/**
	 * @see Context#authenticate(String,String)
	 */
	@Test
	public void authenticate_shouldAuthenticateUserWithUsernameAndPassword() {
		// replay
		Context.logout();
		Context.authenticate("admin", "test");
		
		// verif
		assertEquals("admin", Context.getAuthenticatedUser().getUsername());
	}
	
	/**
	 * @see Context#getLocale()
	 */
	@Test
	public void getLocale_shouldNotFailIfSessionHasntBeenOpened() {
		Context.closeSession();
		assertEquals(LocaleUtility.getDefaultLocale(), Context.getLocale());
	}
	
	/**
	 * @see Context#getUserContext()
	 */
	@Test
	public void getUserContext_shouldFailIfSessionHasntBeenOpened() {
		Context.closeSession();
		assertThrows(APIException.class, () -> Context.getUserContext()); // trigger the api exception
	}
	
	/**
	 * @see Context#logout()
	 */
	@Test
	public void logout_shouldNotFailIfSessionHasntBeenOpenedYet() {
		Context.closeSession();
		Context.logout();
	}
	
	/**
	 * @see Context#isSessionOpen()
	 */
	@Test
	public void isSessionOpen_shouldReturnTrueIfSessionIsClosed() {
		assertTrue(Context.isSessionOpen());
		Context.closeSession();
		assertFalse(Context.isSessionOpen());
	}
	
	/**
	 * @see Context#refreshAuthenticatedUser()
	 */
	@Test
	public void refreshAuthenticatedUser_shouldGetFreshValuesFromTheDatabase() {
		User evictedUser = Context.getAuthenticatedUser();
		Context.evictFromSession(evictedUser);
		
		User fetchedUser = Context.getUserService().getUser(evictedUser.getUserId());
		fetchedUser.getPersonName().setGivenName("new username");
		
		Context.getUserService().saveUser(fetchedUser);
		
		// sanity check to make sure the cached object wasn't updated already
		assertNotSame(Context.getAuthenticatedUser().getGivenName(), fetchedUser.getGivenName());
		
		Context.refreshAuthenticatedUser();
		
		assertEquals("new username", Context.getAuthenticatedUser().getGivenName());
	}
	
	/**
	 * @see Context#getRegisteredComponents(Class)
	 */
	@Test
	public void getRegisteredComponents_shouldReturnAListOfAllRegisteredBeansOfThePassedType() {
		List<Validator> validators = Context.getRegisteredComponents(Validator.class);
		assertTrue(validators.size() > 0);
		assertTrue(Validator.class.isAssignableFrom(validators.iterator().next().getClass()));
	}
	
	/**
	 * @see Context#getRegisteredComponents(Class)
	 */
	@Test
	public void getRegisteredComponents_shouldReturnAnEmptyListIfNoBeansHaveBeenRegisteredOfThePassedType() {
		List<Location> l = Context.getRegisteredComponents(Location.class);
		assertNotNull(l);
		assertEquals(0, l.size());
	}
	
	/**
	 * @see Context#getRegisteredComponent(String,Class)
	 */
	@Test
	public void getRegisteredComponent_shouldReturnBeanHaveBeenRegisteredOfThePassedTypeAndName() {
		
		EncounterVisitHandler registeredComponent = Context.getRegisteredComponent("existingOrNewVisitAssignmentHandler",
		    EncounterVisitHandler.class);
		
		assertTrue(registeredComponent instanceof ExistingOrNewVisitAssignmentHandler);
	}
	
	/**
	 * @see Context#getRegisteredComponent(String, Class)
	 */
	@Test
	public void getRegisteredComponent_shouldFailIfBeanHaveBeenREgisteredOfThePassedTypeAndNameDoesntExist()
	{
		assertThrows(APIException.class, () -> Context.getRegisteredComponent("invalidBeanName", EncounterVisitHandler.class));
		
	}
	
	/**
	 * Prevents regression after patch from #2174:
	 * "Prevent duplicate proxies and AOP in context services"
	 * 
	 * @see Context#getService(Class)
	 */
	@Test
	public void getService_shouldReturnTheSameObjectWhenCalledMultipleTimesForTheSameClass() {
		PatientService ps1 = Context.getService(PatientService.class);
		PatientService ps2 = Context.getService(PatientService.class);
		assertEquals(ps2, ps1);
	}
	
	/**
	 * @see Context#becomeUser(String)
	 */
	@Test
	public void becomeUser_shouldChangeLocaleWhenBecomeAnotherUser() {
		UserService userService = Context.getUserService();
		
		User user = new User(new Person());
		user.addName(new PersonName("givenName", "middleName", "familyName"));
		user.getPerson().setGender("M");
		user.setUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE, "pt_BR");
		userService.createUser(user, "TestPass123");
		
		Context.becomeUser(user.getSystemId());
		
		Locale locale = Context.getLocale();
		assertEquals("pt", locale.getLanguage());
		assertEquals("BR", locale.getCountry());
		
		Context.logout();
	}
	
	/**
	 * @see Context#evictSingleEntity(SessionFactory, Class, String)
	 */
	@Test
	public void evictSingleEntity_shouldClearSingleEntityFromCaches(){
		//SessionFactory sf = (SessionFactory) applicationContext.getBean("sessionFactory");
		PersonName name = Context.getPersonService().getPersonName(PERSON_NAME_ID_2);
		if(name == null){
			log.debug("There is no person name with the id: {}", PERSON_NAME_ID_2);
			return;
		}
		//Load the person so that the names are also stored  in person names collection region
		Context.getPersonService().getPerson(name.getPerson().getPersonId());

		//Let's have the name in a query cache
		sf.getCurrentSession().createCriteria(PERSON_NAME_CLASS)
			.add(Restrictions.eq("personNameId", PERSON_NAME_ID_2))
			.setCacheable(true)
			.setCacheRegion(QUERY_REGION)
			.list();
		assertTrue(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_2));
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
					  .getEntries().get(name.getPerson().getPersonId()));
		assertEquals(1, sf.getCache().getQueryCache(QUERY_REGION).getRegion().getElementCountInMemory());
		Context.evictSingleEntity(sf, PERSON_NAME_CLASS, name.getUuid());

		assertFalse(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_2));
		assertNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name.getPerson().getPersonId()));
		assertEquals(0, sf.getStatistics().getCacheRegionStatistics(QUERY_REGION).getElementCountInMemory());
	}

	/**
	 * @see Context#evictAllEntities(SessionFactory, Class)
	 */
	@Test
	public void evictAllEntities_shouldClearAllEntityFromCaches(){
		//SessionFactory sf = (SessionFactory) applicationContext.getBean("sessionFactory");
		PersonName name1 = Context.getPersonService().getPersonName(PERSON_NAME_ID_2);
		PersonName name2 = Context.getPersonService().getPersonName(PERSON_NAME_ID_9349);
		//Load the person so that the names are also stored  in person names collection region
		Context.getPersonService().getPerson(name1.getPerson().getPersonId());
		Context.getPersonService().getPerson(name2.getPerson().getPersonId());
		//Let's have the names in a query cache
		sf.getCurrentSession().createCriteria(PERSON_NAME_CLASS)
			.add(Restrictions.or(Restrictions.eq("personNameId", PERSON_NAME_ID_2),
				Restrictions.eq("personNameId", PERSON_NAME_ID_9349)))
			.setCacheable(true)
			.setCacheRegion(QUERY_REGION)
			.list();
		assertTrue(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_2));
		assertTrue(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_9349));
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name1.getPerson().getPersonId()));
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name2.getPerson().getPersonId()));
		assertEquals(1, sf.getStatistics().getCacheRegionStatistics(QUERY_REGION).getElementCountInMemory());

		Context.evictAllEntities(sf, PERSON_NAME_CLASS);

		assertFalse(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_2));
		assertFalse(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_9349));
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name1.getPerson().getPersonId()));
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name2.getPerson().getPersonId()));
		assertEquals(0, sf.getStatistics().getCacheRegionStatistics(QUERY_REGION).getElementCountInMemory());

	}

	/**
	 * @see Context#clearEntireCache(SessionFactory)
	 */
	@Test
	public void clearEntireCache_shouldClearEntireCache(){
		//SessionFactory sf = (SessionFactory) applicationContext.getBean("sessionFactory");
		PersonName name1 = Context.getPersonService().getPersonName(PERSON_NAME_ID_2);
		PersonName name2 = Context.getPersonService().getPersonName(PERSON_NAME_ID_9349);
		//Load the person and patient so that the names are also stored  in person names collection region
		Context.getPersonService().getPerson(name1.getPerson().getPersonId());
		Context.getPersonService().getPerson(name2.getPerson().getPersonId());
		Context.getPatientService().getPatient(PERSON_NAME_ID_2);
		//Let's have the names in a query cache
		sf.getCurrentSession().createCriteria(PERSON_NAME_CLASS)
			.add(Restrictions.or(Restrictions.eq("personNameId", PERSON_NAME_ID_2), 
				Restrictions.eq("personNameId", PERSON_NAME_ID_9349)))
			.setCacheable(true)
			.setCacheRegion(QUERY_REGION)
			.list();
		assertTrue(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_2));
		assertTrue(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_9349));
		assertTrue(sf.getCache().containsEntity(Patient.class, PERSON_NAME_ID_2));
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name1.getPerson().getPersonId()));		
		assertNotNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name2.getPerson().getPersonId()));
		assertEquals(1, sf.getStatistics().getCacheRegionStatistics(QUERY_REGION).getElementCountInMemory());

		Context.clearEntireCache(sf);

		assertFalse(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_2));
		assertFalse(sf.getCache().containsEntity(PERSON_NAME_CLASS, PERSON_NAME_ID_9349));
		assertFalse(sf.getCache().containsEntity(Patient.class, PERSON_NAME_ID_2));
		assertNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name1.getPerson().getPersonId()));
		assertNull(sf.getStatistics().getSecondLevelCacheStatistics(Person.class.getName() + ".names")
			.getEntries().get(name2.getPerson().getPersonId()));
		assertEquals(0, sf.getStatistics().getCacheRegionStatistics(QUERY_REGION).getElementCountInMemory());
	}
}
