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

package org.geomajas.layer.common.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.geomajas.geometry.Bbox;
import org.geomajas.layer.RasterLayer;
import org.geomajas.plugin.caching.service.CacheCategory;
import org.geomajas.plugin.caching.service.CacheManagerService;
import org.geomajas.service.ConfigurationService;
import org.geomajas.service.TestRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.locationtech.jts.geom.Envelope;

/**
 * {@link LayerHttpService} with caching support. Be aware that caching bypasses security requirements. To avoid this,
 * set {@link ProxyLayerSupport#isUseCache()} to false.
 * 
 * @author Jan De Moerloose
 * 
 */
@Component
public class CachingLayerHttpService extends LayerHttpServiceImpl {

	/** Group used for {@link TestRecorder} messages. */
	public static final String TEST_RECORDER_GROUP = "WMS";

	/** {@link TestRecorder} message when putting image in cache. */
	public static final String TEST_RECORDER_PUT_IN_CACHE = "Get original and put in cache.";

	/** {@link TestRecorder} message when getting image from cache. */
	public static final String TEST_RECORDER_GET_FROM_CACHE = "Get from cache.";

	@Autowired
	private ConfigurationService configurationService;

	@Autowired(required = false)
	private CacheManagerService cacheManagerService;

	@Autowired
	private TestRecorder testRecorder;
	
	@Autowired(required = false)
	private LayerHttpServiceInterceptors interceptors;

	/**
	 * Get the contents from the request URL.
	 *
	 * @param url URL to get the response from
	 * @param layer the raster layer
	 * @return {@link InputStream} with the content
	 * @throws IOException cannot get content
	 */
	public InputStream getStream(String url, RasterLayer layer) throws IOException {
		if (layer instanceof ProxyLayerSupport) {
			ProxyLayerSupport proxyLayer = (ProxyLayerSupport) layer;
			if (proxyLayer.isUseCache() && null != cacheManagerService) {
				Object cachedObject = cacheManagerService.get(proxyLayer, CacheCategory.RASTER, url);
				if (null != cachedObject) {
					testRecorder.record(TEST_RECORDER_GROUP, TEST_RECORDER_GET_FROM_CACHE);
					return new ByteArrayInputStream((byte[]) cachedObject);
				} else {
					testRecorder.record(TEST_RECORDER_GROUP, TEST_RECORDER_PUT_IN_CACHE);
					InputStream stream = super.getStream(url, proxyLayer);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					int b;
					while ((b = stream.read()) >= 0) {
						os.write(b);
					}
					cacheManagerService.put(proxyLayer, CacheCategory.RASTER, url, os.toByteArray(),
							getLayerEnvelope(proxyLayer));
					return new ByteArrayInputStream((byte[]) os.toByteArray());
				}
			}
		}
		return super.getStream(url, layer);
	}

	/**
	 * Return the max bounds of the layer as envelope.
	 * 
	 * @param layer the layer to get envelope from
	 * @return Envelope the envelope
	 */
	private Envelope getLayerEnvelope(ProxyLayerSupport layer) {
		Bbox bounds = layer.getLayerInfo().getMaxExtent();
		return new Envelope(bounds.getX(), bounds.getMaxX(), bounds.getY(), bounds.getMaxY());
	}
}
