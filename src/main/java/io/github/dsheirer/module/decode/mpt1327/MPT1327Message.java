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
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCFleetsync;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class MPT1327Message extends Message
{
    private static String[] TELEX_LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "\n", "\n", "", "", " ", " "};
    private static String[] TELEX_FIGURES = {"-", "?", ":", "WRU", "3", "{6}", "{7}", "{8}", "8", "{BEEP}", "(", ")", ".", ",", "9", "0", "1", "4", "'", "5", "7", "=", "2", "/", "6", "+", "\n", "\n", "", "", " ", " "};
    private static String[] BCD = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ", "*", "#"};

    private static int BLOCK_1_START = 20;
    private static int BLOCK_2_START = 84;
    private static int BLOCK_3_START = 148;
    private static int BLOCK_4_START = 212;
    private static int BLOCK_5_START = 276;
    private static int BLOCK_6_START = 340;
    private static int[] REVS = {0, 1, 2, 3};
    private static int[] SYNC = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

    /* Block 1 Fields */
    private static int[] B1_SYSDEF = {21, 22, 23, 24, 25};
    private static int[] B1_PREFIX = {21, 22, 23, 24, 25, 26, 27};
    private static int[] B1_TRAFFIC_CHANNEL = {21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
    private static int[] B1_SYSTEM_ID = {26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40};
    private static int[] B1_IDENT1 = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40};
    private static int[] B1_CONTROL_CHANNEL = {31, 32, 33, 34, 35, 36, 37, 38, 39, 40};
    private static int[] B1_CHANNEL = {35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static int[] B1_MESSAGE_TYPE = {41, 42, 43, 44, 45, 46, 47, 48, 49};
    private static int[] B1_GTC_CHAN = {43, 44, 45, 46, 47, 48, 49, 50, 51, 52};
    private static int[] B1_PREFIX2 = {48, 49, 50, 51, 52, 53, 54};
    private static int[] B1_IDENT2 = {50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62};
    private static int[] B1_IDENT2_GTC = {53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65};
    private static int[] B1_IDENT2_HEAD = {55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67};
    private static int[] B1_ADJSITE = {49, 50, 51, 52};
    private static int B1_PERIODIC_CALL_MAINT_MESSAGES = 50;
    private static int[] B1_MAINT_MESSAGE_INTERVAL = {51, 52, 53, 54, 55};
    private static int[] B1_WAIT_TIME = {54, 55, 56};
    private static int B1_PRESSEL_ON_REQUIRED = 56;
    private static int[] B1_ALOHA_RSVD = {57, 58};
    private static int[] B1_ALOHA_M = {59, 60, 61, 62, 63};
    private static int[] B1_ALOHA_N = {64, 65, 66, 67};
    private static int B1_IDENT1_ID_VALUE = 57;
    private static int[] B1_SLOTS = {63, 64};
    private static int[] B1_DESCRIPTOR = {65, 66, 67};
    private static int[] B1_STATUS_MESSAGE = {63, 64, 65, 66, 67};

    /* Block 2 or First Data Codeword Fields */
    private static int[] B2_SYSTEM_ID = {85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99};
    private static int[] B2_PREFIX = {112, 113, 114, 115, 116, 117, 118};
    private static int[] B2_IDENT2 = {119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131};

    private static int B2_SDM_SEGMENT_TRANSACTION_FLAG = 85;
    private static int[] B2_SDM_GENERAL_FORMAT = {86, 87, 88};
    private static int B2_SDM_INITIAL_SEGMENT_FLAG = 89;
    private static int B2_SDM_STF0_START = 86;
    private static int B2_SDM_STF0_END = 131;
    private static int B2_SDM_STF1_START = 90;
    private static int B2_SDM_STF1_END = 131;

    /* Block 3 or Second Data Codeword Fields */
    private static int B3_SDM_RETURN_SLOT_ACCESS = 149;
    private static int B3_SDM_START = 150;
    private static int B3_SDM_END = 195;

    /* Block 4 or Third Data Codeword Fields */
    private static int B4_SDM_SEGMENT_TRANSACTION_FLAG = 213;
    private static int[] B4_SDM_NUMBER_SEGMENTS = {214, 215};
    private static int B4_SDM_CONTINUATION_SEGMENT_FLAG = 216;
    private static int B4_SDM_RESERVED_FLAG = 217;
    private static int B4_SDM_STF0_START = 214;
    private static int B4_SDM_STF0_END = 259;
    private static int B4_SDM_STF1_START = 218;
    private static int B4_SDM_STF1_END = 259;

    /* Block 5 or Fourth Data Codeword Fields */
    private static int B5_SDM_RETURN_SLOT_ACCESS = 277;
    private static int B5_SDM_START = 278;
    private static int B5_SDM_END = 323;

    private BinaryMessage mMessage;
    private CRC[] mCRC = new CRC[5];
    private MPTMessageType mMessageType;

    private MPT1327Talkgroup mFromIdentifier;
    private MPT1327Talkgroup mToIdentifier;
    private List<Identifier> mIdentifiers;

    public MPT1327Message(BinaryMessage message)
    {
        mMessage = message;

        checkParity(0, BLOCK_1_START, BLOCK_2_START);

        if(isValid())
        {
            mMessageType = getMessageType();

            switch(mMessageType)
            {
                /* 1 data block messages */
                case AHYC:
                case CLEAR:
                case MAINT:
                case MOVE:
                    break;
                /* 2 data block messages */
                case AHYQ:
                case ALH:
                case HEAD_PLUS1:
                    checkParity(1, BLOCK_2_START, BLOCK_3_START);
                    break;
                /* 3 data block messages */
                case HEAD_PLUS2:
                    checkParity(1, BLOCK_2_START, BLOCK_3_START);
                    checkParity(2, BLOCK_3_START, BLOCK_4_START);
                    break;
                /* 4 data block messages */
                case ACKT:
                case HEAD_PLUS3:
                    checkParity(1, BLOCK_2_START, BLOCK_3_START);
                    checkParity(2, BLOCK_3_START, BLOCK_4_START);
                    checkParity(3, BLOCK_4_START, BLOCK_5_START);
                    break;
                /* 5 data block messages */
                case HEAD_PLUS4:
                    checkParity(1, BLOCK_2_START, BLOCK_3_START);
                    checkParity(2, BLOCK_3_START, BLOCK_4_START);
                    checkParity(3, BLOCK_4_START, BLOCK_5_START);
                    checkParity(4, BLOCK_5_START, BLOCK_6_START);
                    break;
                case ACK:
                case ACKB:
                case ACKE:
                case ACKI:
                case ACKQ:
                case ACKV:
                case ACKX:
                case AHOY:
                case AHYP:
                case AHYX:
                case ALHD:
                case ALHE:
                case ALHF:
                case ALHR:
                case ALHS:
                case ALHX:
                case BCAST:
                case GTC:
                case MARK:
                case DACKD:
                case DACKZ:
                case DACK_DAL:
                case DACK_DALG:
                case DACK_DALN:
                case DACK_GO:
                case DAHY:
                case DAHYX:
                case DAHYZ:
                case DRQG:
                case DRQX:
                case DRQZ:
                case GTT:
                case RLA:
                case SACK:
                case SAMIS:
                case SAMIU:
                case SAMO:
                case SITH:
                case UNKN:
                default:
                    break;
            }
        }
        else
        {
            mMessageType = MPTMessageType.UNKN;
        }
    }

    /**
     * Performs CRC check against the specified section/block, using the
     * message bits between start and end.
     */
    private void checkParity(int section, int start, int end)
    {
        mCRC[section] = detectAndCorrect(start, end);
    }

    /**
     * Performs CRC check and corrects some bit errors.
     */

    //TODO: move this to the CRC class
    private CRC detectAndCorrect(int start, int end)
    {
        BitSet original = mMessage.get(start, end);

        CRC retVal = CRCFleetsync.check(original);

        //Attempt to correct single-bit errors
        if(retVal == CRC.FAILED_PARITY)
        {
            int[] errorBitPositions = CRCFleetsync.findBitErrors(original);

            if(errorBitPositions != null)
            {
                for(int errorBitPosition : errorBitPositions)
                {
                    mMessage.flip(start + errorBitPosition);
                }

                retVal = CRC.CORRECTED;
            }
        }

        return retVal;
    }

    /**
     * String representing results of the parity check
     *
     * [P] = passes parity check
     * [f] = fails parity check
     * [C] = corrected message
     * [-] = message section not present
     */
    public String getParity()
    {
        return "[" + CRC.format(mCRC) + "]";
    }

    /**
     * Indicates if Block 1 of the message has passed the CRC check.
     */
    public boolean isValid()
    {
        return mCRC[0] == CRC.PASSED ||
            mCRC[0] == CRC.CORRECTED;
    }

    public boolean isValidCall()
    {
        return getPrefix() != 0 &&
            getIdent1() != 0 &&
            getIdent2() != 0;
    }

    /**
     * Constructs a decoded message string
     */
    @Override
    public String toString()
    {
        return getMessage();
    }

    /**
     * Determines the message type from block 1
     */
    public MPTMessageType getMessageType()
    {
        int value = mMessage.getInt(B1_MESSAGE_TYPE);

        return MPTMessageType.fromNumber(value);
    }

    /**
     * MPT1327 Site identifier
     */
    public String getSiteID()
    {
        if(mMessageType == MPTMessageType.BCAST)
        {
            return String.valueOf(mMessage.getInt(B1_SYSTEM_ID));
        }
        else if(mMessageType == MPTMessageType.ALH)
        {
            return String.valueOf(mMessage.getInt(B2_SYSTEM_ID));
        }
        else
        {
            return null;
        }
    }

    /**
     * Indicates if this message has a system identifier
     */
    public boolean hasSystemID()
    {
        return getSiteID() != null;
    }

    public SystemDefinition getSystemDefinition()
    {
        int sysdef = mMessage.getInt(B1_SYSDEF);

        return SystemDefinition.fromNumber(sysdef);
    }

    public boolean getPeriodicMaintenanceMessagesRequired()
    {
        return mMessage.get(B1_PERIODIC_CALL_MAINT_MESSAGES);
    }

    public int getMaintenanceMessageInterval()
    {
        return mMessage.getInt(B1_MAINT_MESSAGE_INTERVAL);
    }

    public boolean getPresselOnRequired()
    {
        return mMessage.get(B1_PRESSEL_ON_REQUIRED);
    }

    public String getMaintMessageIDENT1Value()
    {
        return (mMessage.get(B1_IDENT1_ID_VALUE) ? "GROUP ADDRESS" :
            "INDIVIDUAL ADDRESS");
    }

    public int getChannel()
    {
        switch(mMessageType)
        {
            case CLEAR:
                return mMessage.getInt(B1_TRAFFIC_CHANNEL);
            case GTC:
                return mMessage.getInt(B1_GTC_CHAN);
            default:
                return mMessage.getInt(B1_CHANNEL);
        }
    }

    public int getReturnToChannel()
    {
        if(mMessageType == MPTMessageType.CLEAR)
        {
            return mMessage.getInt(B1_CONTROL_CHANNEL);
        }
        else
        {
            return 0;
        }
    }

    public int getAdjacentSiteSerialNumber()
    {
        return mMessage.getInt(B1_ADJSITE);
    }

    public int getPrefix()
    {
        return mMessage.getInt(B1_PREFIX);
    }

    public int getBlock2Prefix()
    {
        return mMessage.getInt(B2_PREFIX);
    }

    public int getIdent1()
    {
        return mMessage.getInt(B1_IDENT1);
    }

    public IdentType getIdent1Type()
    {
        return IdentType.fromIdent(getIdent1());
    }

    public int getIdent2()
    {
        MPTMessageType type = getMessageType();

        if(type == MPTMessageType.GTC)
        {
            return mMessage.getInt(B1_IDENT2_GTC);
        }
        else if(type == MPTMessageType.HEAD_PLUS1 ||
            type == MPTMessageType.HEAD_PLUS2 ||
            type == MPTMessageType.HEAD_PLUS3 ||
            type == MPTMessageType.HEAD_PLUS4)
        {
            return mMessage.getInt(B1_IDENT2_HEAD);
        }
        else
        {
            return mMessage.getInt(B1_IDENT2);
        }
    }

    public IdentType getIdent2Type()
    {
        return IdentType.fromIdent(getIdent2());
    }

    public int getBlock2Ident2()
    {
        return mMessage.getInt(B2_IDENT2);
    }

    public String getStatusMessage()
    {
        int status = mMessage.getInt(B1_STATUS_MESSAGE);

        switch(status)
        {
            case 0:
                return "STATUS: Request Speech Call";
            case 31:
                return "STATUS: Cancel Request Speech Call";
            default:
                return "STATUS: " + status;
        }
    }

    /**
     * Returns spaces to the fill the string builder to ensure length is >= index
     */
    public String getFiller(StringBuilder sb, int index)
    {
        if(sb.length() < index)
        {
            return String.format("%" + (index - sb.length()) + "s", " ");
        }
        else
        {
            return "";
        }
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

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(pad(mMessageType.toString(), 5, " "));

        switch(mMessageType)
        {
            case ACK:
                sb.append(" ACKNOWLEDGE ");

                if(hasFromID())
                {
                    sb.append(getFromID());
                }

                IdentType type = getIdent1Type();

                switch(type)
                {
                    case ALLI:
                    case IPFIXI:
                    case PABXI:
                    case PSTNGI:
                    case PSTNSI1:
                    case PSTNSI2:
                    case PSTNSI3:
                    case PSTNSI4:
                    case PSTNSI5:
                    case PSTNSI6:
                    case PSTNSI7:
                    case PSTNSI8:
                    case PSTNSI9:
                    case PSTNSI10:
                    case PSTNSI11:
                    case PSTNSI12:
                    case PSTNSI13:
                    case PSTNSI14:
                    case PSTNSI15:
                    case USER:
                        sb.append(type.getLabel());
                        sb.append(" CALL REQUEST");
                        break;
                    case TSCI:
                        sb.append(" RQQ or RQC TRANSACTION");
                        break;
                    case DIVERTI:
                        sb.append(" CANCELLATION OF CALL DIVERSION REQUEST");
                        break;
                    default:
                        sb.append(" ");
                        sb.append(type.getLabel());
                        sb.append(" REQUEST");
                        break;
                }
                break;
            case ACKI:
                sb.append(" MESSAGE ACKNOWLEDGED - MORE TO FOLLOW");

                if(hasFromID())
                {
                    sb.append(" FROM:");
                    sb.append(getFromID());
                }

                if(hasToID())
                {
                    sb.append(" TO:");
                    sb.append(getToID());
                }
                break;
            case ACKT:
                sb.append(" SITE:");
                sb.append(getSiteID());

                sb.append(" LONG ACK MESSAGE");

                if(hasFromID())
                {
                    sb.append(" FROM:");
                    sb.append(getFromID());
                }

                if(hasToID())
                {
                    sb.append(" TO:");
                    sb.append(getToID());
                }

                sb.append("**********************************");
                break;
            case ACKQ:
                sb.append(" SYSTEM:");
                sb.append(getSiteID());

                sb.append(" CALL QUEUED FROM:");

                if(hasFromID())
                {
                    sb.append(" FROM:");
                    sb.append(getFromID());
                }

                if(hasToID())
                {
                    sb.append(" TO:");
                    sb.append(getToID());
                }
                break;
            case ACKX:
                sb.append(" SYSTEM:");
                sb.append(getSiteID());

                sb.append(" MESSAGE REJECTED FROM:");

                if(hasFromID())
                {
                    sb.append(" FROM:");
                    sb.append(getFromID());
                }

                if(hasToID())
                {
                    sb.append(" TO:");
                    sb.append(getToID());
                }
                break;
            case AHYC:
                sb.append(" FM:");
                sb.append(getFromID());
                sb.append(" TO:");
                sb.append(getToID());
                sb.append(" ");
                sb.append(getRequestString());
                break;
            case AHYQ:
                /* Status Message */
                sb.append(" STATUS MESSAGE");

                if(hasFromID())
                {
                    sb.append(" FROM:");
                    sb.append(getFromID());
                }

                if(hasToID())
                {
                    sb.append(" TO:");
                    sb.append(getToID());
                }
                sb.append(" ");
                sb.append(getStatusMessage());
                break;
            case ALH:
            case ALHD:
            case ALHS:
            case ALHE:
            case ALHR:
            case ALHX:
            case ALHF:
                sb.append(" SYSTEM:");
                sb.append(getSiteID());

                if(hasToID())
                {
                    sb.append(" ID:");
                    sb.append(getToID());
                }

                sb.append(" WAIT:");
                sb.append(getWaitTime());
                sb.append(" RSVD:");
                sb.append(getAlohaReserved());
                sb.append(" M:");
                sb.append(getAlohaM());
                sb.append(" N:");
                sb.append(getAlohaN());
                break;
            case BCAST:
                sb.append(" SYSTEM:");
                sb.append(getSiteID());

                SystemDefinition sysdef = getSystemDefinition();

                sb.append(" ");
                sb.append(sysdef.getLabel());

                switch(sysdef)
                {
                    case ANNOUNCE_CONTROL_CHANNEL:
                    case WITHDRAW_CONTROL_CHANNEL:
                        sb.append(" CHAN:");
                        sb.append(getChannel());
                        break;
                    case BROADCAST_ADJACENT_SITE_CONTROL_CHANNEL_NUMBER:
                        sb.append(" CHAN:");
                        sb.append(getChannel());
                        sb.append(" SER:");
                        sb.append(getAdjacentSiteSerialNumber());
                        break;
                    case SPECIFY_CALL_MAINTENANCE_PARAMETERS:
                        sb.append(" PERIODIC MAINT MSG REQD:");
                        sb.append(getPeriodicMaintenanceMessagesRequired());
                        sb.append(" INTERVAL:");
                        sb.append(getMaintenanceMessageInterval());
                        sb.append("(sec) PRESSEL ON REQD:");
                        sb.append(getPresselOnRequired());
                        sb.append(" IDENT1 VALUE:");
                        sb.append(getMaintMessageIDENT1Value());
                        break;
                    default:
                        break;
                }
                break;
            case CLEAR:
                sb.append(" TRAFFIC CHANNEL:");
                sb.append(getChannel());
                sb.append(" RETURN TO CONTROL CHANNEL:");
                sb.append(getReturnToChannel());
                break;
            case GTC:
                if(hasFromID())
                {
                    sb.append(" FROM:");
                    sb.append(getFromID());
                }

                if(hasToID())
                {
                    sb.append(" TO:");
                    sb.append(getToID());
                }

                sb.append(" CHAN:");
                sb.append(getChannel());
                break;
            case MAINT:
                if(hasToID())
                {
                    sb.append(" ID:");
                    sb.append(getToID());
                }

                break;
            case HEAD_PLUS1:
            case HEAD_PLUS2:
            case HEAD_PLUS3:
            case HEAD_PLUS4:
                sb.append(" ");
                sb.append(getSDM());
                break;
            default:
                break;
        }

        return sb.toString();
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MPT1327;
    }

    /**
     * Translates the AHYC request from the values in ident1 and ident2
     */
    public String getRequestString()
    {
        StringBuilder sb = new StringBuilder();

        IdentType type2 = getIdent2Type();

        if(type2 == IdentType.USER)
        {
            IdentType type1 = getIdent1Type();

            Slots slots = getSlots();

            switch(type1)
            {
                case DIVERTI:
                    sb.append("SEND BLOCKED ADDRESS FOR THIRD-PARTY CALL DIVERSION USING ");
                    sb.append(slots.getLabel());
                    break;
                case IPFIXI:
                    sb.append("SEND INTER-PREFIX CALL EXTENDED ADDRESSING INFORMATION USING ");
                    sb.append(slots.getLabel());
                    break;
                case PABXI:
                    sb.append("SEND PABX EXTENSION USING");
                    sb.append(slots.getLabel());
                    break;
                case PSTNGI:
                    if(slots == Slots.SLOTS_1)
                    {
                        sb.append("SEND UP TO 9 PSTN DIALED DIGITS");
                    }
                    else if(slots == Slots.SLOTS_2)
                    {
                        sb.append("SEND 10 TO 31 PSTN DIALED DIGITS");
                    }
                    else
                    {
                        sb.append("SEND PSTN DIALED DIGITS USING ");
                        sb.append(slots.getLabel());
                    }
                    break;
                case SDMI:
                    sb.append("SEND SHORT DATA MESSAGE USING ");
                    sb.append(slots.getLabel());
                    break;
                default:
                    break;
            }
        }
        else
        {
            /* MODE 2 */
            sb.append("SEND ");
            sb.append(getDescriptor().getMode2Label());
        }

        return sb.toString();
    }

    /**
     * Constructs a FROM identifier from the Prefix2 and Ident2 values
     */
    public String getFromID()
    {
        StringBuilder sb = new StringBuilder();

        int ident2 = getIdent2();

        IdentType type = IdentType.fromIdent(ident2);

        /* Inter-Prefix - the from and to idents are different prefixes */
        if(type == IdentType.IPFIXI)
        {
            sb.append(format(getBlock2Prefix(), 3));
            sb.append("-");
            sb.append(format(getBlock2Ident2(), 4));
        }
        else
        {
            sb.append(format(getPrefix(), 3));
            sb.append("-");
            sb.append(format(ident2, 4));
        }

        return sb.toString();
    }

    public boolean hasFromID()
    {
        return getFromID() != null && !getFromID().isEmpty();
    }

    /**
     * Constructs a TO identifier from the Prefix1 and Ident1 fields
     */
    public String getToID()
    {
        StringBuilder sb = new StringBuilder();

        int prefix = getPrefix();
        int ident = getIdent1();

        switch(IdentType.fromIdent(ident))
        {
            case IPFIXI:
                sb.append("INTER-PREFIX");
                break;
            case ALLI:
                sb.append("ALL RADIOS");
                break;
            case PABXI:
                sb.append("PABX EXT");
                break;
            case PSTNSI1:
            case PSTNSI2:
            case PSTNSI3:
            case PSTNSI4:
            case PSTNSI5:
            case PSTNSI6:
            case PSTNSI7:
            case PSTNSI8:
            case PSTNSI9:
            case PSTNSI10:
            case PSTNSI11:
            case PSTNSI12:
            case PSTNSI13:
            case PSTNSI14:
            case PSTNSI15:
                sb.append("PRE-DEFINED PSTN");
                break;
            case PSTNGI:
                sb.append("PSTN GATEWAY");
                break;
            case TSCI:
                sb.append("SYSTEM CONTROLLER");
                break;
            case DIVERTI:
                sb.append("CALL DIVERT");
                break;
            case USER:
            default:
                if(prefix != 0 || ident != 0)
                {
                    sb.append(format(prefix, 3));
                    sb.append("-");
                    sb.append(format(ident, 4));
                }
                break;
        }

        return sb.toString();
    }

    public boolean hasToID()
    {
        return getToID() != null && !getToID().isEmpty();
    }

    /**
     * Data message codeword descriptor.  Indicates the type of data message
     * that the radio unit shall respond with.
     */
    public Descriptor getDescriptor()
    {
        return Descriptor.fromNumber(mMessage.getInt(B1_DESCRIPTOR));
    }

    /**
     * Indicates the number of data codeword slots that are appended to a HEAD
     * message
     */
    public Slots getSlots()
    {
        return Slots.fromNumber(mMessage.getInt(B1_SLOTS));
    }

    /**
     * Aloha wait time - random access protocol
     */
    public int getWaitTime()
    {
        return mMessage.getInt(B1_WAIT_TIME);
    }

    /**
     * Aloha reserved field
     */
    public int getAlohaReserved()
    {
        return mMessage.getInt(B1_ALOHA_RSVD);
    }

    /**
     * Aloha M field
     */
    public int getAlohaM()
    {
        return mMessage.getInt(B1_ALOHA_M);
    }

    /**
     * Aloha N field
     */
    public int getAlohaN()
    {
        return mMessage.getInt(B1_ALOHA_N);
    }

    /**
     * String representation of the CRC check for each 64-bit message block in
     * this message.
     */
    public String getErrorStatus()
    {
        return getParity();
    }

    /**
     * Indicates if the received SDM is formatted according to the MPT-1327 or
     * the MPT-1343 ICD.
     */
    public SDMFormat getSDMFormat()
    {
        return SDMFormat.valueOf(mMessage.get(B2_SDM_SEGMENT_TRANSACTION_FLAG),
            mMessage.getInt(B2_SDM_GENERAL_FORMAT));
    }

    public String getSDM()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("SDM:");

        MPTMessageType type = getMessageType();

        switch(type)
        {
            case HEAD_PLUS1:
            case HEAD_PLUS2:
            case HEAD_PLUS3:
            case HEAD_PLUS4:
                SDMFormat format = getSDMFormat();

                switch(format)
                {
                    case MPT1327:
                        sb.append("MPT1327 ");
                        sb.append(getSDMBinary(type, format));
                        break;
                    case MPT1343_BINARY:
                        sb.append("BINARY ");
                        sb.append(getSDMBinary(type, format));
                        break;
                    case MPT1343_COMMAND:
                        sb.append("COMMAND ");
                        sb.append(getSDMBinary(type, format));
                        break;
                    case MPT1343_MAP27:
                        sb.append("MAP27 ");
                        sb.append(getSDMBinary(type, format));
                        break;
                    case MPT1343_RESERVED:
                        sb.append("RESERVED ");
                        sb.append(getSDMBinary(type, format));
                        break;
                    case MPT1343_SPARE:
                        sb.append("SPARE ");
                        sb.append(getSDMBinary(type, format));
                        break;
                    case MPT1343_ASCII:
                        sb.append("ASCII ");
                        sb.append(getSDMASCII(type));
                        break;
                    case MPT1343_BCD:
                        sb.append("BCD ");
                        sb.append(getSDMBCD(type));
                        break;
                    case MPT1343_TELEX:
                        sb.append("TELEX ");
                        sb.append(getSDMTelex(type));
                        break;
                    case UNKNOWN:
                    default:
                        sb.append("UNKNOWN ");
                        break;
                }

                break;
            case SAMO:
                sb.append("SAMO ");
                break;
            case SAMIS:
                sb.append("SAMIS ");
                break;
            case SAMIU:
                sb.append("SAMIU ");
                break;
            default:
                break;

        }

        return sb.toString();
    }

    public String getSDMBinary(MPTMessageType type, SDMFormat format)
    {
        StringBuilder sb = new StringBuilder();

        switch(type)
        {
            case HEAD_PLUS1:
                if(format == SDMFormat.MPT1327)
                {
                    sb.append(mMessage.getHex(B2_SDM_STF0_START, B2_SDM_STF0_END));
                }
                else
                {
                    sb.append(mMessage.getHex(B2_SDM_STF1_START, B2_SDM_STF1_END));
                }
                break;
            case HEAD_PLUS2:
                if(format == SDMFormat.MPT1327)
                {
                    sb.append(mMessage.getHex(B2_SDM_STF0_START, B2_SDM_STF0_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B3_SDM_START, B3_SDM_END));
                }
                else
                {
                    sb.append(mMessage.getHex(B2_SDM_STF1_START, B2_SDM_STF1_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B3_SDM_START, B3_SDM_END));
                }
                break;
            case HEAD_PLUS3:
                if(format == SDMFormat.MPT1327)
                {
                    sb.append(mMessage.getHex(B2_SDM_STF0_START, B2_SDM_STF0_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B3_SDM_START, B3_SDM_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B4_SDM_STF0_START, B4_SDM_STF0_END));
                }
                else
                {
                    sb.append(mMessage.getHex(B2_SDM_STF1_START, B2_SDM_STF1_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B3_SDM_START, B3_SDM_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B4_SDM_STF1_START, B4_SDM_STF1_END));
                }
                break;
            case HEAD_PLUS4:
                if(format == SDMFormat.MPT1327)
                {
                    sb.append(mMessage.getHex(B2_SDM_STF0_START, B2_SDM_STF0_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B3_SDM_START, B3_SDM_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B4_SDM_STF0_START, B4_SDM_STF0_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B5_SDM_START, B5_SDM_END));
                }
                else
                {
                    sb.append(mMessage.getHex(B2_SDM_STF1_START, B2_SDM_STF1_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B3_SDM_START, B3_SDM_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B4_SDM_STF1_START, B4_SDM_STF1_END));
                    sb.append(" ");
                    sb.append(mMessage.getHex(B5_SDM_START, B5_SDM_END));
                }
                break;
            default:
                break;
        }

        return sb.toString();
    }

    public String getSDMASCII(MPTMessageType type)
    {
        StringBuilder sb = new StringBuilder();

        int blocks = 0;

        switch(type)
        {
            case HEAD_PLUS1:
                blocks = 1;
                break;
            case HEAD_PLUS2:
                blocks = 2;
                break;
            case HEAD_PLUS3:
                blocks = 3;
                break;
            case HEAD_PLUS4:
                blocks = 4;
                break;
            default:
                break;
        }

        if(blocks >= 1)
        {
            sb.append((char)mMessage.getInt(91, 97));
            sb.append((char)mMessage.getInt(98, 104));
            sb.append((char)mMessage.getInt(105, 111));
            sb.append((char)mMessage.getInt(112, 118));
            sb.append((char)mMessage.getInt(119, 125));
        }

        if(blocks >= 2)
        {
            sb.append((char)mMessage.getInt(
                new int[]{126, 127, 128, 129, 130, 131, 150}));
            sb.append((char)mMessage.getInt(151, 157));
            sb.append((char)mMessage.getInt(158, 164));
            sb.append((char)mMessage.getInt(165, 171));
            sb.append((char)mMessage.getInt(172, 178));
            sb.append((char)mMessage.getInt(179, 185));
            sb.append((char)mMessage.getInt(186, 192));
        }

        if(blocks >= 3)
        {
            sb.append((char)mMessage.getInt(
                new int[]{193, 194, 195, 218, 219, 220, 221}));
            sb.append((char)mMessage.getInt(222, 228));
            sb.append((char)mMessage.getInt(229, 235));
            sb.append((char)mMessage.getInt(236, 242));
            sb.append((char)mMessage.getInt(243, 249));
            sb.append((char)mMessage.getInt(250, 256));
        }

        if(blocks == 4)
        {
            sb.append((char)mMessage.getInt(
                new int[]{257, 258, 259, 278, 279, 280, 281}));
            sb.append((char)mMessage.getInt(282, 288));
            sb.append((char)mMessage.getInt(289, 295));
            sb.append((char)mMessage.getInt(296, 302));
            sb.append((char)mMessage.getInt(303, 309));
            sb.append((char)mMessage.getInt(310, 316));
            sb.append((char)mMessage.getInt(317, 323));
        }

        return sb.toString();
    }

    public String getSDMBCD(MPTMessageType type)
    {
        StringBuilder sb = new StringBuilder();

        int blocks = 0;

        switch(type)
        {
            case HEAD_PLUS1:
                blocks = 1;
                break;
            case HEAD_PLUS2:
                blocks = 2;
                break;
            case HEAD_PLUS3:
                blocks = 3;
                break;
            case HEAD_PLUS4:
                blocks = 4;
                break;
            default:
                break;
        }

        if(blocks >= 1)
        {
            sb.append(getBCD(mMessage.getInt(90, 93)));
            sb.append(getBCD(mMessage.getInt(94, 97)));
            sb.append(getBCD(mMessage.getInt(98, 101)));
            sb.append(getBCD(mMessage.getInt(102, 105)));
            sb.append(getBCD(mMessage.getInt(106, 109)));
            sb.append(getBCD(mMessage.getInt(110, 113)));
            sb.append(getBCD(mMessage.getInt(114, 117)));
            sb.append(getBCD(mMessage.getInt(118, 121)));
            sb.append(getBCD(mMessage.getInt(122, 125)));
            sb.append(getBCD(mMessage.getInt(126, 129)));
        }

        if(blocks >= 2)
        {
            sb.append(getBCD(mMessage.getInt(
                new int[]{130, 131, 150, 151})));
            sb.append(getBCD(mMessage.getInt(152, 155)));
            sb.append(getBCD(mMessage.getInt(156, 159)));
            sb.append(getBCD(mMessage.getInt(160, 163)));
            sb.append(getBCD(mMessage.getInt(164, 167)));
            sb.append(getBCD(mMessage.getInt(168, 171)));
            sb.append(getBCD(mMessage.getInt(172, 175)));
            sb.append(getBCD(mMessage.getInt(176, 179)));
            sb.append(getBCD(mMessage.getInt(180, 183)));
            sb.append(getBCD(mMessage.getInt(184, 187)));
            sb.append(getBCD(mMessage.getInt(188, 191)));
            sb.append(getBCD(mMessage.getInt(192, 195)));
        }

        if(blocks >= 3)
        {
            sb.append(getBCD(mMessage.getInt(218, 221)));
            sb.append(getBCD(mMessage.getInt(222, 225)));
            sb.append(getBCD(mMessage.getInt(226, 229)));
            sb.append(getBCD(mMessage.getInt(230, 233)));
            sb.append(getBCD(mMessage.getInt(234, 237)));
            sb.append(getBCD(mMessage.getInt(238, 241)));
            sb.append(getBCD(mMessage.getInt(242, 245)));
            sb.append(getBCD(mMessage.getInt(246, 249)));
            sb.append(getBCD(mMessage.getInt(250, 253)));
            sb.append(getBCD(mMessage.getInt(254, 257)));
        }

        if(blocks == 4)
        {
            sb.append(getBCD(mMessage.getInt(
                new int[]{258, 259, 278, 279})));
            sb.append(getBCD(mMessage.getInt(280, 283)));
            sb.append(getBCD(mMessage.getInt(284, 287)));
            sb.append(getBCD(mMessage.getInt(288, 291)));
            sb.append(getBCD(mMessage.getInt(292, 295)));
            sb.append(getBCD(mMessage.getInt(296, 299)));
            sb.append(getBCD(mMessage.getInt(300, 303)));
            sb.append(getBCD(mMessage.getInt(304, 307)));
            sb.append(getBCD(mMessage.getInt(308, 311)));
            sb.append(getBCD(mMessage.getInt(312, 315)));
            sb.append(getBCD(mMessage.getInt(316, 319)));
            sb.append(getBCD(mMessage.getInt(320, 232)));
        }

        return sb.toString();
    }

    private String getBCD(int value)
    {
        if(0 <= value && value <= 12)
        {
            return BCD[value];
        }
        else
        {
            return "?";
        }
    }

    public String getSDMTelex(MPTMessageType type)
    {
        ArrayList<Integer> values = new ArrayList<>();

        int blocks = 0;

        switch(type)
        {
            case HEAD_PLUS1:
                blocks = 1;
                break;
            case HEAD_PLUS2:
                blocks = 2;
                break;
            case HEAD_PLUS3:
                blocks = 3;
                break;
            case HEAD_PLUS4:
                blocks = 4;
                break;
            default:
                break;
        }

        if(blocks >= 1)
        {
            values.add(mMessage.getInt(91, 95));
            values.add(mMessage.getInt(96, 100));
            values.add(mMessage.getInt(101, 105));
            values.add(mMessage.getInt(106, 110));
            values.add(mMessage.getInt(111, 115));
            values.add(mMessage.getInt(116, 120));
            values.add(mMessage.getInt(121, 125));
            values.add(mMessage.getInt(126, 130));
        }

        if(blocks >= 2)
        {
            values.add(mMessage.getInt(
                new int[]{131, 150, 151, 152, 153}));
            values.add(mMessage.getInt(154, 158));
            values.add(mMessage.getInt(159, 163));
            values.add(mMessage.getInt(164, 168));
            values.add(mMessage.getInt(169, 173));
            values.add(mMessage.getInt(174, 178));
            values.add(mMessage.getInt(179, 183));
            values.add(mMessage.getInt(184, 188));
            values.add(mMessage.getInt(189, 193));
        }

        if(blocks >= 3)
        {
            values.add(mMessage.getInt(
                new int[]{194, 195, 218, 219, 220}));
            values.add(mMessage.getInt(221, 225));
            values.add(mMessage.getInt(226, 230));
            values.add(mMessage.getInt(231, 235));
            values.add(mMessage.getInt(236, 240));
            values.add(mMessage.getInt(241, 245));
            values.add(mMessage.getInt(246, 250));
        }

        if(blocks == 4)
        {
            values.add(mMessage.getInt(
                new int[]{251, 252, 253, 254, 278}));
            values.add(mMessage.getInt(279, 283));
            values.add(mMessage.getInt(284, 288));
            values.add(mMessage.getInt(289, 293));
            values.add(mMessage.getInt(294, 298));
            values.add(mMessage.getInt(299, 303));
            values.add(mMessage.getInt(304, 308));
            values.add(mMessage.getInt(309, 313));
            values.add(mMessage.getInt(314, 318));
            values.add(mMessage.getInt(319, 323));
        }

        StringBuilder sb = new StringBuilder();

        /* Decode the numeric values.  28 and 29 are used to shift the decoder
         * to use the figures alphabet or the letters alphabet */
        boolean figure = false;

        for(Integer value : values)
        {
            if(0 <= value && value < 32)
            {
                if(value == 28)
                {
                    figure = false;
                }
                else if(value == 29)
                {
                    figure = true;
                }
                else
                {
                    if(figure)
                    {
                        sb.append(TELEX_FIGURES[value]);
                    }
                    else
                    {
                        sb.append(TELEX_LETTERS[value]);
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * SDM Message format ICDs
     */
    public enum SDMFormat
    {
        MPT1327,
        MPT1343_BINARY,
        MPT1343_BCD,
        MPT1343_TELEX,
        MPT1343_ASCII,
        MPT1343_RESERVED,
        MPT1343_SPARE,
        MPT1343_COMMAND,
        MPT1343_MAP27,
        UNKNOWN;

        public static SDMFormat valueOf(boolean stf, int gfi)
        {
            if(stf) //MPT1343
            {
                switch(gfi)
                {
                    case 0:
                        return MPT1343_BINARY;
                    case 1:
                        return MPT1343_BCD;
                    case 2:
                        return MPT1343_TELEX;
                    case 3:
                        return MPT1343_ASCII;
                    case 4:
                        return MPT1343_RESERVED;
                    case 5:
                        return MPT1343_SPARE;
                    case 6:
                        return MPT1343_COMMAND;
                    case 7:
                        return MPT1343_MAP27;
                    default:
                        return UNKNOWN;
                }
            }
            else
            {
                return MPT1327;
            }
        }
    }


    public enum IdentType
    {
        ALLI("SYSTEM-WIDE"),
        DIVERTI("DIVERT"),
        DNI("DATA NETWORK GATEWAY"),
        DUMMYI("DUMMY IDENT"),
        INCI("INCLUDE IN CALL"),
        IPFIXI("INTER-PREFIX"),
        PABXI("PABX GATEWAY"),
        PSTNGI("PSTN GATEWAY"),
        PSTNSI1("PSTN OR NETWORK 1"),
        PSTNSI2("PSTN OR NETWORK 2"),
        PSTNSI3("PSTN OR NETWORK 3"),
        PSTNSI4("PSTN OR NETWORK 4"),
        PSTNSI5("PSTN OR NETWORK 5"),
        PSTNSI6("PSTN OR NETWORK 6"),
        PSTNSI7("PSTN OR NETWORK 7"),
        PSTNSI8("PSTN OR NETWORK 8"),
        PSTNSI9("PSTN OR NETWORK 9"),
        PSTNSI10("PSTN OR NETWORK 10"),
        PSTNSI11("PSTN OR NETWORK 11"),
        PSTNSI12("PSTN OR NETWORK 12"),
        PSTNSI13("PSTN OR NETWORK 13"),
        PSTNSI14("PSTN OR NETWORK 14"),
        PSTNSI15("PSTN OR NETWORK 15"),
        REGI("REGISTRATION"),
        RESERVED("RESERVED"),
        SDMI("SHORT DATA MESSAGE"),
        SPARE("SPARE"),
        TSCI("SYSTEM CONTROLLER"),
        USER("COMMON-PREFIX IDENT"),
        UNKNOWN("UNKNOWN");

        private String mLabel;

        IdentType(String label)
        {
            mLabel = label;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public static IdentType fromIdent(int ident)
        {
            switch(ident)
            {
                case 0:
                    return DUMMYI;
                case 8101:
                    return PSTNGI;
                case 8102:
                    return PABXI;
                case 8103:
                    return DNI;
                case 8121:
                    return PSTNSI1;
                case 8122:
                    return PSTNSI2;
                case 8123:
                    return PSTNSI3;
                case 8124:
                    return PSTNSI4;
                case 8125:
                    return PSTNSI5;
                case 8126:
                    return PSTNSI6;
                case 8127:
                    return PSTNSI7;
                case 8128:
                    return PSTNSI8;
                case 8129:
                    return PSTNSI9;
                case 8130:
                    return PSTNSI10;
                case 8131:
                    return PSTNSI11;
                case 8132:
                    return PSTNSI12;
                case 8133:
                    return PSTNSI13;
                case 8134:
                    return PSTNSI14;
                case 8135:
                    return PSTNSI15;
                case 8181:
                case 8182:
                case 8183:
                case 8184:
                    return RESERVED;
                case 8185:
                    return REGI;
                case 8186:
                    return INCI;
                case 8187:
                    return DIVERTI;
                case 8188:
                    return SDMI;
                case 8189:
                    return IPFIXI;
                case 8190:
                    return TSCI;
                case 8191:
                    return ALLI;
                default:
                    if(1 <= ident && ident <= 8100)
                    {
                        return USER;
                    }
                    else if((8104 <= ident && ident <= 8120) ||
                        (8136 <= ident && ident <= 8180))
                    {
                        return SPARE;
                    }
                    else
                    {
                        return UNKNOWN;
                    }
            }
        }
    }

    public enum Descriptor
    {
        DESC0("EXTENDED ADDRESSING INFORMATION",
            "ESN",
            "SINGLE SEGMENT TRANSACTIONS (SST) ONLY"),
        DESC1("PSTN DIALED DIGITS", "RESERVED", "N/A"),
        DESC2("PABX EXTENSION", "RESERVED", "N/A"),
        DESC3("N/A", "N/A", "N/A"),
        DESC4("N/A", "N/A", "FIRST MST SEGMENT OR SINGLE SST"),
        DESC5("N/A", "N/A", "SECOND MST SEGMENT OR SINGLE SST"),
        DESC6("N/A", "N/A", "THIRD MST SEGMENT OR SINGLE SST"),
        DESC7("RESERVED", "RESERVED", "FOURTH MST SEGMENT OR SINGLE SST"),
        UNKNOWN("UNKNOWN", "UNKNOWN", "UNKNOWN");

        private String mMode1Label;
        private String mMode2Label;
        private String mSDMLabel;

        Descriptor(String mode1Label, String mode2Label, String sdmLabel)
        {
            mMode1Label = mode1Label;
            mMode2Label = mode2Label;
            mSDMLabel = sdmLabel;
        }

        /* Label that applies to AHYC Mode 1 responses */
        public String getMode1Label()
        {
            return mMode1Label;
        }

        /* Label that applies to AHYC Mode 2 responses */
        public String getMode2Label()
        {
            return mMode2Label;
        }

        /* Label that applies to Short Data Messages (SDM) */
        public String getSDMLabel()
        {
            return mSDMLabel;
        }

        public static Descriptor fromNumber(int number)
        {
            switch(number)
            {
                case 0:
                    return DESC0;
                case 1:
                    return DESC1;
                case 2:
                    return DESC2;
                case 3:
                    return DESC3;
                case 4:
                    return DESC4;
                case 5:
                    return DESC5;
                case 6:
                    return DESC6;
                case 7:
                    return DESC7;
                default:
                    return UNKNOWN;
            }
        }
    }

    /**
     * Indicates the number of data codeword slots appended to a HEAD message
     */
    public enum Slots
    {
        SLOTS_0("RESERVED"),
        SLOTS_1("ADDRESS CODEWORD ONLY"),
        SLOTS_2("ADDRESS CODEWORD & 1-2 DATA CODEWORDS"),
        SLOTS_3("ADDRESS CODEWORD & 3-4 DATA CODEWORDS"),
        UNKNOWN("UNKNOWN");

        private String mLabel;

        Slots(String label)
        {
            mLabel = label;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public static Slots fromNumber(int number)
        {
            switch(number)
            {
                case 0:
                    return SLOTS_0;
                case 1:
                    return SLOTS_1;
                case 2:
                    return SLOTS_2;
                case 3:
                    return SLOTS_3;
                default:
                    return UNKNOWN;
            }
        }
    }


    public enum SystemDefinition
    {
        UNKNOWN("UNKNOWN"),
        ANNOUNCE_CONTROL_CHANNEL("ANNOUNCE CONTROL CHANNEL"),
        WITHDRAW_CONTROL_CHANNEL("WITHDRAW CONTROL CHANNEL"),
        SPECIFY_CALL_MAINTENANCE_PARAMETERS("CALL MAINT PARAMETERS"),
        SPECIFY_REGISTRATION_PARAMETERS("REGISTRATION PARAMETERS"),
        BROADCAST_ADJACENT_SITE_CONTROL_CHANNEL_NUMBER("NEIGHBOR"),
        VOTE_NOW_ADVICE("VOTE NOW");

        private String mLabel;

        SystemDefinition(String label)
        {
            mLabel = label;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public static SystemDefinition fromNumber(int number)
        {
            switch(number)
            {
                case 0:
                    return ANNOUNCE_CONTROL_CHANNEL;
                case 1:
                    return WITHDRAW_CONTROL_CHANNEL;
                case 2:
                    return SPECIFY_CALL_MAINTENANCE_PARAMETERS;
                case 3:
                    return SPECIFY_REGISTRATION_PARAMETERS;
                case 4:
                    return BROADCAST_ADJACENT_SITE_CONTROL_CHANNEL_NUMBER;
                case 5:
                    return VOTE_NOW_ADVICE;
                default:
                    return UNKNOWN;
            }
        }
    }

    public enum MPTMessageType
    {
        UNKN("Unknown"),
        GTC("GTC - Goto Channel"),                    //0
        ALH("ALH - Aloha"),                           //256
        ALHS("ALHS - Standard Data Excluded"),         //257
        ALHD("ALHD - Simple Calls Excluded"),          //258
        ALHE("ALHE - Emergency Calls Only"),           //259
        ALHR("ALHR - Emergency or Registration"),      //260
        ALHX("ALHX - Registration Excluded"),          //261
        ALHF("ALHF - Fallback Mode"),                  //262
        //263
        ACK("ACK - Acknowledge"),                     //264
        ACKI("ACKI - More To Follow"),                 //265
        ACKQ("ACKQ - Call Queued"),                    //266
        ACKX("ACKX - Message Rejected"),               //267
        ACKV("ACKV - Called Unit Unavailable"),        //268
        ACKE("ACKE - Emergency"),                      //269
        ACKT("ACKT - Try On Given Address"),           //270
        ACKB("ACKB - Call Back/Negative Ack"),         //271
        AHOY("AHOY - General Availability Check"),     //272
        AHYX("AHYX - Cancel Alert/Waiting Status"),    //274
        AHYP("AHYP - Called Unit Presence Monitoring"),//277
        AHYQ("AHYQ - Status Message"),                 //278
        AHYC("AHYC - Short Data Message"),             //279
        MARK("MARK - Control Channel Marker"),         //280
        MAINT("MAINT - Call Maintenance Message"),      //281
        CLEAR("CLEAR - Down From Allocated Channel"),  //282
        MOVE("MOVE - To Specified Channel"),           //283
        BCAST("BCAST - System Parameters"),            //284
        //285 - 287
        SAMO("SAMO - Outbound Single Address"),        //288 - 303
        SAMIS("SAMIS - Inbound Solicited Single Address"),   //288 - 295
        SAMIU("SAMIU - Inbound Unsolicited Single Address"), //296 - 303
        HEAD_PLUS1("HEAD - 1 DATA CODEWORD"),          //304 - 307
        HEAD_PLUS2("HEAD - 2 DATA CODEWORDS"),         //308 - 311
        HEAD_PLUS3("HEAD - 3 DATA CODEWORDS"),         //312 - 315
        HEAD_PLUS4("HEAD - 4 DATA CODEWORDS"),         //316 - 319
        GTT("GTT - Go To Transaction"),                //320 - 335
        SACK("SACK - Standard Data Selective Ack Header"), //416 - 423
        DACK_DAL("DACK - Data Ack + DAL"),             //416
        DACK_DALG("DACK - Data Ack + DALG"),           //417
        DACK_DALN("DACK - Data Ack + DALN"),           //418
        DACK_GO("DACK - GO Fragment Transmit Invitation"),   //419
        DACKZ("DACKZ - Data Ack For Expedited Data"),   //420
        DACKD("DACKD - Data Ack For Standard Data"),   //421
        DAHY("DAHY - Standard Data Ahoy"),             //424
        DRQG("DRQG - Repeat Group Message"),           //426
        DRQZ("DRQZ - Request Containing Expedited Data"), //428
        DAHYZ("DAHYZ - Expedited Data Ahoy"),          //428
        DAHYX("DAHYX - Standard Data For Closing TRANS"),//430
        DRQX("DRQX - Request To Close A Transaction"),   //430
        RLA("RLA - Repeat Last ACK"), //431
        SITH("SITH - Standard Data Address Codeword Data Item"); //440 - 443

        private String mDescription;

        MPTMessageType(String description)
        {
            mDescription = description;
        }

        public String getDescription()
        {
            return mDescription;
        }

        public String toString()
        {
            return getDescription();
        }

        public static MPTMessageType fromNumber(int number)
        {
            if(number < 256)
            {
                return GTC;
            }

            switch(number)
            {
                case 256:
                    return ALH;
                case 257:
                    return ALHS;
                case 258:
                    return ALHD;
                case 259:
                    return ALHE;
                case 260:
                    return ALHR;
                case 261:
                    return ALHX;
                case 262:
                    return ALHF;
                case 264:
                    return ACK;
                case 265:
                    return ACKI;
                case 266:
                    return ACKQ;
                case 267:
                    return ACKX;
                case 268:
                    return ACKV;
                case 269:
                    return ACKE;
                case 270:
                    return ACKT;
                case 271:
                    return ACKB;
                case 272:
                    return AHOY;
                case 274:
                    return AHYX;
                case 277:
                    return AHYP;
                case 278:
                    return AHYQ;
                case 279:
                    return AHYC;
                case 280:
                    return MARK;
                case 281:
                    return MAINT;
                case 282:
                    return CLEAR;
                case 283:
                    return MOVE;
                case 284:
                    return BCAST;
                case 288:
                case 289:
                case 290:
                case 291:
                case 292:
                case 293:
                case 294:
                case 295:
                case 296:
                case 297:
                case 298:
                case 299:
                case 300:
                case 301:
                case 302:
                case 303:
                    return SAMO;
                case 304:
                case 305:
                case 306:
                case 307:
                    return HEAD_PLUS1;
                case 308:
                case 309:
                case 310:
                case 311:
                    return HEAD_PLUS2;
                case 312:
                case 313:
                case 314:
                case 315:
                    return HEAD_PLUS3;
                case 316:
                case 317:
                case 318:
                case 319:
                    return HEAD_PLUS4;
                case 320:
                case 321:
                case 322:
                case 323:
                case 324:
                case 325:
                case 326:
                case 327:
                case 328:
                case 329:
                case 330:
                case 331:
                case 332:
                case 333:
                case 334:
                case 335:
                    return GTT;
                case 416:
                    return DACK_DAL;
                case 417:
                    return DACK_DALG;
                case 418:
                    return DACK_DALN;
                case 419:
                    return DACK_GO;
                case 420:
                    return DACKZ;
                case 421:
                    return DACKD;
                case 424:
                    return DAHY;
                case 426:
                    return DRQG;
                case 428:
                    return DAHYZ;
                case 430:
                    return DAHYX;
                case 440:
                case 441:
                case 442:
                case 443:
                    return SITH;
                default:
                    return UNKN;
            }
        }
    }

    public MPT1327Talkgroup getFromIdentifier()
    {
        if(mFromIdentifier == null)
        {
            mFromIdentifier = MPT1327Talkgroup.createFrom(getPrefix(), getIdent2());
        }

        return mFromIdentifier;
    }

    public MPT1327Talkgroup getToIdentifier()
    {
        if(mToIdentifier == null)
        {
            mToIdentifier = MPT1327Talkgroup.createTo(getPrefix(), getIdent1());
        }

        return mToIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getToIdentifier());
            mIdentifiers.add(getFromIdentifier());
        }

        return mIdentifiers;
    }
}
