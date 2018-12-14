/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.mpt1327.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.mpt1327.MPTMessageType;
import io.github.dsheirer.module.decode.mpt1327.identifier.IdentType;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327SiteIdentifier;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;
import io.github.dsheirer.protocol.Protocol;

public abstract class MPT1327BaseMessage extends Message
{
    private static final int ADDRESS_DATA_FLAG = 0;
    private static final int[] SYSTEM_IDENTITY_CODE = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] CODEWORD_COMPLETION_SEQUENCE = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31};
    private static final int[] PREAMBLE = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] PARITY = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int ADDRESS_WORD_FLAG = 64;
    private static final int[] COMMON_PREFIX = {65, 66, 67, 68, 69, 70, 71};
    private static final int[] TO_IDENT = {72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84};
    private static final int[] MESSAGE_TYPE = {85, 86, 87, 88, 89, 90, 91, 92, 93};

    private CorrectedBinaryMessage mMessage;
    private CRC[] mCRC = new CRC[5];
    private MPT1327SiteIdentifier mSystemIdentityCode;
    private boolean mValid;
    private MPT1327Talkgroup mToTalkgroup;

    public MPT1327BaseMessage(CorrectedBinaryMessage message, long timestamp)
    {
        super(timestamp);
        mMessage = message;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MPT1327;
    }

    /**
     * Sets the valid flag for a message.  A message can be tagged invalid and still allowed to progress through the
     * system in order to accurately account for bit error rates.
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Indicates if this message is valid and has passed all CRC and error checks
     */
    @Override
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Raw binary underlying message
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Message Type
     */
    public abstract MPTMessageType getMessageType();

    /**
     * System/Site identifier
     */
    public MPT1327SiteIdentifier getSystemIdentityCode()
    {
        if(mSystemIdentityCode == null)
        {
            mSystemIdentityCode = MPT1327SiteIdentifier.create(getMessage().getInt(SYSTEM_IDENTITY_CODE));
        }

        return mSystemIdentityCode;
    }

    /**
     * Called/TO talkgroup
     */
    public MPT1327Talkgroup getToTalkgroup()
    {
        if(mToTalkgroup == null)
        {
            mToTalkgroup = MPT1327Talkgroup.createTo(getPrefix(), getToIdent());
        }

        return mToTalkgroup;
    }

    /**
     * TO ident value
     */
    public int getToIdent()
    {
        return getToIdent(getMessage());
    }

    /**
     * Extracts the TO ident value from the message
     */
    public static int getToIdent(CorrectedBinaryMessage message)
    {
        return message.getInt(TO_IDENT);
    }

    /**
     * Indicates the type of ident value for the TO talkgroup
     */
    public IdentType getToIdentType()
    {
        return getToIdentType(getMessage());
    }

    /**
     * Indicates the type of ident value for the TO talkgroup
     */
    public static IdentType getToIdentType(CorrectedBinaryMessage message)
    {
        return IdentType.fromIdent(getToIdent(message));
    }

    /**
     * Prefix value
     */
    protected int getPrefix()
    {
        return getMessage().getInt(COMMON_PREFIX);
    }
}
