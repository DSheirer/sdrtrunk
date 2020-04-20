/*
 * WMSTileFactory.java
 *
 * Created on October 7, 2006, 6:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.wms;

import org.apache.commons.math3.util.FastMath;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/**
 * A tile factory that uses a WMS service.
 * @author joshy
 */
public class WMSTileFactory extends DefaultTileFactory
{
	/** 
	 * Creates a new instance of WMSTileFactory 
	 * @param wms the WMSService
	 */
	public WMSTileFactory(final WMSService wms)
	{
		// tile size and x/y orientation is r2l & t2b
		super(new TileFactoryInfo(0, 15, 17, 500, true, true, "", "x", "y", "zoom")
		{
			@Override
			public String getTileUrl(int x, int y, int zoom)
			{
				int zz = 17 - zoom;
				int z = 4;
				z = (int) FastMath.pow(2, (double) zz - 1);
				return wms.toWMSURL(x - z, z - 1 - y, zz, getTileSize(zoom));
			}

		});
	}

}
