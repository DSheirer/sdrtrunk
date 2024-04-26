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

package io.github.dsheirer.identifier.talkgroup;

import io.github.dsheirer.identifier.Role;

/**
 * Fully qualified radio identifier
 */
public abstract class FullyQualifiedTalkgroupIdentifier extends TalkgroupIdentifier
{
    private int mWacn;
    private int mSystem;
    private int mTalkgroup;

    /**
     * Constructs an instance
     * @param localAddress used on the local system as an alias to the fully qualified talkgroup.
     * @param wacn for the talkgroup home system.
     * @param system for the talkgroup home system.
     * @param id for the talkgroup within the home system.
     * @param role played by the talkgroup.
     */
    public FullyQualifiedTalkgroupIdentifier(int localAddress, int wacn, int system, int id, Role role)
    {
        super(localAddress > 0 ? localAddress : id, role);
        mWacn = wacn;
        mSystem = system;
        mTalkgroup = id;
    }

    public int getWacn()
    {
        return mWacn;
    }

    public int getSystem()
    {
        return mSystem;
    }

    public int getTalkgroup()
    {
        return mTalkgroup;
    }

    /**
     * Fully qualified talkgroup identity.
     * @return talkgroup identity
     */
    public String getFullyQualifiedTalkgroupAddress()
    {
        return mWacn + "." + mSystem + "." + mTalkgroup;
    }

    /**
     * Indicates if the talkgroup identify is aliased with a persona value that is different from the talkgroup ID.
     * @return true if aliased.
     */
    public boolean isAliased()
    {
        return getValue() != mTalkgroup;
    }

    @Override
    public String toString()
    {
        if(isAliased())
        {
            return super.toString() + "(" + getFullyQualifiedTalkgroupAddress() + ")";
        }
        else
        {
            return getFullyQualifiedTalkgroupAddress();
        }
    }
}
