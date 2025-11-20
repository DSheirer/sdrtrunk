/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import java.util.ArrayList;
import java.util.List;

/**
 * Restriction information field.
 */
public class RestrictionInformation
{
    private static final int ACCESS_RESTRICTION_FLAG = 0;
    private static final int MAINTENANCE_RESTRICTION_FLAG = 1;
    private static final IntField ACCESS_CYCLE_INTERVAL = IntField.length4(3);
    private static final IntField RESTRICTION_GROUP = IntField.length4(8);
    private static final int LOCATION_REGISTRATION_RESTRICTION_FLAG = 12;
    private static final int CALL_RESTRICTION_FLAG = 13;
    private static final int SHORT_DATA_RESTRICTION_FLAG = 14;
    private static final IntField RESTRICTION_GROUP_RATIO = IntField.length2(16);
    private static final IntField DELAY_EXTENSION = IntField.length2(18);
    private static final int ISOLATED_SITE_FAIL_SAFE_FLAG = 23;
    private final CorrectedBinaryMessage mMessage;
    private final int mOffset;

    /**
     * Constructs an instance
     * @param message contining the field
     * @param offset to the field.
     */
    public RestrictionInformation(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    @Override
    public String toString()
    {
        return "RESTRICTIONS: " + getRestrictions();
    }

    public List<String> getRestrictions()
    {
        List<String> restrictions = new ArrayList<>();

        if(mMessage.get(mOffset + ACCESS_RESTRICTION_FLAG))
        {
            restrictions.add("ACCESS");
        }
        if(mMessage.get(mOffset + MAINTENANCE_RESTRICTION_FLAG))
        {
            restrictions.add("MAINTENANCE");
        }
        if(mMessage.get(mOffset + LOCATION_REGISTRATION_RESTRICTION_FLAG))
        {
            restrictions.add("LOCATION REGISTRATION");
        }
        if(mMessage.get(mOffset + CALL_RESTRICTION_FLAG))
        {
            restrictions.add("CALLS");
        }
        if(mMessage.get(mOffset + SHORT_DATA_RESTRICTION_FLAG))
        {
            restrictions.add("SHORT DATA");
        }
        if(mMessage.get(mOffset + ISOLATED_SITE_FAIL_SAFE_FLAG))
        {
            restrictions.add("SITE ISOLATED-TEMPORARY");
        }

        int groupRatio = mMessage.getInt(RESTRICTION_GROUP_RATIO, mOffset);

        if(groupRatio > 0)
        {
            switch(groupRatio)
            {
                case 1:
                    restrictions.add("50% GROUP RATIO");
                    break;
                case 2:
                    restrictions.add("75% GROUP RATIO");
                    break;
                case 3:
                    restrictions.add("87.5% GROUP RATIO");
                    break;
            }
        }

        int intervals = mMessage.getInt(ACCESS_CYCLE_INTERVAL, mOffset);

        if(intervals > 0)
        {
            restrictions.add("ACCESS CYCLE INTERVAL " + (intervals * 20) + " FRAMES");
            restrictions.add("UNIT ID MASK:0x" + mMessage.getInt(RESTRICTION_GROUP, mOffset));
            restrictions.add("DELAY TIMER T2 x " + ((mMessage.getInt(DELAY_EXTENSION, mOffset) + 1) * 6) + " FRAMES");
        }

        return restrictions;
    }
}
