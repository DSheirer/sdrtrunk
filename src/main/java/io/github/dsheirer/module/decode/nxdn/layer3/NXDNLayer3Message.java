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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;

/**
 * Base NXDN layer 3 message implementation
 */
public abstract class NXDNLayer3Message extends NXDNMessage
{
    protected static int FLAG_1_INDEX = 0;
    protected static int FLAG_2_INDEX = 1;
    protected static final IntField MESSAGE_TYPE = IntField.length6(OCTET_0 + 2);
    protected static final IntField IDENTIFIER_OCTET_3 = IntField.length16(OCTET_3);
    protected static final IntField IDENTIFIER_OCTET_5 = IntField.length16(OCTET_5);
    private final NXDNMessageType mType;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public NXDNLayer3Message(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, ran, lich);
        mType = type;
    }

    /**
     * Identifies the message type value from the binary message.
     *
     * @param message with message type value.
     * @return value.
     */
    public static int getTypeValue(CorrectedBinaryMessage message)
    {
        return message.getInt(MESSAGE_TYPE);
    }

    /**
     * Message type
     *
     * @return message type
     */
    public NXDNMessageType getMessageType()
    {
        return mType;
    }

}
