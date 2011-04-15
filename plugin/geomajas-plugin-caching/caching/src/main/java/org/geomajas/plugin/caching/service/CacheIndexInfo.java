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

package org.geomajas.plugin.caching.service;

import org.geomajas.global.FutureApi;

import javax.validation.constraints.NotNull;

/**
 * Configuration of the spatial index to use for a cache.
 *
 * @author Joachim Van der Auwera
 * @since 1.0.0
 */
@FutureApi(allMethods = true)
public class CacheIndexInfo extends LayerCategoryInfo {

	@NotNull
	private CacheIndexFactory cacheIndexFactory;

	public CacheIndexFactory getCacheIndexFactory() {
		return cacheIndexFactory;
	}

	public void setCacheIndexFactory(CacheIndexFactory indexFactory) {
		this.cacheIndexFactory = indexFactory;
	}

}
