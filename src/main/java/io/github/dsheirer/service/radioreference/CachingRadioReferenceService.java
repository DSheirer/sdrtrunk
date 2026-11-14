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

package io.github.dsheirer.service.radioreference;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.AgencyInfo;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Frequency;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.StateInfo;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Talkgroup;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Provides local caching for frequently retrieved items from the radio reference service.
 *
 * Cached entries expire after {@link #CACHE_EXPIRATION} so that a long running application sees changes to the radio
 * reference database.  Failed calls aren't cached: the error is thrown to the caller and the next call tries again.
 * Cached lists are read-only because they are shared by all callers.
 */
public class CachingRadioReferenceService extends RadioReferenceService
{
    /**
     * Limit for receiving each response.  The JDK HTTP client applies the request timeout until the response body is
     * fully received, so this is longer than the library's 30 second default to allow the talkgroup list of a large
     * system to download on a slow connection.
     */
    public static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);

    /**
     * Time that a cached entry is kept after it is retrieved
     */
    public static final Duration CACHE_EXPIRATION = Duration.ofHours(1);

    private final LoadingCache<Integer,AgencyInfo> mAgencyInfoCache;
    private final LoadingCache<Integer,CountyInfo> mCountyInfoCache;
    private final LoadingCache<Integer,StateInfo> mStateInfoCache;
    private final LoadingCache<Integer,SystemInformation> mSystemInfoCache;
    private final LoadingCache<Integer,List<Site>> mSystemSitesCache;
    private final LoadingCache<Integer,List<Frequency>> mSubCategoryFrequencyCache;
    private final LoadingCache<Integer,List<Talkgroup>> mTalkgroupCache;

    /**
     * Constructs an instance of the service
     *
     * @param authorizationInformation with username and password for accessing the web service
     * @throws RadioReferenceException if there are any errors while accessing the service
     */
    public CachingRadioReferenceService(AuthorizationInformation authorizationInformation) throws RadioReferenceException
    {
        super(authorizationInformation, DEFAULT_CONNECT_TIMEOUT, REQUEST_TIMEOUT);

        mAgencyInfoCache = cache(1000, super::getAgencyInfo);
        mCountyInfoCache = cache(1000, super::getCountyInfo);
        mStateInfoCache = cache(100, super::getStateInfo);
        mSystemInfoCache = cache(100, super::getSystemInformation);
        mSystemSitesCache = cache(1000, id -> readOnly(super.getSites(id)));
        mSubCategoryFrequencyCache = cache(1000, id -> readOnly(super.getSubCategoryFrequencies(id)));
        mTalkgroupCache = cache(100, id -> readOnly(super.getTalkgroups(id)));
    }

    @Override
    public AgencyInfo getAgencyInfo(int agencyId) throws RadioReferenceException
    {
        return get(mAgencyInfoCache, agencyId);
    }

    @Override
    public CountyInfo getCountyInfo(final int countyId) throws RadioReferenceException
    {
        return get(mCountyInfoCache, countyId);
    }

    @Override
    public StateInfo getStateInfo(final int stateId) throws RadioReferenceException
    {
        return get(mStateInfoCache, stateId);
    }

    @Override
    public SystemInformation getSystemInformation(int systemId) throws RadioReferenceException
    {
        return get(mSystemInfoCache, systemId);
    }

    /**
     * Sites for the system
     * @param systemId to query
     * @return read-only list of sites
     * @throws RadioReferenceException if there is an error
     */
    @Override
    public List<Site> getSites(int systemId) throws RadioReferenceException
    {
        return get(mSystemSitesCache, systemId);
    }

    /**
     * Frequencies for the subcategory
     * @param subCategoryId to query
     * @return read-only list of frequencies
     * @throws RadioReferenceException if there is an error
     */
    @Override
    public List<Frequency> getSubCategoryFrequencies(int subCategoryId) throws RadioReferenceException
    {
        return get(mSubCategoryFrequencyCache, subCategoryId);
    }

    /**
     * Talkgroups for the system
     * @param systemId to query
     * @return read-only list of talkgroups
     * @throws RadioReferenceException if there is an error
     */
    @Override
    public List<Talkgroup> getTalkgroups(int systemId) throws RadioReferenceException
    {
        return get(mTalkgroupCache, systemId);
    }

    /**
     * Loads a value from the web service
     */
    @FunctionalInterface
    @VisibleForTesting
    interface Loader<V>
    {
        V load(int id) throws RadioReferenceException;
    }

    /**
     * Creates a cache that loads values with the loader
     * @param maximumSize of the cache
     * @param loader to call the web service
     * @return cache
     */
    @VisibleForTesting
    static <V> LoadingCache<Integer,V> cache(int maximumSize, Loader<V> loader)
    {
        return CacheBuilder.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(CACHE_EXPIRATION)
            .build(new CacheLoader<Integer,V>()
            {
                @Override
                public V load(Integer id) throws RadioReferenceException
                {
                    return loader.load(id);
                }
            });
    }

    /**
     * Gets the cached value, or loads it from the web service.
     * @param cache to query
     * @param id of the value
     * @return value, or null if the web service didn't provide one.  Null values aren't cached.
     * @throws RadioReferenceException thrown by the web service call
     */
    @VisibleForTesting
    static <V> V get(LoadingCache<Integer,V> cache, int id) throws RadioReferenceException
    {
        try
        {
            return cache.get(id);
        }
        catch(CacheLoader.InvalidCacheLoadException e)
        {
            //The loader returned null, which the cache doesn't store
            return null;
        }
        catch(ExecutionException e)
        {
            if(e.getCause() instanceof RadioReferenceException rre)
            {
                throw rre;
            }

            throw new RadioReferenceException("Error retrieving value from radio reference service", e.getCause());
        }
        catch(UncheckedExecutionException e)
        {
            if(e.getCause() instanceof RuntimeException re)
            {
                throw re;
            }

            throw e;
        }
        catch(ExecutionError e)
        {
            if(e.getCause() instanceof Error error)
            {
                throw error;
            }

            throw e;
        }
    }

    /**
     * Read-only view of the list
     * @param list to wrap, or null
     * @return read-only list, or null if the list is null
     */
    private static <T> List<T> readOnly(List<T> list)
    {
        return list != null ? Collections.unmodifiableList(list) : null;
    }
}
