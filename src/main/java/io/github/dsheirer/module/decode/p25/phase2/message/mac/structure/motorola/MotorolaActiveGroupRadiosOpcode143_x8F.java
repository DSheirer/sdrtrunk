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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Group Radios Active Feature.  Observed on NM-DTRS Phase 2 network.  My theory is that this lists the
 * subset of talkgroup member radios that are currently active on the traffic channel.
 *
 * Opcodes 130 and 143 are almost the same, except 143 is mostly transmitted during an ACTIVE call state and 130 is
 * mostly transmitted during the HANGTIME state.  I say mostly, because there are examples where both opcodes are seen
 * in both channel states.
 *
 * Curiously: opcode 130 is in the middle of the message number sequence where GROUP REGROUP messages are allocated.
 *
 * Opcode 143 includes an additional 1-byte status field that seems to contain a 2-bit sequence counter that mostly
 * cycles through all values, but sometimes it skips values or just toggles between two of the observed values.
 * Examples: 0x00, 0x40, 0x80, 0xC0
 * There doesn't seem to be any correlation between the value of this nibble and the radio IDs being transmitted.
 *
 * The lower nibble of this 1-byte field seems to have an additional flag bit that is sometimes transmitted:
 * Examples: 0x08, 0x88
 *
 * Both opcode 130 and 143 employ up to 2x 1-byte fields that always transmit a 0x09 value.  The second field is only
 * present when the message is sending 3 or 4 radio IDs.
 *
 * It seems that the complete list of active member radios is transmitted in a fixed sequence with zero to four of those
 * radios transmitted per message.  The messages are transmitted when the talkgroup is first allocated to the channel
 * and eventually the 130/143 messages stop listing radios and the empty 143 and the 191 messages are intermittently
 * sent.  Occasionally there are radios listed in the message with all zeros values.
 *
 * Opcode 143 is transmitted with just the status byte and no radio IDs after the full sequence of radios has been
 * sent and are no longer being sent.
 *
 * I'm not sure what Opcode 191 indicates.  It's always the same (3-bytes) value: BF9003.  It may be some kind of
 * feature marker to let the listening radios/devices know that the feature is active on that traffic channel.
 *
 * These three opcodes seem to be used for this feature.
 * Opcode 130/x82 - talkgroup member radios currently active - mostly transmitted during HANGTIME
 * Opcode 143/x8F - talkgroup member radios currently active - mostly transmitted during ACTIVE call state
 * Opcode 191/xBF - transmitted continuously while channel is allocated to a talkgroup - fixed value: 0xBF9003
 */
public class MotorolaActiveGroupRadiosOpcode143_x8F extends MacStructureVendor
{
    private static final IntField STATUS = IntField.length8(OCTET_4_BIT_24);
    private static final IntField SEPARATOR_1 = IntField.length8(OCTET_5_BIT_32);
    private static final IntField RADIO_1 = IntField.length24(OCTET_6_BIT_40);
    private static final IntField RADIO_2 = IntField.length24(OCTET_9_BIT_64);
    private static final IntField SEPARATOR_2 = IntField.length8(OCTET_12_BIT_88);
    private static final IntField RADIO_3 = IntField.length24(OCTET_13_BIT_96);
    private static final IntField RADIO_4 = IntField.length24(OCTET_16_BIT_120);
    private RadioIdentifier mRadio1;
    private RadioIdentifier mRadio2;
    private RadioIdentifier mRadio3;
    private RadioIdentifier mRadio4;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaActiveGroupRadiosOpcode143_x8F(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA 143 ACTIVE GROUP RADIOS: ");

        if(hasRadio1())
        {
            sb.append(getRadio1());

            if(hasRadio2())
            {
                sb.append(", ").append(getRadio2());

                if(hasRadio3())
                {
                    sb.append(", ").append(getRadio3());

                    if(hasRadio4())
                    {
                        sb.append(", ").append(getRadio4());
                    }
                }
            }
        }
        else
        {
            sb.append("NONE");
        }

        sb.append(" STATUS:").append(getStatusCode());

        CorrectedBinaryMessage cbm = getMessage().getSubMessage(getOffset(), getOffset() + (getLength() * 8));
        sb.append(" MSG:").append(cbm.toHexString());

        return sb.toString();
    }

    /**
     * Unknown status byte.  First two bits seem to be a cycle counter 0x00, 0x40, 0x80, 0xC0.  Second nibble, high
     * bit observed to be set sometimes: 0x08, 0x88
     * @return code value in hex.
     */
    public String getStatusCode()
    {
        return Integer.toHexString(getInt(STATUS)).toUpperCase();
    }

    /**
     * Radio identifier or null if the message doesn't contain the value.
     */
    public RadioIdentifier getRadio1()
    {
        if(mRadio1 == null && hasRadio1())
        {
            mRadio1 = APCO25RadioIdentifier.createAny(getInt(RADIO_1));
        }

        return mRadio1;
    }

    /**
     * Radio identifier or null if the message doesn't contain the value.
     */
    public RadioIdentifier getRadio2()
    {
        if(mRadio2 == null && hasRadio2())
        {
            mRadio2 = APCO25RadioIdentifier.createAny(getInt(RADIO_2));
        }

        return mRadio2;
    }

    /**
     * Radio identifier or null if the message doesn't contain the value.
     */
    public RadioIdentifier getRadio3()
    {
        if(mRadio3 == null && hasRadio3())
        {
            mRadio3 = APCO25RadioIdentifier.createAny(getInt(RADIO_3));
        }

        return mRadio3;
    }

    /**
     * Radio identifier or null if the message doesn't contain the value.
     */
    public RadioIdentifier getRadio4()
    {
        if(mRadio4 == null && hasRadio4())
        {
            mRadio4 = APCO25RadioIdentifier.createAny(getInt(RADIO_4));
        }

        return mRadio4;
    }

    /**
     * Indicates if this message contains the radio value.
     */
    public boolean hasRadio1()
    {
        return getLength() > 4;
    }

    /**
     * Indicates if this message contains the radio value.
     */
    public boolean hasRadio2()
    {
        return getLength() > 8;
    }

    /**
     * Indicates if this message contains the radio value.
     */
    public boolean hasRadio3()
    {
        return getLength() > 11;
    }

    /**
     * Indicates if this message contains the radio value.
     */
    public boolean hasRadio4()
    {
        return getLength() > 15;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
