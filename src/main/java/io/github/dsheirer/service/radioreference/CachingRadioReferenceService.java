/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.service.radioreference;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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

import java.util.List;

/**
 * Provides local caching for frequently retrieved items from the radio reference service
 */
public class CachingRadioReferenceService extends RadioReferenceService
{
    private LoadingCache<Integer,AgencyInfo> mAgencyInfoCache;
    private LoadingCache<Integer,CountyInfo> mCountyInfoCache;
    private LoadingCache<Integer,StateInfo> mStateInfoCache;
    private LoadingCache<Integer,SystemInformation> mSystemInfoCache;
    private LoadingCache<Integer,List<Site>> mSystemSitesCache;
    private LoadingCache<Integer,List<Frequency>> mSubCategoryFrequencyCache;
    private LoadingCache<Integer,List<Talkgroup>> mTalkgroupCache;

    /**
     * Constructs an instance of the service
     *
     * @param authorizationInformation with username and password for accessing the web service
     * @throws RadioReferenceException if there are any errors while accessing the service
     */
    public CachingRadioReferenceService(AuthorizationInformation authorizationInformation) throws RadioReferenceException
    {
        super(authorizationInformation);

        mAgencyInfoCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<Integer,AgencyInfo>()
            {
                @Override
                public AgencyInfo load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getAgencyInfo(key);
                }
            });

        mCountyInfoCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<Integer,CountyInfo>()
            {
                @Override
                public CountyInfo load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getCountyInfo(key);
                }
            });

        mStateInfoCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(new CacheLoader<Integer,StateInfo>()
            {
                @Override
                public StateInfo load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getStateInfo(key);
                }
            });

        mSystemInfoCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(new CacheLoader<Integer,SystemInformation>()
            {
                @Override
                public SystemInformation load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getSystemInformation(key);
                }
            });

        mSystemSitesCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<Integer,List<Site>>()
            {
                @Override
                public List<Site> load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getSites(key);
                }
            });

        mSubCategoryFrequencyCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<Integer,List<Frequency>>()
            {
                @Override
                public List<Frequency> load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getSubCategoryFrequencies(key);
                }
            });

        mTalkgroupCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(new CacheLoader<Integer,List<Talkgroup>>()
            {
                @Override
                public List<Talkgroup> load(Integer key) throws Exception
                {
                    return CachingRadioReferenceService.super.getTalkgroups(key);
                }
            });
    }

    @Override
    public AgencyInfo getAgencyInfo(int agencyId) throws RadioReferenceException
    {
        try
        {
            return mAgencyInfoCache.get(agencyId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getAgencyInfo(agencyId);
        }
    }

    @Override
    public CountyInfo getCountyInfo(final int countyId) throws RadioReferenceException
    {
        try
        {
            return mCountyInfoCache.get(countyId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getCountyInfo(countyId);
        }
    }

    @Override
    public StateInfo getStateInfo(final int stateId) throws RadioReferenceException
    {
        try
        {
            return mStateInfoCache.get(stateId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getStateInfo(stateId);
        }
    }

    @Override
    public SystemInformation getSystemInformation(int systemId) throws RadioReferenceException
    {
        try
        {
            return mSystemInfoCache.get(systemId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getSystemInformation(systemId);
        }
    }

    @Override
    public List<Site> getSites(int systemId) throws RadioReferenceException
    {
        try
        {
            return mSystemSitesCache.get(systemId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getSites(systemId);
        }
    }

    @Override
    public List<Frequency> getSubCategoryFrequencies(int subCategoryId) throws RadioReferenceException
    {
        try
        {
            return mSubCategoryFrequencyCache.get(subCategoryId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getSubCategoryFrequencies(subCategoryId);
        }
    }


    @Override
    public List<Talkgroup> getTalkgroups(int systemId) throws RadioReferenceException
    {
        try
        {
            return mTalkgroupCache.get(systemId);
        }
        catch(Exception e)
        {
            return CachingRadioReferenceService.super.getTalkgroups(systemId);
        }
    }
}
