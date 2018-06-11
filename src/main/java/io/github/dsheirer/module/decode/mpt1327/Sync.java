/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.bits.SyncPattern;

public enum Sync
{
    NORMAL("Normal", SyncPattern.MPT1327_CONTROL, SyncPattern.MPT1327_TRAFFIC),
    FRENCH("French", SyncPattern.MPT1327_CONTROL_FRENCH, SyncPattern.MPT1327_TRAFFIC_FRENCH);

    private String mLabel;
    private SyncPattern mControlSyncPattern;
    private SyncPattern mTrafficSyncPattern;

    Sync(String label, SyncPattern controlPattern, SyncPattern trafficPattern)
    {
        mLabel = label;
        mControlSyncPattern = controlPattern;
        mTrafficSyncPattern = trafficPattern;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public SyncPattern getControlSyncPattern()
    {
        return mControlSyncPattern;
    }

    public SyncPattern getTrafficSyncPattern()
    {
        return mTrafficSyncPattern;
    }

    public String toString()
    {
        return getLabel();
    }
}
