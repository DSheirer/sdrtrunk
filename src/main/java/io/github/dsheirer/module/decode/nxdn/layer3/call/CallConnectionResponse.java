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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.VoiceCallOption;
import java.util.List;

/**
 * Type-D call connection response to respond to an SU call connection across sites.
 */
public class CallConnectionResponse extends CallWithOptionalLocation
{
    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public CallConnectionResponse(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("CALL CONNECTION RESPONSE FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());

        if(getEncryptionKeyIdentifier().isEncrypted())
        {
            sb.append(" ").append(getEncryptionKeyIdentifier());
        }

        return sb.toString();
    }

    @Override
    protected int getLocationOffset()
    {
        return OCTET_8;
    }

    @Override
    public CallOption getCallOption()
    {
        return new VoiceCallOption(0);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination());
    }
}
