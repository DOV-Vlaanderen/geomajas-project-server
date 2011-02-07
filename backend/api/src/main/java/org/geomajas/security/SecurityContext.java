/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.security;

import java.util.List;

import org.geomajas.global.Api;
import org.geomajas.global.CacheableObject;

/**
 * The security context is a thread scoped service which allows you to query the authorization details for the
 * logged in user.
 *
 * @author Joachim Van der Auwera
 * @since 1.6.0
 */
@Api(allMethods = true)
public interface SecurityContext extends Authorization, UserInfo {

	/**
	 * Get the direct replies of the security services which build the security context.
	 * <p/>
	 * In principle this method should not be used.
	 *
	 * @return array of security service id's
	 */
	List<Authentication> getSecurityServiceResults();

	/**
	 * Get the token which was used for the authentication.
	 *
	 * @return token which was used.
	 */
	String getToken();
	
	/**
	 * Restores the context from the specified cacheable object.
	 * @param object cacheable object
	 * @since 1.9.0
	 */
	void restore(CacheableObject object);
	
	/**
	 * Returns the cacheable object that allows to (persistently) cache this security context.
	 * @return the cacheable object
	 * @since 1.9.0
	 */
	CacheableObject getCacheableObject();
	
}
