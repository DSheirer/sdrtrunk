/*
 * EmptyTileFactory.java
 *
 * Created on June 7, 2006, 4:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.empty;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.jdesktop.swingx.mapviewer.Tile;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/**
 * A null implementation of TileFactory. Draws empty areas.
 * @author joshy
 */
public class EmptyTileFactory extends TileFactory
{
	/** 
	 * The empty tile image. 
	 */
	private BufferedImage emptyTile;

	/** 
	 * Creates a new instance of EmptyTileFactory 
	 */
	public EmptyTileFactory()
	{
		this(new TileFactoryInfo("EmptyTileFactory 256x256", 1, 15, 17, 256, true, true, "", "x", "y", "z"));
	}

	/** 
	 * Creates a new instance of EmptyTileFactory using the specified info. 
	 * @param info the tile factory info
	 */
	public EmptyTileFactory(TileFactoryInfo info)
	{
		super(info);
		int tileSize = info.getTileSize(info.getMinimumZoomLevel());
		emptyTile = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = emptyTile.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GRAY);
		g.fillRect(0, 0, tileSize, tileSize);
		g.setColor(Color.WHITE);
		g.drawOval(10, 10, tileSize - 20, tileSize - 20);
		g.fillOval(70, 50, 20, 20);
		g.fillOval(tileSize - 90, 50, 20, 20);
		g.fillOval(tileSize / 2 - 10, tileSize / 2 - 10, 20, 20);
		g.dispose();
	}

	/**
	 * Gets an instance of an empty tile for the given tile position and zoom on the world map.
	 * @param x The tile's x position on the world map.
	 * @param y The tile's y position on the world map.
	 * @param zoom The current zoom level.
	 */
	@Override
	public Tile getTile(int x, int y, int zoom)
	{
		return new Tile(x, y, zoom)
		{
			@Override
			public synchronized boolean isLoaded()
			{
				return true;
			}

			@Override
			public BufferedImage getImage()
			{
				return emptyTile;
			}

		};
	}

	@Override
	public void dispose()
	{
		// noop
	}

	/**
	 * Override this method to load the tile using, for example, an <code>ExecutorService</code>.
	 * @param tile The tile to load.
	 */
	@Override
	protected void startLoading(Tile tile)
	{
		// noop
	}

}
