/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.notification.db.hibernate;

import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseContextSensitiveTest;

public class HibernateSentMessageDAOTest extends BaseContextSensitiveTest {

	private HibernateSentMessageDAO hibernateSentMessageDAO;
	private SessionFactory sessionFactory;
	
	@Before
	public void getSentMessageDAO() {
		hibernateSentMessageDAO = (HibernateSentMessageDAO) applicationContext.getBean("sentMessageDAO");
		sessionFactory = (SessionFactory) applicationContext.getBean("sessionFactory");
	}
	
	@Test
	public void sentMessageDAO_shouldNotBeNull() {
		Assert.assertNotNull(hibernateSentMessageDAO);
		Assert.assertNotNull(sessionFactory);
	}
}
