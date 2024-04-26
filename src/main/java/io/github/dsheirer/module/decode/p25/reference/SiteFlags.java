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
package io.github.dsheirer.module.decode.p25.reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Site flags (C, F, V, A)
 */
public class SiteFlags
{
    private static final int CONVENTIONAL_CHANNEL_FLAG = 0x08;
    private static final int SITE_FAILURE_FLAG = 0x04;
    private static final int VALID_INFORMATION_FLAG = 0x02;
    private static final int ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG = 0x01;

    private int mFlags;

    public SiteFlags(int flags)
    {
        mFlags = flags;
    }

    public List<String> getFlags()
    {
        List<String> flags = new ArrayList<>();

        if(hasFlag(CONVENTIONAL_CHANNEL_FLAG))
        {
            flags.add("CONVENTIONAL CHANNEL");
        }

        if(hasFlag(SITE_FAILURE_FLAG))
        {
            flags.add("FAILURE CONDITION");
        }

        if(hasFlag(VALID_INFORMATION_FLAG))
        {
            flags.add("VALID INFORMATION");
        }

        if(hasFlag(ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG))
        {
            flags.add("ACTIVE RFSS CONNECTION");
        }

        return flags;
    }

    private boolean hasFlag(int flag)
    {
        return (mFlags & flag) == flag;
    }

    @Override
    public String toString()
    {
        return getFlags().toString();
    }

    /**
     * Utility method to create flags instance.
     * @param flags number value.
     * @return instance
     */
    public static SiteFlags create(int flags)
    {
        return new SiteFlags(flags);
    }
}
