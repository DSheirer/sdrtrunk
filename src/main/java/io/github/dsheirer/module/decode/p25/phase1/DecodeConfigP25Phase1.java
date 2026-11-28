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
package io.github.dsheirer.module.decode.p25.phase1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import java.util.List;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * APCO25 Phase 1 decoder configuration
 */
public class DecodeConfigP25Phase1 extends DecodeConfigP25
{
    public static final int CHANNEL_ROTATION_DELAY_MINIMUM_MS = 400;
    public static final int CHANNEL_ROTATION_DELAY_DEFAULT_MS = 500;
    public static final int CHANNEL_ROTATION_DELAY_MAXIMUM_MS = 2000;
    public static final int NAC_MAXIMUM = 0xFFF;

    private Modulation mModulation = Modulation.C4FM;
    private AccessCodeFilter mNacFilter = new AccessCodeFilter(AccessCodeFilter.Format.HEX);

    /**
     * Constructs an instance
     */
    public DecodeConfigP25Phase1()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "modulation")
    public Modulation getModulation()
    {
        return mModulation;
    }

    public void setModulation(Modulation modulation)
    {
        mModulation = modulation;
    }

    /**
     * Network Access Code (NAC) filter for the channel.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "nac_access_filter")
    public AccessCodeFilter getNacFilter()
    {
        return mNacFilter;
    }

    /**
     * Sets the Network Access Code (NAC) filter for the channel.
     * @param nacFilter to apply, or null to remove the filter.
     */
    public void setNacFilter(AccessCodeFilter nacFilter)
    {
        mNacFilter = nacFilter != null ? nacFilter : new AccessCodeFilter(AccessCodeFilter.Format.HEX);
    }

    /**
     * Loads a single NAC filter value from playlists created by earlier builds of this feature (PR #2382) that stored
     * the filter as a decimal 'nac_filter' attribute.  The value is converted to an enabled, include-mode filter.
     * There is no getter, so the legacy attribute is read but never written.
     * @param nac value, where null or zero indicates no filter.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "nac_filter")
    public void setLegacyNacFilter(Integer nac)
    {
        if(nac != null && nac > 0 && nac <= NAC_MAXIMUM)
        {
            AccessCodeFilter filter = new AccessCodeFilter(AccessCodeFilter.Format.HEX);
            filter.setEnabled(true);
            filter.setValueSet(List.of(nac));
            mNacFilter = filter;
        }
    }

    /**
     * Source channel specification for this decoder
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0, getModulation() == Modulation.CQPSK ?
            P25P1DecoderLSM.getChannelFilter() : P25P1DecoderC4FM.getChannelFilter());
    }
}
