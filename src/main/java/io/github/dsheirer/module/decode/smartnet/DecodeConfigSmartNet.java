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

package io.github.dsheirer.module.decode.smartnet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.dsp.filter.equalizer.GraphicEqualizer;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.config.WithCallTimeout;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * Motorola Type II (SmartNet / SmartZone) control channel configuration.
 */
public class DecodeConfigSmartNet extends DecodeConfiguration implements WithCallTimeout
{
    public static final int CALL_TIMEOUT_DEFAULT_SECONDS = 180;
    public static final int CHANNEL_ROTATION_DELAY_MINIMUM_MS = 500;
    public static final int CHANNEL_ROTATION_DELAY_DEFAULT_MS = 1000;
    public static final int CHANNEL_ROTATION_DELAY_MAXIMUM_MS = 5000;

    public static final int SITE_ANY = 0;
    public static final int SITE_MAXIMUM = 64;
    private SmartNetBandPlan mBandPlan = SmartNetBandPlan.BAND_800_REBANDED;
    private int mCallTimeoutSeconds = CALL_TIMEOUT_DEFAULT_SECONDS;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private boolean mTrafficChannel = false;
    private boolean mDigital = false;
    private boolean mIgnoreUnaliasedTalkgroups = false;
    private int mSite = SITE_ANY;
    private boolean mGraphicEQEnabled = false;
    private double[] mGraphicEQBandGains = new double[GraphicEqualizer.BAND_COUNT];

    public DecodeConfigSmartNet()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.SMARTNET;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "band_plan")
    public SmartNetBandPlan getBandPlan()
    {
        return mBandPlan;
    }

    public void setBandPlan(SmartNetBandPlan bandPlan)
    {
        mBandPlan = bandPlan != null ? bandPlan : SmartNetBandPlan.BAND_800_REBANDED;
    }

    /**
     * Longest a followed call may last.  Calls normally end when the control channel stops announcing them; this is
     * only the backstop.
     */
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
     * SmartZone site number (1-64) this channel is for, or SITE_ANY (0).  When set, calls are followed only while the
     * control channel announces this site, and a control channel of another site on the same frequency is left.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "site")
    public int getSite()
    {
        return mSite;
    }

    public void setSite(int site)
    {
        mSite = (SITE_ANY <= site && site <= SITE_MAXIMUM) ? site : SITE_ANY;
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
     * Graphic equalizer (the same 10-band 200 Hz - 3.5 kHz equalizer as P25) applied to voice channel audio
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

    /**
     * @param gains in dB (-12 to +12), one per band; a shorter array is padded with 0 dB
     */
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
     * True for the copy given to the voice channels the control channel starts (never saved).
     */
    @JsonIgnore
    public boolean isTrafficChannel()
    {
        return mTrafficChannel;
    }

    /**
     * Copy of this configuration for a voice (traffic) channel
     */
    @JsonIgnore
    public DecodeConfigSmartNet copyForTrafficChannel()
    {
        return copyForTrafficChannel(false);
    }

    /**
     * True for the voice channel of a digital call: P25 Phase 1 (IMBE) voice instead of analog FM (never saved).
     */
    @JsonIgnore
    public boolean isDigital()
    {
        return mDigital;
    }

    /**
     * Copy of this configuration for a voice (traffic) channel
     * @param digital true for a call granted as digital (P25 Phase 1 voice), false for analog
     */
    @JsonIgnore
    public DecodeConfigSmartNet copyForTrafficChannel(boolean digital)
    {
        DecodeConfigSmartNet copy = new DecodeConfigSmartNet();
        copy.setBandPlan(getBandPlan());
        copy.setCallTimeoutSeconds(getCallTimeoutSeconds());
        copy.setTrafficChannelPoolSize(getTrafficChannelPoolSize());
        copy.setGraphicEQEnabled(isGraphicEQEnabled());
        copy.setGraphicEQBandGains(getGraphicEQBandGains());
        copy.setIgnoreUnaliasedTalkgroups(getIgnoreUnaliasedTalkgroups());
        copy.setSite(getSite());
        copy.mTrafficChannel = true;
        copy.mDigital = digital;
        return copy;
    }

    /**
     * Control channel: 12.5 kHz, as recorded and tested (MSP Metro Boston).  Voice channels: 25 kHz analog FM with
     * 5 kHz deviation, the same filter as an NBFM channel set to 25 kHz.
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        if(mTrafficChannel && mDigital)
        {
            return new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0); //as a P25 Phase 1 channel
        }

        if(mTrafficChannel)
        {
            return new ChannelSpecification(50000.0, 25000, 12500.0, 13500.0);
        }

        return new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);
    }
}
