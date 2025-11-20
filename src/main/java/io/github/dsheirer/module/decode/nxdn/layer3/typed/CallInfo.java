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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNEncryptionKey;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.List;

/**
 * Call information
 */
public class CallInfo extends Information1
{
    private NXDNEncryptionKey mEncryptionKey;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public CallInfo(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append(getCallOption());
        sb.append(" INFO FREE REPEATER 1:").append(getRepeater());
        sb.append(" 2:").append(getRepeater2());
        return sb.toString();
    }

    /**
     * Encryption for the call
     */
    public NXDNEncryptionKey getEncryptionKey()
    {
        if(mEncryptionKey == null)
        {
            mEncryptionKey = NXDNEncryptionKey.create(getMessage().getInt(CIPHER_TYPE),
                    getMessage().getInt(KEY_ID_OR_INIT_VECTOR));
        }

        return mEncryptionKey;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
