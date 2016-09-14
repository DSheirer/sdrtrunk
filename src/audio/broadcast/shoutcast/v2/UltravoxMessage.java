/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.broadcast.shoutcast.v2;

import bits.BinaryMessage;
import com.sun.org.apache.regexp.internal.RE;

public abstract class UltravoxMessage
{
    public static final int[] SYNC = {0,1,2,3,4,5,6,7};
    public static final int[] RESERVED = {8,9,10,11};
    public static final int REQUIRED_DELIVERY = 12;
    public static final int[] SEND_QUEUE_PRIORITY = {13,14,15};
    public static final int[] MESSAGE_CLASS = {16,17,18,19};
    public static final int[] MESSAGE_CLASS_AND_TYPE = {16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};
    public static final int[] PAYLOAD_LENGTH = {32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47};

    private BinaryMessage mMessage;

    /**
     * Constructs a message from the byte array
     */
    public UltravoxMessage(byte[] data)
    {
        mMessage = new BinaryMessage(data);
    }

    /**
     * Constructs an empty message of the specified length
     *
     * @param ultravoxMessageType type of message
     * @param length of message in bits (64 minimum)
     */
    public UltravoxMessage(UltravoxMessageType ultravoxMessageType, int length)
    {
        assert(length >= 64);

        mMessage = new BinaryMessage(length);

        setSync();
        setMessageType(ultravoxMessageType);
    }

    /**
     * Sets the message sync bits to the predefined sync pattern: 01011010 (0x5A)
     */
    private void setSync()
    {
        mMessage.setInt(0x5A, SYNC);
    }

    /**
     * Ultravox message class
     */
    public UltravoxMessageClass getMessageClass()
    {
        return UltravoxMessageClass.fromValue(mMessage.getInt(MESSAGE_CLASS));
    }

    /**
     * Ultravox message type
     * @return
     */
    public UltravoxMessageType getMessageType()
    {
        return UltravoxMessageType.fromValue(mMessage.getInt(MESSAGE_CLASS_AND_TYPE));
    }

    /**
     * Sets the message bits according to the message class and type
     * @param messageType of ultravox message
     */
    private void setMessageType(UltravoxMessageType messageType)
    {
        mMessage.setInt(messageType.getValue(), MESSAGE_CLASS_AND_TYPE);
    }

    /**
     * Indicates if this message has the required delivery flag set
     */
    public boolean isRequiredDelivery()
    {
        return mMessage.get(REQUIRED_DELIVERY);
    }

    /**
     * Sets the required delivery flag according to the argument
     */
    public void setRequiredDelivery(boolean required)
    {
        if(required)
        {
            mMessage.set(REQUIRED_DELIVERY);
        }
        else
        {
            mMessage.clear(REQUIRED_DELIVERY);
        }
    }

    /**
     * Send queue priority (0 - 7) for this message
     */
    public int getSendQueuePriority()
    {
        return mMessage.getInt(SEND_QUEUE_PRIORITY);
    }

    /**
     * Sets send queue priority for this message
     * @param priority 0 - 7
     */
    public void setSendQueuePriority(int priority)
    {
        assert(0 <= priority && priority <= 7);

        mMessage.setInt(priority, SEND_QUEUE_PRIORITY);
    }

    /**
     * Message payload
     */
    public String getPayload()
    {
        //TODO: parse payload field
        return null;
    }

    /**
     * Sets the message payload, updates the message length field, and appends a trailing 0x00 byte after the payload.
     */
    public void setPayload(String payload)
    {
        //TODO: implement payload, along with trailing 0x00 byte and update overall payload size field.
    }

    private void setPayloadLength(int length)
    {
        //TODO: correctly size the message to payload + 1 byte and set the payload length field
    }
}
