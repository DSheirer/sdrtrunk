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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.Collections;
import java.util.List;

/**
 * Base user data message
 */
public abstract class UserData extends NXDNLayer3Message
{
    private static final IntField PACKET_FRAME_NUMBER = IntField.length4(OCTET_1);
    private static final IntField BLOCK_NUMBER = IntField.length4(OCTET_1 + 4);
    private static final int OFFSET_USER_DATA = OCTET_2;

    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public UserData(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * User data payload from this message.
     * @param lengthOctets in octets of the user data element.
     * @return extracted user data payload.
     */
    public BinaryMessage getUserData(int lengthOctets)
    {
        return getMessage().get(OFFSET_USER_DATA, OFFSET_USER_DATA + (lengthOctets * 8));
    }

    /**
     * Packet number
     */
    public int getPacketFrameNumber()
    {
        return getMessage().getInt(PACKET_FRAME_NUMBER);
    }

    /**
     * Block number
     */
    public int getBlockNumber()
    {
        return getMessage().getInt(BLOCK_NUMBER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
