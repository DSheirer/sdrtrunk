/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.Collections;
import java.util.List;

/**
 * Secondary control channel broadcast
 */
public class SystemServiceBroadcast extends OSPMessage
{
    private static final int[] RESERVED = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] AVAILABLE_SERVICES = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47};
    private static final int[] SUPPORTED_SERVICES = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71};
    private static final int[] REQUEST_PRIORITY_LEVEL = {72, 73, 74, 75, 76, 77, 78, 79};

    private List<Service> mAvailableServices;
    private List<Service> mSupportedServices;


    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public SystemServiceBroadcast(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" AVAILABLE SERVICES ").append(getAvailableServices());
        sb.append(" SUPPORTED SERVICES ").append(getSupportedServices());
        return sb.toString();
    }

    public List<Service> getAvailableServices()
    {
        if(mAvailableServices == null)
        {
            mAvailableServices = Service.getServices(getMessage().getInt(AVAILABLE_SERVICES));
        }

        return mAvailableServices;
    }

    public List<Service> getSupportedServices()
    {
        if(mSupportedServices == null)
        {
            mSupportedServices = Service.getServices(getMessage().getInt(SUPPORTED_SERVICES));
        }

        return mSupportedServices;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
