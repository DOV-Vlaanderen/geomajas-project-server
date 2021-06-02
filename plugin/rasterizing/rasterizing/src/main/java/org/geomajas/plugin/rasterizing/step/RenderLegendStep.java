/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2015 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */
package org.geomajas.plugin.rasterizing.step;

import org.geomajas.global.GeomajasException;
import org.geomajas.plugin.rasterizing.api.RasterizingContainer;
import org.geomajas.plugin.rasterizing.api.RasterizingPipelineCode;
import org.geomajas.plugin.rasterizing.api.RenderingService;
import org.geomajas.service.pipeline.PipelineContext;
import org.geotools.map.MapContent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Step which does the actual legend rendering.
 *
 * @author Jan De Moerloose
 */
public class RenderLegendStep extends AbstractRasterizingStep {

	@Autowired
	private RenderingService renderingService;

	@Override
    public void execute(PipelineContext context, RasterizingContainer response) throws GeomajasException {
		MapContent mapContext = context.get(RasterizingPipelineCode.MAP_CONTEXT_KEY, MapContent.class);
		context.put(RasterizingPipelineCode.RENDERED_IMAGE, renderingService.paintLegend(mapContext));
	}

}
