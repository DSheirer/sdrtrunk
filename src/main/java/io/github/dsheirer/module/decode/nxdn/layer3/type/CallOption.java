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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Call options field base class.
 */
public abstract class CallOption extends Option
{
    private static int MASK_DUPLEX = 0x10;
    private static int MASK_TRANSMISSION_MODE = 0x02;

    /**
     * Constructs an instance
     * @param value for the field
     */
    public CallOption(int value)
    {
        super(value);
    }

    /**
     * Indicates the duplex mode for the call.
     * @return duplex mode.
     */
    public Duplex getDuplex()
    {
        return (mValue & MASK_DUPLEX) == MASK_DUPLEX ? Duplex.DUPLEX : Duplex.HALF_DUPLEX;
    }

    /**
     * Transmission mode for the call.
     * @return mode
     */
    public TransmissionMode getTransmissionMode()
    {
        return (mValue & MASK_TRANSMISSION_MODE) == MASK_TRANSMISSION_MODE ?
                TransmissionMode.M9600 : TransmissionMode.M4800;
    }
}
