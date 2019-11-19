/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.Collections;
import java.util.List;

/**
 * System services broadcast
 */
public class SystemServiceBroadcast extends MacStructure
{
    private static final int[] TWUID_VALIDITY = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] AVAILABLE_SERVICES = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SUPPORTED_SERVICES = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
        56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] REQUEST_PRIORITY_LEVEL = {64, 65, 66, 67, 68, 69, 70, 71};

    private List<Service> mAvailableServices;
    private List<Service> mSupportedServices;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SystemServiceBroadcast(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" AVAILABLE SERVICES ").append(getAvailableServices());
        sb.append(" SUPPORTED SERVICES ").append(getSupportedServices());
        return sb.toString();
    }

    public List<Service> getAvailableServices()
    {
        if(mAvailableServices == null)
        {
            mAvailableServices = Service.getServices(getMessage().getInt(AVAILABLE_SERVICES, getOffset()));
        }

        return mAvailableServices;
    }

    public List<Service> getSupportedServices()
    {
        if(mSupportedServices == null)
        {
            mSupportedServices = Service.getServices(getMessage().getInt(SUPPORTED_SERVICES, getOffset()));
        }

        return mSupportedServices;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
