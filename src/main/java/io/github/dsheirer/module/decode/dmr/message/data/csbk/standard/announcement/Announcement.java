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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.AnnouncementType;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Announcement Message
 */
public class Announcement extends CSBKMessage
{
    private static final int[] ANNOUNCEMENT_TYPE = new int[]{16, 17, 18, 19, 20};
    private static final int[] PARAMS_1 = new int[]{21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34};
    private static final int REGISTRATION_REQUIRED_FLAG = 35;
    private static final int[] BACKOFF = new int[]{36, 37, 38, 39};
    private static final int SYSTEM_IDENTITY_CODE_OFFSET = 40;
    private static final int[] PARAMS_2 = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75, 76, 77, 78, 79};

    private List<Identifier> mIdentifiers;
    private SystemIdentityCode mSystemIdentityCode;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public Announcement(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        if(hasRAS())
        {
            sb.append(" RAS:").append(getBPTCReservedBits());
        }
        sb.append(" ANNOUNCEMENT ").append(getAnnouncementType());

        return sb.toString();
    }

    /**
     * Indicates if registration is required on this site
     */
    public boolean isRegistrationRequired()
    {
        return getMessage().get(REGISTRATION_REQUIRED_FLAG);
    }

    /**
     * Backoff value
     */
    public int getBackoff()
    {
        return getMessage().getInt(BACKOFF);
    }

    /**
     * System Identity Code structure
     */
    public SystemIdentityCode getSystemIdentityCode()
    {
        if(mSystemIdentityCode == null)
        {
            mSystemIdentityCode = new SystemIdentityCode(getMessage(), SYSTEM_IDENTITY_CODE_OFFSET, true);
        }

        return mSystemIdentityCode;
    }
    /**
     * Announcement message type for this message
     */
    public AnnouncementType getAnnouncementType()
    {
        return getAnnouncementType(getMessage());
    }

    /**
     * Utility to get announcement message type for the specified message
     */
    public static AnnouncementType getAnnouncementType(CorrectedBinaryMessage message)
    {
        return AnnouncementType.fromValue(message.getInt(ANNOUNCEMENT_TYPE));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
        }

        return mIdentifiers;
    }
}
