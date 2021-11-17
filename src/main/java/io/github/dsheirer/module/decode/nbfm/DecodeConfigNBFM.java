/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.nbfm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.dsp.squelch.ISquelchConfiguration;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * Decoder configuration for an NBFM channel
 */
public class DecodeConfigNBFM extends DecodeConfiguration implements ISquelchConfiguration
{
    private Bandwidth mBandwidth = Bandwidth.BW_12_5;
    private Boolean mRecordAudio;
    private int mTalkgroup = 1;
    private int mSquelchThreshold = -60;

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

//    @Deprecated //No longer used: 20211107
//    @JacksonXmlProperty(isAttribute =  true, localName = "recordAudio")
//    public boolean getRecordAudio()
//    {
//        return false;
//    }
//
//    @Deprecated //No longer used: 20211107
//    public void setRecordAudio(boolean recordAudio)
//    {
//    }
//
    /**
     * Talkgroup to associate with audio produced by the NBFM decoder
     */
    @JacksonXmlProperty(isAttribute =  true, localName = "talkgroup")
    public int getTalkgroup()
    {
        return mTalkgroup;
    }

    /**
     * Sets the talkgroup identifier to attach to any demodulated audio streams
     * @param talkgroup (1-65,535)
     */
    public void setTalkgroup(int talkgroup)
    {
        if(talkgroup < 1 || talkgroup > 65535)
        {
            throw new IllegalArgumentException("Valid talkgroup range is 1 - 65,535");
        }

        mTalkgroup = talkgroup;
    }

    /**
     * Sets squelch threshold
     * @param threshold (dB)
     */
    @JacksonXmlProperty(isAttribute =  true, localName = "squelch")
    @Override
    public void setSquelchThreshold(int threshold)
    {
        mSquelchThreshold = threshold;
    }

    /**
     * Squelch threshold
     * @return threshold (dB)
     */
    @Override
    public int getSquelchThreshold()
    {
        return mSquelchThreshold;
    }

    public enum Bandwidth
    {
        BW_12_5("12.5 kHz", 12500.0),
        BW_25_0("25.0 kHz", 25000.0);

        private String mLabel;
        private double mValue;

        Bandwidth(String label, double value)
        {
            mLabel = label;
            mValue = value;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }

        public double getValue()
        {
            return mValue;
        }
    }
}
