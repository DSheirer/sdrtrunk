/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.decode.analog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.dsp.squelch.ISquelchConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.util.EnumSet;

/**
 * Decoder configuration for an squelching analog channel
 */
public abstract class DecodeConfigAnalog extends DecodeConfiguration implements ISquelchConfiguration
{
    private Bandwidth mBandwidth;
    private int mTalkgroup = 1;
    private int mSquelchThreshold = -78;
    private boolean mSquelchAutoTrack = true;

    /**
     * Constructs an instance
     */
    public DecodeConfigAnalog()
    {
        mBandwidth = getDefaultBandwidth();
    }

    protected abstract Bandwidth getDefaultBandwidth();

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

    /**
     * Enable or disable the squelch noise floor auto-track feature.
     * @param autoTrack true to enable.
     */
    @JacksonXmlProperty(isAttribute =  true, localName = "autoTrack")
    @Override
    public void setSquelchAutoTrack(boolean autoTrack)
    {
        mSquelchAutoTrack = autoTrack;
    }

    /**
     * Indicates if the squelch noise floor auto-track feature is enabled.
     * @return true if enabled.
     */
    @Override
    public boolean isSquelchAutoTrack()
    {
        return mSquelchAutoTrack;
    }

    public enum Bandwidth
    {
        BW_3_0("3.0 kHz", 3000.0),
        BW_5_0("5.0 kHz", 5000.0),
        BW_7_5("7.5 kHz", 7500.0),
        BW_8_33("8.33 kHz", 8333.0),
        BW_12_5("12.5 kHz", 12500.0),
        BW_15_0("15.0 kHz", 15000.0),
        BW_25_0("25.0 kHz", 25000.0);

        private String mLabel;
        private double mValue;

        /**
         * Constructs an instance
         * @param label for display
         * @param value of the entry in Hertz
         */
        Bandwidth(String label, double value)
        {
            mLabel = label;
            mValue = value;
        }

        //AM demodulator channel bandwidth options
        public static EnumSet<Bandwidth> AM_BANDWIDTHS = EnumSet.of(BW_3_0, BW_5_0, BW_8_33, BW_15_0, BW_25_0);

        //FM demodulator channel bandwidth options
        public static EnumSet<Bandwidth> FM_BANDWIDTHS = EnumSet.of(BW_7_5, BW_12_5, BW_25_0);

        /**
         * Indicates if this entry is valid for the AM decoder.
         */
        public boolean isAM()
        {
            return AM_BANDWIDTHS.contains(this);
        }

        /**
         * Indicates if this entry is valid for the FM decoder.
         */
        public boolean isFM()
        {
            return FM_BANDWIDTHS.contains(this);
        }

        /**
         * Overrides the default toString to provide a pretty label.
         */
        @Override
        public String toString()
        {
            return mLabel;
        }

        /**
         * Value of the bandwidth
         * @return value in Hertz
         */
        public double getValue()
        {
            return mValue;
        }
    }
}
