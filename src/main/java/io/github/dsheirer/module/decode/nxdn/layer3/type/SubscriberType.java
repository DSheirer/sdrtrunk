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
 * Subscriber type
 */
public class SubscriberType
{
    private static final int MASK_CLASSIFICATION = 0xC000;
    private static final int MASK_TX_POWER = 0x3800;
    private static final int MASK_ACCESS = 0x0700;
    private static final int MASK_BIT_RATE = 0x00E0;
    private static final int MASK_CODEC = 0x0018;
    private static final int MASK_SPARE = 0x0007;

    private int mValue;

    /**
     * Constructs an instance
     * @param value of the 16-bit field.
     */
    public SubscriberType(int value)
    {
        mValue = value;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getType()).append(" SUBSCRIBER-").append(getAccess());
        sb.append(" SUPPORTS:").append(getBitRate());
        return sb.toString();
    }

    /**
     * Indicates the type of subscriber
     * @return FIXED or MOBILE
     */
    public String getType()
    {
        return (mValue & MASK_CLASSIFICATION) == MASK_CLASSIFICATION ? "FIXED" : "MOBILE";
    }

    public String getAccess()
    {
        return ((mValue & MASK_ACCESS) == MASK_ACCESS) ? "DUPLEX" : "SIMPLEX";
    }

    public String getBitRate()
    {
        return switch(mValue & MASK_BIT_RATE)
        {
            case 0x20 -> "4800";
            case 0x40 -> "9600";
            default -> "4800/9600";
        };
    }
}
