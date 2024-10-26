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
import io.github.dsheirer.identifier.Identifier;
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
 * Examples: 0x08, 0x48, 0x88
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
public class MotorolaActiveGroupRadiosOpcode191_xBF extends MacStructureVendor
{
    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaActiveGroupRadiosOpcode191_xBF(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        CorrectedBinaryMessage cbm = getMessage().getSubMessage(getOffset(), getOffset() + (getLength() * 8));
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA 191 ACTIVE GROUP RADIOS-FEATURE ACTIVE MSG:").append(cbm.toHexString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
