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

package org.geomajas.puregwt.client.map.render;

import java.util.List;

import org.geomajas.geometry.Coordinate;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;

/**
 * Extension of the GWT animation definition for navigation around the map. It has support for both zooming and panning.
 * 
 * @author Pieter De Graef
 */
public class MapNavigationAnimation extends Animation {

	private AbstractNavigationFunction function;

	private int nrAnimatedLayers = 1;

	private boolean running;

	protected List<MapScalesRenderer> mapScalesRenderers;

	private double currentScale;

	private double currentX;

	private double currentY;

	// ------------------------------------------------------------------------
	// Constructor:
	// ------------------------------------------------------------------------

	/** Initialize the animation. */
	public MapNavigationAnimation(AbstractNavigationFunction function) {
		super();
		this.function = function;
	}

	// ------------------------------------------------------------------------
	// public methods:
	// ------------------------------------------------------------------------

	/**
	 * Start the animation right now, using the given parameters. Only the botton X layers will be animated, where X
	 * equals the value set through <code>setNrAnimatedLayers</code>.
	 * 
	 * @param layerPresenters
	 *            A collection of {@link MapScalesRenderer}s that should be animated. This class will call the
	 *            navigation methods onto these presenters directly.
	 * @param sourceScale
	 *            The source zooming factor. This is a delta value. Value=1 will keep the layer presenters at their
	 *            current scale level.
	 * @param targetScale
	 *            The target zooming factor. This is a delta value. Value=1 will keep the layer presenters at their
	 *            current scale level.
	 * @param sourcePosition
	 *            The source translation factor.
	 * @param targetPosition
	 *            The target translation factor.
	 * @param millis
	 *            The time in milliseconds this animation should run.
	 */
	public void start(List<MapScalesRenderer> layerPresenters, double sourceScale, double targetScale,
			Coordinate sourcePosition, Coordinate targetPosition, int millis) {
		this.mapScalesRenderers = layerPresenters;

		function.setBeginLocation(sourcePosition.getX(), sourcePosition.getY(), sourceScale);
		function.setEndLocation(targetPosition.getX(), targetPosition.getY(), targetScale);

		//GWT.log("Animation.start (running=true)");
		running = true;
		run(millis);
	}

	public void extend(double targetScale, Coordinate targetPosition, int millis) {
		if (running) {
			//cancel();
			start(mapScalesRenderers, currentScale, targetScale, new Coordinate(currentX, currentY), targetPosition,
					millis);
		}
	}

	/**
	 * Is this animation currently running?
	 * 
	 * @return yes or no, true or false...
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Get the navigation function that's currently being used.
	 * 
	 * @return The navigation function that's currently being used.
	 */
	public AbstractNavigationFunction getFunction() {
		return function;
	}

	/**
	 * Set the navigation function be be used.
	 * 
	 * @param function
	 *            Apply a new navigation function.
	 */
	public void setFunction(AbstractNavigationFunction function) {
		this.function = function;
	}

	/**
	 * Get the number of layers that should be animated when navigating.
	 * 
	 * @return The number of layers that should be animated when navigating.
	 */
	public int getNrAnimatedLayers() {
		return nrAnimatedLayers;
	}

	/**
	 * Set the total number of layers that should be animated when navigating.
	 * 
	 * @param nrAnimatedLayers
	 *            The number of layers that should be animated when navigating.
	 */
	public void setNrAnimatedLayers(int nrAnimatedLayers) {
		this.nrAnimatedLayers = nrAnimatedLayers;
	}

	/**
	 * Get the current list of map scale renderers.
	 * 
	 * @return The current list of map scale renderers.
	 */
	public List<MapScalesRenderer> getMapScaleRenderers() {
		return mapScalesRenderers;
	}

	// ------------------------------------------------------------------------
	// Overridden methods:
	// ------------------------------------------------------------------------
	
	/**
	 * Method that keeps tabs on the animation progress, and automatically transforms all {@link MapScalesRenderer}s
	 * accordingly.
	 * 
	 * @param progress
	 *            The progress within the animation. Is a value between 0 and 1, where 1 means that the animation come
	 *            to it's end.
	 */
	protected void onUpdate(double progress) {
		running = true;
		double[] location = function.getLocation(progress);
		currentX = location[0];
		currentY = location[1];
		currentScale = location[2];
		if (Double.isNaN(currentScale) || Double.isInfinite(currentScale)) {
			currentScale = 1;
		}

		for (int i = 0; i < mapScalesRenderers.size(); i++) {
			MapScalesRenderer presenter = mapScalesRenderers.get(i);
			TiledScaleRenderer scalePresenter = presenter.getVisibleScale();
			if (scalePresenter != null) {
				if (i < nrAnimatedLayers) {
					scalePresenter.getHtmlContainer().zoomToLocation(currentScale, 0, 0);
					scalePresenter.getHtmlContainer().setLeft((int) Math.round(currentX));
					scalePresenter.getHtmlContainer().setTop((int) Math.round(currentY));
				} else {
					GWT.log("onUpdate. Make invisible...");
					scalePresenter.getHtmlContainer().setVisible(false);
				}
			}
		}
	}

	/**
	 * Called when the current animation has been canceled. Also cancels the navigation process on all
	 * {@link MapScalesRenderer}s.
	 */
	protected void onCancel() {
		//GWT.log("Animation.onCancel (running=false)");
		running = false;
		for (MapScalesRenderer presenter : mapScalesRenderers) {
			presenter.cancel();
		}
	}

	/**
	 * Called when the animation has been completed successfully. Finishes the navigation process on all
	 * {@link MapScalesRenderer}s.
	 */
	protected void onComplete() {
		onUpdate(1); // Needed when millis = 0 (no animation). This fakes a zoom.
		running = false;
	}
}