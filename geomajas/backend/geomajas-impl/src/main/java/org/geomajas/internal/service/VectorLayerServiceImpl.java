/*
 * This file is part of Geomajas, a component framework for building
 * rich Internet applications (RIA) with sophisticated capabilities for the
 * display, analysis and management of geographic information.
 * It is a building block that allows developers to add maps
 * and other geographic data capabilities to their web applications.
 *
 * Copyright 2008-2010 Geosparc, http://www.geosparc.com, Belgium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.geomajas.internal.service;

import java.util.ArrayList;
import java.util.List;

import org.geomajas.configuration.NamedStyleInfo;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.global.GeomajasSecurityException;
import org.geomajas.internal.layer.tile.InternalTileImpl;
import org.geomajas.internal.service.vector.GetBoundsContainer;
import org.geomajas.layer.VectorLayer;
import org.geomajas.layer.feature.InternalFeature;
import org.geomajas.layer.tile.InternalTile;
import org.geomajas.layer.tile.TileMetadata;
import org.geomajas.service.pipeline.PipelineCode;
import org.geomajas.service.pipeline.PipelineContext;
import org.geomajas.service.pipeline.PipelineService;
import org.geomajas.security.SecurityContext;
import org.geomajas.service.ConfigurationService;
import org.geomajas.service.GeoService;
import org.geomajas.service.VectorLayerService;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Implementation of {@link org.geomajas.service.VectorLayerService}, a service which allows accessing data from a
 * vector layer.
 * <p/>
 * All access to vector layers should be done through this service, not by accessing the layer directly as this
 * adds possible caching, security etc. These services are implemented using pipelines (see
 * {@link org.geomajas.service.pipeline.PipelineService}) to make them configurable.
 * 
 * @author Joachim Van der Auwera
 */
@Component
public class VectorLayerServiceImpl implements VectorLayerService {

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private GeoService geoService;

	@Autowired
	private SecurityContext securityContext;

	@Autowired
	private PipelineService pipelineService;

	private VectorLayer getVectorLayer(String layerId) throws GeomajasException {
		if (!securityContext.isLayerVisible(layerId)) {
			throw new GeomajasSecurityException(ExceptionCode.LAYER_NOT_VISIBLE, layerId, securityContext.getUserId());
		}
		VectorLayer layer = configurationService.getVectorLayer(layerId);
		if (null == layer) {
			throw new GeomajasException(ExceptionCode.VECTOR_LAYER_NOT_FOUND, layerId);
		}
		return layer;
	}

	public void saveOrUpdate(String layerId, CoordinateReferenceSystem crs, List<InternalFeature> oldFeatures,
			List<InternalFeature> newFeatures) throws GeomajasException {
		VectorLayer layer = getVectorLayer(layerId);
		MathTransform mapToLayer = geoService.findMathTransform(crs, layer.getCrs());
		PipelineContext context = pipelineService.createContext();
		context.put(PipelineCode.LAYER_ID_KEY, layerId);
		context.put(PipelineCode.LAYER_KEY, layer);
		context.put(PipelineCode.CRS_TRANSFORM_KEY, mapToLayer);
		context.put(PipelineCode.OLD_FEATURES_KEY, oldFeatures);
		context.put(PipelineCode.NEW_FEATURES_KEY, newFeatures);
		context.put(PipelineCode.CRS_KEY, crs);
		pipelineService.execute(PipelineCode.PIPELINE_SAVE_OR_UPDATE, layerId, context, null);
	}

	public List<InternalFeature> getFeatures(String layerId, CoordinateReferenceSystem crs, Filter queryFilter,
			NamedStyleInfo style, int featureIncludes, int offset, int maxResultSize) throws GeomajasException {
		VectorLayer layer = getVectorLayer(layerId);
		MathTransform transformation = null;
		if ((featureIncludes & FEATURE_INCLUDE_GEOMETRY) != 0 && crs != null && !crs.equals(layer.getCrs())) {
			transformation = geoService.findMathTransform(layer.getCrs(), crs);
		}
		List<InternalFeature> res = new ArrayList<InternalFeature>();
		PipelineContext context = pipelineService.createContext();
		context.put(PipelineCode.LAYER_ID_KEY, layerId);
		context.put(PipelineCode.LAYER_KEY, layer);
		context.put(PipelineCode.CRS_TRANSFORM_KEY, transformation);
		context.put(PipelineCode.CRS_KEY, crs);
		context.put(PipelineCode.FILTER_KEY, queryFilter);
		context.put(PipelineCode.STYLE_KEY, style);
		context.put(PipelineCode.FEATURE_INCLUDES_KEY, featureIncludes);
		context.put(PipelineCode.OFFSET_KEY, offset);
		context.put(PipelineCode.MAX_RESULT_SIZE_KEY, maxResultSize);
		pipelineService.execute(PipelineCode.PIPELINE_GET_FEATURES, layerId, context, res);
		return res;
	}

	public List<InternalFeature> getFeatures(String layerId, CoordinateReferenceSystem crs, Filter filter,
			NamedStyleInfo style, int featureIncludes) throws GeomajasException {
		return getFeatures(layerId, crs, filter, style, featureIncludes, 0, 0);
	}


	public Envelope getBounds(String layerId, CoordinateReferenceSystem crs, Filter queryFilter)
			throws GeomajasException {
		VectorLayer layer = getVectorLayer(layerId);
		GetBoundsContainer container = new GetBoundsContainer();
		PipelineContext context = pipelineService.createContext();
		context.put(PipelineCode.LAYER_ID_KEY, layerId);
		context.put(PipelineCode.LAYER_KEY, layer);
		context.put(PipelineCode.CRS_TRANSFORM_KEY, geoService.findMathTransform(layer.getCrs(), crs));
		context.put(PipelineCode.FILTER_KEY, queryFilter);
		pipelineService.execute(PipelineCode.PIPELINE_GET_BOUNDS, layerId, context, container);
		return container.getEnvelope();
	}

	public List<Object> getObjects(String layerId, String attributeName, Filter queryFilter) throws GeomajasException {
		VectorLayer layer = getVectorLayer(layerId);
		PipelineContext context = pipelineService.createContext();
		context.put(PipelineCode.LAYER_ID_KEY, layerId);
		context.put(PipelineCode.LAYER_KEY, layer);
		context.put(PipelineCode.FILTER_KEY, queryFilter);
		context.put(PipelineCode.ATTRIBUTE_NAME_KEY, attributeName);
		List<Object> response = new ArrayList<Object>();
		pipelineService.execute(PipelineCode.PIPELINE_GET_OBJECTS, layerId, context, response);
		return response;
	}

	public InternalTile getTile(TileMetadata tileMetadata) throws GeomajasException {
		String layerId = tileMetadata.getLayerId();
		VectorLayer layer = getVectorLayer(layerId);
		PipelineContext context = pipelineService.createContext();
		context.put(PipelineCode.LAYER_ID_KEY, layerId);
		context.put(PipelineCode.LAYER_KEY, layer);
		context.put(PipelineCode.TILE_METADATA_KEY, tileMetadata);
		CoordinateReferenceSystem crs = configurationService.getCrs(tileMetadata.getCrs());
		context.put(PipelineCode.CRS_KEY, crs);
		context.put(PipelineCode.CRS_TRANSFORM_KEY, geoService.findMathTransform(crs, layer.getCrs()));
		context.put(PipelineCode.FEATURE_INCLUDES_KEY, tileMetadata.getFeatureIncludes());
		InternalTile response = new InternalTileImpl(tileMetadata.getCode(), layer, tileMetadata.getScale());
		pipelineService.execute(PipelineCode.PIPELINE_GET_VECTOR_TILE, layerId, context, response);
		return response;
	}
}
