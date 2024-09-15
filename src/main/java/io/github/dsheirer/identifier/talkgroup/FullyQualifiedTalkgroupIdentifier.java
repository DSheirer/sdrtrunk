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
        super(localAddress, role);
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
     * Indicates if the talkgroup identity is aliased with a persona value that is different from the talkgroup ID.
     * @return true if aliased.
     */
    public boolean isAliased()
    {
        return getValue() != 0 && getValue() != mTalkgroup;
    }

    /**
     * Override the default behavior of TalkgroupIdentifier.isValid() to allow for fully qualified TGID's with a local
     * ID of zero which indicates an ISSI patch.
     */
    @Override
    public boolean isValid()
    {
        return true;
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

    @Override
    public boolean equals(Object o)
    {
        //Attempt to match as a fully qualified talkgroup match first
        if(o instanceof FullyQualifiedTalkgroupIdentifier fqti)
        {
            return getWacn() == fqti.getWacn() &&
                    getSystem() == fqti.getSystem() &&
                    getTalkgroup() == fqti.getTalkgroup();
        }
        //Attempt to match the local address against a simple talkgroup version
        else if(o instanceof TalkgroupIdentifier ti)
        {
            return getValue() != 0 && ti.getValue() != 0 && getValue().equals(ti.getValue());
        }

        return super.equals(o);
    }
}
