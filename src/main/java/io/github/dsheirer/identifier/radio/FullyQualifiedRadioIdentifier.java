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

package io.github.dsheirer.identifier.radio;

import io.github.dsheirer.identifier.Role;

/**
 * Fully qualified radio identifier
 */
public abstract class FullyQualifiedRadioIdentifier extends RadioIdentifier
{
    private int mWacn;
    private int mSystem;

    /**
     * Constructs an instance
     * @param wacn
     * @param system
     * @param id
     */
    public FullyQualifiedRadioIdentifier(int wacn, int system, int id, Role role)
    {
        super(id, role);
        mWacn = wacn;
        mSystem = system;
    }

    @Override
    public String toString()
    {
        return mWacn + "." + mSystem + "." + super.toString();
    }
}
