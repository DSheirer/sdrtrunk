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

package io.github.dsheirer.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

/**
 * Audio formats
 */
public class AudioFormats
{
	public static final boolean LITTLE_ENDIAN = false;

	public static final float PCM_8_KHZ_RATE = 8000;
	public static final float PCM_16_KHZ_RATE = 16000;
	public static final float PCM_22050_HZ_RATE = 22050;
	public static final float PCM_44100_HZ_RATE = 44100;

	public static final int ONE_CHANNEL = 1;
	public static final int TWO_CHANNELS = 2;
	public static final int PCM_SAMPLE_SIZE_16_BITS = 16;
	public static final int PCM_SAMPLE_SIZE_32_BITS = 32;
	public static final int PCM_FRAME_SIZE_BYTES_16_BIT_MONO = 2;
	public static final int PCM_FRAME_SIZE_BYTES_32_BIT_MONO = 4;
	public static final int PCM_FRAME_SIZE_BYTES_16_BIT_STEREO = 4;

	public static AudioFormat PCM_SIGNED_8000_HZ_32_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_8_KHZ_RATE,
					PCM_SAMPLE_SIZE_32_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_32_BIT_MONO,
					PCM_8_KHZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_8000_HZ_16_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_8_KHZ_RATE,
					PCM_SAMPLE_SIZE_16_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_16_BIT_MONO,
					PCM_8_KHZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_16000_HZ_16_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_16_KHZ_RATE,
					PCM_SAMPLE_SIZE_16_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_16_BIT_MONO,
					PCM_16_KHZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_16000_HZ_32_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_16_KHZ_RATE,
					PCM_SAMPLE_SIZE_32_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_32_BIT_MONO,
					PCM_16_KHZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_22050_HZ_16_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_22050_HZ_RATE,
					PCM_SAMPLE_SIZE_16_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_16_BIT_MONO,
					PCM_22050_HZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_22050_HZ_32_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_22050_HZ_RATE,
					PCM_SAMPLE_SIZE_32_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_32_BIT_MONO,
					PCM_22050_HZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_44100_HZ_16_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_44100_HZ_RATE,
					PCM_SAMPLE_SIZE_16_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_16_BIT_MONO,
					PCM_44100_HZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_44100_HZ_32_BIT_MONO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_44100_HZ_RATE,
					PCM_SAMPLE_SIZE_32_BITS,
					ONE_CHANNEL,
					PCM_FRAME_SIZE_BYTES_32_BIT_MONO,
					PCM_44100_HZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_8000_HZ_16BITS_STEREO = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_8_KHZ_RATE,
					PCM_SAMPLE_SIZE_16_BITS,
					TWO_CHANNELS,
					PCM_FRAME_SIZE_BYTES_16_BIT_STEREO,
					PCM_8_KHZ_RATE,
					LITTLE_ENDIAN );

	/**
	 * Source Data Line Info for a 48 kHz, 16-bits signed PCM one channel
	 */
	public static final Line.Info MONO_SOURCE_DATALINE_INFO = 
		new DataLine.Info( SourceDataLine.class, PCM_SIGNED_8000_HZ_16_BIT_MONO);
	
	/**
	 * Source Data Line Info for a 48 kHz, 16-bits signed PCM two channels
	 */
	public static final Line.Info STEREO_SOURCE_DATALINE_INFO = 
		new DataLine.Info( SourceDataLine.class, PCM_SIGNED_8000_HZ_16BITS_STEREO);
}
