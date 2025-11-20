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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import org.jspecify.annotations.NonNull;

/**
 * Packet information field.
 *
 * @param message containing the packet information field
 * @param offset to the start of the packet information field
 * @param hasTxFragmentCount indicates if the field is 2 or 3 octets long and has (true) TX fragment count or not (false).
 */
public record PacketInformation(CorrectedBinaryMessage message, int offset, boolean hasTxFragmentCount)
{
    private static final int DELIVERY_FLAG = 0;
    private static final int SELECTIVE_RETRY_FLAG = 2;
    private static final IntField BLOCK_COUNT = IntField.length4(4);
    private static final IntField PAD_OCTET_COUNT = IntField.length4(8);
    private static final int START_FRAGMENT_FLAG = 13;
    private static final int CIRCULAR_FRAGMENT_FLAG = 14;
    private static final IntField TX_FRAGMENT_COUNT = IntField.length9(15);

    @NonNull
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getDelivery()).append(" DELIVERY");
        if(isSelectiveRetry())
        {
            sb.append(" SELECTIVE RETRY");
        }
        sb.append(" BLOCKS:").append(getBlockCount());
        sb.append(" PAD OCTETS:").append(getPadOctetCount());
        if(isFirstFragment())
        {
            sb.append(" FIRST FRAGMENT");
        }
        else
        {
            sb.append(" FRAGMENTS");
        }

        if(hasTxFragmentCount)
        {
            sb.append(" REMAINING:").append(getTXFragmentCount());
        }

        if(isCircularFragmentCount())
        {
            sb.append(" OF MANY");
        }

        return sb.toString();
    }

    /**
     * Packet delivery confirmation
     */
    public Delivery getDelivery()
    {
        return message.get(DELIVERY_FLAG + offset) ? Delivery.CONFIRMED : Delivery.UNCONFIRMED;
    }

    /**
     * Indicates if this is a selective retry (true) or normal (false) transmission packet.
     */
    public boolean isSelectiveRetry()
    {
        return message.get(SELECTIVE_RETRY_FLAG + offset);
    }

    /**
     * User data block count (to follow)
     */
    public int getBlockCount()
    {
        return message.getInt(BLOCK_COUNT, offset);
    }

    /**
     * Count of pad octets added to the message to round up to the nearest block size.
     */
    public int getPadOctetCount()
    {
        return message.getInt(PAD_OCTET_COUNT, offset);
    }

    /**
     * Indicates if this is the first fragment (true) or not first fragment (false).
     */
    public boolean isFirstFragment()
    {
        return message.get(START_FRAGMENT_FLAG + offset);
    }

    /**
     * Indicates if the TX fragment count circulates (true) or not (false).
     */
    public boolean isCircularFragmentCount()
    {
        return message.get(CIRCULAR_FRAGMENT_FLAG + offset);
    }

    /**
     * Fragment sequence count.
     *
     * Circular Fragment Count=FALSE: value is total fragments minus one and last fragment will be zero
     * Circular Fragment Count=TRUE: value is 15 in first fragment, and then decremented mod 9 each fragment after
     */
    public int getTXFragmentCount()
    {
        return message.getInt(TX_FRAGMENT_COUNT, offset);
    }
}
