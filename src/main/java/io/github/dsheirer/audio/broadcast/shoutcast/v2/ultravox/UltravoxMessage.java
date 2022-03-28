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
package io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox;

import io.github.dsheirer.bits.BinaryMessage;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public abstract class UltravoxMessage
{
    public static final String ULTRAVOX_VERSION = "2.1";
    public static final String VALID_RESPONSE_PREFIX = "ACK";
    public static final String VALID_RESPONSE_PAYLOAD_PREFIX = "ACK:";
    public static final String ERROR_RESPONSE_PREFIX = "NAK:";

    public static final int[] SYNC = {0,1,2,3,4,5,6,7};
    public static final int[] RESERVED = {8,9,10,11};
    public static final int REQUIRED_DELIVERY = 12;
    public static final int[] SEND_QUEUE_PRIORITY = {13,14,15};
    public static final int[] MESSAGE_CLASS = {16,17,18,19};
    public static final int[] MESSAGE_CLASS_AND_TYPE = {16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};
    public static final int[] PAYLOAD_LENGTH = {32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47};

    private BinaryMessage mMessage;

    /**
     * Constructs a ultravox from the byte array
     */
    public UltravoxMessage(byte[] data)
    {
        mMessage = new BinaryMessage(data.length * 8);

        int pointer = 0;

        for(byte b: data)
        {
            mMessage.setByte(pointer, b);
            pointer += 8;
        }
    }

    /**
     * Constructs an empty ultravox of the specified length
     *
     * @param ultravoxMessageType type of ultravox
     */
    public UltravoxMessage(UltravoxMessageType ultravoxMessageType)
    {
        mMessage = new BinaryMessage(56);

        setSync();
        setMessageType(ultravoxMessageType);
    }

    /**
     * Message byte array
     *
     * @return ultravox bytes
     */
    public byte[] getMessage()
    {
        return mMessage.toByteArray();
    }

    public String toString()
    {
        return getMessageType().name();
    }

    /**
     * Sets the ultravox sync bits to the predefined sync pattern: 01011010 (0x5A)
     */
    private void setSync()
    {
        mMessage.setInt(0x5A, SYNC);
    }

    /**
     * Ultravox ultravox class
     */
    public UltravoxMessageClass getMessageClass()
    {
        return UltravoxMessageClass.fromValue(mMessage.getInt(MESSAGE_CLASS));
    }

    /**
     * Ultravox ultravox type
     * @return
     */
    public UltravoxMessageType getMessageType()
    {
        return UltravoxMessageType.fromValue(mMessage.getInt(MESSAGE_CLASS_AND_TYPE));
    }

    /**
     * Sets the ultravox bits according to the ultravox class and type
     * @param messageType of ultravox ultravox
     */
    private void setMessageType(UltravoxMessageType messageType)
    {
        mMessage.setInt(messageType.getValue(), MESSAGE_CLASS_AND_TYPE);
    }

    /**
     * Indicates if this ultravox has the required delivery flag set
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
     * Send queue priority (0 - 7) for this ultravox
     */
    public int getSendQueuePriority()
    {
        return mMessage.getInt(SEND_QUEUE_PRIORITY);
    }

    /**
     * Sets send queue priority for this ultravox
     * @param priority 0 - 7
     */
    public void setSendQueuePriority(int priority)
    {
        Validate.isTrue(0 <= priority && priority <= 7);

        mMessage.setInt(priority, SEND_QUEUE_PRIORITY);
    }

    /**
     * Message payload
     */
    public String getPayload()
    {
        int payloadLength = mMessage.getInt(PAYLOAD_LENGTH);

        byte[] payload = new byte[payloadLength];

        int payloadPointer = 48;

        for(int x = 0; x < payloadLength; x++)
        {
            payload[x] = mMessage.getByte(payloadPointer);
            payloadPointer += 8;
        }

        //Strip the trailing 0x00 null value from a server response
        if(payload[payload.length - 1] == 0)
        {
            return new String(Arrays.copyOf(payload, payload.length - 1));
        }

        return new String(payload);
    }

    /**
     * Sets the ultravox payload, updates the ultravox length field, and appends a trailing 0x00 byte after the payload.
     */
    public void setPayload(String payload)
    {
        setPayload(payload.getBytes());
    }

    public void setPayload(byte[] payload)
    {
        int messageBitLength = (7 + payload.length) * 8;

        mMessage.setSize(messageBitLength);
        mMessage.setInt(payload.length, PAYLOAD_LENGTH);

        int payloadPointer = 48;

        for(byte payloadByte: payload)
        {
            mMessage.setByte(payloadPointer, payloadByte);
            payloadPointer += 8;
        }

        mMessage.setByte(payloadPointer, (byte)0x00);
    }

    /**
     * Indicates if the response is a valid (ie non-error) response
     */
    public boolean isValidResponse()
    {
        String payload = getPayload();

        return payload != null && payload.startsWith(VALID_RESPONSE_PREFIX);
    }

    /**
     * Indicates if the response message contains an error message.
     */
    public boolean isErrorResponse()
    {
        String payload = getPayload();

        return payload != null && payload.startsWith(ERROR_RESPONSE_PREFIX);
    }

    /**
     * Textual message available when the error response flag is set.
     */
    public String getErrorMessage()
    {
        String payload = getPayload();

        if(payload != null)
        {
            return payload.replace(ERROR_RESPONSE_PREFIX, "");
        }

        return null;
    }
}
