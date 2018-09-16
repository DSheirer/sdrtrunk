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
package io.github.dsheirer.module.decode.nbfm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigNBFM extends DecodeConfiguration
{
    private Bandwidth mBandwidth = Bandwidth.BW_12_5;

    public DecodeConfigNBFM()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    /**
     * Channel bandwidth
     */
    @JacksonXmlProperty(isAttribute = true, localName = "bandwidth")
    public Bandwidth getBandwidth()
    {
        return mBandwidth;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        if(mBandwidth == Bandwidth.BW_12_5)
        {
            return new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);
        }
        else
        {
            return new ChannelSpecification(50000.0, 25000, 12500.0, 13500.0);
        }
    }

    /**
     * Sets the channel bandwidth
     */
    public void setBandwidth(Bandwidth bandwidth)
    {
        mBandwidth = bandwidth;
    }

    public enum Bandwidth
    {
        BW_12_5("12.5 kHz"),
        BW_25_0("25.0 kHz");

        private String mLabel;

        Bandwidth(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    };
}
