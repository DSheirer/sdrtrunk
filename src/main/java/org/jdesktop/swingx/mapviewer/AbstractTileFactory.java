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
package org.jdesktop.swingx.mapviewer;

import org.apache.commons.math3.util.FastMath;
import org.jdesktop.swingx.mapviewer.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * The <code>AbstractTileFactory</code> provides 
 * a basic implementation for the TileFactory.
 */
public abstract class AbstractTileFactory extends TileFactory
{
	private final static Logger mLog = LoggerFactory.getLogger( AbstractTileFactory.class );
	private boolean mSSLSuitesLogged = false;

	/**
	 * Creates a new instance of DefaultTileFactory using the spcified TileFactoryInfo
	 * @param info a TileFactoryInfo to configure this TileFactory
	 */
	public AbstractTileFactory(TileFactoryInfo info)
	{
		super(info);
	}

	// private static final boolean doEagerLoading = true;

	private int threadPoolSize = 4;
	private ExecutorService service;

	// TODO the tile map should be static ALWAYS, regardless of the number
	// of GoogleTileFactories because each tile is, really, a singleton.
	private Map<String, Tile> tileMap = new HashMap<String, Tile>();

	private TileCache cache = new TileCache();

	/**
	 * Returns the tile that is located at the given tilePoint 
	 * for this zoom. For example, if getMapSize() returns 10x20
	 * for this zoom, and the tilePoint is (3,5), then the 
	 * appropriate tile will be located and returned.
	 */
	@Override
	public Tile getTile(int x, int y, int zoom)
	{
		return getTile(x, y, zoom, true);
	}

	private Tile getTile(int tpx, int tpy, int zoom, boolean eagerLoad)
	{
		// wrap the tiles horizontally --> mod the X with the max width
		// and use that
		int tileX = tpx;// tilePoint.getX();
		int numTilesWide = (int) getMapSize(zoom).getWidth();
		if (tileX < 0)
		{
			tileX = numTilesWide - (FastMath.abs(tileX) % numTilesWide);
		}

		tileX = tileX % numTilesWide;
		int tileY = tpy;
		// TilePoint tilePoint = new TilePoint(tileX, tpy);
		String url = getInfo().getTileUrl(tileX, tileY, zoom);// tilePoint);
		// System.out.println("loading: " + url);

		Tile.Priority pri = Tile.Priority.High;
		if (!eagerLoad)
		{
			pri = Tile.Priority.Low;
		}
		Tile tile = null;
		// System.out.println("testing for validity: " + tilePoint + " zoom = " + zoom);
		if (!tileMap.containsKey(url))
		{
			if (!GeoUtil.isValidTile(tileX, tileY, zoom, getInfo()))
			{
				tile = new Tile(tileX, tileY, zoom);
			}
			else
			{
				tile = new Tile(tileX, tileY, zoom, url, pri, this);
				startLoading(tile);
			}
			tileMap.put(url, tile);
		}
		else
		{
			tile = tileMap.get(url);
			// if its in the map but is low and isn't loaded yet
			// but we are in high mode
			if (tile.getPriority() == Tile.Priority.Low && eagerLoad && !tile.isLoaded())
			{
				// System.out.println("in high mode and want a low");
				// tile.promote();
				promote(tile);
			}
		}

		/*
		 * if (eagerLoad && doEagerLoading) { for (int i = 0; i<1; i++) { for (int j = 0; j<1; j++) { // preload the 4
		 * tiles under the current one if(zoom > 0) { eagerlyLoad(tilePoint.getX()*2, tilePoint.getY()*2, zoom-1);
		 * eagerlyLoad(tilePoint.getX()*2+1, tilePoint.getY()*2, zoom-1); eagerlyLoad(tilePoint.getX()*2,
		 * tilePoint.getY()*2+1, zoom-1); eagerlyLoad(tilePoint.getX()*2+1, tilePoint.getY()*2+1, zoom-1); } } } }
		 */

		return tile;
	}

	/*
	 * private void eagerlyLoad(int x, int y, int zoom) { TilePoint t1 = new TilePoint(x,y); if(!isLoaded(t1,zoom)) {
	 * getTile(t1,zoom,false); } }
	 */

	// private boolean isLoaded(int x, int y, int zoom) {
	// String url = getInfo().getTileUrl(zoom,x,y);
	// return tileMap.containsKey(url);
	// }

	/**
	 * @return the tile cache
	 */
	public TileCache getTileCache()
	{
		return cache;
	}

	/**
	 * @param cache the tile cache
	 */
	public void setTileCache(TileCache cache)
	{
		this.cache = cache;
	}

	/** ==== threaded tile loading stuff === */
	/**
	 * Thread pool for loading the tiles
	 */
	private static BlockingQueue<Tile> tileQueue = new PriorityBlockingQueue<Tile>(5, new Comparator<Tile>()
	{
		@Override
		public int compare(Tile o1, Tile o2)
		{
			if (o1.getPriority() == Tile.Priority.Low && o2.getPriority() == Tile.Priority.High)
			{
				return 1;
			}
			if (o1.getPriority() == Tile.Priority.High && o2.getPriority() == Tile.Priority.Low)
			{
				return -1;
			}
			return 0;

		}
	});

	/**
	 * Subclasses may override this method to provide their own executor services. This method will be called each time
	 * a tile needs to be loaded. Implementations should cache the ExecutorService when possible.
	 * @return ExecutorService to load tiles with
	 */
	protected synchronized ExecutorService getService()
	{
		if (service == null)
		{
			// System.out.println("creating an executor service with a threadpool of size " + threadPoolSize);
			service = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory()
			{
				private int count = 0;

				@Override
				public Thread newThread(Runnable r)
				{
					Thread t = new Thread(r, "map tile fetcher-pool-" + count++);
					t.setPriority(Thread.MIN_PRIORITY);
					t.setDaemon(true);
					return t;
				}
			});
		}
		return service;
	}
	
	@Override
	public void dispose()
	{
		if (service != null)
		{
			service.shutdown();
			service = null;
		}
	}

	/**
	 * Set the number of threads to use for loading the tiles. This controls the number of threads used by the
	 * ExecutorService returned from getService(). Note, this method should be called before loading the first tile.
	 * Calls after the first tile are loaded will have no effect by default.
	 * @param size the thread pool size
	 */
	public void setThreadPoolSize(int size)
	{
		if (size <= 0)
		{
			throw new IllegalArgumentException("size invalid: " + size
					+ ". The size of the threadpool must be greater than 0.");
		}
		threadPoolSize = size;
	}

	@Override
	protected synchronized void startLoading(Tile tile)
	{
		if (tile.isLoading())
		{
			System.out.println("already loading. bailing");
			return;
		}
		tile.setLoading(true);
		try
		{
			tileQueue.put(tile);
			getService().submit(createTileRunner(tile));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Subclasses can override this if they need custom TileRunners for some reason
	 * @param tile the tile (unused!)
	 * @return the tile runner
	 */
	protected Runnable createTileRunner(Tile tile)
	{
		return new TileRunner();
	}

	/**
	 * Increase the priority of this tile so it will be loaded sooner.
	 * @param tile the tile
	 */
	public synchronized void promote(Tile tile)
	{
		if (tileQueue.contains(tile))
		{
			try
			{
				tileQueue.remove(tile);
				tile.setPriority(Tile.Priority.High);
				tileQueue.put(tile);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	/**
	 * An inner class which actually loads the tiles. Used by the thread queue. Subclasses can override this if
	 * necessary.
	 */
	private class TileRunner implements Runnable
	{
		/**
		 * Gets the full URI of a tile.
		 * @param tile the tile
		 * @throws URISyntaxException if the URI is invalid
		 * @return a URI for the tile
		 */
		protected URI getURI(Tile tile) throws URISyntaxException
		{
			if (tile.getURL() == null)
			{
				return null;
			}
			return new URI(tile.getURL());
		}

		/**
		 * implementation of the Runnable interface.
		 */
		@Override
		public void run()
		{
			/*
			 * 3 strikes and you're out. Attempt to load the url. If it fails, decrement the number of tries left and
			 * try again. Log failures. If I run out of try s just get out. This way, if there is some kind of serious
			 * failure, I can get out and let other tiles try to load.
			 */
			final Tile tile = tileQueue.remove();

			int trys = 3;
			while (!tile.isLoaded() && trys > 0)
			{
				try
				{
					BufferedImage img = null;
					URI uri = getURI(tile);
					img = cache.get(uri);
					if (img == null)
					{
						byte[] bimg = cacheInputStream(uri.toURL());
						// img = PaintUtils.loadCompatibleImage(new ByteArrayInputStream(bimg));
						img = ImageIO.read(new ByteArrayInputStream(bimg));
						cache.put(uri, bimg, img);
						img = cache.get(uri);
					}
					if (img == null)
					{
						trys--;
					}
					else
					{
						final BufferedImage i = img;
						SwingUtilities.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								tile.image = new SoftReference<BufferedImage>(i);
								tile.setLoaded(true);
								fireTileLoadedEvent(tile);
							}
						});
					}
				}
				catch (OutOfMemoryError memErr)
				{
					cache.needMoreMemory();
				}
				catch(SSLException ssle)
				{
					if(ssle.getMessage() != null && ssle.getMessage().startsWith("No PSK available"))
					{
						//JDK 11 bug: https://bugs.openjdk.java.net/browse/JDK-8213202
					}
					else
					{
						mLog.error("SSL Exception");
					}
				}
				catch (Throwable e)
				{
					if (trys == 0)
					{
						mLog.error("Failed to load a tile at url: " + tile.getURL() + ", stopping", e );
					}
					else
					{
						mLog.error("Failed to load a tile at url: " + tile.getURL() + ", retrying", e );
						trys--;
					}
				}
			}
			tile.setLoading(false);
		}

		private byte[] cacheInputStream(URL url) throws IOException
		{
			//Ugly hack.  We only use OSM tiles, so detect it here and comply with SOM Terms of Service (ie user agent)
			if(url.toString().startsWith("https://tile.openstreetmap.org"))
			{
				HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
				conn.addRequestProperty("User-Agent", "sdrtrunk");

				try
				{
					SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
					sslContext.init(null, null, new SecureRandom());
					conn.setSSLSocketFactory(sslContext.getSocketFactory());
				}
				catch(KeyManagementException kme)
				{
					mLog.error("Error initializing SSL context");
				}
				catch(NoSuchAlgorithmException nsae)
				{
					mLog.error("Unable to use TLSv1.3 for OpenStreetMap tiles");
				}

				InputStream is = conn.getInputStream();
				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				byte[] buf = new byte[256];
				while (true)
				{
					int n = is.read(buf);
					if (n == -1)
						break;
					bout.write(buf, 0, n);
				}

				conn.disconnect();

				return bout.toByteArray();
			}
			else
			{
				InputStream ins = url.openStream();
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				byte[] buf = new byte[256];
				while (true)
				{
					int n = ins.read(buf);
					if (n == -1)
						break;
					bout.write(buf, 0, n);
				}
				return bout.toByteArray();
			}
		}
	}

	public static void main(String[] args)
	{
		String a = "https://tile.openstreetmap.org/12/1178/1504.png";
//		String a = "https://tile.openstreetmap.org/11/589/751.png";

		try
		{
			URL url = new URL(a);

			System.out.println("Fetching URL: " + url.getClass() + " " + url.toString());

			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			conn.addRequestProperty("User-Agent", "sdrtrunk");
			InputStream is = conn.getInputStream();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();

			byte[] buf = new byte[256];
			while (true)
			{
				int n = is.read(buf);
				if (n == -1)
					break;
				bout.write(buf, 0, n);
			}

			System.out.println("Bytes:" + Arrays.toString(bout.toByteArray()));
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
