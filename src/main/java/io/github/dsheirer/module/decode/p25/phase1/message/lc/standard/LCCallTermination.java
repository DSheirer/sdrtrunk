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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Termination or cancellation of a call and release of the channel.
 */
public class LCCallTermination extends LinkControlWord
{
    private static final int MOTOROLA_SYSTEM_CONTROLLER_1 = 0xFFFFFD;
    private static final int MOTOROLA_SYSTEM_CONTROLLER_2 = 0xFFFFFF;
    private static final int HARRIS_SYSTEM_CONTROLLER = 0;
    private static final IntField ADDRESS = IntField.length24(OCTET_6_BIT_48);
    private Identifier mAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCCallTermination(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" BY:").append(getAddress());
        return sb.toString();
    }

    /**
     * To/From radio identifier communicating with a landline
     */
    public Identifier getAddress()
    {
        if(mAddress == null)
        {
            mAddress = APCO25Talkgroup.create(getInt(ADDRESS));
        }

        return mAddress;
    }

    /**
     * Indicates if the call termination was ordered by the system controller
     */
    public boolean isNetworkCommandedTeardown()
    {
        int address = getInt(ADDRESS);
        return address == MOTOROLA_SYSTEM_CONTROLLER_1 || address == MOTOROLA_SYSTEM_CONTROLLER_2 || address == HARRIS_SYSTEM_CONTROLLER;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getAddress());
        }

        return mIdentifiers;
    }
}
