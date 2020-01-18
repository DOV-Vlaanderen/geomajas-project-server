// Copyright (C) 2010-2017 DOV, http://dov.vlaanderen.be/
// All rights reserved

package org.geomajas.internal.service;

import org.geomajas.geometry.Bbox;
import org.geomajas.geometry.service.GeometryService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Implementation of the geomajas GeometryConverterService with support for the
 * Locationtech JTS topology suite.
 *
 * @author Ruben Vervaeke
 */
public final class GeometryConverterService {

    private GeometryConverterService() {
    }

    public static Geometry toJts(org.geomajas.geometry.Geometry geometry) throws JtsConversionException {
        if (geometry == null) {
            throw new JtsConversionException("Cannot convert null argument");
        } else {
            int srid = geometry.getSrid();
            int precision = geometry.getPrecision();
            PrecisionModel model;
            if (precision == -1) {
                model = new PrecisionModel(PrecisionModel.FLOATING);
            } else {
                model = new PrecisionModel(Math.pow(10.0D, (double) precision));
            }

            GeometryFactory factory = new GeometryFactory(model, srid);
            String geometryType = geometry.getGeometryType();
            Object jts;
            if (GeometryService.isEmpty(geometry)) {
                jts = createEmpty(factory, geometryType);
            } else if ("Point".equals(geometryType)) {
                jts = factory.createPoint(convertCoordinates(geometry)[0]);
            } else if ("LinearRing".equals(geometryType)) {
                jts = factory.createLinearRing(convertCoordinates(geometry));
            } else if ("LineString".equals(geometryType)) {
                jts = factory.createLineString(convertCoordinates(geometry));
            } else if ("Polygon".equals(geometryType)) {
                org.geomajas.geometry.Geometry[] geometries = geometry.getGeometries();
                if (null != geometries && geometries.length > 0) {
                    LinearRing exteriorRing = (LinearRing) toJts(geometries[0]);
                    LinearRing[] interiorRings = new LinearRing[geometries.length - 1];

                    for (int i = 0; i < interiorRings.length; ++i) {
                        interiorRings[i] = (LinearRing) toJts(geometries[i + 1]);
                    }

                    jts = factory.createPolygon(exteriorRing, interiorRings);
                } else {
                    jts = factory.createPolygon((LinearRing) null, (LinearRing[]) null);
                }
            } else if ("MultiPoint".equals(geometryType)) {
                Point[] points = new Point[geometry.getGeometries().length];
                jts = factory.createMultiPoint((Point[]) ((Point[]) convertGeometries(geometry, points)));
            } else if ("MultiLineString".equals(geometryType)) {
                LineString[] lineStrings = new LineString[geometry.getGeometries().length];
                jts = factory.createMultiLineString((LineString[]) ((LineString[]) convertGeometries(geometry,
                                                                                                     lineStrings)));
            } else {
                if (!"MultiPolygon".equals(geometryType)) {
                    throw new JtsConversionException("Cannot convert geometry: Unsupported type.");
                }

                Polygon[] polygons = new Polygon[geometry.getGeometries().length];
                jts = factory.createMultiPolygon((Polygon[]) ((Polygon[]) convertGeometries(geometry, polygons)));
            }

            return (Geometry) jts;
        }
    }

    public static Envelope toJts(Bbox bbox) throws JtsConversionException {
        if (bbox == null) {
            throw new JtsConversionException("Cannot convert null argument");
        } else {
            return new Envelope(bbox.getX(), bbox.getMaxX(), bbox.getY(), bbox.getMaxY());
        }
    }

    public static Coordinate toJts(org.geomajas.geometry.Coordinate coordinate) throws JtsConversionException {
        if (coordinate == null) {
            throw new JtsConversionException("Cannot convert null argument");
        } else {
            return new Coordinate(coordinate.getX(), coordinate.getY());
        }
    }

    public static org.geomajas.geometry.Geometry fromJts(Geometry geometry) throws JtsConversionException {
        if (geometry == null) {
            throw new JtsConversionException("Cannot convert null argument");
        } else {
            int srid = geometry.getSRID();
            int precision = -1;
            PrecisionModel precisionmodel = geometry.getPrecisionModel();
            if (!precisionmodel.isFloating()) {
                precision = (int) Math.log10(precisionmodel.getScale());
            }

            String geometryType = getGeometryType(geometry);
            org.geomajas.geometry.Geometry dto = new org.geomajas.geometry.Geometry(geometryType, srid, precision);
            if (!geometry.isEmpty()) {
                if (geometry instanceof Point) {
                    dto.setCoordinates(convertCoordinates(geometry));
                } else if (geometry instanceof LinearRing) {
                    dto.setCoordinates(convertCoordinates(geometry));
                } else if (geometry instanceof LineString) {
                    dto.setCoordinates(convertCoordinates(geometry));
                } else if (geometry instanceof Polygon) {
                    Polygon polygon = (Polygon) geometry;
                    org.geomajas.geometry.Geometry[] geometries = new org.geomajas.geometry.Geometry[
                            polygon.getNumInteriorRing() + 1];

                    for (int i = 0; i < geometries.length; ++i) {
                        if (i == 0) {
                            geometries[i] = fromJts((Geometry) polygon.getExteriorRing());
                        } else {
                            geometries[i] = fromJts((Geometry) polygon.getInteriorRingN(i - 1));
                        }
                    }

                    dto.setGeometries(geometries);
                } else if (geometry instanceof MultiPoint) {
                    dto.setGeometries(convertGeometries(geometry));
                } else if (geometry instanceof MultiLineString) {
                    dto.setGeometries(convertGeometries(geometry));
                } else {
                    if (!(geometry instanceof MultiPolygon)) {
                        throw new JtsConversionException("Cannot convert geometry: Unsupported type.");
                    }

                    dto.setGeometries(convertGeometries(geometry));
                }
            }

            return dto;
        }
    }

    public static Bbox fromJts(Envelope envelope) throws JtsConversionException {
        if (envelope == null) {
            throw new JtsConversionException("Cannot convert null argument");
        } else {
            return new Bbox(envelope.getMinX(), envelope.getMinY(), envelope.getWidth(), envelope.getHeight());
        }
    }

    public static org.geomajas.geometry.Coordinate fromJts(Coordinate coordinate) throws JtsConversionException {
        if (coordinate == null) {
            throw new JtsConversionException("Cannot convert null argument");
        } else {
            return new org.geomajas.geometry.Coordinate(coordinate.x, coordinate.y);
        }
    }

    private static Geometry createEmpty(GeometryFactory factory, String geometryType) throws JtsConversionException {
        if ("Point".equals(geometryType)) {
            return new Point((CoordinateSequence) null, factory);
        } else if ("LinearRing".equals(geometryType)) {
            return factory.createLinearRing((Coordinate[]) null);
        } else if ("LineString".equals(geometryType)) {
            return factory.createLineString((Coordinate[]) null);
        } else if ("Polygon".equals(geometryType)) {
            return factory.createPolygon((LinearRing) null, (LinearRing[]) null);
        } else if ("MultiPoint".equals(geometryType)) {
            return factory.createMultiPoint((Point[]) null);
        } else if ("MultiLineString".equals(geometryType)) {
            return factory.createMultiLineString((LineString[]) null);
        } else if ("MultiPolygon".equals(geometryType)) {
            return factory.createMultiPolygon((Polygon[]) null);
        } else {
            throw new JtsConversionException("Error while converting to Geomajas: Unknown geometry type.");
        }
    }

    private static String getGeometryType(Geometry geometry) throws JtsConversionException {
        if (geometry instanceof Point) {
            return "Point";
        } else if (geometry instanceof LinearRing) {
            return "LinearRing";
        } else if (geometry instanceof LineString) {
            return "LineString";
        } else if (geometry instanceof Polygon) {
            return "Polygon";
        } else if (geometry instanceof MultiPoint) {
            return "MultiPoint";
        } else if (geometry instanceof MultiLineString) {
            return "MultiLineString";
        } else if (geometry instanceof GeometryCollection) {
            return "MultiPolygon";
        } else {
            throw new JtsConversionException("Error while converting to Geomajas: Unknown geometry type.");
        }
    }

    private static org.geomajas.geometry.Coordinate[] convertCoordinates(Geometry geometry) {
        org.geomajas.geometry.Coordinate[] coordinates =
                new org.geomajas.geometry.Coordinate[geometry.getCoordinates().length];

        for (int i = 0; i < coordinates.length; ++i) {
            coordinates[i] = new org.geomajas.geometry.Coordinate(geometry.getCoordinates()[i].x,
                                                                  geometry.getCoordinates()[i].y);
        }

        return coordinates;
    }

    private static org.geomajas.geometry.Geometry[] convertGeometries(Geometry geometry)
            throws JtsConversionException {
        org.geomajas.geometry.Geometry[] geometries = new org.geomajas.geometry.Geometry[geometry.getNumGeometries()];

        for (int i = 0; i < geometries.length; ++i) {
            geometries[i] = fromJts(geometry.getGeometryN(i));
        }

        return geometries;
    }

    private static Coordinate[] convertCoordinates(org.geomajas.geometry.Geometry geometry) {
        Coordinate[] coordinates = new Coordinate[geometry.getCoordinates().length];

        for (int i = 0; i < coordinates.length; ++i) {
            coordinates[i] = new Coordinate(geometry.getCoordinates()[i].getX(), geometry.getCoordinates()[i].getY());
        }

        return coordinates;
    }

    private static Geometry[] convertGeometries(org.geomajas.geometry.Geometry geometry, Geometry[] geometries)
            throws JtsConversionException {
        for (int i = 0; i < geometries.length; ++i) {
            geometries[i] = toJts(geometry.getGeometries()[i]);
        }

        return geometries;
    }
}
