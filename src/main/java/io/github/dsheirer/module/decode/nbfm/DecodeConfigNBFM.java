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
package io.github.dsheirer.module.decode.nbfm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog;
import io.github.dsheirer.module.decode.squelchDecoder.squelchDecoderConfig;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoder configuration for an NBFM channel.
 *
 * Supports channel-level CTCSS/DCS tone filtering and FM de-emphasis.
 */
public class DecodeConfigNBFM extends DecodeConfigAnalog
{
    private boolean mAudioHPFilter = true;
    private float mSquelchNoiseOpenThreshold = NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD;
    private float mSquelchNoiseCloseThreshold = NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD;
    private int mSquelchHysteresisOpenThreshold = NoiseSquelch.DEFAULT_HYSTERESIS_OPEN_THRESHOLD;
    private int mSquelchHysteresisCloseThreshold = NoiseSquelch.DEFAULT_HYSTERESIS_CLOSE_THRESHOLD;

    // Channel-level squelch filtering
    private List<squelchDecoderConfig> mSquelchFilters = new ArrayList<>();
    private boolean mSquelchFilterEnabled = false;

     // FM de-emphasis
    private DeemphasisMode mDeemphasis = DeemphasisMode.US_75US;

    /**
     * FM de-emphasis time constant options
     *
     * Commercial broadcast stations in North America and Europe use 75 us and 53 us respectively
     * But this is NBFM and reliable documentation for de-emphasis is difficult to find. There was one
     * reference on a repeater builder site that mentions 3dB @ 3 KHz, but it sounds pretty severe and weak
     * signals come through pretty muffled sounding. There is no standard found for NBFM. So included are
     * 3 known values and one to bridge the gap. A user should be able to find a personal preference from
     * the values below.
     */
    public enum DeemphasisMode
    {
        NONE("None", 0),
        CEPT_53US("53 µs (Europe/CEPT)", 53),
        US_75US("75 µs (North America)", 75),
        OTHER_166US("166 µs (Other)", 166),
        NBFM_333US("333 µs (3dB @ 3KHz)", 333);

        private final String mLabel;
        private final int mMicroseconds;

        DeemphasisMode(String label, int microseconds)
        {
            mLabel = label;
            mMicroseconds = microseconds;
        }

        public int getMicroseconds()
        {
            return mMicroseconds;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    /**
     * Constructs an instance
     */
    public DecodeConfigNBFM()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    @Override
    protected Bandwidth getDefaultBandwidth()
    {
        return Bandwidth.BW_12_5;
    }

    /**
     * Channel sample stream specification.
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        switch(getBandwidth())
        {
            case BW_7_5:
                return new ChannelSpecification(25000.0, 7500, 3500.0, 3750.0);
            case BW_12_5:
                return new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);
            case BW_25_0:
                return new ChannelSpecification(50000.0, 25000, 12500.0, 13500.0);
            default:
                throw new IllegalArgumentException("Unrecognized FM bandwidth value: " + getBandwidth());
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "audioFilter")
    public boolean isAudioFilter()
    {
        return mAudioHPFilter;
    }

    public void setAudioFilter(boolean audioFilter)
    {
        mAudioHPFilter = audioFilter;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "squelchNoiseOpenThreshold")
    public float getSquelchNoiseOpenThreshold()
    {
        return mSquelchNoiseOpenThreshold;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "squelchNoiseCloseThreshold")
    public float getSquelchNoiseCloseThreshold()
    {
        return mSquelchNoiseCloseThreshold;
    }

    public void setSquelchNoiseOpenThreshold(float open)
    {
        if(open < NoiseSquelch.MINIMUM_NOISE_THRESHOLD || open > NoiseSquelch.MAXIMUM_NOISE_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch noise open threshold is out of range: " + open);
        }
        mSquelchNoiseOpenThreshold = open;
    }

    public void setSquelchNoiseCloseThreshold(float close)
    {
        if(close < NoiseSquelch.MINIMUM_NOISE_THRESHOLD || close > NoiseSquelch.MAXIMUM_NOISE_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch noise close threshold is out of range: " + close);
        }
        mSquelchNoiseCloseThreshold = close;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "squelchHysteresisOpenThreshold")
    public int getSquelchHysteresisOpenThreshold()
    {
        return mSquelchHysteresisOpenThreshold;
    }

    public void setSquelchHysteresisOpenThreshold(int open)
    {
        if(open < NoiseSquelch.MINIMUM_HYSTERESIS_THRESHOLD || open > NoiseSquelch.MAXIMUM_HYSTERESIS_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch hysteresis open threshold is out of range: " + open);
        }
        mSquelchHysteresisOpenThreshold = open;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "squelchHysteresisCloseThreshold")
    public int getSquelchHysteresisCloseThreshold()
    {
        return mSquelchHysteresisCloseThreshold;
    }

    public void setSquelchHysteresisCloseThreshold(int close)
    {
        if(close < NoiseSquelch.MINIMUM_HYSTERESIS_THRESHOLD || close > NoiseSquelch.MAXIMUM_HYSTERESIS_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch hysteresis close threshold is out of range: " + close);
        }
        mSquelchHysteresisCloseThreshold = close;
    }

     /**
     * List of CTCSS/DCS squelch filters for this channel. When enabled, audio is only passed
     * when the received signal matches at least one of the configured codes.
     * Empty list with filtering enabled means no audio passes (muted).
     * Filtering disabled means all audio passes (backward compatible).
     */
    @JacksonXmlElementWrapper(localName = "squelchFilters")
    @JacksonXmlProperty(localName = "squelchFilter")
    public List<squelchDecoderConfig> getSquelchFilters()
    {
        return mSquelchFilters;
    }

    public void setSquelchFilters(List<squelchDecoderConfig> squelchFilters)
    {
        mSquelchFilters = squelchFilters != null ? squelchFilters : new ArrayList<>();
    }

    /**
     * Adds a tone filter to the channel configuration
     */
    public void addSquelchFilter(squelchDecoderConfig filter)
    {
        if(filter != null)
        {
            mSquelchFilters.add(filter);
        }
    }

    /**
     * Removes a squelch filter from the channel configuration
     */
    public void removeSquelchFilter(squelchDecoderConfig filter)
    {
        mSquelchFilters.remove(filter);
    }

    /**
     * Indicates if squelch filtering is enabled for this channel
     */
    @JacksonXmlProperty(isAttribute = true, localName = "squelchFilterEnabled")
    public boolean isSquelchFilterEnabled()
    {
        return mSquelchFilterEnabled;
    }

    public void setSquelchFilterEnabled(boolean enabled)
    {
        mSquelchFilterEnabled = enabled;
    }

    /**
     * Indicates if this channel has valid, enabled tone filters configured
     */
    @JsonIgnore
    public boolean hasSquelchFiltering()
    {
        return mSquelchFilterEnabled && !mSquelchFilters.isEmpty();
    }

    /**
     * FM de-emphasis mode. Standard FM broadcasting uses pre-emphasis to boost high
     * frequencies during transmission. De-emphasis restores flat frequency response
     * during receive, improving audio clarity.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "deemphasis")
    public DeemphasisMode getDeemphasis()
    {
        return mDeemphasis;
    }

    public void setDeemphasis(DeemphasisMode deemphasis)
    {
        mDeemphasis = deemphasis != null ? deemphasis : DeemphasisMode.NONE;
    }
}
