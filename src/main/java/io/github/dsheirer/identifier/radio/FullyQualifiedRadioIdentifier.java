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

package io.github.dsheirer.identifier.radio;

import io.github.dsheirer.identifier.Role;

/**
 * Fully qualified radio identifier.  This is used for roaming radios or any time there is a need to fully qualify a
 * radio.
 */
public abstract class FullyQualifiedRadioIdentifier extends RadioIdentifier
{
    private int mWacn;
    private int mSystem;
    private int mRadio;

    /**
     * Constructs an instance
     * @param localAddress radio identifier, aka alias.  This can be the same as the radio ID when the fully qualified radio
     * is not being aliased on a local radio system.
     * @param wacn of the home network for the radio.
     * @param system of the home network for the radio.
     * @param id of the radio within the home network.
     */
    public FullyQualifiedRadioIdentifier(int localAddress, int wacn, int system, int id, Role role)
    {
        super(localAddress > 0 ? localAddress : id, role);
        mWacn = wacn;
        mSystem = system;
        mRadio = id;
    }

    public int getWacn()
    {
        return mWacn;
    }

    public int getSystem()
    {
        return mSystem;
    }

    public int getRadio()
    {
        return mRadio;
    }

    /**
     * Fully qualified radio identity.
     * @return radio identity
     */
    public String getFullyQualifiedRadioAddress()
    {
        return mWacn + "." + mSystem + "." + mRadio;
    }

    /**
     * Indicates if the radio identify is aliased with a persona value that is different from the radio ID.
     * @return true if aliased.
     */
    public boolean isAliased()
    {
        return getValue() != mRadio;
    }

    @Override
    public String toString()
    {
        if(isAliased())
        {
            return super.toString() + "(" + getFullyQualifiedRadioAddress() + ")";
        }
        else
        {
            return getFullyQualifiedRadioAddress();
        }
    }
}
