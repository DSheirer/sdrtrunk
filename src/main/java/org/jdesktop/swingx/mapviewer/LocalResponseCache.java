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
package org.jdesktop.swingx.mapviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * @author joshy
 */
public class LocalResponseCache extends ResponseCache
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( LocalResponseCache.class );

	private final File cacheDir;

	private boolean checkForUpdates;

	private String baseURL;

	/**
	 * Private constructor to prevent instantiation.
	 * @param baseURL the URI that should be cached or <code>null</code> (for all URLs)
	 * @param cacheDir the cache directory
	 * @param checkForUpdates true if the URL is queried for newer versions of a file first
	 */
	private LocalResponseCache(String baseURL, File cacheDir, boolean checkForUpdates)
	{
		this.baseURL = baseURL;
		this.cacheDir = cacheDir;
		this.checkForUpdates = checkForUpdates;

		if (!cacheDir.exists())
		{
			cacheDir.mkdirs();
		}
	}

	/**
	 * Sets this cache as default response cache
	 * @param baseURL the URL, the caching should be restricted to or <code>null</code> for none
	 * @param cacheDir the cache directory
	 * @param checkForUpdates true if the URL is queried for newer versions of a file first
	 */
	public static void installResponseCache(String baseURL, File cacheDir, boolean checkForUpdates)
	{
		ResponseCache.setDefault(new LocalResponseCache(baseURL, cacheDir, checkForUpdates));
	}

	/**
	 * Returns the local File corresponding to the given remote URI.
	 * @param remoteUri the remote URI
	 * @return the corresponding local file
	 */
	public File getLocalFile(URI remoteUri)
	{
		if (baseURL != null)
		{
			String remote = remoteUri.toString();
			
			if (!remote.startsWith(baseURL))
			{
				return null;
			}
		}
		
		
		StringBuilder sb = new StringBuilder();
		
	    String host = remoteUri.getHost();
	    String query = remoteUri.getQuery();
	    String path = remoteUri.getPath();
	    String fragment = remoteUri.getFragment();
	    
		if (host != null)
		{
			sb.append(host);
		}
		if (path != null)
		{
			sb.append(path);
		}
		if (query != null)
		{
			sb.append('?');
			sb.append(query);
		}
		if (fragment != null)
		{
			sb.append('#');
			sb.append(fragment);
		}

		String name;
		
		final int maxLen = 250;
		
		if (sb.length() < maxLen)
		{
			name = sb.toString();
		}
		else
		{
			name = sb.substring(0, maxLen);
		}
		
		name = name.replace('?', '$');
		name = name.replace('*', '$');
		name = name.replace(':', '$');
		name = name.replace('<', '$');
		name = name.replace('>', '$');
		name = name.replace('"', '$');
		
		File f = new File(cacheDir, name);
		
		return f;
	}

	/**
	 * @param remoteUri the remote URI
	 * @param localFile the corresponding local file
	 * @return true if the resource at the given remote URI is newer than the resource cached locally.
	 */
	private static boolean isUpdateAvailable(URI remoteUri, File localFile)
	{
		URLConnection conn;
		try
		{
			conn = remoteUri.toURL().openConnection();
		}
		catch (MalformedURLException ex)
		{
			mLog.error("An exception occurred - ", ex );
			return false;
		}
		catch (IOException ex)
		{
			mLog.error("An exception occurred - ", ex );
			return false;
		}
		if (!(conn instanceof HttpURLConnection))
		{
			// don't bother with non-http connections
			return false;
		}

		long localLastMod = localFile.lastModified();
		long remoteLastMod = 0L;
		HttpURLConnection httpconn = (HttpURLConnection) conn;
		// disable caching so we don't get in feedback loop with ResponseCache
		httpconn.setUseCaches(false);
		try
		{
			httpconn.connect();
			remoteLastMod = httpconn.getLastModified();
		}
		catch (IOException ex)
		{
			// log.error("An exception occurred", ex);();
			return false;
		}
		finally
		{
			httpconn.disconnect();
		}

		return (remoteLastMod > localLastMod);
	}

	@Override
	public CacheResponse get(URI uri, String rqstMethod, Map<String, List<String>> rqstHeaders) throws IOException
	{
		File localFile = getLocalFile(uri);
		
		if (localFile == null)
		{
			// we don't want to cache this URL 
			return null;
		}
		
		if (!localFile.exists())
		{
			// the file isn't already in our cache, return null
			return null;
		}

		if (checkForUpdates)
		{
			if (isUpdateAvailable(uri, localFile))
			{
				// there is an update available, so don't return cached version
				return null;
			}
		}

		return new LocalCacheResponse(localFile, rqstHeaders);
	}

	@Override
	public CacheRequest put(URI uri, URLConnection conn) throws IOException
	{
		// only cache http(s) GET requests
		if (!(conn instanceof HttpURLConnection) || !(((HttpURLConnection) conn).getRequestMethod().equals("GET")))
		{
			return null;
		}

		File localFile = getLocalFile(uri);
		
		if (localFile == null)
		{
			// we don't want to cache this URL 
			return null;
		}
		
		new File(localFile.getParent()).mkdirs();
		return new LocalCacheRequest(localFile);
	}

	private class LocalCacheResponse extends CacheResponse
	{
		private FileInputStream fis;
		private final Map<String, List<String>> headers;

		private LocalCacheResponse(File localFile, Map<String, List<String>> rqstHeaders)
		{
			try
			{
				this.fis = new FileInputStream(localFile);
			}
			catch (FileNotFoundException ex)
			{
				// should not happen, since we already checked for existence
				mLog.error("An exception occurred - ", ex );
			}
			this.headers = rqstHeaders;
		}

		@Override
		public Map<String, List<String>> getHeaders() throws IOException
		{
			return headers;
		}

		@Override
		public InputStream getBody() throws IOException
		{
			return fis;
		}
	}

	private class LocalCacheRequest extends CacheRequest
	{
		private final File localFile;
		private FileOutputStream fos;

		private LocalCacheRequest(File localFile)
		{
			this.localFile = localFile;
			try
			{
				this.fos = new FileOutputStream(localFile);
			}
			catch (FileNotFoundException ex)
			{
				// should not happen if cache dir is valid
				mLog.error("An exception occurred", ex );
			}
		}

		@Override
		public OutputStream getBody() throws IOException
		{
			return fos;
		}

		@Override
		public void abort()
		{
			// abandon the cache attempt by closing the stream and deleting
			// the local file
			try
			{
				fos.close();
				localFile.delete();
			}
			catch (IOException e)
			{
				// ignore
			}
		}
	}
}
