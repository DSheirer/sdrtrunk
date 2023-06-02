/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.passport;


import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCPassport;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.MessageType;
import io.github.dsheirer.module.decode.passport.identifier.PassportRadioId;
import io.github.dsheirer.module.decode.passport.identifier.PassportTalkgroup;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PassportMessage extends Message
{
    private static final int[] SYNC = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] DIGITAL_COLOR_CODE = {9, 10};
    private static final int[] CHANNEL_NUMBER = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static final int[] SITE = {22, 23, 24, 25, 26, 27, 28};
    private static final int[] GROUP = {29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int[] RADIO_ID = {22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int[] NEIGHBOR_BAND = {33, 34, 35, 36};
    private static final int[] SITE_BAND = {41, 42, 43, 44};
    private static final int[] TYPE = {45, 46, 47, 48};
    private static final int[] FREE = {49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59};
    private static final int[] CHECKSUM = {60, 61, 62, 63, 64, 65, 66, 67};

    private CorrectedBinaryMessage mMessage;
    private CRC mCRC;
    private PassportMessage mIdleMessage;
    private PassportRadioId mFromIdentifier;
    private PassportTalkgroup mToIdentifier;
    private List<Identifier> mIdentifiers;

    public PassportMessage(CorrectedBinaryMessage message, PassportMessage idleMessage)
    {
        mMessage = CRCPassport.correct(message);
        mIdleMessage = idleMessage;
        mCRC = CRCPassport.check(mMessage);
    }

    public PassportMessage(CorrectedBinaryMessage message)
    {
        this(message, null);
    }

    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    public PassportTalkgroup getToIdentifier()
    {
        if(mToIdentifier == null)
        {
            mToIdentifier = PassportTalkgroup.create(getMessage().getInt(GROUP));
        }

        return mToIdentifier;
    }

    public PassportRadioId getFromIdentifier()
    {
        if(mFromIdentifier == null)
        {
            mFromIdentifier = PassportRadioId.create(getMessage().getInt(RADIO_ID));
        }

        return mFromIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasFromIdentifier())
            {
                mIdentifiers.add(getFromIdentifier());
            }

            if(hasToIdentifier())
            {
                mIdentifiers.add(getToIdentifier());
            }
        }

        return mIdentifiers;
    }

    public boolean isValid()
    {
        return mCRC.passes();
    }

    public CRC getCRC()
    {
        return mCRC;
    }

    public MessageType getMessageType()
    {
        MessageType retVal = MessageType.UN_KNWN;

        int type = getMessageTypeNumber();
        int lcn = getLCN();

        switch(type)
        {
            case 0: //Group Call
                retVal = MessageType.CA_STRT;
                break;
            case 1:
                if(getFree() == 2042)
                {
                    retVal = MessageType.ID_TGAS;
                }
                else if(lcn < 1792)
                {
                    retVal = MessageType.CA_STRT;
                }
                else if(lcn == 1792 || lcn == 1793)
                {
                    retVal = MessageType.SY_IDLE;
                }
                else if(lcn == 2047)
                {
                    retVal = MessageType.CA_ENDD;
                }
                break;
            case 2:
                retVal = MessageType.CA_STRT;
                break;
            case 5:
                retVal = MessageType.CA_PAGE;
                break;
            case 6:
                retVal = MessageType.ID_RDIO;
                break;
            case 9:
                retVal = MessageType.DA_STRT;
                break;
            case 11:
                retVal = MessageType.RA_REGI;
                break;
            default:
                break;
        }

        return retVal;
    }


    public int getColorCode()
    {
        return getMessage().getInt(DIGITAL_COLOR_CODE);
    }

    public int getSite()
    {
        return getMessage().getInt(SITE);
    }

    public int getMessageTypeNumber()
    {
        return getMessage().getInt(TYPE);
    }

    public boolean hasToIdentifier()
    {
        return getMessageType() != MessageType.SY_IDLE;
    }

    public int getLCN()
    {
        return getMessage().getInt(CHANNEL_NUMBER);
    }

    public long getLCNFrequency()
    {
        return getSiteFrequency(getLCN());
    }

    public PassportBand getSiteBand()
    {
        return PassportBand.lookup(getMessage().getInt(SITE_BAND));
    }

    public PassportBand getNeighborBand()
    {
        return PassportBand.lookup(getMessage().getInt(NEIGHBOR_BAND));
    }

    public int getFree()
    {
        return getMessage().getInt(FREE);
    }

    public long getFreeFrequency()
    {
        return getSiteFrequency(getFree());
    }

    public long getNeighborFrequency()
    {
        if(getMessageType() == MessageType.SY_IDLE)
        {
            PassportBand band = getNeighborBand();

            return band.getFrequency(getFree());
        }

        return 0;
    }

    public boolean hasFromIdentifier()
    {
        return getMessageType() == MessageType.ID_RDIO;
    }

    /**
     * Appends spaces to the end of the stringbuilder to make it length long
     */
    private void pad(StringBuilder sb, int length)
    {
        while(sb.length() < length)
        {
            sb.append(" ");
        }
    }

    /**
     * Pads an integer value with additional zeroes to make it decimalPlaces long
     */
    public String format(int number, int decimalPlaces)
    {
        return StringUtils.leftPad(Integer.valueOf(number).toString(), decimalPlaces, '0');
    }

    public String format(String val, int places)
    {
        return StringUtils.leftPad(val, places);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.PASSPORT;
    }

    public long getSiteFrequency(int channel)
    {
        if(mIdleMessage != null && 0 < channel && channel < 1792)
        {
            PassportBand band = mIdleMessage.getSiteBand();

            if(band != PassportBand.BAND_UNKNOWN)
            {
                return band.getFrequency(channel);
            }
        }

        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DCC:").append(getColorCode());

        switch(getMessageType())
        {
            case SY_IDLE:
                sb.append(" IDLE SITE:").append(format(getSite(), 3));
                sb.append(" NEIGHBOR:").append(format(getFree(), 3)).append("/").append(getFreeFrequency());
                break;
            case CA_PAGE:
                sb.append(" PAGING TG:").append(getToIdentifier());
                sb.append(" SITE:").append(format(getSite(), 3));
                sb.append(" CHAN:").append(format(getLCN(), 4)).append("/").append(getLCNFrequency());
                sb.append(" FREE:").append(format(getFree(), 3)).append("/").append(getFreeFrequency());
                break;
            case CA_STRT:
                sb.append(" CALL TG:").append(getToIdentifier());
                sb.append(" SITE:").append(format(getSite(), 3));
                sb.append(" CHAN:").append(format(getLCN(), 4)).append("/").append(getLCNFrequency());
                sb.append(" FREE:").append(format(getFree(), 3)).append("/").append(getFreeFrequency());
                break;
            case DA_STRT:
                sb.append(" ** DATA TG:").append(getToIdentifier());
                sb.append(" SITE:").append(format(getSite(), 3));
                sb.append(" CHAN:").append(format(getLCN(), 4)).append("/").append(getLCNFrequency());
                sb.append(" FREE:").append(format(getFree(), 3)).append("/").append(getFreeFrequency());
                break;
            case CA_ENDD:
                sb.append(" END  TG:").append(getToIdentifier());
                sb.append(" SITE:").append(format(getSite(), 3));
                sb.append(" CHAN:").append(format(getLCN(), 4)).append("/").append(getLCNFrequency());
                sb.append(" FREE:").append(format(getFree(), 3)).append("/").append(getFreeFrequency());
                break;
            case ID_RDIO:
                sb.append(" MOBILE ID MIN:").append(getFromIdentifier());
                sb.append(" FREE:").append(format(getFree(), 3)).append("/").append(getFreeFrequency());
                break;
            case ID_TGAS:
                sb.append(" ASSIGN TALKGROUP:").append(getToIdentifier());
                sb.append(" SITE:").append(format(getSite(), 3));
                sb.append(" CHAN:").append(format(getLCN(), 4)).append("/").append(getLCNFrequency());
                break;
            case RA_REGI:
                sb.append(" RADIO REGISTER TG: ").append(getToIdentifier());
                break;
            default:
                sb.append(" UNKNOWN SITE:").append(format(getSite(), 3));
                sb.append(" CHAN:").append(format(getLCN(), 4)).append("/").append(getLCNFrequency());
                sb.append(" FREE:");
                int free = getFree();
                sb.append(format(free, 3));
                if(free > 0 && free < 896)
                {
                    sb.append("/");
                    sb.append(getFreeFrequency());
                }
                sb.append(" TYP:").append(format(getMessageTypeNumber(), 2));
                sb.append(" TG:").append(getToIdentifier());
                break;
        }

        sb.append(" MSG:").append(getMessage().toString());

        return sb.toString();
    }

    public boolean matches(PassportMessage otherMessage)
    {
        return this.getMessage().equals(otherMessage.getMessage());
    }
}
