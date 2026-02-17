/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DecodeConfigP25Phase1.class, name = "decodeConfigP25Phase1"),
        @JsonSubTypes.Type(value = DecodeConfigP25Phase2.class, name = "decodeConfigP25Phase2"),
})
public abstract class DecodeConfigP25 extends DecodeConfiguration
{
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private boolean mIgnoreDataCalls = false;
    private List<Integer> mAllowedNACs = new ArrayList<>();
    private boolean mNacFilterEnabled = false;
    private int mTalkgroup = 0;

    public DecodeConfigP25()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "ignore_data_calls")
    public boolean getIgnoreDataCalls()
    {
        return mIgnoreDataCalls;
    }

    public void setIgnoreDataCalls(boolean ignore)
    {
        mIgnoreDataCalls = ignore;
    }


    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    /**
     * Sets the traffic channel pool size which is the maximum number of
     * simultaneous traffic channels that can be allocated.
     *
     * This limits the maximum calls so that busy systems won't cause more
     * traffic channels to be allocated than the decoder/software/host computer
     * can support.
     */
    public void setTrafficChannelPoolSize(int size)
    {
        mTrafficChannelPoolSize = size;
    }

    /**
     * List of allowed Network Access Codes (NACs) for this channel.
     * When NAC filtering is enabled, only messages with a matching NAC will be processed.
     */
    @JacksonXmlElementWrapper(localName = "allowedNACs")
    @JacksonXmlProperty(localName = "nac")
    public List<Integer> getAllowedNACs()
    {
        return mAllowedNACs;
    }

    public void setAllowedNACs(List<Integer> nacs)
    {
        mAllowedNACs = nacs != null ? nacs : new ArrayList<>();
    }

    /**
     * Adds an allowed NAC value
     * @param nac in range 0-4095
     */
    public void addAllowedNAC(int nac)
    {
        if(nac >= 0 && nac <= 4095 && !mAllowedNACs.contains(nac))
        {
            mAllowedNACs.add(nac);
        }
    }

    /**
     * Indicates if NAC filtering is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "nacFilterEnabled")
    public boolean isNacFilterEnabled()
    {
        return mNacFilterEnabled;
    }

    public void setNacFilterEnabled(boolean enabled)
    {
        mNacFilterEnabled = enabled;
    }

    /**
     * Returns the set of allowed NACs for fast lookup, or null if filtering is disabled.
     */
    @JsonIgnore
    public Set<Integer> getAllowedNACSet()
    {
        if(mNacFilterEnabled && !mAllowedNACs.isEmpty())
        {
            return new HashSet<>(mAllowedNACs);
        }
        return null;
    }

    /**
     * Talkgroup to assign for conventional (non-trunked) P25 operation.
     * Value of 0 means no override (default).
     */
    @JacksonXmlProperty(isAttribute = true, localName = "talkgroup")
    public int getTalkgroup()
    {
        return mTalkgroup;
    }

    public void setTalkgroup(int talkgroup)
    {
        mTalkgroup = Math.max(0, Math.min(talkgroup, 65535));
    }

    /**
     * Indicates if a talkgroup override is configured
     */
    @JsonIgnore
    public boolean hasTalkgroupOverride()
    {
        return mTalkgroup > 0;
    }
}
