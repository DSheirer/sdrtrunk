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
        super(localAddress, role);
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
     * Indicates if the radio identity is aliased with a persona value that is different from the radio ID.
     * @return true if aliased.
     */
    public boolean isAliased()
    {
        return getValue() != 0 && getValue() != mRadio;
    }

    /**
     * Override the default behavior of RadioIdentifier.isValid() to allow for fully qualified SUID's with a local
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
            return super.toString() + "(" + getFullyQualifiedRadioAddress() + ")";
        }
        else
        {
            return getFullyQualifiedRadioAddress();
        }
    }

    @Override
    public boolean equals(Object o)
    {
        //Attempt to match as a fully qualified radio match first
        if(o instanceof FullyQualifiedRadioIdentifier fqri)
        {
            return getWacn() == fqri.getWacn() &&
                    getSystem() == fqri.getSystem() &&
                    getRadio() == fqri.getRadio();
        }
        //Attempt to match the local address against a simple radio version
        else if(o instanceof RadioIdentifier ri)
        {
            return getValue() != 0 && ri.getValue() != 0 && getValue().equals(ri.getValue());
        }

        return super.equals(o);
    }
}
