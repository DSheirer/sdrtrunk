/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;

import java.util.Collections;
import java.util.List;

/**
 * Unknown Motorola Opcode that is suspected to be used to indicate Site/Channel loading
 *
 * Note: I did some analysis of the first 2 octets following the vendor id octet.  The second octet value appears to
 * only use the 2 MSBs and these seem to be locked to the first octet value, serving as a (redundant) counter to the
 * first octet.  What's odd is that the sequencing of the first octet skips every 4th value in the sequence, and this
 * is supported by the rollover of the second octet counter.
 *
 * 000 00000000 00
 * 014 00000001 01
 * 028 00000010 10
 * 03C 00000011 11
 *
 * 050 00000101 00 (skipped 040 ?)
 * 064 00000110 01
 * 078 00000111 10 (lowest observed value?)
 * 08C 00001000 11
 *
 * 0A0 00001010 00 (skipped 090)
 * 0B4 00001011 01
 * 0C8 00001100 10
 * 0DC 00001101 11
 *
 * 0F0 00001111 00 (skipped 0E0)
 * 104 00010000 01
 * 118 00010001 10
 * 12C 00010010 11
 *
 * 140 00010100 00 (skipped 130)
 * 154 00010101 01
 * 168 00010110 10
 * 17C 00010111 11
 *
 * 190 00011001 00 (skipped 180)
 * 1A4 00011010 01
 * 1B8 00011011 10
 * 1CC 00011100 11
 */
public class ChannelLoading extends OSPMessage
{
    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public ChannelLoading(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" MOTOROLA");
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
