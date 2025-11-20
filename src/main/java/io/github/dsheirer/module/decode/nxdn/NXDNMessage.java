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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base NXDN message implementation
 */
public abstract class NXDNMessage implements IMessage
{
    protected static int OCTET_0 = 0;
    protected static int OCTET_1 = 8;
    protected static int OCTET_2 = 16;
    protected static int OCTET_3 = 24;
    protected static int OCTET_4 = 32;
    protected static int OCTET_5 = 40;
    protected static int OCTET_6 = 48;
    protected static int OCTET_7 = 56;
    protected static int OCTET_8 = 64;
    protected static int OCTET_9 = 72;
    protected static int OCTET_10 = 80;
    protected static int OCTET_11 = 88;
    protected static int OCTET_12 = 96;
    protected static int OCTET_13 = 104;
    protected static int OCTET_14 = 112;
    protected static int OCTET_15 = 120;
    protected static int OCTET_16 = 128;
    protected static int OCTET_17 = 136;
    protected static int OCTET_18 = 144;
    protected static int OCTET_19 = 152;
    protected static int OCTET_20 = 160;
    protected final int mRAN;
    protected final LICH mLICH;

    private CorrectedBinaryMessage mMessage;
    private long mTimestamp;
    private boolean mValid = true;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public NXDNMessage(CorrectedBinaryMessage message, long timestamp, int ran, LICH lich)
    {
        mMessage = message;
        mTimestamp = timestamp;
        mRAN = ran;
        mLICH = lich;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Underlying binary message
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    @Override
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Marks this message as valid or invalid.
     *
     * @param valid flag
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    @Override
    public int getTimeslot()
    {
        return 0;
    }

    /**
     * Formatted message prefix indicating CRC status, RAN and RF Channel.
     */
    public StringBuilder getMessageBuilder()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }
        sb.append(getLICH().getRFChannel().name());
        if(hasRAN())
        {
            sb.append(" RAN:").append(getRAN());
        }
        sb.append(" ");
        return sb;
    }

    public boolean hasRAN()
    {
        return mRAN != 0;
    }

    /**
     * Radio access network (RAN) from the frame carrier
     */
    public int getRAN()
    {
        return mRAN;
    }

    /**
     * Link Information Channel (LICH) from the frame carrier
     */
    public LICH getLICH()
    {
        return mLICH;
    }
}
