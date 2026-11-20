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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import io.github.dsheirer.module.decode.config.AccessCodeGate;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceEMBMessage;

/**
 * Applies a Color Code (CC) filter to a DMR message stream for one timeslot.
 *
 * The color code is carried in the Slot Type field of data/control bursts and in the EMB field of voice bursts B-F.
 * Voice burst A (voice sync) has no EMB, and assembled messages (link control, packets) don't carry the color code;
 * those follow the decision for the most recent burst with a valid Slot Type or EMB.
 */
public class DMRColorCodeGate extends AccessCodeGate
{
    /**
     * Constructs an instance
     * @param filter to apply, or null to pass all messages
     */
    public DMRColorCodeGate(AccessCodeFilter filter)
    {
        super(filter);
    }

    @Override
    protected Integer getTrustedAccessCode(IMessage message)
    {
        if(message instanceof VoiceEMBMessage voice && voice.getEMB().isValid())
        {
            return voice.getEMB().getColorCode();
        }
        else if(message instanceof DataMessage data && data.getSlotType().isValid())
        {
            return data.getSlotType().getColorCode();
        }

        return null;
    }
}
