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
    private boolean mAudioALC = false;
    private float mSquelchNoiseOpenThreshold = NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD;
    private float mSquelchNoiseCloseThreshold = NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD;
    private int mSquelchHysteresisOpenThreshold = NoiseSquelch.DEFAULT_HYSTERESIS_OPEN_THRESHOLD;
    private int mSquelchHysteresisCloseThreshold = NoiseSquelch.DEFAULT_HYSTERESIS_CLOSE_THRESHOLD;

    // Channel-level squelch filtering
    private List<squelchDecoderConfig> mSquelchDecoders = new ArrayList<>();

     // FM de-emphasis
    private DeemphasisMode mDeemphasis = DeemphasisMode.NONE;

    /**
     * FM de-emphasis
     *
     * Per TIA-603-E, all NBFM use a -6 dB per octave roll off from 300 Hz to 3000 Hz.
     * It also specifies an additional -12 dB above 2500 (not implemented to save on filter passes, the resampler
     * takes care of a lot of that), and an additional -6 dB below 500 (not implemented to save on filter passes, the
     * existing high pass filter takes care of most of that). European standard has same specifications (unlike
     * commercial FM, which the search engines struggle with).
     */
    public enum DeemphasisMode
    {
        NONE("None", 0),
        //OTHER_166US("166 µs (Other)", 6024),
        NBFM_300("-6dB/octave @ 300-3KHz", 300);

        private final String mLabel;
        private final int mCutoff;

        DeemphasisMode(String label, int cutoffFreq)
        {
            mLabel = label;
            mCutoff = cutoffFreq;
        }

        public int getCutoff()
        {
            return mCutoff;
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

    /**
     * Indicates if the user wants the demodulated audio to be high-pass filtered.
     * @return enable status, defaults to true.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "audioHPFilter")
    public boolean isAudioFilter()
    {
        return mAudioHPFilter;
    }

    /**
     * Sets the enabled state of high-pass filtering of the demodulated audio.
     * @param audioHPFilter to true to enable high-pass filtering.
     */
    public void setAudioFilter(boolean audioHPFilter)
    {
        mAudioHPFilter = audioHPFilter;
    }


    /**
     * Indicates if the user wants automatic level control
     * @return enable status, defaults to false;
     */
    @JacksonXmlProperty(isAttribute = true, localName = "audioALC")
    public boolean isAudioALC()
    {
        return mAudioALC;
    }

    /**
     * Sets the enabled state of high-pass filtering of the demodulated audio.
     * @param audioALC set to enabled automatic level control (ALC)
     */
    public void setAudioALC(boolean audioALC)
    {
        mAudioALC = audioALC;
    }

    /**
     * Squelch noise open threshold in the range 0.0 to 1.0 with a default of 0.1
     * @return noise open threshold
     */
    @JacksonXmlProperty(isAttribute = true, localName = "squelchNoiseOpenThreshold")
    public float getSquelchNoiseOpenThreshold()
    {
        return mSquelchNoiseOpenThreshold;
    }

    /**
     * Sets the squelch noise threshold.
     * @param open in range 0.0 to 1.0 with a default of 0.1
     */
    public void setSquelchNoiseOpenThreshold(float open)
    {
        if(open < NoiseSquelch.MINIMUM_NOISE_THRESHOLD || open > NoiseSquelch.MAXIMUM_NOISE_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch noise open threshold is out of range: " + open);
        }
        mSquelchNoiseOpenThreshold = open;
    }

    /**
     * Squelch noise close threshold in the range 0.0 to 1.0, greater than or equal to open threshold, with a default of 0.2
     * @return noise close threshold
     */
    @JacksonXmlProperty(isAttribute = true, localName = "squelchNoiseCloseThreshold")
    public float getSquelchNoiseCloseThreshold()
    {
        return mSquelchNoiseCloseThreshold;
    }

    /**
     * Sets the squelch noise close threshold.
     * @param close in range 0.0 to 1.0 and greater than or equal to open, with a default of 0.1
     */
    public void setSquelchNoiseCloseThreshold(float close)
    {
        if(close < NoiseSquelch.MINIMUM_NOISE_THRESHOLD || close > NoiseSquelch.MAXIMUM_NOISE_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch noise close threshold is out of range: " + close);
        }
        mSquelchNoiseCloseThreshold = close;
    }

    /**
     * Squelch hysteresis open threshold in range 1-10 with a default of 4.
     * @return hysteresis open threshold
     */
    @JacksonXmlProperty(isAttribute = true, localName = "squelchHysteresisOpenThreshold")
    public int getSquelchHysteresisOpenThreshold()
    {
        return mSquelchHysteresisOpenThreshold;
    }

    /**
     * Sets the squelch time threshold in the range 1-10.
     * @param open threshold
     */
    public void setSquelchHysteresisOpenThreshold(int open)
    {
        if(open < NoiseSquelch.MINIMUM_HYSTERESIS_THRESHOLD || open > NoiseSquelch.MAXIMUM_HYSTERESIS_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch hysteresis open threshold is out of range: " + open);
        }
        mSquelchHysteresisOpenThreshold = open;
    }

    /**
     * Squelch hysteresis close threshold in range 1-10 with a default of 4.
     * @return hysteresis close threshold
     */
    @JacksonXmlProperty(isAttribute = true, localName = "squelchHysteresisCloseThreshold")
    public int getSquelchHysteresisCloseThreshold()
    {
        return mSquelchHysteresisCloseThreshold;
    }

    /**
     * Sets the squelch close threshold in the range 1-10.
     * @param close threshold
     */
    public void setSquelchHysteresisCloseThreshold(int close)
    {
        if(close < NoiseSquelch.MINIMUM_HYSTERESIS_THRESHOLD || close > NoiseSquelch.MAXIMUM_HYSTERESIS_THRESHOLD)
        {
            throw new IllegalArgumentException("Squelch hysteresis close threshold is out of range: " + close);
        }
        mSquelchHysteresisCloseThreshold = close;
    }

     /**
     * List of CTCSS/DCS squelch decoders for this channel.
     */
    @JacksonXmlElementWrapper(localName = "squelchDecoders")
    @JacksonXmlProperty(localName = "squelchDecoder")
    public List<squelchDecoderConfig> getSquelchDecoders()
    {
        return mSquelchDecoders;
    }

    public void setSquelchDecoders(List<squelchDecoderConfig> squelchDecoders)
    {
        mSquelchDecoders = squelchDecoders != null ? squelchDecoders : new ArrayList<>();
    }

    /**
     * Adds a squelch decoder to the channel configuration
     */
    public void addSquelchDecoder(squelchDecoderConfig decoder)
    {
        if(decoder != null)
        {
            mSquelchDecoders.add(decoder);

        }
    }

    /**
     * Removes a squelch filter from the channel configuration
     */
    public void removeSquelchDecoder(squelchDecoderConfig decoder)
    {
        mSquelchDecoders.remove(decoder);
    }


    /**
     * Indicates if squelch filtering is enabled for this channel
     */
    @JsonIgnore
    public boolean isSquelchDecoderEnabled()
    {
        List<squelchDecoderConfig> decoders = getSquelchDecoders();
        // TODO right now only looking at first and only decoder, need to fix when multiple decoders are possible
        return !decoders.isEmpty() && decoders.getFirst().getSquelchType() != squelchDecoderConfig.SquelchType.NONE;
    }

    /**
     * FM de-emphasis mode. Standard FM broadcasting uses pre-emphasis to boost high
     * frequencies during transmission. De-emphasis restores flat frequency response
     * during receive, improving audio clarity.
     * TIA-603-E is the US standard for NBFM de-emphasis.
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
