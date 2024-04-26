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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris Opcode 10 (0x0A) - This seems to be a 'return to control channel' or 'private data call paging'.
 * <p>
 * Observed on a phase 1 traffic data channel carried by a TDU during an SNDCP packet data session.  The controller sent
 * an 'All blocks received' PDU to the radio and an 'Activate TDS context' in the middle of a stream of these TDU/LC
 * messages, and then the radio terminated the traffic channel.  So, this may be some form of return to control
 * channel or maybe a private data call paging where the mobile returns to the control channel to receive the private
 * data channel grant for another data call
 *
 * When the radio returned to the control channel, control sent the following two MAC messages on the Phase 2 control:
 * LOCCH-U NAC:9/x009 SIGNAL CUSTOM/UNKNOWN VENDOR:HARRIS ID:A4 OPCODE:160 LENGTH:9 MSG:A0A409AC0312014871     (radio 0x014871 go to data channel 0x0312??)
 * LOCCH-U NAC:9/x009 SIGNAL CUSTOM/UNKNOWN VENDOR:HARRIS ID:A4 OPCODE:172 LENGTH:12 MSG:ACA40C000312014871980418 (from 0x014871 to 0x980418 unit-2-unit data channel grant?)
 *
 * Both messages seem to refer to channel 0-786 (0x0312) so this may be a unit-2-unit private Phase 1 call or maybe
 * a private data call. Radio addresses: 0x014871 and 0x980418
 *
 * In a second observation (Rockwall, TX), the control channel used the SNDCP data channel grant to send the
 * mobile to the data channel and then the data channel transmitted a sequence of TDULCs containing only this message.
 *
 */
public class LCHarrisReturnToControlChannel extends LinkControlWord
{
    private static final IntField UNKNOWN_1 = IntField.length8(OCTET_2_BIT_16);
    private static final IntField SOURCE_RADIO = IntField.length24(OCTET_3_BIT_24);
    private static final IntField TARGET_RADIO = IntField.length24(OCTET_6_BIT_48);
    private RadioIdentifier mSourceRadio;
    private RadioIdentifier mTargetRadio;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCHarrisReturnToControlChannel(CorrectedBinaryMessage message)
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
            sb.append("L3HARRIS RETURN TO CONTROL CHANNEL OR PRIVATE DATA CALL PAGING");
            sb.append(" FM:").append(getSourceRadio());
            sb.append(" TO:").append(getTargetRadio());
            sb.append(" UNK:").append(getUnknown());
            sb.append(" MSG:").append(getMessage().toHexString());
        }
        return sb.toString();
    }

    public String getUnknown()
    {
        return getMessage().getHex(UNKNOWN_1);
    }

    /**
     * Source radio identifier
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = APCO25RadioIdentifier.createFrom(getInt(SOURCE_RADIO));
        }

        return mSourceRadio;
    }

    /**
     * Target radio identifier
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = APCO25RadioIdentifier.createTo(getInt(TARGET_RADIO));
        }

        return mTargetRadio;
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
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
