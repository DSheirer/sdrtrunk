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

package io.github.dsheirer.module.decode.nxdn.layer3.proprietary;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkerAliasIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.List;

/**
 * Reassembled talker alias message
 */
public class TalkerAliasComplete extends NXDNLayer3Message
{
    private NXDNTalkerAliasIdentifier mTalkerAliasIdentifier;

    /**
     * Constructs an instance
     *
     * @param alias value
     * @param timestamp for the alias
     * @param ran for the channel
     * @param lich for the channel
     */
    public TalkerAliasComplete(String alias, long timestamp, int ran, LICH lich)
    {
        super(new CorrectedBinaryMessage(0), timestamp, NXDNMessageType.TALKER_ALIAS_COMPLETE, ran, lich);
        mTalkerAliasIdentifier = new NXDNTalkerAliasIdentifier(alias);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("TALKER ALIAS COMPLETE:").append(getTalkerAlias());
        return sb.toString();
    }

    /**
     * Reassembled/complete talker alias identifier
     */
    public NXDNTalkerAliasIdentifier getTalkerAlias()
    {
        return mTalkerAliasIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getTalkerAlias());
    }
}
