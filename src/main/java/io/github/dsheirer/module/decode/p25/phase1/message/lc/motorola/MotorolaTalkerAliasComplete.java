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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.P25TalkerAliasIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.TimeslotMessage;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola completely assembled talker alias.  Note: this is not a true link control word.  It is reassembled from a
 * header and data blocks.
 */
public class MotorolaTalkerAliasComplete extends TimeslotMessage implements IMessage
{
    private static final IntField SUID_WACN = IntField.length20(OCTET_0_BIT_0);
    private static final IntField SUID_SYSTEM = IntField.length12(OCTET_2_BIT_16 + 4);
    private static final IntField SUID_ID = IntField.length24(OCTET_4_BIT_32);
    private static final IntField CHUNK = IntField.length16(0);
    private static final int ENCODED_ALIAS_START = OCTET_7_BIT_56;
    private static final int CHUNK_SIZE = 16;

    private APCO25Talkgroup mTalkgroup;
    private APCO25FullyQualifiedRadioIdentifier mRadio;
    private P25TalkerAliasIdentifier mAlias;
    private List<Identifier> mIdentifiers;
    private int mSequence;
    private Protocol mProtocol;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message assembled from the data blocks
     * @param talkgroup from the header
     * @param dataBlockCount from the header
     * @param sequence number for the alias
     * @param timeslot for the message
     * @param timestamp of the most recent header or data block
     * @param protocol for the message
     */
    public MotorolaTalkerAliasComplete(CorrectedBinaryMessage message, APCO25Talkgroup talkgroup, int sequence,
                                       int timeslot, long timestamp, Protocol protocol)
    {
        super(message, timeslot, timestamp);
        mTalkgroup = talkgroup;
        mSequence = sequence;
        mProtocol = protocol;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(getTimeslot() > TIMESLOT_0)
        {
            sb.append("TS").append(getTimeslot()).append(" ");
        }
        sb.append("MOTOROLA TALKER ALIAS COMPLETE");
        sb.append(" RADIO:").append(getRadio());
        sb.append(" TG:").append(getTalkgroup());
        sb.append(" ENCODED:").append(getEncodedAlias().toHexString());
        sb.append(" ALIAS:").append(getAlias());
        sb.append(" SEQUENCE:").append(mSequence);
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Protocol - P25 Phase 1 or Phase 2
     */
    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    /**
     * Sequence number for the alias.
     */
    public int getSequence()
    {
        return mSequence;
    }

    /**
     * Decoded alias string
     */
    public P25TalkerAliasIdentifier getAlias()
    {
        if(mAlias == null)
        {
            //BinaryMessage encoded = getEncodedAlias();
            //TODO: implement decoding of the encoded alias.
            String alias = "*ALIAS " + getRadio().getValue() + "*"; //Temporary value until we implement decoding
            mAlias = P25TalkerAliasIdentifier.create(alias);
        }

        return mAlias;
    }

    /**
     * Calculates the 16-bit value at the specified chunk number.
     * @param chunk to get
     * @return int value at the chunk
     */
    private int getChunkValue(int chunk)
    {
        int lastIndex = ENCODED_ALIAS_START + (chunk * CHUNK_SIZE) - 1;

        if(getMessage().size() >= lastIndex)
        {
            return getInt(CHUNK, ENCODED_ALIAS_START + ((chunk - 1) * CHUNK_SIZE));
        }

        return 0;
    }

    /**
     * Extracts the encoded alias payload.
     * @return encoded alias binary message
     */
    private BinaryMessage getEncodedAlias()
    {
        int length = 1;

        for(int x = 16; x > 1; x--)
        {
            if(getChunkValue(x) > 0)
            {
                length = x;
                break;
            }
        }

        return getMessage().getSubMessage(ENCODED_ALIAS_START, ENCODED_ALIAS_START + (length * 16));
    }

    /**
     * Talkgroup
     */
    public APCO25Talkgroup getTalkgroup()
    {
        return mTalkgroup;
    }

    /**
     * Radio that is being aliased.
     */
    public APCO25FullyQualifiedRadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            int wacn = getInt(SUID_WACN);
            int system = getInt(SUID_SYSTEM);
            int id = getInt(SUID_ID);

            mRadio = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getRadio());
            //TODO: uncomment once alias decoding is implemented.
//            mIdentifiers.add(getAlias());
        }

        return mIdentifiers;
    }
}
