/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.reference;

/**
 * Capability bitmap parser for FDMA/TDMA capabilities
 */
public class Capability
{
    private static int VALID_FLAG = 0x40;
    private static int TDMA_HALF_RATE_2_SLOT_FLAG = 0x20;
    private static int TDMA_HALF_RATE_4_SLOT_FLAG = 0x10;
    private static int FDMA_HALF_RATE_FLAG = 0x08;
    private static int TDMA_8PSK_FLAG = 0x04;

    private int mCapability;

    /**
     * Constructs a capability bitmap parser
     * @param capability
     */
    public Capability(int capability)
    {
        mCapability = capability;
    }

    /**
     * Indicates that the bitmap contains valid values.  This is an all or nothing field.
     */
    public boolean isValid()
    {
        return (mCapability & VALID_FLAG) == VALID_FLAG;
    }

    /**
     * Indicates the device supports 2-slot TDMA and the Half-Rate vocoder
     */
    public boolean isTDMAHalfRate2Slot()
    {
        return isValid() && (mCapability & TDMA_HALF_RATE_2_SLOT_FLAG) == TDMA_HALF_RATE_2_SLOT_FLAG;
    }

    /**
     * Indicates the device supports 4-slot TDMA and the Half-Rate vocoder
     */
    public boolean isTDMAHalfRate4Slot()
    {
        return isValid() && (mCapability & TDMA_HALF_RATE_4_SLOT_FLAG) == TDMA_HALF_RATE_4_SLOT_FLAG;
    }

    /**
     * Indicates the device supports the FDMA Half-Rate vocoder (in addition to the standard Full-Rate)
     */
    public boolean isFDMAHalfRate()
    {
        return isValid() && (mCapability & FDMA_HALF_RATE_FLAG) == FDMA_HALF_RATE_FLAG;
    }

    /**
     * Indicates the device supports TDMA with 8PSK modulation
     */
    public boolean isTDMA8PSK()
    {
        return isValid() && (mCapability & TDMA_8PSK_FLAG) == TDMA_8PSK_FLAG;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("FDMA/FULL-RATE");

        if(isFDMAHalfRate())
        {
            sb.append("/HALF-RATE");
        }

        if(isTDMAHalfRate2Slot() || isTDMAHalfRate4Slot() || isTDMA8PSK())
        {
            sb.append(" TDMA");

            if(isTDMAHalfRate2Slot() || isTDMAHalfRate4Slot())
            {
                sb.append("/HALF-RATE");
            }

            if(isTDMAHalfRate2Slot())
            {
                sb.append("/2-SLOT");
            }

            if(isTDMAHalfRate4Slot())
            {
                sb.append("/4-SLOT");
            }

            if(isTDMA8PSK())
            {
                sb.append("/8PSK");
            }
        }

        return sb.toString();
    }
}
