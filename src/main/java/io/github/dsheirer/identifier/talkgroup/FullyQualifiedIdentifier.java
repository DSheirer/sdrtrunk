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

package io.github.dsheirer.identifier.talkgroup;

/**
 * Fully qualified radio identifier
 */
public class FullyQualifiedIdentifier
{
    private int mWacn;
    private int mSystem;
    private int mId;

    /**
     * Constructs an instance
     * @param wacn
     * @param system
     * @param id
     */
    public FullyQualifiedIdentifier(int wacn, int system, int id)
    {
        mWacn = wacn;
        mSystem = system;
        mId = id;
    }

    @Override
    public String toString()
    {
        return mWacn + "." + mSystem + "." + mId;
    }

    /**
     * Creates an instance with the specified values
     * @param wacn value
     * @param system value
     * @param id value
     * @return instance
     */
    public static FullyQualifiedIdentifier create(int wacn, int system, int id)
    {
        return new FullyQualifiedIdentifier(wacn, system, id);
    }
}
