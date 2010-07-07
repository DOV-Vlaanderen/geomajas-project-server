/*
 * This file is part of Geomajas, a component framework for building
 * rich Internet applications (RIA) with sophisticated capabilities for the
 * display, analysis and management of geographic information.
 * It is a building block that allows developers to add maps
 * and other geographic data capabilities to their web applications.
 *
 * Copyright 2008-2009 Geosparc, http://www.geosparc.com, Belgium
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
package org.geomajas.gwt.client.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.geomajas.geometry.Coordinate;
import org.geomajas.gwt.client.map.event.MapViewChangedEvent;
import org.geomajas.gwt.client.map.event.MapViewChangedHandler;
import org.geomajas.gwt.client.spatial.Bbox;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Tests map view behaviour (zoom options, resolutions)
 * 
 * @author Jan De Moerloose
 */
public class MapViewTest {

	MapView mapView;

	@Before
	public void setUp() {
		mapView = new MapView();
		mapView.setSize(200, 100);
		mapView.setMaxBounds(new Bbox(0, 0, 1000, 400));
		mapView.setMaximumScale(2);
		mapView.setCurrentScale(1.0, MapView.ZoomOption.LEVEL_CLOSEST);
		mapView.setCenterPosition(new Coordinate(500, 200));
	}

	@Test
	public void testPanning() {
		CaptureHandler handler = new CaptureHandler();
		mapView.addMapViewChangedHandler(handler);
		// pan to other allowed position
		mapView.setCenterPosition(new Coordinate(900, 300));
		handler.expect(new Bbox(800, 250, 200, 100), 1.0, true);
		// pan outside max bounds
		mapView.setCenterPosition(new Coordinate(1000, 400));
		// should pan as far as possible
		handler.expect(new Bbox(800, 300, 200, 100), 1.0, true);
		// translate outside max bounds
		mapView.translate(100, 100);
		// no movement
		handler.expect(new Bbox(800, 300, 200, 100), 1.0, true);
		handler.validate();
	}

	@Test
	public void testZoomingNoResolutions() {
		CaptureHandler handler = new CaptureHandler();
		mapView.addMapViewChangedHandler(handler);
		// pan to initial position
		mapView.applyBounds(new Bbox(400, 150, 200, 100), MapView.ZoomOption.LEVEL_CLOSEST);
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, false);
		// zoom out
		mapView.setCurrentScale(0.5, MapView.ZoomOption.LEVEL_CLOSEST);
		handler.expect(new Bbox(300, 100, 400, 200), 0.5, false);
		// zoom out beyond maximum bounds
		mapView.setCurrentScale(0.2, MapView.ZoomOption.LEVEL_CLOSEST);
		// should zoom out as far as possible
		handler.expect(new Bbox(0, -50, 1000, 500), 0.2, false);
		// zoom in beyond maximum scale
		mapView.setCurrentScale(3, MapView.ZoomOption.LEVEL_CLOSEST);
		// should zoom in as far as possible
		handler.expect(new Bbox(450, 175, 100, 50), 2, false);
		handler.validate();
	}

	@Test
	public void testZoomingWithResolutions() {
		List<Double> resolutions = new ArrayList<Double>();
		resolutions.add(1 / 0.01);
		resolutions.add(1 / 0.1);
		resolutions.add(1 / 0.4);
		resolutions.add(1 / 1.0);
		resolutions.add(1 / 2.0);
		mapView.setResolutions(resolutions);
		CaptureHandler handler = new CaptureHandler();
		mapView.addMapViewChangedHandler(handler);
		// apply initial bounds
		mapView.applyBounds(new Bbox(300, 100, 400, 200), MapView.ZoomOption.LEVEL_CLOSEST);
		// should snap to closest (scale 0.5 -> 0.4)
		handler.expect(new Bbox(250, 75, 500, 250), 0.4, false);
		// force next level
		mapView.scale(1.001, MapView.ZoomOption.LEVEL_CHANGE);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, false);
		handler.validate();
	}

	/**
	 * Tests the lower and upper boundaries of the resolution list
	 */
	@Test
	public void testGWT36() {
		List<Double> resolutions = new ArrayList<Double>();
		resolutions.add(1 / 0.01);
		resolutions.add(1 / 0.1);
		resolutions.add(1 / 0.4);
		resolutions.add(1 / 1.0);
		resolutions.add(1 / 2.0);
		mapView.setResolutions(resolutions);
		
		// no scale limitations
		mapView.setMaximumScale(Double.MAX_VALUE);
		mapView.setMaxBounds(new Bbox(-1E20, -1E20, 2E20, 2E20));
		
		CaptureHandler handler = new CaptureHandler();
		HandlerRegistration registration = mapView.addMapViewChangedHandler(handler);
		// force 1.0
		mapView.setCurrentScale(1.0, MapView.ZoomOption.LEVEL_CLOSEST);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, false);
		// force 2.0
		mapView.setCurrentScale(2.0, MapView.ZoomOption.LEVEL_CLOSEST);
		// zooms in to 2.0
		handler.expect(new Bbox(450, 175, 100, 50), 2.0, false);
		// force 0.01
		mapView.setCurrentScale(0.01, MapView.ZoomOption.LEVEL_CLOSEST);
		// zooms in to 0.01
		handler.expect(new Bbox(-9500, -4800, 20000, 10000), 0.01, false);
		// fitting
		// force 1.0
		mapView.setCurrentScale(1.0, MapView.ZoomOption.LEVEL_FIT);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, false);
		// force 2.0
		mapView.setCurrentScale(2.0, MapView.ZoomOption.LEVEL_FIT);
		// zooms in to 2.0
		handler.expect(new Bbox(450, 175, 100, 50), 2.0, false);
		// force 0.01
		mapView.setCurrentScale(0.01, MapView.ZoomOption.LEVEL_FIT);
		// zooms in to 0.01
		handler.expect(new Bbox(-9500, -4800, 20000, 10000), 0.01, false);
		handler.validate();
		registration.removeHandler();
		
		// test for 1 resolution
		resolutions = new ArrayList<Double>();
		resolutions.add(1 / 1.0);
		mapView.setResolutions(resolutions);
		handler = new CaptureHandler();
		mapView.addMapViewChangedHandler(handler);
		// force 2.0
		mapView.setCurrentScale(2.0, MapView.ZoomOption.LEVEL_FIT);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, false);
		// force 1.0
		mapView.setCurrentScale(2.0, MapView.ZoomOption.LEVEL_FIT);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, true);
		// force 0.5
		mapView.setCurrentScale(0.5, MapView.ZoomOption.LEVEL_FIT);
		// zooms in to 1.0 (which is not fitting, but there is no other option)
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, true);
		// force 2.0
		mapView.setCurrentScale(2.0, MapView.ZoomOption.LEVEL_CLOSEST);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, true);
		// force 1.0
		mapView.setCurrentScale(2.0, MapView.ZoomOption.LEVEL_CLOSEST);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, true);
		// force 0.5
		mapView.setCurrentScale(0.5, MapView.ZoomOption.LEVEL_CLOSEST);
		// zooms in to 1.0
		handler.expect(new Bbox(400, 150, 200, 100), 1.0, true);
		handler.validate();
		
	}

	private class CaptureHandler implements MapViewChangedHandler {

		private List<MapViewChangedEvent> actualEvents = new LinkedList<MapViewChangedEvent>();

		private List<MapViewChangedEvent> expectedEvents = new LinkedList<MapViewChangedEvent>();

		private double delta = 0.0000001;

		public void onMapViewChanged(MapViewChangedEvent event) {
			actualEvents.add(event);
		}

		public void expect(Bbox bounds, double scale, boolean panning) {
			MapViewChangedEvent event = new MapViewChangedEvent(bounds, scale, panning, false, false, null);
			expectedEvents.add(event);
		}

		public void validate() {
			for (MapViewChangedEvent expected : expectedEvents) {
				Assert.assertFalse("Expected event " + expected + " but got nothing", actualEvents.isEmpty());
				MapViewChangedEvent actual = actualEvents.remove(0);
				Assert.assertEquals(expected.getScale(), actual.getScale(), delta);
				Assert.assertTrue("Expected " + expected.getBounds() + " but was " + actual.getBounds()
						+ " for precision " + (delta), expected.getBounds().equals(actual.getBounds(), delta));
				Assert.assertEquals(expected.isSameScaleLevel(), actual.isSameScaleLevel());
			}
			Assert.assertTrue(actualEvents.size() + " unexpected events", actualEvents.isEmpty());
		}
	}
}