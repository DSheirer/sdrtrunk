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

package io.github.dsheirer.module.decode.edacs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.dsp.filter.equalizer.GraphicEqualizer;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.config.WithCallTimeout;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.util.ArrayList;
import java.util.List;

/**
 * EDACS Extended Addressing control channel configuration.
 *
 * The control channel names voice channels by logical channel number (LCN), so the configuration holds the system's
 * frequencies in LCN order (LCN 1 first), as Radio Reference lists them.
 */
public class DecodeConfigEDACS extends DecodeConfiguration implements WithCallTimeout
{
    public static final int CALL_TIMEOUT_DEFAULT_SECONDS = 180;
    public static final int CHANNEL_ROTATION_DELAY_MINIMUM_MS = 500;
    public static final int CHANNEL_ROTATION_DELAY_DEFAULT_MS = 1000;
    public static final int CHANNEL_ROTATION_DELAY_MAXIMUM_MS = 5000;

    /** What a voice (traffic) channel started by the control channel carries */
    public enum VoiceMode {CONTROL, ANALOG, PROVOICE}

    private List<Long> mLcnFrequencies = new ArrayList<>();
    private int mCallTimeoutSeconds = CALL_TIMEOUT_DEFAULT_SECONDS;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private VoiceMode mVoiceMode = VoiceMode.CONTROL;
    private boolean mIgnoreUnaliasedTalkgroups = false;
    private int mSystemKey = 0;
    private boolean mGraphicEQEnabled = false;
    private double[] mGraphicEQBandGains = new double[GraphicEqualizer.BAND_COUNT];

    public DecodeConfigEDACS()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS;
    }

    /**
     * Frequencies in Hz in LCN order: index 0 is LCN 1
     */
    @JacksonXmlElementWrapper(localName = "lcn_frequencies")
    @JacksonXmlProperty(localName = "frequency")
    public List<Long> getLcnFrequencies()
    {
        return mLcnFrequencies;
    }

    public void setLcnFrequencies(List<Long> frequencies)
    {
        mLcnFrequencies = frequencies != null ? new ArrayList<>(frequencies) : new ArrayList<>();
    }

    /**
     * Frequency of a logical channel number, or 0 when the LCN is not in the list
     */
    @JsonIgnore
    public long getFrequency(int lcn)
    {
        if(lcn >= 1 && lcn <= mLcnFrequencies.size())
        {
            Long frequency = mLcnFrequencies.get(lcn - 1);
            return frequency != null ? frequency : 0;
        }

        return 0;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "call_timeout")
    public int getCallTimeoutSeconds()
    {
        return mCallTimeoutSeconds;
    }

    public void setCallTimeoutSeconds(int timeout)
    {
        if(CALL_TIMEOUT_MINIMUM <= timeout && timeout <= CALL_TIMEOUT_MAXIMUM)
        {
            mCallTimeoutSeconds = timeout;
        }
        else
        {
            mCallTimeoutSeconds = CALL_TIMEOUT_DEFAULT_SECONDS;
        }
    }

    /**
     * When true, a call to a talkgroup that has no alias in the channel's alias list is listed but not followed.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "ignore_unaliased_talkgroups")
    public boolean getIgnoreUnaliasedTalkgroups()
    {
        return mIgnoreUnaliasedTalkgroups;
    }

    public void setIgnoreUnaliasedTalkgroups(boolean ignore)
    {
        mIgnoreUnaliasedTalkgroups = ignore;
    }

    /**
     * EDACS System Key (ESK): some systems XOR the top 8 bits of each control channel message with this value.  0 for
     * none (most systems); 0xA0 is the value in use.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "system_key")
    public int getSystemKey()
    {
        return mSystemKey;
    }

    public void setSystemKey(int systemKey)
    {
        mSystemKey = systemKey & 0xFF;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    public void setTrafficChannelPoolSize(int size)
    {
        mTrafficChannelPoolSize = size;
    }

    /**
     * Graphic equalizer (10-band, 200 Hz - 3.5 kHz) applied to voice channel audio
     */
    @JacksonXmlProperty(isAttribute = true, localName = "graphic_eq_enabled")
    public boolean isGraphicEQEnabled()
    {
        return mGraphicEQEnabled;
    }

    public void setGraphicEQEnabled(boolean enabled)
    {
        mGraphicEQEnabled = enabled;
    }

    @JacksonXmlElementWrapper(localName = "graphic_eq_band_gains_200_3500")
    @JacksonXmlProperty(localName = "gain")
    public double[] getGraphicEQBandGains()
    {
        return mGraphicEQBandGains;
    }

    public void setGraphicEQBandGains(double[] gains)
    {
        if(gains != null && gains.length > 0)
        {
            double[] copy = new double[GraphicEqualizer.BAND_COUNT];
            System.arraycopy(gains, 0, copy, 0, Math.min(gains.length, GraphicEqualizer.BAND_COUNT));
            mGraphicEQBandGains = copy;
        }
    }

    /**
     * CONTROL for the control channel; ANALOG or PROVOICE for the copy given to a voice channel (never saved)
     */
    @JsonIgnore
    public VoiceMode getVoiceMode()
    {
        return mVoiceMode;
    }

    @JsonIgnore
    public boolean isTrafficChannel()
    {
        return mVoiceMode != VoiceMode.CONTROL;
    }

    /**
     * Copy of this configuration for a voice channel carrying the given kind of call
     */
    @JsonIgnore
    public DecodeConfigEDACS copyForTrafficChannel(VoiceMode mode)
    {
        DecodeConfigEDACS copy = copy();
        copy.mVoiceMode = mode;
        return copy;
    }

    /**
     * Copy of the saved settings
     */
    @JsonIgnore
    public DecodeConfigEDACS copy()
    {
        DecodeConfigEDACS copy = new DecodeConfigEDACS();
        copy.setLcnFrequencies(getLcnFrequencies());
        copy.setCallTimeoutSeconds(getCallTimeoutSeconds());
        copy.setTrafficChannelPoolSize(getTrafficChannelPoolSize());
        copy.setIgnoreUnaliasedTalkgroups(getIgnoreUnaliasedTalkgroups());
        copy.setSystemKey(getSystemKey());
        copy.setGraphicEQEnabled(isGraphicEQEnabled());
        copy.setGraphicEQBandGains(getGraphicEQBandGains());
        return copy;
    }

    /**
     * Control channel and ProVoice voice channels: 9600 baud FSK in a 12.5 kHz filter, as recorded and tested (MBTA).
     * Analog voice channels: 25 kHz analog FM, the same filter as an NBFM channel set to 25 kHz.
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        if(mVoiceMode == VoiceMode.ANALOG)
        {
            return new ChannelSpecification(50000.0, 25000, 12500.0, 13500.0);
        }

        return new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);
    }
}
