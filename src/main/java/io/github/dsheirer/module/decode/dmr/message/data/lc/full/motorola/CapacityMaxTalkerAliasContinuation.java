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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.DmrTalkerAliasIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Capacity Max Talker Alias Continuation (FLCO 21 / FID 0x10)
 */
public class CapacityMaxTalkerAliasContinuation extends CapacityPlusVoiceChannelUser
{
    private static final int ALIAS_START = 16;
    private static final int ALIAS_END = ALIAS_START + (7 * 8);
    private DmrTalkerAliasIdentifier mTalkerAliasIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     * @param timestamp
     * @param timeslot
     */
    public CapacityMaxTalkerAliasContinuation(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }
        if(isEncrypted())
        {
            sb.append(" *ENCRYPTED*");
        }
        if(isReservedBitSet())
        {
            sb.append(" *RESERVED-BIT*");
        }

        sb.append("FLC MOTOROLA CAPMAX TALKER ALIAS CONTINUED:").append(getTalkerAliasIdentifier());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Talker alias identifier
     * @return identifier
     */
    public DmrTalkerAliasIdentifier getTalkerAliasIdentifier()
    {
        if(mTalkerAliasIdentifier == null)
        {
            String alias = new String(getMessage().get(ALIAS_START, ALIAS_END).getBytes()).trim();
            mTalkerAliasIdentifier = DmrTalkerAliasIdentifier.create(alias);
        }

        return mTalkerAliasIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkerAliasIdentifier());
        }

        return mIdentifiers;
    }
}
