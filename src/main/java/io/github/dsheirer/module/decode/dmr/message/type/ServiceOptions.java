/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.type;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

/**
 * DMR Service Options parsing class
 */
public class ServiceOptions
{
    private static final int EMERGENCY_MASK = 0x80;
    private static final int ENCRYPTION_MASK = 0x40;
    private static final int BROADCAST_SERVICE_MASK = 0x8;
    private static final int OPEN_VOICE_CALL_MODE_MASK = 0x4;
    private static final int PRIORITY_MASK = 0x3;
    private int mValue;

    /**
     * Constructs an instance
     * @param value of the service options bitmap
     */
    public ServiceOptions(int value)
    {
        mValue = value;
    }

    /**
     * Indicates if this is an emergency communication
     */
    public boolean isEmergency()
    {
        return isSet(EMERGENCY_MASK);
    }

    /**
     * Indicates if the call is encrypted
     */
    public boolean isEncrypted()
    {
        return isSet(ENCRYPTION_MASK);
    }

    /**
     * Indicates if this is broadcast service
     */
    public boolean isBroadcastService()
    {
        return isSet(BROADCAST_SERVICE_MASK);
    }

    /**
     * Indicates if the call is using open voice call mode
     */
    public boolean isOpenVoiceCallMode()
    {
        return isSet(OPEN_VOICE_CALL_MODE_MASK);
    }

    /**
     * Priority of the communication
     * @return priority 0-3, 0 = lowest/routine, 3 = highest
     */
    public int getPriority()
    {
        return (mValue & PRIORITY_MASK);
    }

    /**
     * Indicates if the bits corresponding to the mask value are set in the service options bitmap value.
     */
    private boolean isSet(int mask)
    {
        return (mValue & mask) == mask;
    }

    @Override
    public String toString()
    {
        List<String> flags = new ArrayList<>();

        if(isEmergency())
        {
            flags.add("EMERGENCY");
        }

        if(isEncrypted())
        {
            flags.add("ENCRYPTED");
        }

        if(isBroadcastService())
        {
            flags.add("BROADCAST");
        }

        if(isOpenVoiceCallMode())
        {
            flags.add("OVCM");
        }

        if(getPriority() > 0)
        {
            flags.add("PRIORITY-" + getPriority());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SERVICE OPTIONS [");
        if(!flags.isEmpty())
        {
            sb.append(Joiner.on(",").join(flags));
        }
        sb.append("]");
        return sb.toString();
    }
}
