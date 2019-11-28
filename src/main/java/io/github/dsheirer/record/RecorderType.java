/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.record;

import java.util.EnumSet;

/**
 * Supported recorder types.
 */
public enum RecorderType
{
    /**
     * Audio - 8kHz PCM -- **DO NOT USE - DEPRECATED - AUDIO RECORDING IS NOW MANAGED BY ALIASES AND/OR DECODE CONFIG
     * for AM/NBFM
     */
	AUDIO( "Audio (.wav)"),

    /**
     * Baseband, 16-bit complex inphase/quadrature samples at the channel sample rate.
     */
    BASEBAND( "Baseband I/Q (.wav)"),

    /**
     * Demodulated bit stream at the baud rate of the decoder
     */
    DEMODULATED_BIT_STREAM("Demodulated Bitstream (.bits)"),

    /**
     * Traffic channel baseband, 16-bit complex inphase/quadrature samples at the channel sample rate.
     */
	TRAFFIC_BASEBAND( "Traffic Channel Baseband I/Q (.wav)" ),

    /**
     * Traffic channel demodulated bit stream at the baud rate of the decoder
     */
    TRAFFIC_DEMODULATED_BIT_STREAM("Traffic Channel Demodulated Bitstream (.bits)"),

    /**
     * MBE Audio Codec frames
     */
    MBE_CALL_SEQUENCE("MBE Audio CODEC Frames (.mbe)"),

    /**
     * Traffic channel MBE Audio Codec frames
     */
    TRAFFIC_MBE_CALL_SEQUENCE("Traffic Channel MBE Audio CODEC Frames (.mbe)");

    private String mDisplayString;

    /**
     * Recorders available to all decoders
     */
    public static final EnumSet<RecorderType> DEFAULT_RECORDER_TYPES = EnumSet.of(BASEBAND, TRAFFIC_BASEBAND);

    RecorderType( String displayString )
    {
        mDisplayString = displayString;
    }
    
    public String getDisplayString()
    {
        return mDisplayString;
    }
    
    @Override
    public String toString()
    {
    	return mDisplayString;
    }
}
