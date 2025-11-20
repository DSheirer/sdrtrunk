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
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Digit;
import java.util.List;

/**
 * Voice call request message
 */
public class VoiceCallRequest extends VoiceCallWithOptionalLocation
{
    private static final int LOCATION_ID_OFFSET = OCTET_7;
    private static final int LOCATION_ID_DIALING_CALL_OFFSET = OCTET_8;
    private static final IntField SPEED_DIAL = IntField.length8(OCTET_7);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public VoiceCallRequest(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    protected int getLocationOffset()
    {
        return getCallType() == CallType.INTERCONNECT ? LOCATION_ID_DIALING_CALL_OFFSET : LOCATION_ID_OFFSET;
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

        sb.append(getCallType()).append(" CALL");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());

        switch(getCallType())
        {
            case INTERCONNECT:
                if(getCallControlOption().isSecondMessage())
                {
                    sb.append(" CONTINUATION PSTN DIGITS:").append(getDialedDigits());
                }
                else
                {
                    sb.append(" PSTN:").append(getDialedDigits());
                }
                break;
            case SPEED_DIAL:
                sb.append(" SPEED DIAL:").append(getSpeedDialNumber());
                break;
        }

        sb.append(" ").append(getEncryptionKeyIdentifier());
        sb.append(getCallOption());

        return sb.toString();
    }

    /**
     * Speed dial number ID for call type SPEED DIAL
     */
    public int getSpeedDialNumber()
    {
        return getMessage().getInt(SPEED_DIAL);
    }

    /**
     * Dialed digits when call type is INTERCONNECT.
     */
    public String getDialedDigits()
    {
        if(getCallType() == CallType.INTERCONNECT)
        {
            StringBuilder sb = new StringBuilder();

            int offset, length;

            if(getCallControlOption().isSecondMessage())
            {
                offset = OCTET_2;
                length = 32;
            }
            else if(getCallControlOption().hasLocationId())
            {
                offset = OCTET_10;
                length = 12;
            }
            else
            {
                offset = OCTET_7;
                length = 18;
            }

            Digit digit;

            for(int x = 0; x < length; x++)
            {
                digit = Digit.fromValue(getMessage().getInt(offset, offset + 3));

                if(digit == Digit.FILLER)
                {
                    break;
                }
                else
                {
                    sb.append(digit);
                }

                offset += 4;
            }

            return sb.toString();
        }

        return "";
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}
