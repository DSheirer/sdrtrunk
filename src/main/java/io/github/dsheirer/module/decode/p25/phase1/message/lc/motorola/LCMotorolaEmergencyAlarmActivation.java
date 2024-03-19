/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Link Control Opcode 10 appears to be an Emergency Alarm Activation message.
 *
 * This was observed in the following sequence:
 * 1. Radio registers on network.
 * 2. Radio assigned group and affiliation group
 * 3. Network acknowledges radio's emergency alarm request
 * 4. Network sends this message, interspersed with the ack message (3)
 * 5. Network grants emergency group voice channel to radio and the same talkgroup references in this message, which
 *    is also the assigned talkgroup during initial registration.
 * 6. Network grants second non-emerg group voice channel to radio and a second (unknown 0x39) talkgroup that may be
 *    a supervisor talkgroup.
 *
 *  This link control message was also transmitted on active traffic channels.
 *
 * Note: the same opcode is used on both on control channel TSBK and traffic channel LCW messaging.
 */
public class LCMotorolaEmergencyAlarmActivation extends LinkControlWord
{
    private static final IntField GROUP_ADDRESS = IntField.length16(OCTET_2_BIT_16);
    //There seems to be room for another group address here, octets 4/5??
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_6_BIT_48);
    private TalkgroupIdentifier mGroupAddress;
    private RadioIdentifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaEmergencyAlarmActivation(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("**CRC-FAILED** ");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        else
        {
            sb.append("MOTOROLA EMERGENCY ALARM ACTIVATION RADIO:").append(getSourceAddress());
            sb.append(" TALKGROUP:").append(getGroupAddress());
            sb.append(" MSG:").append(getMessage().toHexString());
        }

        return sb.toString();
    }

    /**
     * Source Address that activated the emergency alarm.
     */
    public RadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createTo(getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    /**
     * Talkgroup to be activated for the emergency voice call
     */
    public TalkgroupIdentifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.create(getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    /**
     * List of identifiers contained in this message
     */

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
