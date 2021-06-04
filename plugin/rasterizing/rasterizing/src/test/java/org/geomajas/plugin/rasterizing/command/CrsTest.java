// Copyright (C) 2010-2021 DOV, http://dov.vlaanderen.be/
// All rights reserved

package org.geomajas.plugin.rasterizing.command;

import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Patrick De Baets
 *
 */
public class CrsTest {

    String WKT = "PROJCS[\"Google Mercator\",\r\n" + "           GEOGCS[\"WGS 84\",\r\n"
            + "           DATUM[\"World Geodetic System 1984\",\r\n"
            + "           SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]],\r\n"
            + "           AUTHORITY[\"EPSG\",\"6326\"]],\r\n"
            + "           PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],\r\n"
            + "           UNIT[\"degree\", 0.017453292519943295],\r\n"
            + "           AXIS[\"Geodetic latitude\", NORTH],\r\n"
            + "           AXIS[\"Geodetic longitude\", EAST],\r\n" + "           AUTHORITY[\"EPSG\",\"4326\"]],\r\n"
            + "           PROJECTION[\"Mercator (1SP)\", AUTHORITY[\"EPSG\",\"9804\"]],\r\n"
            + "           PARAMETER[\"semi_major\", 6378137.0],\r\n"
            + "           PARAMETER[\"semi_minor\", 6378137.0],\r\n"
            + "           PARAMETER[\"latitude_of_origin\", 0.0],\r\n"
            + "           PARAMETER[\"central_meridian\", 0.0],\r\n"
            + "           PARAMETER[\"scale_factor\", 1.0],\r\n" + "           PARAMETER[\"false_easting\", 0.0],\r\n"
            + "           PARAMETER[\"false_northing\", 0.0],\r\n" + "           UNIT[\"m\", 1.0],\r\n"
            + "           AXIS[\"Easting\", EAST],\r\n" + "           AXIS[\"Northing\", NORTH],\r\n"
            + "           AUTHORITY[\"EPSG\",\"900913\"]]";

    @Test
    public void wktCRS() throws Exception {
        CRS.parseWKT(WKT);
    }

    @Test
    public void parse() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:31370");
        crs.getIdentifiers();
        crs.getCoordinateSystem().getName().getCode();

        CRS.lookupEpsgCode(crs, true);
        CRS.lookupEpsgCode(crs, false);
    }

}
