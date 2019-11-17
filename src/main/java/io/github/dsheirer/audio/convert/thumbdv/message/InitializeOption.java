/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.audio.convert.thumbdv.message;

/**
 * AMBE-3000 Initialization Options
 */
public enum InitializeOption
{
    ENCODER((byte)0x01),
    DECODER((byte)0x02),
    ENCODER_AND_DECODER((byte)0x03),
    ECHO_CANCELLER((byte)0x04),
    ENCODER_DECODER_ECHO_CANCELLER((byte)0x07);

    private byte mCode;

    InitializeOption(byte code)
    {
        mCode = code;
    }

    public byte getCode()
    {
        return mCode;
    }
}
