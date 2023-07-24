/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dcs;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.dcs.DCSIdentifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * Digital Coded Squelch (DCS) tone detected message
 */
public class DCSMessage extends Message
{
    private final DCSCode mDCSCode;

    /**
     * Constructs an instance
     * @param code that was detected
     * @param timestamp when the code was detected
     */
    public DCSMessage(DCSCode code, long timestamp)
    {
        super(timestamp);
        mDCSCode = code;
    }

    @Override
    public String toString()
    {
        return "Digital Coded Squelch (DCS) Detected: " + mDCSCode.toString();
    }

    /**
     * The DCS code that was detected.
     * @return code
     */
    public DCSCode getDCSCode()
    {
        return mDCSCode;
    }

    @Override
    public boolean isValid()
    {
        return true; //We only send a message when the tone was valid
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DCS;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.singletonList(new DCSIdentifier(mDCSCode));
    }
}
