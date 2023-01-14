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

package io.github.dsheirer.identifier.tone;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * IMessage wrapper for a ToneIdentifier decoded from a sequence of AMBE audio frames.
 * @param protocol of the decoded tone
 * @param timeslot where the tone occurred
 * @param timestamp of the event
 * @param toneIdentifier that was decoded
 * @param messageText to convey with the tone identifier
 */
public record ToneIdentifierMessage(Protocol protocol, int timeslot, long timestamp, ToneIdentifier toneIdentifier,
                                    String messageText) implements IMessage
{
    @Override
    public long getTimestamp()
    {
        return timestamp();
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return protocol();
    }

    @Override
    public int getTimeslot()
    {
        return timeslot();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.singletonList(toneIdentifier());
    }

    @Override
    public String toString()
    {
        return messageText();
    }
}
