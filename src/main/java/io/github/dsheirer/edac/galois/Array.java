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

package io.github.dsheirer.edac.galois;

public abstract class Array<T>
{
    public Array(int size)
    {
        allocate(size);
    }

    public abstract T get(int index);
    public abstract void set(int index, T value);
    public abstract void set_size(int size, boolean copy);
    public abstract int size();
    protected abstract void allocate(int size);

    public void set_size(int size)
    {
        set_size(size, false);
    }

    /**
     * Assigns the value to all elements of this array.  Resizes zero length array to have at least one element to
     * receive the value assignment.
     * @param value to assign.
     */
    public void set(T value)
    {
        if(size() == 0)
        {
            allocate(1);
        }

        for(int x = 0; x < size(); x++)
        {
            set(x, value);
        }
    }
}
