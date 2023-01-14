/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.audio.convert;

import io.github.dsheirer.audio.AudioFormats;
import java.util.EnumSet;

import javax.sound.sampled.AudioFormat;

/**
 * Enumeration of MP3 input audio formats for mono audio.
 */
public enum InputAudioFormat
{
    SR_8000(AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO, "16-Bit 8000 Hz (no resample)"),
    SR_16000(AudioFormats.PCM_SIGNED_16000_HZ_16_BIT_MONO, "16-Bit 16000 Hz (default)"),
    SR_22050(AudioFormats.PCM_SIGNED_22050_HZ_16_BIT_MONO, "16-Bit 22050 Hz"),
    SR_44100(AudioFormats.PCM_SIGNED_44100_HZ_16_BIT_MONO, "16-Bit 44100 Hz"),

    SR_32_8000(AudioFormats.PCM_SIGNED_8000_HZ_32_BIT_MONO, "32-Bit 8000 Hz (no resample)"),
    SR_32_16000(AudioFormats.PCM_SIGNED_16000_HZ_32_BIT_MONO, "32-Bit 16000 Hz"),
    SR_32_22050(AudioFormats.PCM_SIGNED_22050_HZ_32_BIT_MONO, "32-Bit 22050 Hz"),
    SR_32_44100(AudioFormats.PCM_SIGNED_44100_HZ_32_BIT_MONO, "32-Bit 44100 Hz");

    private AudioFormat mAudioFormat;
    private String mLabel;

    /**
     * Set that includes sample rates of 8 or 16 kHz
     */
    public static final EnumSet<InputAudioFormat> SAMPLE_RATES_8_16 = EnumSet.of(SR_8000, SR_32_8000, SR_16000, SR_32_16000);

    /**
     * Set that includes sample rates of 8, 16, or 22 kHz
     */
    public static final EnumSet<InputAudioFormat> SAMPLE_RATES_8_16_22 = EnumSet.of(SR_8000, SR_32_8000, SR_16000,
            SR_32_16000, SR_22050, SR_32_22050);

    /**
     * Constructs an instance
     * @param audioFormat for the specified sample rate
     */
    InputAudioFormat(AudioFormat audioFormat, String label)
    {
        mAudioFormat = audioFormat;
        mLabel = label;
    }

    /**
     * Default sample rate
     */
    public static InputAudioFormat getDefault()
    {
        return SR_16000;
    }

    /**
     * Audio format for the specified sample rate entry
     */
    public AudioFormat getAudioFormat()
    {
        return mAudioFormat;
    }

    /**
     * Sample rate for the format
     */
    public double getSampleRate()
    {
        return getAudioFormat().getSampleRate();
    }


    @Override
    public String toString()
    {
        return mLabel;
    }
}
