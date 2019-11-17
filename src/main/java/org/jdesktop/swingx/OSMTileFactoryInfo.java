/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package org.jdesktop.swingx;

import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/**
 * Uses OpenStreetMap
 * @author Martin Dummer
 */
public class OSMTileFactoryInfo extends TileFactoryInfo
{
	private static final int max = 19;

	/**
	 * Default constructor
	 */
	public OSMTileFactoryInfo()
	{
		super("OpenStreetMap", 
				1, max - 2, max, 
				256, true, true, 					// tile size is 256 and x/y orientation is normal
				"https://tile.openstreetmap.org",
				"x", "y", "z");						// 5/15/10.png
	}

	@Override
	public String getTileUrl(int x, int y, int zoom)
	{
		zoom = max - zoom;
		String url = this.baseURL + "/" + zoom + "/" + x + "/" + y + ".png";
		return url;
	}

}
