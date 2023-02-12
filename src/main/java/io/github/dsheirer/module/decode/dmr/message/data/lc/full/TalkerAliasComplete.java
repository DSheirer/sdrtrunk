/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.DmrTalkerAliasIdentifier;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import io.github.dsheirer.module.decode.dmr.message.type.TalkerAliasDataFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complete talker alias assembled from talker alias header and 0 or more talker alias blocks.
 */
public class TalkerAliasComplete extends FullLCMessage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TalkerAliasComplete.class);

    private TalkerAliasDataFormat mDataFormat;
    private int mCharacterCount;
    private String mAlias;
    private DmrTalkerAliasIdentifier mTalkerAliasIdentifier;

    /**
     * Constructs an instance
     *
     * @param message for link control payload
     * @param timestamp for the header
     * @param timeslot for the header and blocks
     */
    public TalkerAliasComplete(CorrectedBinaryMessage message, TalkerAliasDataFormat format, int characterCount,
                               long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
        mDataFormat = format;
        mCharacterCount = characterCount;
    }

    @Override
    public LCOpcode getOpcode()
    {
        return LCOpcode.FULL_STANDARD_TALKER_ALIAS_COMPLETE;
    }

    @Override
    public int getOpcodeValue()
    {
        return getOpcode().getValue();
    }

    /**
     * Alias value
     * @return alias value if it can be decoded or empty string.
     */
    public String getAliasValue()
    {
        if(mAlias == null)
        {
            switch(mDataFormat)
            {
                //Note: there are 31 possible 7-bit characters, 27 possible 8-bit characters and 13.5 possible 16-bit
                //characters.  We check each format to ensure character count is at least one and no more than max.
                case BIT_7:
                    if(1 <= mCharacterCount && mCharacterCount <= 31)
                    {
                        mAlias = getMessage().parseISO7(0, mCharacterCount);
                    }
                    break;
                case BIT_8:
                    if(1 <= mCharacterCount && mCharacterCount <= 27)
                    {
                        mAlias = getMessage().parseISO8(1, mCharacterCount);
                    }
                    break;
                case UTF_8:
                    if(1 <= mCharacterCount && mCharacterCount <= 27)
                    {
                        try
                        {
                            mAlias = getMessage().parseUTF8(1, mCharacterCount);
                        }
                        catch(Exception e)
                        {
                            LOGGER.info("Error decoding talker alias as UTF-8: " + e.getLocalizedMessage());
                        }
                    }
                    break;
                case UNICODE_UTF_16_BE:
                    if(1 <= mCharacterCount && mCharacterCount <= 13)
                    {
                        mAlias = getMessage().parseUnicode(1, mCharacterCount);
                    }
                    break;
            }

            //If we couldn't decode, set it to empty so that we don't reprocess every time.
            if(mAlias == null)
            {
                mAlias = "";
            }
        }

        return mAlias;
    }

    /**
     * Talker Alias identifier.
     * @return identifier or null if the alias string is null or empty, meaning it could not be decoded.
     */
    public DmrTalkerAliasIdentifier getTalkerAliasIdentifier()
    {
        if(mTalkerAliasIdentifier == null && getAliasValue() != null && !getAliasValue().isEmpty())
        {
            mTalkerAliasIdentifier = DmrTalkerAliasIdentifier.create(getAliasValue());
        }

        return mTalkerAliasIdentifier;
    }

    /**
     * Indicates if the talker alias identifier is non-null.
     * @return true if non-null.
     */
    public boolean hasTalkerAliasIdentifier()
    {
        return getTalkerAliasIdentifier() != null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FLC TALKER ALIAS:");

        if(hasTalkerAliasIdentifier())
        {
            sb.append(getTalkerAliasIdentifier());
        }
        else
        {
            sb.append("(empty/error)");
        }

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(hasTalkerAliasIdentifier())
        {
            List<Identifier> identifiers = new ArrayList<>();
            identifiers.add(getTalkerAliasIdentifier());
            return identifiers;
        }

        return Collections.emptyList();
    }
}
