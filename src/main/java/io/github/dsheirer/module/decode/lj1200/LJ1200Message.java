/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.lj1200;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCLJ;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.esn.ESNIdentifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LJ1200Message extends Message
{
    private final static Logger mLog = LoggerFactory.getLogger(LJ1200Message.class);

    public static final String[] REPLY_CODE = {"0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", "A", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S",
        "T", "U", "V", "W", "X", "Y"};

    public static int[] SYNC = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    public static int[] VRC = {23, 22, 21, 20, 19, 18, 17, 16};
    public static int[] LRC = {31, 30, 29, 28, 27, 26, 25, 24};
    public static int[] FUNCTION = {35, 34, 33, 32};
    public static int[] ADDRESS = {63, 62, 61, 60, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43,
        42, 41, 40, 39, 38, 37, 36};
    public static int[] REPLY_1 = {39, 38, 37, 36, 43};
    public static int[] REPLY_2 = {42, 41, 40, 47, 46};
    public static int[] REPLY_3 = {45, 44, 51, 50, 49};
    public static int[] REPLY_4 = {48, 55, 54, 53, 52};
    public static int[] REPLY_5 = {59, 58, 57, 56, 63};

    public static int[] MESSAGE_CRC = {79, 78, 77, 76, 75, 74, 73, 72, 71, 70, 69, 68, 67, 66, 65, 64};

    private BinaryMessage mMessage;
    private CRC mCRC;
    private FunctionAndReplyCodeIdentifier mFunctionAndReplyCodeIdentifier;
    private ESNIdentifier mTransponderIdentifier;
    private List<Identifier> mIdentifiers;

    public LJ1200Message(BinaryMessage message)
    {
        mMessage = message;

        checkCRC();

        switch(mCRC)
        {
            case CORRECTED:
                mLog.debug("CORR:" + message.toString());
                break;
            case FAILED_CRC:
                mLog.debug("FAIL:" + message.toString());
                break;
            case PASSED:
                mLog.debug("PASS:" + message.toString());
                break;
            default:
                break;
        }
    }

    private void checkCRC()
    {
        mCRC = CRCLJ.checkAndCorrect(mMessage);
    }

    public boolean isValid()
    {
        return mCRC == CRC.PASSED || mCRC == CRC.CORRECTED;
    }

    public FunctionAndReplyCodeIdentifier getFunctionAndReplyCodeIdentifier()
    {
        if(mFunctionAndReplyCodeIdentifier == null)
        {
            mFunctionAndReplyCodeIdentifier = FunctionAndReplyCodeIdentifier.create(getFunction(), getReplyCode());
        }

        return mFunctionAndReplyCodeIdentifier;
    }

    public ESNIdentifier getTransponder()
    {
        if(mTransponderIdentifier == null)
        {
            mTransponderIdentifier = ESNIdentifier.create(getAddress(), Protocol.LOJACK, Role.TO);
        }

        return mTransponderIdentifier;
    }

    public String getVRC()
    {
        return mMessage.getHex(VRC, 2);
    }

    public String getLRC()
    {
        return mMessage.getHex(LRC, 2);
    }

    public String getCRC()
    {
        return mMessage.getHex(MESSAGE_CRC, 4);
    }

    public Function getFunction()
    {
        return Function.fromValue(mMessage.getInt(FUNCTION), mMessage.getInt(REPLY_3));
    }

    public String getAddress()
    {
        return mMessage.getHex(ADDRESS, 7);
    }

    /**
     * 5 character reply code for function E and F, transponder activation.
     *
     * @return - reply code
     */
    public String getReplyCode()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(REPLY_CODE[mMessage.getInt(REPLY_1)]);
        sb.append(REPLY_CODE[mMessage.getInt(REPLY_2)]);
        sb.append(REPLY_CODE[mMessage.getInt(REPLY_3)]);
        sb.append(REPLY_CODE[mMessage.getInt(REPLY_4)]);
        sb.append(REPLY_CODE[mMessage.getInt(REPLY_5)]);

        return sb.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        Function function = getFunction();

        sb.append("FUNCTION: ");
        sb.append(function.toString());

        switch(function)
        {
            case F1_SITE_ID:
                sb.append(" SITE [");
                break;
            case F1_SPEED_UP:
            case F2_TEST:
            case F3_DEACTIVATE:
            case F4_ACTIVATE:
            case FF_TRACK_PULSE:
            default:
                sb.append(" REPLY CODE [");
                break;
        }

        sb.append(getReplyCode());

        sb.append("] ADDRESS [");
        sb.append(getAddress());
        sb.append("] VRC [").append(getVRC());
        sb.append("] LRC [").append(getLRC());
        sb.append("] CRC [").append(getCRC());
        sb.append("]");

        return sb.toString();
    }

    /**
     * Pads spaces onto the end of the value to make it 'places' long
     */
    public String pad(String value, int places, String padCharacter)
    {
        return StringUtils.rightPad(value, places, padCharacter);
    }

    /**
     * Pads an integer value with additional zeroes to make it decimalPlaces long
     */
    public String format(int number, int decimalPlaces)
    {
        return StringUtils.leftPad(Integer.valueOf(number).toString(), decimalPlaces, '0');
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LOJACK;
    }

    public enum Function
    {
        /* Big Endian Format */
        F0_UNKNOWN("0-UNKNOWN"),
        F1_SITE_ID("1Y-SITE ID"),
        F1_SPEED_UP("1-SPEED UP"),
        F2_TEST("2-TOWER TEST"),
        F3_DEACTIVATE("3-DEACTIVATE"),
        F4_ACTIVATE("4-ACTIVATE"),
        F5_TRANSPONDER_TEST_REPLY("5-TRANSPONDER TEST REPLY"),
        F6_TRANSPONDER_TEST_COMMAND("6-TRANSPONDER TEST COMMAND"),
        F7_QUIET_COMMAND("7-QUIET COMMAND"),
        F8_UNKNOWN("8-UNKNOWN"),
        F9_UNKNOWN("9-UNKNOWN"),
        FA_UNKNOWN("A-UNKNOWN"),
        FB_UNKNOWN("B-UNKNOWN"),
        FC_ALT_DEACTIVATE("C-ALT-DEACTIVATE"),
        FD_UNKNOWN("D-UNKNOWN"),
        FE_UNKNOWN("E-UNKNOWN"),
        FF_TRACK_PULSE("F-TRACK PULSE"),

        UNKNOWN("UNKNOWN");

        private String mLabel;

        private Function(String label)
        {
            mLabel = label;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return getLabel();
        }

        public static Function fromValue(int value, int replyCodeDigit3)
        {
            switch(value)
            {
                case 0:
                    return Function.F0_UNKNOWN;
                case 1:
                    if(replyCodeDigit3 == 31) /* 'Y' middle character */
                    {
                        return Function.F1_SITE_ID;
                    }
                    else
                    {
                        return Function.F1_SPEED_UP;
                    }
                case 2:
                    return Function.F2_TEST;
                case 3:
                    return Function.F3_DEACTIVATE;
                case 4:
                    return Function.F4_ACTIVATE;
                case 5:
                    return Function.F5_TRANSPONDER_TEST_REPLY;
                case 6:
                    return Function.F6_TRANSPONDER_TEST_COMMAND;
                case 7:
                    return Function.F7_QUIET_COMMAND;
                case 8:
                    return Function.F8_UNKNOWN;
                case 9:
                    return Function.F9_UNKNOWN;
                case 10:
                    return Function.FA_UNKNOWN;
                case 11:
                    return Function.FB_UNKNOWN;
                case 12:
                    return Function.FC_ALT_DEACTIVATE;
                case 13:
                    return Function.FD_UNKNOWN;
                case 14:
                    return Function.FE_UNKNOWN;
                case 15:
                    return Function.FF_TRACK_PULSE;
                default:
                    return Function.UNKNOWN;
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getFunctionAndReplyCodeIdentifier());
            mIdentifiers.add(getTransponder());
        }

        return mIdentifiers;
    }
}
