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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.IExtendedSourceMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.VoiceLinkControlMessage;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel user.
 */
public class LCGroupVoiceChannelUser extends VoiceLinkControlMessage implements IExtendedSourceMessage
{
    private static final IntField GROUP_ADDRESS = IntField.length16(OCTET_4_BIT_32);
    private static final int SOURCE_ADDRESS_EXTENSION_FLAG = OCTET_3_BIT_24 + 7;
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_6_BIT_48);
    private Identifier mGroupAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;
    private LCSourceIDExtension mSourceIdExtension;
    private long mTimestamp;
    private boolean mTerminator;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     * @param timestamp of the carrier message
     * @param isTerminator to indicate if the carrier message is a TDULC terminator message
     */
    public LCGroupVoiceChannelUser(CorrectedBinaryMessage message, long timestamp, boolean isTerminator)
    {
        super(message);
        mTimestamp = timestamp;
        mTerminator = isTerminator;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());

        if(isExtensionRequired() && !isFullyExtended())
        {
            sb.append(" (INCOMPLETE-FULLY QUALIFIED SOURCE REQUIRED)");
        }

        sb.append(" TO:").append(getGroupAddress());
        sb.append(" SERVICE OPTIONS:").append(getServiceOptions());
        return sb.toString();
    }

    @Override
    public boolean isTerminator()
    {
        return mTerminator;
    }

    @Override
    public boolean isFullyExtended()
    {
        return isExtensionRequired() && mSourceIdExtension != null;
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
     * Indicates if this message requires an optional source ID extension.
     * @return true if required.
     */
    @Override
    public boolean isExtensionRequired()
    {
        return getMessage().get(SOURCE_ADDRESS_EXTENSION_FLAG);
    }

    /**
     * Sets the optional source ID extension for this message.
     * @param sourceIDExtension to add
     */
    @Override
    public void setSourceIDExtension(LCSourceIDExtension sourceIDExtension)
    {
        mSourceIdExtension = sourceIDExtension;

        //Nullify the source address so that it can be recreated with the full source extension values.
        mSourceAddress = null;
        mIdentifiers = null;
    }

    /**
     * Talkgroup address
     */
    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.create(getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    /**
     * Source address
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            if(isExtensionRequired() && mSourceIdExtension != null && mSourceIdExtension.isValidExtendedSource())
            {
                mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(getInt(SOURCE_ADDRESS),
                        mSourceIdExtension.getWACN(), mSourceIdExtension.getSystem(), mSourceIdExtension.getId());
            }
            else
            {
                mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
            }
        }

        return mSourceAddress;
    }

    /**
     * Indicates if the source address is valid and non-zero
     */
    public boolean hasSourceAddress()
    {
        if(isExtensionRequired() && isFullyExtended())
        {
            return true;
        }

        return getInt(SOURCE_ADDRESS) > 0;
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
            if(hasSourceAddress())
            {
                mIdentifiers.add(getSourceAddress());
            }
        }

        return mIdentifiers;
    }
}
