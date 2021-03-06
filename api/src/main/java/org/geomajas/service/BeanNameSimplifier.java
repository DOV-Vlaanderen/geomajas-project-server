/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2016 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.service;

import org.geomajas.annotation.Api;

/**
 * Simplifier to change fully qualified class names into Geomajas bean names.
 *
 * @author Joachim Van der Auwera
 * @since 1.6.0
 */
@Api(allMethods = true)
public interface BeanNameSimplifier {

	/**
	 * Convert fully qualified class name in Geomajas bean fqn.
	 *
	 * @param fqn fully qualified class name
	 * @return bean name
	 */
	String simplify(String fqn);
}
