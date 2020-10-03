/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.cellocator;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Controller (Inbound) Acknowledge Packet
 */
public class AcknowledgeMessage extends MCGPPacket
{
    //This field is byte reversed (ie big endian)
    private static final int[] TARGET_UNIT_ID = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22,
        23, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] COMMAND_NUMERATOR = new int[]{32, 33, 34, 35, 36, 37, 38, 39};
    //This field is (possibly) byte reversed (ie big endian)
    private static final int[] AUTHENTICATION_CODE = new int[]{64, 65, 66, 67, 68, 69, 70, 71, 56, 57, 58, 59, 60, 61,
        62, 63, 48, 49, 50, 51, 52, 53, 54, 55, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] ACTION_CODE = new int[]{72, 73, 74, 75, 76, 77, 78, 79};
    //This field is byte reversed (ie big endian)
    private static final int[] MAIN_ACKNOWLEDGE_NUMBER = new int[]{88, 89, 90, 91, 92, 93, 94, 95, 80, 81, 82, 83, 84,
        85, 86, 87};
    //This field is byte reversed (ie big endian)
    private static final int[] SECONDARY_ACKNOWLEDGE_NUMBER = new int[]{104, 105, 106, 107, 108, 109, 110, 111, 96, 97,
        98, 99, 100, 101, 102, 103};
    private static final int[] RESERVED = new int[]{112, 113, 114, 115, 116, 117, 118, 119};
    private static final int[] YEAR = new int[]{121, 122, 123, 124, 125, 126, 127};
    private static final int[] MONTH = new int[]{133, 134, 135, 120};
    private static final int[] DAY = new int[]{128, 129, 130, 131, 132};
    private static final int[] RESERVED_2 = new int[]{152, 153, 154, 155, 156, 157, 158}; //Always 0x40
    private static final int[] HOUR = new int[]{139, 140, 141, 142, 143};
    private static final int[] MINUTE = new int[]{149, 150, 151, 136, 137, 138};
    private static final int[] SECOND = new int[]{159, 144, 145, 146, 147, 148};

    private CellocatorRadioIdentifier mTargetRadioId;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public AcknowledgeMessage(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(header, message, offset);
    }

    public CellocatorRadioIdentifier getRadioId()
    {
        if(mTargetRadioId == null)
        {
            mTargetRadioId = CellocatorRadioIdentifier.createTo(getMessage().getInt(TARGET_UNIT_ID, getOffset()));
        }

        return mTargetRadioId;
    }

    public MCGPMessageType getAcknowledgedCommand()
    {
        int command = getMessage().getInt(COMMAND_NUMERATOR, getOffset());
        return MCGPMessageType.getOutboundMessageType(command);
    }

    public String getAuthenticationCode()
    {
        return Integer.toHexString(getMessage().getInt(AUTHENTICATION_CODE, getOffset())).toUpperCase();
    }

    /**
     * Always set to zero for an acknowledge message
     */
    public int getActionCode()
    {
        return getMessage().getInt(ACTION_CODE, getOffset());
    }

    public int getMainAcknowledgeNumber()
    {
        return getMessage().getInt(MAIN_ACKNOWLEDGE_NUMBER, getOffset());
    }

    /**
     * Currently not used and set to zero.
     */
    public int getSecondaryAcknowledgeNumber()
    {
        return getMessage().getInt(SECONDARY_ACKNOWLEDGE_NUMBER, getOffset());
    }

    public long getTimestamp()
    {
        Calendar calendar = new GregorianCalendar();
        calendar.clear();

        int year = 2000 + getMessage().getInt(YEAR, getOffset());
        int month = getMessage().getInt(MONTH, getOffset()) - 1; //Calendar month is zero based
        int day = getMessage().getInt(DAY, getOffset());
        int hour = getMessage().getInt(HOUR, getOffset());
        int minute = getMessage().getInt(MINUTE, getOffset());
        int second = getMessage().getInt(SECOND, getOffset());

        calendar.set(year, month, day, hour, minute, second);

        return calendar.getTimeInMillis();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CELLOCATOR RADIO:");
        sb.append(getRadioId());
        sb.append(" ACKNOWLEDGE:").append(getAcknowledgedCommand());
        sb.append(" MESSAGE NUMBER:").append(getMainAcknowledgeNumber());
        sb.append(" TIME:").append(new Date(getTimestamp()));
        sb.append(" AUTH:").append(getAuthenticationCode());
        return sb.toString();
    }
}
