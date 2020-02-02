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

package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;

import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Control Channel System Parameters
 */
public class ControlChannelSystemParameters extends ShortLCMessage
{
    private static final int SYSTEM_IDENTITY_CODE_OFFSET = 4;
    private static final int REGISTRATION_REQUIRED_FLAG = 18;
    private static final int[] COMMON_SLOT_COUNTER = new int[]{19, 20, 21, 22, 23, 24, 25, 26, 27};

    private SystemIdentityCode mSystemIdentityCode;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param message containing link control data
     */
    public ControlChannelSystemParameters(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC ERROR] ");
        }
        sb.append("SLC TIER III CONTROL CHANNEL ").append(getSystemIdentityCode().getModel());
        sb.append(" NET:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());
        if(isRegistrationRequired())
        {
            sb.append(" REGISTRATION REQUIRED");
        }
        sb.append(" SLOT COUNTER:").append(getCommonSlotCounter());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * System Identity Code structure
     */
    public SystemIdentityCode getSystemIdentityCode()
    {
        if(mSystemIdentityCode == null)
        {
            mSystemIdentityCode = new SystemIdentityCode(getMessage(), SYSTEM_IDENTITY_CODE_OFFSET, false);
        }

        return mSystemIdentityCode;
    }

    /**
     * Indicates if registration is required to access this network
     */
    public boolean isRegistrationRequired()
    {
        return getMessage().get(REGISTRATION_REQUIRED_FLAG);
    }

    /**
     * Common Slot Counter
     */
    public int getCommonSlotCounter()
    {
        return getMessage().getInt(COMMON_SLOT_COUNTER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }
}
