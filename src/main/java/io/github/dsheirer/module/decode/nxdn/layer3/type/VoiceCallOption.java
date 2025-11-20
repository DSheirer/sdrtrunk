/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Voice call options field.
 */
public class VoiceCallOption extends CallOption
{
    /**
     * Constructs an instance
     *
     * @param value for the field
     */
    public VoiceCallOption(int value)
    {
        super(value);
    }

    public AudioCodec getCodec()
    {
        return (mValue & 0x1) == 0x1 ? AudioCodec.FULL_RATE : AudioCodec.HALF_RATE;
    }

    @Override
    public String toString()
    {
        return getTransmissionMode() + " " + getCodec() + " " + getDuplex();
    }
}
