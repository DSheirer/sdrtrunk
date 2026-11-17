/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.map;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads OpenStreetMap tile images in the background, with a memory cache of recently drawn tiles and a disk cache that
 * follows the tile server's caching headers.  Follows the OpenStreetMap tile usage policy: an identifying User-Agent,
 * two download threads, and local caching.  The newest requests load first and requests for tiles that scrolled out
 * of view are dropped.
 */
public class TileLoader
{
    private static final Logger mLog = LoggerFactory.getLogger(TileLoader.class);
    public static final String TILE_SERVER = "https://tile.openstreetmap.org";
    private static final String USER_AGENT = "sdrtrunk (https://github.com/DSheirer/sdrtrunk)";
    private static final int DOWNLOAD_THREADS = 2;
    //256 tiles of 256x256 pixels is about 64 MB
    private static final int MEMORY_CACHE_TILES = 256;
    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(7);
    private static final Duration MINIMUM_EXPIRY = Duration.ofHours(1);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(60);
    private static final long DISK_CACHE_LIMIT_BYTES = 512L * 1024 * 1024;
    private static final long DISK_CACHE_TRIM_BYTES = 400L * 1024 * 1024;
    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");

    private final Path mCacheDirectory;
    private final Consumer<TileKey> mLoadedListener;
    private final HttpClient mHttpClient;
    private final Map<TileKey,Image> mMemoryCache = new LinkedHashMap<>(MEMORY_CACHE_TILES, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TileKey,Image> eldest)
        {
            return size() > MEMORY_CACHE_TILES;
        }
    };
    private final LinkedBlockingDeque<TileKey> mQueue = new LinkedBlockingDeque<>();
    private final Set<TileKey> mPending = ConcurrentHashMap.newKeySet();
    private final Map<TileKey,Long> mFailures = new ConcurrentHashMap<>();
    private volatile Set<TileKey> mWanted = Collections.emptySet();
    private final AtomicBoolean mTrimmed = new AtomicBoolean();
    private final List<Thread> mThreads = new ArrayList<>();
    private volatile boolean mRunning = true;

    /**
     * Constructs an instance and starts the download threads
     * @param cacheDirectory for the disk cache, or null for no disk cache
     * @param loadedListener notified on a download thread when a requested tile is ready in {@link #get(TileKey)}
     */
    public TileLoader(Path cacheDirectory, Consumer<TileKey> loadedListener)
    {
        mCacheDirectory = cacheDirectory;
        mLoadedListener = loadedListener;
        mHttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

        for(int index = 0; index < DOWNLOAD_THREADS; index++)
        {
            Thread thread = Thread.ofPlatform().daemon().name("sdrtrunk map tile loader " + index)
                .unstarted(this::run);
            mThreads.add(thread);
            thread.start();
        }
    }

    /**
     * Tile image from the memory cache
     * @param key of the tile
     * @return image, or null when it isn't loaded
     */
    public Image get(TileKey key)
    {
        synchronized(mMemoryCache)
        {
            return mMemoryCache.get(key);
        }
    }

    /**
     * Sets the tiles that are in view.  Queued requests for other tiles are dropped when they come up.
     */
    public void setWanted(Set<TileKey> wanted)
    {
        mWanted = Set.copyOf(wanted);
    }

    /**
     * Requests a tile, unless it's loaded, already requested, or failed recently.  The newest request loads first.
     */
    public void request(TileKey key)
    {
        if(get(key) != null)
        {
            return;
        }

        Long failed = mFailures.get(key);

        if(failed != null && System.currentTimeMillis() < failed + RETRY_DELAY.toMillis())
        {
            return;
        }

        if(mPending.add(key))
        {
            mQueue.addLast(key);
        }
        else if(mQueue.remove(key))
        {
            //Requested again, so move it to the front of the line
            mQueue.addLast(key);
        }
    }

    /**
     * Stops the download threads
     */
    public void dispose()
    {
        mRunning = false;
        mThreads.forEach(Thread::interrupt);
    }

    /**
     * Download thread: takes the newest request, from the disk cache when it's fresh, else from the tile server
     */
    private void run()
    {
        trimDiskCache();

        while(mRunning)
        {
            TileKey key;

            try
            {
                key = mQueue.takeLast();
            }
            catch(InterruptedException e)
            {
                return;
            }

            try
            {
                if(mWanted.contains(key))
                {
                    Image image = load(key);

                    if(image != null)
                    {
                        mFailures.remove(key);

                        synchronized(mMemoryCache)
                        {
                            mMemoryCache.put(key, image);
                        }

                        mLoadedListener.accept(key);
                    }
                    else
                    {
                        mFailures.put(key, System.currentTimeMillis());
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Error loading map tile " + key, e);
                mFailures.put(key, System.currentTimeMillis());
            }
            finally
            {
                mPending.remove(key);
            }
        }
    }

    /**
     * Loads a tile from the disk cache while it's fresh, else downloads it, falling back to a stale cached copy when the
     * download fails
     * @return image, or null
     */
    private Image load(TileKey key)
    {
        Path file = getCacheFile(key);
        byte[] bytes = null;
        boolean fresh = false;

        if(file != null && Files.isRegularFile(file))
        {
            try
            {
                //The file's modified time is when it expires
                fresh = Files.getLastModifiedTime(file).toMillis() > System.currentTimeMillis();
                bytes = Files.readAllBytes(file);
            }
            catch(IOException e)
            {
                bytes = null;
            }
        }

        if(!fresh)
        {
            byte[] downloaded = download(key, file);

            if(downloaded != null)
            {
                bytes = downloaded;
            }
        }

        if(bytes == null)
        {
            return null;
        }

        Image image = new Image(new ByteArrayInputStream(bytes));
        return image.isError() ? null : image;
    }

    /**
     * Downloads the tile and stores it in the disk cache
     * @param key of the tile
     * @param file of the disk cache, or null
     * @return tile image bytes, or null when the download failed
     */
    private byte[] download(TileKey key, Path file)
    {
        if(!mRunning)
        {
            return null;
        }

        URI uri = URI.create(TILE_SERVER + "/" + key.zoom() + "/" + key.x() + "/" + key.y() + ".png");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
            .header("User-Agent", USER_AGENT).GET().build();

        try
        {
            HttpResponse<byte[]> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if(response.statusCode() != 200)
            {
                mLog.debug("Map tile " + uri + " response " + response.statusCode());
                return null;
            }

            byte[] bytes = response.body();

            if(file != null)
            {
                store(file, bytes, getExpiry(response));
            }

            return bytes;
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch(IOException e)
        {
            mLog.debug("Unable to download map tile " + uri + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * Time the tile expires: the Cache-Control max-age, else the Expires header, else a week, and at least an hour
     */
    private static long getExpiry(HttpResponse<?> response)
    {
        long now = System.currentTimeMillis();
        Duration expiry = DEFAULT_EXPIRY;
        Optional<String> cacheControl = response.headers().firstValue("Cache-Control");
        Optional<String> expires = response.headers().firstValue("Expires");

        if(cacheControl.isPresent())
        {
            Matcher matcher = MAX_AGE.matcher(cacheControl.get());

            if(matcher.find())
            {
                expiry = Duration.ofSeconds(Long.parseLong(matcher.group(1)));
            }
        }
        else if(expires.isPresent())
        {
            try
            {
                long time = ZonedDateTime.parse(expires.get(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                    .toEpochMilli();
                expiry = Duration.ofMillis(time - now);
            }
            catch(Exception e)
            {
                //Use the default
            }
        }

        if(expiry.compareTo(MINIMUM_EXPIRY) < 0)
        {
            expiry = MINIMUM_EXPIRY;
        }

        return now + expiry.toMillis();
    }

    /**
     * Writes the tile to the disk cache, with its modified time set to when it expires
     */
    private static void store(Path file, byte[] bytes, long expiry)
    {
        try
        {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "tile", ".tmp");
            Files.write(temporary, bytes);
            Files.setLastModifiedTime(temporary, FileTime.fromMillis(expiry));
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(IOException e)
        {
            mLog.debug("Unable to cache map tile " + file + " - " + e.getMessage());
        }
    }

    private Path getCacheFile(TileKey key)
    {
        return mCacheDirectory != null ? mCacheDirectory.resolve(String.valueOf(key.zoom()))
            .resolve(String.valueOf(key.x())).resolve(key.y() + ".png") : null;
    }

    /**
     * Deletes the tiles that expire first when the disk cache grows past its limit.  Runs once per instance.
     */
    private void trimDiskCache()
    {
        if(mCacheDirectory == null || !Files.isDirectory(mCacheDirectory) || mTrimmed.getAndSet(true))
        {
            return;
        }

        record CachedTile(Path path, long size, long expiry) {}

        List<CachedTile> tiles = new ArrayList<>();

        try(Stream<Path> stream = Files.walk(mCacheDirectory))
        {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try
                {
                    tiles.add(new CachedTile(path, Files.size(path), Files.getLastModifiedTime(path).toMillis()));
                }
                catch(IOException e)
                {
                    //Skip it
                }
            });
        }
        catch(IOException e)
        {
            mLog.debug("Unable to read the map tile cache - " + e.getMessage());
            return;
        }

        long total = tiles.stream().mapToLong(CachedTile::size).sum();

        if(total <= DISK_CACHE_LIMIT_BYTES)
        {
            return;
        }

        tiles.sort(Comparator.comparingLong(CachedTile::expiry));
        int deleted = 0;

        for(CachedTile tile: tiles)
        {
            if(total <= DISK_CACHE_TRIM_BYTES)
            {
                break;
            }

            try
            {
                Files.deleteIfExists(tile.path());
                total -= tile.size();
                deleted++;
            }
            catch(IOException e)
            {
                //Skip it
            }
        }

        mLog.info("Trimmed the map tile cache: deleted " + deleted + " tiles");
    }
}
