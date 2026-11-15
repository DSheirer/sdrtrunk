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

// Reference: OP25 tk_smartnet.py (boatbod/op25)
package io.github.dsheirer.module.decode.moto.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.moto.identifier.MotorolaTypeIIRadioIdentifier;
import io.github.dsheirer.module.decode.moto.identifier.MotorolaTypeIITalkgroup;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MotorolaTypeIIMessage extends Message
{
    private int mAddress;
    private boolean mIsGroup;
    private int mCommand;
    private int mChannelNumber;
    private int mSourceAddress;
    private CorrectedBinaryMessage mRawData;
    private boolean mValid;
    private MotorolaTypeIIMessageType mMessageType;

    public MotorolaTypeIIMessage(int address, boolean isGroup, int command, CorrectedBinaryMessage rawData, boolean valid)
    {
        this(address, isGroup, command, rawData, valid, MotorolaTypeIIMessageType.UNKNOWN);
    }

    public MotorolaTypeIIMessage(int address, boolean isGroup, int command, CorrectedBinaryMessage rawData, boolean valid,
                                 MotorolaTypeIIMessageType messageType)
    {
        this(address, isGroup, command, 0, rawData, valid, messageType);
    }

    public MotorolaTypeIIMessage(int address, boolean isGroup, int command, int channelNumber,
                                  CorrectedBinaryMessage rawData, boolean valid, MotorolaTypeIIMessageType messageType)
    {
        this(address, isGroup, command, channelNumber, 0, rawData, valid, messageType);
    }

    public MotorolaTypeIIMessage(int address, boolean isGroup, int command, int channelNumber, int sourceAddress,
                                  CorrectedBinaryMessage rawData, boolean valid, MotorolaTypeIIMessageType messageType)
    {
        super();
        mAddress = address;
        mIsGroup = isGroup;
        mCommand = command;
        mChannelNumber = channelNumber;
        mSourceAddress = sourceAddress;
        mRawData = rawData;
        mValid = valid;
        mMessageType = messageType;
    }

    public int getAddress()
    {
        return mAddress;
    }

    public boolean isGroup()
    {
        return mIsGroup;
    }

    public int getCommand()
    {
        return mCommand;
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    public int getSourceAddress()
    {
        return mSourceAddress;
    }

    public CorrectedBinaryMessage getRawData()
    {
        return mRawData;
    }

    public MotorolaTypeIIMessageType getMessageType()
    {
        return mMessageType;
    }

    public void setMessageType(MotorolaTypeIIMessageType messageType)
    {
        mMessageType = messageType;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MOTOROLA_TYPE_II;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTO T2 ");
        sb.append(mMessageType.name());

        switch(mMessageType)
        {
            case ANALOG_GROUP_GRANT:
            case DIGITAL_GROUP_GRANT:
                sb.append(" TG:").append(String.format("0x%04X", mAddress));
                if(mSourceAddress > 0)
                {
                    sb.append(" SRC:").append(String.format("0x%04X", mSourceAddress));
                }
                sb.append(" CH:").append(String.format("0x%03X", mChannelNumber));
                break;
            case ANALOG_PRIVATE_CALL:
            case DIGITAL_PRIVATE_CALL:
                sb.append(" TO:").append(String.format("0x%04X", mAddress));
                if(mSourceAddress > 0)
                {
                    sb.append(" FROM:").append(String.format("0x%04X", mSourceAddress));
                }
                sb.append(" CH:").append(String.format("0x%03X", mChannelNumber));
                break;
            case GROUP_UPDATE:
                sb.append(" TG:").append(String.format("0x%04X", mAddress));
                sb.append(" CH:").append(String.format("0x%03X", mChannelNumber));
                break;
            case SYSTEM_ID:
                sb.append(" SYS:").append(String.format("0x%04X", mAddress));
                break;
            case CONTROL_CHANNEL:
                sb.append(" SYS:").append(String.format("0x%04X", mAddress));
                sb.append(" CH:").append(String.format("0x%03X", mChannelNumber));
                break;
            case AMSS:
                int siteId = mCommand - 0x360 + 1;
                sb.append(" SITE:").append(siteId);
                break;
            case SYSTEM_ID_CC:
                sb.append(" SYS:").append(String.format("0x%04X", mAddress));
                sb.append(" CH:").append(String.format("0x%03X", mChannelNumber));
                break;
            case NETWORK_STATUS:
            case SYSTEM_STATUS:
                sb.append(" CMD:").append(String.format("0x%03X", mCommand));
                break;
            default:
                sb.append(" ");
                sb.append(mIsGroup ? "GRP" : "IND");
                sb.append(" ADDR:").append(String.format("%04X", mAddress));
                sb.append(" CMD:").append(String.format("%03X", mCommand));
                break;
        }

        if(!mValid)
        {
            sb.append(" CRC:FAIL");
        }
        return sb.toString();
    }

    @Override
    public boolean isValid()
    {
        return mValid;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        switch(mMessageType)
        {
            case ANALOG_GROUP_GRANT:
            case DIGITAL_GROUP_GRANT:
            case GROUP_UPDATE:
                List<Identifier> groupIdentifiers = new ArrayList<>();
                groupIdentifiers.add(new MotorolaTypeIITalkgroup(mAddress));

                if(mSourceAddress > 0)
                {
                    groupIdentifiers.add(new MotorolaTypeIIRadioIdentifier(mSourceAddress, Role.FROM));
                }

                return groupIdentifiers;
            case ANALOG_PRIVATE_CALL:
            case DIGITAL_PRIVATE_CALL:
                List<Identifier> privateCallIdentifiers = new ArrayList<>();
                privateCallIdentifiers.add(new MotorolaTypeIIRadioIdentifier(mAddress, Role.TO));

                if(mSourceAddress > 0)
                {
                    privateCallIdentifiers.add(new MotorolaTypeIIRadioIdentifier(mSourceAddress, Role.FROM));
                }

                return privateCallIdentifiers;
            default:
                return Collections.emptyList();
        }
    }
}
