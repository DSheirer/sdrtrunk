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

package io.github.dsheirer.record;

/**
 * File formats for baseband (complex I/Q sample) recordings.
 */
public enum BasebandRecordFormat
{
    /**
     * sdrtrunk wave format: 2-channel, 16-bit PCM wave file with frequency in the file name.
     */
    WAVE("sdrtrunk (.wav)", ".wav"),

    /**
     * SDRSharp raw I/Q format: headerless, interleaved 16-bit little-endian I/Q samples with the start time,
     * tuner type, center frequency and sample rate encoded in the file name.
     */
    SDRSHARP_IQ("SDRSharp (.iq)", ".iq");

    private final String mLabel;
    private final String mExtension;

    BasebandRecordFormat(String label, String extension)
    {
        mLabel = label;
        mExtension = extension;
    }

    /**
     * File extension, including the leading period.
     */
    public String getExtension()
    {
        return mExtension;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
