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

package io.github.dsheirer.module.decode.moto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * Decode configuration for Motorola Type II trunking systems (Smartnet, SmartZone,
 * SmartZone OmniLink, Hybrid).
 */
public class DecodeConfigMotorolaTypeII extends DecodeConfiguration
{
    public static final int CHANNEL_ROTATION_DELAY_MINIMUM_MS = 500;
    public static final int CHANNEL_ROTATION_DELAY_DEFAULT_MS = 500;
    public static final int CHANNEL_ROTATION_DELAY_MAXIMUM_MS = 2000;

    private static final int TRAFFIC_CHANNEL_POOL_SIZE_DEFAULT = 4;
    private static final int TRAFFIC_CHANNEL_POOL_SIZE_MIN = 1;
    private static final int TRAFFIC_CHANNEL_POOL_SIZE_MAX = 16;

    private BandplanType mBandplanType = BandplanType.EIGHT_HUNDRED_REBANDED;
    private double mObtBaseFrequency;
    private double mObtSpacing;
    private int mObtOffset;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_POOL_SIZE_DEFAULT;

    /**
     * De-emphasis for a new configuration, chosen by ear on an analog simulcast system.
     */
    public static final AnalogDeemphasis ANALOG_DEEMPHASIS_DEFAULT = AnalogDeemphasis.US_150;
    private AnalogDeemphasis mAnalogDeemphasis = ANALOG_DEEMPHASIS_DEFAULT;

    public DecodeConfigMotorolaTypeII()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.MOTOROLA_TYPE_II;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "bandplan_type")
    public BandplanType getBandplanType()
    {
        return mBandplanType;
    }

    public void setBandplanType(BandplanType bandplanType)
    {
        mBandplanType = bandplanType != null ? bandplanType : BandplanType.EIGHT_HUNDRED_REBANDED;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "obt_base_frequency")
    public double getObtBaseFrequency()
    {
        return mObtBaseFrequency;
    }

    public void setObtBaseFrequency(double obtBaseFrequency)
    {
        mObtBaseFrequency = obtBaseFrequency;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "obt_spacing")
    public double getObtSpacing()
    {
        return mObtSpacing;
    }

    public void setObtSpacing(double obtSpacing)
    {
        mObtSpacing = obtSpacing;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "obt_offset")
    public int getObtOffset()
    {
        return mObtOffset;
    }

    public void setObtOffset(int obtOffset)
    {
        mObtOffset = obtOffset;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    public void setTrafficChannelPoolSize(int poolSize)
    {
        if(poolSize < TRAFFIC_CHANNEL_POOL_SIZE_MIN)
        {
            mTrafficChannelPoolSize = TRAFFIC_CHANNEL_POOL_SIZE_MIN;
        }
        else if(poolSize > TRAFFIC_CHANNEL_POOL_SIZE_MAX)
        {
            mTrafficChannelPoolSize = TRAFFIC_CHANNEL_POOL_SIZE_MAX;
        }
        else
        {
            mTrafficChannelPoolSize = poolSize;
        }
    }

    /**
     * De-emphasis applied to analog voice on the traffic channels.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "analog_deemphasis")
    public AnalogDeemphasis getAnalogDeemphasis()
    {
        return mAnalogDeemphasis;
    }

    public void setAnalogDeemphasis(AnalogDeemphasis deemphasis)
    {
        mAnalogDeemphasis = deemphasis != null ? deemphasis : ANALOG_DEEMPHASIS_DEFAULT;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);
    }
}
