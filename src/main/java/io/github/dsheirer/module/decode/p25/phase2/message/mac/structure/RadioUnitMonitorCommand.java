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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import java.util.List;

/**
 * Radio unit monitor command base implementation
 */
public abstract class RadioUnitMonitorCommand extends MacStructure
{
    private static final IntField TRANSMIT_TIME = IntField.length8(OCTET_3_BIT_16);
    private static final int SILENT_MONITOR = 24;
    private static final IntField TRANSMIT_MULTIPLIER = IntField.range(30, 31);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_5_BIT_32);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RadioUnitMonitorCommand(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Indicates if the target radio should not indicate to the user that the radio is being monitored.
     */
    public boolean isSilentMonitor()
    {
        return getMessage().get(SILENT_MONITOR + getOffset());
    }

    /**
     * Transmit time.
     */
    public int getTransmitTime()
    {
        return getInt(TRANSMIT_TIME);
    }

    /**
     * Multiplier for transmit time.
     */
    public int getTransmitMultiplier()
    {
        return getInt(TRANSMIT_MULTIPLIER);
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }
}
