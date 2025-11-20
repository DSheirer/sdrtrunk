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

package io.github.dsheirer.module.decode.nxdn.layer3.typed;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CipherType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TypeDCallOption;

/**
 * Information 4 message
 */
public abstract class Information1 extends SCCH
{
    protected static final IntField CALL_OPTION = IntField.length3(13);
    protected static final IntField CIPHER_TYPE = IntField.length2(16);
    protected static final IntField KEY_ID_OR_INIT_VECTOR = IntField.length6(18);
    protected static final int FLAG_IV_TYPE = 24;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public Information1(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Cipher type
     */
    public CipherType getCipherType()
    {
        return CipherType.fromValue(getMessage().getInt(CIPHER_TYPE));
    }

    /**
     * Call option
     * @return
     */
    public TypeDCallOption getCallOption()
    {
        return new TypeDCallOption(getMessage().getInt(CALL_OPTION));
    }

    /**
     * Static method to parse the IV Type bit flag from the message
     * @param message with flag
     * @return value
     */
    public static boolean getIVType(CorrectedBinaryMessage message)
    {
        return message.get(FLAG_IV_TYPE);
    }

    /**
     * Utility method to identify the information 4 message type
     * @return type
     */
    public static NXDNMessageType getMessageType(CorrectedBinaryMessage message, LICH lich)
    {
        if(getIVType(message))
        {
            return lich.isOutbound() ? NXDNMessageType.SCCH_OUT_INFO_1_INITIALIZATION_VECTOR_PART1 :
                    NXDNMessageType.SCCH_IN_INFO_1_INITIALIZATION_VECTOR_PART1;
        }

        return lich.isOutbound() ? NXDNMessageType.SCCH_OUT_INFO_1_CALL_INFO : NXDNMessageType.SCCH_IN_INFO_1_CALL_INFO;
    }
}
