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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.IExtendedSourceMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * Source ID extension word.  This is used in conjunction with another link control message to carry a fully qualified
 * source SUID.
 */
public class LCSourceIDExtension extends LinkControlWord implements IExtendedSourceMessage
{
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_2_BIT_16);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_4_BIT_32 + 4);
    private static final IntField SOURCE_SUID_RADIO = IntField.length24(OCTET_6_BIT_48);
    private long mTimestamp;
    private boolean mTerminator;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     * @param isTerminator to indicate if the carrier message is a TDULC terminator message
     */
    public LCSourceIDExtension(CorrectedBinaryMessage message, long timestamp, boolean isTerminator)
    {
        super(message);
        mTimestamp = timestamp;
        mTerminator = isTerminator;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWACN());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" ID:").append(getId());
        return sb.toString();
    }

    /**
     * Indicates that at least one of the WACN, System, or ID values are non-zero.
     * @return
     */
    public boolean isValidExtendedSource()
    {
        return getWACN() != 0 && getSystem() != 0 && getId() != 0;
    }


    @Override
    public boolean isTerminator()
    {
        return mTerminator;
    }

    @Override
    public boolean isExtensionRequired()
    {
        return false;
    }

    @Override
    public void setSourceIDExtension(LCSourceIDExtension sourceIDExtension)
    {
        throw new IllegalArgumentException("Extended source cannot be attached to this message");
    }

    @Override
    public boolean isFullyExtended()
    {
        return true;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public int getTimeslot()
    {
        return P25P1Message.TIMESLOT_0;
    }

    /**
     * Source SUID WACN value.
     */
    public int getWACN()
    {
        return getInt(SOURCE_SUID_WACN);
    }

    /**
     * Source SUID System value.
     */
    public int getSystem()
    {
        return getInt(SOURCE_SUID_SYSTEM);
    }

    /**
     * Source SUID radio value.
     */
    public int getId()
    {
        return getInt(SOURCE_SUID_RADIO);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
