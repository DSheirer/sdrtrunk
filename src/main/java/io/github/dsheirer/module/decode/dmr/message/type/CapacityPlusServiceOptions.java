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

package io.github.dsheirer.module.decode.dmr.message.type;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus flavor of service options.  Motorola uses the 2x reserved bits from the default implementation.
 */
public class CapacityPlusServiceOptions extends ServiceOptions
{
    /**
     * Constructs an instance
     *
     * @param value of the service options bitmap
     */
    public CapacityPlusServiceOptions(int value)
    {
        super(value);
    }

    /**
     * Reserved bit 1 is used to signal if the system is IP Site Connect or Conventional (true) or Capacity Plus (false).
     * @return
     */
    public boolean isCapacityPlus()
    {
        return !isReserved1();
    }

    /**
     * Indicates if the transmission is interruptible.
     * @return true if interruptible.
     */
    public boolean isInterruptible()
    {
        return isReserved2();
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

        if(isInterruptible())
        {
            flags.add("INTERRUPTIBLE CALL"); //Indicates another user can interrupt this call
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
