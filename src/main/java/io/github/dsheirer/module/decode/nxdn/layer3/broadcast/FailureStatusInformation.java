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

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallTimer;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import java.util.Collections;
import java.util.List;

/**
 * Site failure status information
 */
public class FailureStatusInformation extends NXDNLayer3Message
{
    private static final int LOCATION_ID = OCTET_1;
    private static final IntField CALL_TIMER = IntField.length6(OCTET_5);
    private LocationID mLocationID;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public FailureStatusInformation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("FAIL-SOFT MODE ").append(getLocationID()).append(" CALL TIMER:").append(getCallTimer());
        return sb.toString();
    }

    /**
     * Location that is in failsoft
     */
    public LocationID getLocationID()
    {
        if(mLocationID == null)
        {
            mLocationID = new LocationID(getMessage(), LOCATION_ID);
        }

        return mLocationID;
    }

    /**
     * Call timer.
     */
    public CallTimer getCallTimer()
    {
        return CallTimer.fromValue(getMessage().getInt(CALL_TIMER));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
