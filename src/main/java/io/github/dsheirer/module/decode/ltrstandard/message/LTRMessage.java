/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ltrstandard.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.identifier.talkgroup.LTRTalkgroup;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrstandard.LtrStandardMessageType;
import io.github.dsheirer.protocol.Protocol;

/**
 * LTR Standard Base Message
 */
public abstract class LTRMessage extends Message
{
    public static final int[] SYNC = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    public static final int[] AREA = {9};
    public static final int[] CHANNEL = {10, 11, 12, 13, 14};
    public static final int[] HOME_REPEATER = {15, 16, 17, 18, 19};
    public static final int[] GROUP = {20, 21, 22, 23, 24, 25, 26, 27};
    public static final int[] FREE = {28, 29, 30, 31, 32};
    public static final int[] CHECKSUM = {33, 34, 35, 36, 37, 38, 39};

    private CorrectedBinaryMessage mMessage;
    private MessageDirection mMessageDirection;
    private CRC mCRC;
    private LTRTalkgroup mTalkgroup;

    /**
     * Constructs the message
     * @param message containing the raw bits
     * @param direction of the messsage, ISW or OSW
     * @param crc error check
     */
    public LTRMessage(CorrectedBinaryMessage message, MessageDirection direction, CRC crc)
    {
        mMessage = message;
        mMessageDirection = direction;
        mCRC = crc;
    }

    /**
     * Identifies the type of message
     */
    public abstract LtrStandardMessageType getMessageType();

    /**
     * Indicates if this message passes the CRC check
     */
    public boolean isValid()
    {
        return mCRC.passes();
    }

    /**
     * Raw binary message
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * CRC error check stats
     */
    public CRC getCRC()
    {
        return mCRC;
    }

    /**
     * Area: 0 or 1
     */
    public int getArea()
    {
        return mMessage.getInt(AREA);
    }

    /**
     * Logical Channel Number (LCN) for the repeater
     */
    public int getChannel()
    {
        return mMessage.getInt(CHANNEL);
    }

    /**
     * Home repeater number for the talkgroup
     */
    public int getHomeRepeater()
    {
        return mMessage.getInt(HOME_REPEATER);
    }

    /**
     * Talkgroup number
     */
    public int getGroup()
    {
        return mMessage.getInt(GROUP);
    }

    /**
     * Free or available repeater channel for other subscribers to use when this repeater channel is busy
     */
    public int getFree()
    {
        return mMessage.getInt(FREE);
    }

    /**
     * Talkgroup identifier
     */
    public LTRTalkgroup getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = LTRTalkgroup.create((getArea() << 13) + (getHomeRepeater() << 8) + getGroup());
        }

        return mTalkgroup;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LTR;
    }
}