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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.AbstractVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.type.CapacityPlusServiceOptions;

/**
 * Any Capacity Plus Voice Channel User link control message that contains vendor-specific service options.
 */
public abstract class CapacityPlusVoiceChannelUser extends AbstractVoiceChannelUser implements IServiceOptionsProvider
{
    private static final int[] SERVICE_OPTIONS = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    //Reed Solomon FEC: 72-95

    private CapacityPlusServiceOptions mServiceOptions;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public CapacityPlusVoiceChannelUser(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    /**
     * Service options for the call
     */
    public CapacityPlusServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new CapacityPlusServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }
}
