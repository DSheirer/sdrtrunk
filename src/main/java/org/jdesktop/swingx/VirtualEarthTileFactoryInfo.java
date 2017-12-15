/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.jdesktop.swingx;

import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/*******************************************************************************
 * http://www.viavirtualearth.com/vve/Articles/RollYourOwnTileServer.ashx
 * @author Fabrizio Giudici
 * @version $Id: MicrosoftVirtualEarthProvider.java 115 2007-11-08 22:04:36Z
 * fabriziogiudici $
 ********************************************************************************/
public class VirtualEarthTileFactoryInfo extends TileFactoryInfo
{
	/**
	 * Use road map
	 */
	public final static MVEMode MAP = new MVEMode("map", "map", "r", ".png");

	/**
	 * Use satellite map
	 */
	public final static MVEMode SATELLITE = new MVEMode("satellite", "satellite", "a", ".jpeg");

	/**
	 * Use hybrid map
	 */
	public final static MVEMode HYBRID = new MVEMode("hybrid", "hybrid", "h", ".jpeg");

	/**
	 * The map mode
	 */
	public static class MVEMode
	{
		private String type;
		private String ext;
		private String name;
		private String label;

		private MVEMode(final String name, final String label, final String type, final String ext)
		{
			this.type = type;
			this.ext = ext;
			this.name = name;
			this.label = label;
		}
	}

	private final static int TOP_ZOOM_LEVEL = 19;

	private final static int MAX_ZOOM_LEVEL = 17;

	private final static int MIN_ZOOM_LEVEL = 2;

	private final static int TILE_SIZE = 256;

	private MVEMode mode;

	/**
	 * @param mode the mode
	 */
	public VirtualEarthTileFactoryInfo(MVEMode mode)
	{
		super("Virtual Earth", MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL, TOP_ZOOM_LEVEL, TILE_SIZE, false, false, "", "", "", "");
		
		this.mode = mode;
	}

	/**
	 * @return the name of the selected mode
	 */
	public String getModeName()
	{
		return mode.name;
	}
	
	/**
	 * @return the label of the selected mode
	 */
	public String getModeLabel()
	{
		return mode.label;
	}
	
	@Override
	public String getTileUrl(final int x, final int y, final int zoom)
	{
		final String quad = tileToQuadKey(x, y, TOP_ZOOM_LEVEL - 0 - zoom);
		return "http://" + mode.type + quad.charAt(quad.length() - 1) + 
				".ortho.tiles.virtualearth.net/tiles/"
				+ mode.type + quad + mode.ext + "?g=1";
	}

	private String tileToQuadKey(final int tx, final int ty, final int zl)
	{
		String quad = "";

		for (int i = zl; i > 0; i--)
		{
			int mask = 1 << (i - 1);
			int cell = 0;

			if ((tx & mask) != 0)
			{
				cell++;
			}

			if ((ty & mask) != 0)
			{
				cell += 2;
			}

			quad += cell;
		}

		return quad;
	}
}

