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
package io.github.dsheirer.module.decode.p25.phase2;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigP25Phase2 extends DecodeConfiguration
{
    private ScrambleParameters mScrambleParameters;
    private boolean mAutoDetectScrambleParameters;

    public DecodeConfigP25Phase2()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE2;
    }

    /**
     * Source channel specification for this decoder
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(50000.0, 12500, 6500.0, 7200.0);
    }

    @Override
    public int getTimeslotCount()
    {
        return 2;
    }

    @Override
    public int[] getTimeslots()
    {
        int[] timeslots = new int[2];
        timeslots[0] = 0;
        timeslots[1] = 1;
        return timeslots;
    }

    /**
     * Optional scramble (ie randomizer) parameters to use for the channel.
     */
    @JacksonXmlProperty(localName = "scramble_parameters")
    public ScrambleParameters getScrambleParameters()
    {
        return mScrambleParameters;
    }

    public void setScrambleParameters(ScrambleParameters scrambleParameters)
    {
        mScrambleParameters = scrambleParameters;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "auto_detect_scramble_parameters")
    public boolean isAutoDetectScrambleParameters()
    {
        return mAutoDetectScrambleParameters;
    }

    public void setAutoDetectScrambleParameters(boolean autoDetect)
    {
        mAutoDetectScrambleParameters = autoDetect;
    }
}
