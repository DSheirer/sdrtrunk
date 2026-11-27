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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import java.util.Collections;
import java.util.List;

/**
 * Base paging message, common to all paging protocols.  A paging message is addressed to a capcode and carries
 * optional alphanumeric, numeric or binary content.
 */
public abstract class PagerMessage extends Message
{
    private final long mCapcode;
    private final PagerMessageType mMessageType;
    private final String mContent;
    private final int mBitRate;
    private final int mErrorCount;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     * @param timestamp of the message
     * @param capcode for the receiving pager
     * @param messageType content type
     * @param content decoded message content, or an empty string
     * @param bitRate of the transmission
     * @param errorCount of uncorrectable message content codewords.  Characters from uncorrectable codewords are
     * shown as '?' in the content.
     */
    protected PagerMessage(long timestamp, long capcode, PagerMessageType messageType, String content, int bitRate,
                           int errorCount)
    {
        super(timestamp);
        mCapcode = capcode;
        mMessageType = messageType;
        mContent = content != null ? content : "";
        mBitRate = bitRate;
        mErrorCount = errorCount;
    }

    /**
     * Capcode (address) of the receiving pager.
     */
    public long getCapcode()
    {
        return mCapcode;
    }

    /**
     * Message content type
     */
    public PagerMessageType getMessageType()
    {
        return mMessageType;
    }

    /**
     * Decoded message content, or an empty string for tone-only pages.
     */
    public String getContent()
    {
        return mContent;
    }

    /**
     * Indicates if the message has content.
     */
    public boolean hasContent()
    {
        return !mContent.isEmpty();
    }

    /**
     * Transmission bit rate.
     */
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Number of message content codewords that could not be corrected.
     */
    public int getErrorCount()
    {
        return mErrorCount;
    }

    /**
     * Indicates if the message content was decoded without uncorrectable errors.
     */
    public boolean isComplete()
    {
        return mErrorCount == 0;
    }

    /**
     * The address codeword(s) passed error correction, so the message is valid even when parts of the content were
     * not recoverable (see {@link #isComplete()}).
     */
    @Override
    public boolean isValid()
    {
        return true;
    }

    /**
     * Capcode identifier, or null when the capcode is larger than the identifier value range.
     */
    public PagerCapcodeIdentifier getCapcodeIdentifier()
    {
        if(mCapcode >= 0 && mCapcode <= Integer.MAX_VALUE)
        {
            return new PagerCapcodeIdentifier((int)mCapcode, getProtocol());
        }

        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            PagerCapcodeIdentifier capcode = getCapcodeIdentifier();
            mIdentifiers = capcode != null ? Collections.singletonList(capcode) : Collections.emptyList();
        }

        return mIdentifiers;
    }

    /**
     * Protocol-specific details that are appended after the capcode in the message text, for example the POCSAG
     * function code or the FLEX frame and phase.
     */
    protected abstract String getProtocolDetails();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getProtocol()).append(" ").append(mBitRate);
        sb.append(" CAPCODE:").append(mCapcode);

        String details = getProtocolDetails();

        if(details != null && !details.isEmpty())
        {
            sb.append(" ").append(details);
        }

        sb.append(" ").append(mMessageType.getShortLabel());

        if(!mContent.isEmpty())
        {
            sb.append(" [").append(mContent).append("]");
        }

        if(mErrorCount > 0)
        {
            sb.append(" ERRORS:").append(mErrorCount);
        }

        return sb.toString();
    }
}
