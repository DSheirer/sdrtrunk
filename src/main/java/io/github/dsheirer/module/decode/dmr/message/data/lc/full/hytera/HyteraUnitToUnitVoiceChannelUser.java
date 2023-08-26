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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera Individual Voice Channel User
 */
public class HyteraUnitToUnitVoiceChannelUser extends HyteraFullLC
{
    private RadioIdentifier mTargetRadio;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public HyteraUnitToUnitVoiceChannelUser(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("FLC HYTERA XPT INDIVIDUAL VOICE CHANNEL USER FM:");
        sb.append(getSourceRadio());
        sb.append(" TO:").append(getTargetRadio());
        if(isAllChannelsBusy())
        {
            sb.append(" ALL REPEATERS BUSY");
        }
        else
        {
            sb.append(" FREE REPEATER:").append(getFreeRepeater());
        }

        if(hasPriorityCall())
        {
            sb.append(" PRIORITY CALL FOR:").append(getPriorityCallHashedAddress());
            sb.append(" ON REPEATER:").append(getPriorityCallRepeater());
        }
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * Target Radio address
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetRadio());
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
