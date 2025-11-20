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
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseVD;
import java.util.List;

/**
 * Voice call reception response
 */
public class VoiceCallReceptionResponse extends VoiceCallWithOptionalLocation
{
    private static final IntField CAUSE_VD = IntField.length8(OCTET_7);
    private static final int LOCATION_ID_OFFSET = OCTET_8;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public VoiceCallReceptionResponse(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    protected int getLocationOffset()
    {
        return LOCATION_ID_OFFSET;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        CauseVD cause = getCause();

        if(cause == CauseVD.ACCEPTED_NORMAL)
        {
            sb.append(getCallType()).append(" VOICE CALL RECEPTION ACCEPTED");
        }
        else
        {
            sb.append(getCallType()).append(" VOICE CALL RECEPTION FAIL:").append(cause);

        }
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getEncryptionKeyIdentifier());
        sb.append(getCallOption());
        return sb.toString();
    }

    /**
     * Amplifying cause for the response.
     */
    public CauseVD getCause()
    {
        return CauseVD.fromValue(getMessage().getInt(CAUSE_VD));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
