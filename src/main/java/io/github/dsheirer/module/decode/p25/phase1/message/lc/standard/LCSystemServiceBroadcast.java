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
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.Service;
import java.util.Collections;
import java.util.List;

/**
 * Termination or cancellation of a call and release of the channel.
 */
public class LCSystemServiceBroadcast extends LinkControlWord
{
    private static final IntField REQUEST_PRIORITY_LEVEL = IntField.length4(OCTET_2_BIT_16 + 4);
    private static final IntField AVAILABLE_SERVICES = IntField.length24(OCTET_3_BIT_24);
    private static final IntField SUPPORTED_SERVICES = IntField.length24(OCTET_6_BIT_48);

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCSystemServiceBroadcast(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" REQUEST PRIORITY LEVEL:").append(getRequestPriorityLevel());
        sb.append(" AVAILABLE SERVICES ").append(getAvailableServices());
        sb.append(" SUPPORTED SERVICES ").append(getSupportedServices());
        return sb.toString();
    }

    /**
     * Threshold priority for service requests for this site
     */
    public int getRequestPriorityLevel()
    {
        return getInt(REQUEST_PRIORITY_LEVEL);
    }


    public List<Service> getAvailableServices()
    {
        return Service.getServices(getInt(AVAILABLE_SERVICES));
    }

    public List<Service> getSupportedServices()
    {
        return Service.getServices(getInt(SUPPORTED_SERVICES));
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
