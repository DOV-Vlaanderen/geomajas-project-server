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

import com.vividsolutions.jts.algorithm.InteriorPointArea;
import com.vividsolutions.jts.algorithm.InteriorPointLine;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.geomajas.geometry.Bbox;
import org.geomajas.geometry.Crs;
import org.geomajas.geometry.CrsTransform;
import org.geomajas.global.CrsInfo;
import org.geomajas.global.CrsTransformInfo;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.internal.service.crs.CrsFactory;
import org.geomajas.layer.LayerException;
import org.geomajas.layer.feature.InternalFeature;
import org.geomajas.service.GeoService;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collection of utility functions concerning geometries.
 *
 * @author Joachim Van der Auwera
 * @author Jan De Moerloose
 * @author Pieter De Graef
 */
@Component()
public final class GeoServiceImpl implements GeoService {

	private final Logger log = LoggerFactory.getLogger(GeoServiceImpl.class);

	@Autowired(required = false)
	private Map<String, CrsInfo> crsDefinitions;

	@Autowired(required = false)
	private Map<String, CrsTransformInfo> crsTransformDefinitions;

	private Map<String, Crs> crsCache = new ConcurrentHashMap<String, Crs>();

	private Map<String, CrsTransform> transformCache = new ConcurrentHashMap<String, CrsTransform>();

	@PostConstruct
	private void postConstruct() throws GeomajasException {
		if (null != crsDefinitions) {
			for (CrsInfo crsInfo : crsDefinitions.values()) {
				try {
					CoordinateReferenceSystem crs = CRS.parseWKT(crsInfo.getCrsWkt());
					String code = crsInfo.getKey();
					crsCache.put(code, CrsFactory.getCrs(code, crs));
				} catch (FactoryException e) {
					throw new GeomajasException(e, ExceptionCode.CRS_DECODE_FAILURE_FOR_MAP, crsInfo.getKey());
				}
			}
		}
		if (null != crsTransformDefinitions) {
			for (CrsTransformInfo crsTransformInfo : crsTransformDefinitions.values()) {
				String key = getTransformKey(crsTransformInfo);
				transformCache.put(key, getCrsTransform(key, crsTransformInfo));
			}
		}
	}

	private String getTransformKey(CrsTransformInfo crsTransformInfo) {
		return crsTransformInfo.getSource() + "->" + crsTransformInfo.getTarget();
	}

	private String getTransformKey(Crs source, Crs target) {
		return source.getId() + "->" + target.getId();
	}

	private CrsTransform getCrsTransform(String key, CrsTransformInfo crsTransformInfo) throws GeomajasException {
		Crs source = getCrs2(crsTransformInfo.getSource());
		Crs target = getCrs2(crsTransformInfo.getTarget());

		MathTransform mathTransform = getBaseMathTransform(source, target);

		return new CrsTransformImpl(key, source, target, mathTransform, crsTransformInfo.getTransformableArea());
	}

	public CoordinateReferenceSystem getCrs(String crs) throws LayerException {
		return getCrs2(crs);
	}

	/**
	 * @inheritDoc
	 */
	public Crs getCrs2(String crs) throws LayerException {
		try {
			Crs res = crsCache.get(crs);
			if (null == res) {
				res = CrsFactory.getCrs(crs, CRS.decode(crs));
				crsCache.put(crs, res);
			}
			return res;
		} catch (NoSuchAuthorityCodeException e) {
			throw new LayerException(e, ExceptionCode.CRS_DECODE_FAILURE_FOR_MAP, crs);
		} catch (FactoryException e) {
			throw new LayerException(e, ExceptionCode.CRS_DECODE_FAILURE_FOR_MAP, crs);
		}
	}

	/**
	 * Isn't there a method for this in GeoTools?
	 *
	 * @param crs
	 *            CRS string in the form of 'EPSG:<srid>'.
	 * @return SRID as integer.
	 */
	public int getSridFromCrs(String crs) {
		int crsInt;
		if (crs.indexOf(':') != -1) {
			crsInt = Integer.parseInt(crs.substring(crs.indexOf(':') + 1));
		} else {
			try {
				crsInt = Integer.parseInt(crs);
			} catch (Exception e) {
				crsInt = 0;
			}
		}
		return crsInt;
	}

	/**
	 * @inheritDoc
	 */
	public String getCodeFromCrs(Crs crs) {
		return crs.getId();
	}

	/**
	 * @inheritDoc
	 */
	public String getCodeFromCrs(CoordinateReferenceSystem crs) {
		return "EPSG:" + getSridFromCrs(crs);
	}

	/**
	 * Unreliable but works if srids are same as EPSG numbers.
	 *
	 * @param crs reference system of EPSG type.
	 * @return SRID as integer.
	 */
	public int getSridFromCrs(CoordinateReferenceSystem crs) {
		return getSridFromCrs(crs.getIdentifiers().iterator().next().toString());
	}

	private MathTransform getBaseMathTransform(Crs sourceCrs, Crs targetCrs) throws GeomajasException {
		try {
			MathTransform transform;
			try {
				transform = CRS.findMathTransform(sourceCrs, targetCrs);
			} catch (Exception e) {
				transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
			}
			return transform;
		} catch (FactoryException fe) {
			throw new GeomajasException(fe, ExceptionCode.CRS_TRANSFORMATION_NOT_POSSIBLE,
					sourceCrs.getId(), targetCrs.getId());
		}
	}

	/**
	 * @inheritDoc
	 */
	public MathTransform findMathTransform(CoordinateReferenceSystem sourceCrs,
			CoordinateReferenceSystem targetCrs) throws GeomajasException {
		return getCrsTransform(getCrs2(getCodeFromCrs(sourceCrs)), getCrs2(getCodeFromCrs(targetCrs)));
	}

	/**
	 * @inheritDoc
	 */
	public CrsTransform getCrsTransform(String sourceCrs, String targetCrs)
			throws GeomajasException {
		return getCrsTransform(getCrs2(sourceCrs), getCrs2(targetCrs));
	}

	/**
	 * @inheritDoc
	 */
	public CrsTransform getCrsTransform(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs)
			throws GeomajasException {
		Crs source, target;
		if (sourceCrs instanceof Crs) {
			source = (Crs) sourceCrs;
		} else {
			source = getCrs2(getCodeFromCrs(sourceCrs));
		}
		if (targetCrs instanceof Crs) {
			target = (Crs) targetCrs;
		} else {
			target = getCrs2(getCodeFromCrs(targetCrs));
		}
		return getCrsTransform(source, target);
	}

	/**
	 * @inheritDoc
	 */
	public CrsTransform getCrsTransform(Crs sourceCrs, Crs targetCrs)
			throws GeomajasException {
		String key = getTransformKey(sourceCrs, targetCrs);
		CrsTransform transform = transformCache.get(key);
		if (null == transform) {
			MathTransform mathTransform = getBaseMathTransform(sourceCrs, targetCrs);

			// as there was no transformable area configured, try to build it instead
			Envelope transformableArea = null;
			try {
				org.opengis.geometry.Envelope ogEnvelope = CRS.getEnvelope(targetCrs);
				if (null != ogEnvelope) {
					Envelope envelope = new Envelope(ogEnvelope.getLowerCorner().getCoordinate()[0],
							ogEnvelope.getUpperCorner().getCoordinate()[0],
							ogEnvelope.getLowerCorner().getCoordinate()[1],
							ogEnvelope.getUpperCorner().getCoordinate()[1]);
					log.info("CRS " + targetCrs.getId() + " envelope " + envelope);
					transformableArea = JTS.transform(envelope, getBaseMathTransform(targetCrs, sourceCrs));
					log.info("transformable area for " + key + " is " + transformableArea);
				}
			} catch (MismatchedDimensionException mde) {
				log.warn(
						"Cannot build transformableArea for CRS transformation between " + sourceCrs.getId() + " and " +
								targetCrs.getId() + ", " + mde.getMessage());
			} catch (TransformException te) {
				log.warn(
						"Cannot build transformableArea for CRS transformation between " + sourceCrs.getId() + " and " +
								targetCrs.getId() + ", " + te.getMessage());
			}

			transform = new CrsTransformImpl(key, sourceCrs, targetCrs, mathTransform, transformableArea);
			transformCache.put(key, transform);
		}
		return transform;
	}

	/**
	 * @inheritDoc
	 */
	public Geometry transform(Geometry source, CrsTransform crsTransform) {
		try {
			Geometry transformableArea = crsTransform.getTransformableGeometry();
			if (null != transformableArea) {
				source = source.intersection(transformableArea);
			}
			return JTS.transform(source, crsTransform);
		} catch (TransformException te) {
			log.warn("Problem during transformation " + crsTransform.getId() + "of " + source +
					", maybe you need to configure the transformable area using a CrsTransformInfo object for this " +
					"transformation. Object replaced by empty Geometry.", te);
			return JTS.toGeometry(new Envelope());
		}
	}

	/**
	 * @inheritDoc
	 */
	public Geometry transform(Geometry geometry, Crs sourceCrs, Crs targetCrs)
			throws GeomajasException {
		if (sourceCrs == targetCrs) {
			// only works when the caching of the CRSs works
			return geometry;
		}

		CrsTransform crsTransform = getCrsTransform(sourceCrs, targetCrs);
		return transform(geometry, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Geometry transform(Geometry geometry, String sourceCrs, String targetCrs)
			throws GeomajasException {
		if (sourceCrs.equals(targetCrs)) {
			return geometry;
		}

		CrsTransform crsTransform = getCrsTransform(sourceCrs, targetCrs);
		return transform(geometry, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Geometry transform(Geometry geometry, CoordinateReferenceSystem sourceCrs,
			CoordinateReferenceSystem targetCrs)
			throws GeomajasException {
		if (sourceCrs == targetCrs) {
			// only works when the caching of the CRSs works
			return geometry;
		}
		Crs source, target;
		if (sourceCrs instanceof Crs) {
			source = (Crs) sourceCrs;
		} else {
			source = getCrs2(getCodeFromCrs(sourceCrs));
		}
		if (targetCrs instanceof Crs) {
			target = (Crs) targetCrs;
		} else {
			target = getCrs2(getCodeFromCrs(targetCrs));
		}

		CrsTransform crsTransform = getCrsTransform(source, target);
		return transform(geometry, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Bbox transform(Bbox source, CrsTransform crsTransform) {
		try {
			Envelope envelope = new Envelope(source.getX(), source.getMaxX(), source.getY(), source.getMaxY());
			Envelope transformableArea = crsTransform.getTransformableEnvelope();
			if (null != transformableArea) {
				envelope = envelope.intersection(transformableArea);
			}
			if (envelope.isNull()) {
				return new Bbox();
			} else {
				envelope = JTS.transform(envelope, crsTransform);
				return new Bbox(envelope.getMinX(), envelope.getMinY(), envelope.getWidth(), envelope.getHeight());
			}
		} catch (TransformException te) {
			log.warn("Problem during transformation " + crsTransform.getId() + "of " + source +
					", maybe you need to configure the transformable area using a CrsTransformInfo object for this " +
					"transformation. Object replaced by empty Bbox.", te);
			return new Bbox();
		}
	}

	/**
	 * @inheritDoc
	 */
	public Bbox transform(Bbox bbox, Crs sourceCrs, Crs targetCrs)
			throws GeomajasException {
		if (sourceCrs == targetCrs) {
			// only works when the caching of the CRSs works
			return bbox;
		}

		CrsTransform crsTransform = getCrsTransform(sourceCrs, targetCrs);
		return transform(bbox, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Bbox transform(Bbox bbox, String sourceCrs, String targetCrs)
			throws GeomajasException {
		if (sourceCrs.equals(targetCrs)) {
			// only works when the caching of the CRSs works
			return bbox;
		}

		CrsTransform crsTransform = getCrsTransform(sourceCrs, targetCrs);
		return transform(bbox, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Envelope transform(Envelope source, CrsTransform crsTransform) {
		try {
			Envelope transformableArea = crsTransform.getTransformableEnvelope();
			if (null != transformableArea) {
				source = source.intersection(transformableArea);
			}
			return source.isNull() ? source : JTS.transform(source, crsTransform);
		} catch (TransformException te) {
			log.warn("Problem during transformation " + crsTransform.getId() + "of " + source +
					", maybe you need to configure the transformable area using a CrsTransformInfo object for this " +
					"transformation. Object replaced by empty Envelope.", te);
			return new Envelope();
		}
	}

	/**
	 * @inheritDoc
	 */
	public Envelope transform(Envelope source, Crs sourceCrs, Crs targetCrs)
			throws GeomajasException {
		if (sourceCrs == targetCrs) {
			// only works when the caching of the CRSs works
			return source;
		}

		CrsTransform crsTransform = getCrsTransform(sourceCrs, targetCrs);
		return transform(source, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Envelope transform(Envelope source, String sourceCrs, String targetCrs)
			throws GeomajasException {
		if (sourceCrs.equals(targetCrs)) {
			// only works when the caching of the CRSs works
			return source;
		}

		CrsTransform crsTransform = getCrsTransform(sourceCrs, targetCrs);
		return transform(source, crsTransform);
	}

	/**
	 * @inheritDoc
	 */
	public Coordinate calcDefaultLabelPosition(InternalFeature feature) {
		Geometry geometry = feature.getGeometry();
		Coordinate labelPoint = null;
		if (geometry != null && !geometry.isEmpty() && geometry.isValid()) {
			if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
				try {
					InteriorPointArea ipa = new InteriorPointArea(geometry);
					labelPoint = ipa.getInteriorPoint();
				} catch (Throwable t) {
					// BUG in JTS for some valid geometries ? fall back to centroid
				}
			} else if (geometry instanceof LineString || geometry instanceof MultiLineString) {
				InteriorPointLine ipa = new InteriorPointLine(geometry);
				labelPoint = ipa.getInteriorPoint();
			} else {
				labelPoint = geometry.getCentroid().getCoordinate();
			}
		}
		if (null == labelPoint && null != geometry) {
			Point centroid = geometry.getCentroid();
			if (null != centroid) {
				labelPoint = centroid.getCoordinate();
			}
		}
		if (null != labelPoint && (Double.isNaN(labelPoint.x) || Double.isNaN(labelPoint.y))) {
			labelPoint = new Coordinate(geometry.getCoordinate());
		}
		return null == labelPoint ? null : new Coordinate(labelPoint);
	}

	/**
	 * @inheritDoc
	 */
	public Geometry createCircle(final Point point, final double radius, final int nrPoints) {
		double x = point.getX();
		double y = point.getY();
		Coordinate[] coords = new Coordinate[nrPoints + 1];
		for (int i = 0; i < nrPoints; i++) {
			double angle = ((double) i / (double) nrPoints) * Math.PI * 2.0;
			double dx = Math.cos(angle) * radius;
			double dy = Math.sin(angle) * radius;
			coords[i] = new Coordinate(x + dx, y + dy);
		}
		coords[nrPoints] = coords[0];

		LinearRing ring = point.getFactory().createLinearRing(coords);
		return point.getFactory().createPolygon(ring, null);
	}
}