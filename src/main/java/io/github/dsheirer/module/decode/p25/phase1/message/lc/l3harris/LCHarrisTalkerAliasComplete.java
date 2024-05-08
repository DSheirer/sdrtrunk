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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.P25TalkerAliasIdentifier;
import io.github.dsheirer.identifier.alias.TalkerAliasIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris talker alias complete.  This is a talker alias fully reassembled from data blocks 1-4
 */
public class LCHarrisTalkerAliasComplete extends LinkControlWord implements IMessage
{
    private static final int PAYLOAD_START = OCTET_2_BIT_16;
    private static final int PAYLOAD_END = OCTET_9_BIT_72;

    private String mFragment2;
    private String mFragment3;
    private String mFragment4;
    private TalkerAliasIdentifier mTalkerAlias;
    private long mTimestamp;

    /**
     * Constructs an instance
     * @param correctedBinaryMessage that is LCW for block 1
     * @param fragment2 alias fragment from block 2
     * @param fragment3 alias fragment from block 3
     * @param fragment4 alias fragment from block 4
     * @param timestamp of the most recent LDU and LCW message
     */
    public LCHarrisTalkerAliasComplete(CorrectedBinaryMessage correctedBinaryMessage, String fragment2, String fragment3, String fragment4, long timestamp)
    {
        super(correctedBinaryMessage);
        mFragment2 = fragment2;
        mFragment3 = fragment3;
        mFragment4 = fragment4;
        mTimestamp = timestamp;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("L3HARRIS TALKER ALIAS REASSEMBLED:").append(getTalkerAlias());
        return sb.toString();
    }

    @Override
    public LinkControlOpcode getOpcode()
    {
        return LinkControlOpcode.TALKER_ALIAS_COMPLETE;
    }

    @Override
    public int getOpcodeNumber()
    {
        return LinkControlOpcode.TALKER_ALIAS_COMPLETE.getCode();
    }

    /**
     * Fully (or partially) reassembled talker alias.
     *
     * Note: in real world examples, data blocks 1-2 have the alias and data blocks 3-4 repeat the same content as
     * data blocks 1-2, so we inspect each data block fragment to see if that fragment is already contained in the
     * talker alias as we reconstruct it from the fragments and don't add any content that's already in the assembly.
     * @return alias.
     */
    public TalkerAliasIdentifier getTalkerAlias()
    {
        if(mTalkerAlias == null)
        {
            String alias = getPayloadFragmentString();

            if(mFragment2 != null && !alias.contains(mFragment2))
            {
                alias += mFragment2;

                if(mFragment3 != null && !alias.contains(mFragment3))
                {
                    alias += mFragment3;

                    if(mFragment4 != null && !alias.contains(mFragment4))
                    {
                        alias += mFragment4;
                    }
                }
            }

            mTalkerAlias = P25TalkerAliasIdentifier.create(alias);
        }

        return mTalkerAlias;
    }

    /**
     * Payload fragment carried by the header.
     * @return payload fragment.
     */
    public CorrectedBinaryMessage getPayloadFragment()
    {
        return getMessage().getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    /**
     * Payload fragment as a string with empty space removed.
     * @return fragment
     */
    public String getPayloadFragmentString()
    {
        return new String(getPayloadFragment().toByteArray()).trim();
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

    @Override
    public List<Identifier> getIdentifiers()
    {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(getTalkerAlias());
        return identifiers;
    }
}
