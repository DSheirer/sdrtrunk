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
package io.github.dsheirer.module.decode.ltrnet.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCLTR;
import io.github.dsheirer.identifier.talkgroup.LTRTalkgroup;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.protocol.Protocol;

public abstract class LtrNetMessage extends Message
{
    protected static final int[] SYNC = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    protected static final int[] AREA = {9};
    protected static final int[] CHANNEL = {10, 11, 12, 13, 14};
    protected static final int[] HOME_REPEATER = {15, 16, 17, 18, 19};
    protected static final int[] GROUP = {20, 21, 22, 23, 24, 25, 26, 27};
    protected static final int[] FREE = {28, 29, 30, 31, 32};
    protected static final int[] CRC_FIELD = {33, 34, 35, 36, 37, 38, 39};
    protected static final int[] SIXTEEN_BITS = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};

    protected CorrectedBinaryMessage mMessage;
    protected CRC mCRC;
    private LTRTalkgroup mTalkgroup;
    private MessageDirection mMessageDirection;

    public LtrNetMessage(CorrectedBinaryMessage message, MessageDirection direction, long timestamp)
    {
        super(timestamp);
        mMessage = message;
        mMessageDirection = direction;
        mCRC = CRCLTR.check(message, direction);
    }

    /**
     * Message direction: outbound (OSW) from the repeater or inbound (ISW) to the repeater
     * @return message direction
     */
    public MessageDirection getMessageDirection()
    {
        return mMessageDirection;
    }

    /**
     * Underlying binary message
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    public abstract LtrNetMessageType getLtrNetMessageType();

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LTR_NET;
    }

    /**
     * Talkgroup identifier
     */
    public LTRTalkgroup getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = LTRTalkgroup.create((getArea(getMessage()) << 13) + (getHomeRepeater(getMessage()) << 8) +
                getGroup(getMessage()));
        }

        return mTalkgroup;
    }

    public boolean isValid()
    {
        return mCRC.passes();
    }

    public CRC getCRC()
    {
        return mCRC;
    }

    public static int getArea(CorrectedBinaryMessage message)
    {
        return message.getInt(AREA);
    }

    public static int getChannel(CorrectedBinaryMessage message)
    {
        return message.getInt(CHANNEL);
    }

    public static int getHomeRepeater(CorrectedBinaryMessage message)
    {
        return message.getInt(HOME_REPEATER);
    }

    public static int getGroup(CorrectedBinaryMessage message)
    {
        return message.getInt(GROUP);
    }

    public static int getFree(CorrectedBinaryMessage message)
    {
        return message.getInt(FREE);
    }
}
