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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25Radio;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Radio unit monitor command
 */
public class RadioUnitMonitorCommandExtended extends MacStructure
{
    private static final int[] TRANSMIT_TIME = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int SILENT_MONITOR = 24; //??
    private static final int[] TRANSMIT_MULTIPLIER = {30, 31};
    private static final int[] TARGET_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55};

    private static final int[] SOURCE_WACN = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75};
    private static final int[] SOURCE_SYSTEM = {76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] SOURCE_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103,
        104, 105, 106, 107, 108, 109, 110, 111};

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private APCO25FullyQualifiedRadioIdentifier mSourceSuid;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RadioUnitMonitorCommandExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" FM:").append(getSourceSuid());

        if(isSilentMonitor())
        {
            sb.append(" SILENT MONITORING");
        }
        sb.append(" TIME:").append(getTransmitTime());
        sb.append(" MULTIPLIER:").append(getTransmitMultiplier());
        return sb.toString();
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
        return getMessage().getInt(TRANSMIT_TIME, getOffset());
    }

    /**
     * Multiplier for transmit time.
     */
    public int getTransmitMultiplier()
    {
        return getMessage().getInt(TRANSMIT_MULTIPLIER, getOffset());
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25Radio.createTo(getMessage().getInt(TARGET_ADDRESS, getOffset()));
        }

        return mTargetAddress;
    }

    /**
     * From Radio Unit
     */
    public APCO25FullyQualifiedRadioIdentifier getSourceSuid()
    {
        if(mSourceSuid == null)
        {
            mSourceSuid = APCO25FullyQualifiedRadioIdentifier.createFrom(getMessage().getInt(SOURCE_WACN, getOffset()),
                getMessage().getInt(SOURCE_SYSTEM, getOffset()), getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceSuid;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceSuid());
        }

        return mIdentifiers;
    }
}
