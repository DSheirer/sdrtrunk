/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.TimeslotMessage;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base APCO25 Phase 1 Message
 */
public abstract class P25P1Message extends TimeslotMessage implements IMessage
{
    private Identifier mNAC;

    /**
     * Constructs a P25 message.
     *
     * @param message containing the binary message and optional corrected bit count
     * @param nac Network Access Code (NAC) for the message
     * @param timestamp when the message was transmitted
     */
    public P25P1Message(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, 0, timestamp);
        mNAC = APCO25Nac.create(nac);
    }

    /**
     * Constructs a P25 message using current system time for when the message was transmitted.
     *
     * @param message containing the binary message and optional corrected bit count
     * @param nac Network Access Code (NAC) for the message
     */
    public P25P1Message(CorrectedBinaryMessage message, int nac)
    {
        this(message, nac, System.currentTimeMillis());
    }

    /**
     * Constructs a P25 message where the binary message will be created/updated after construction.
     * @param nac Network Access Code (NAC) for the message
     * @param timestamp when the message was transmitted
     */
    protected P25P1Message(int nac, long timestamp)
    {
        super(null, 0, timestamp);
        mNAC = APCO25Nac.create(nac);
    }

    /**
     * NAC code for this message
     */
    public Identifier getNAC()
    {
        return mNAC;
    }

    /**
     * Data Unit ID indicates the type of P25 message
     */
    public abstract P25P1DataUnitID getDUID();

    public String toString()
    {
        return getMessageStub();
    }

    protected String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("NAC:");
        sb.append(getNAC());
        sb.append(" ");
        sb.append(getDUID().getLabel());

        return sb.toString();
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }
}
