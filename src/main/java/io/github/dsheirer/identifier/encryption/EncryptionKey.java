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

package io.github.dsheirer.identifier.encryption;

import java.util.Objects;

public abstract class EncryptionKey implements Comparable<EncryptionKey>
{
    private int mAlgorithm;
    private int mKey;

    public EncryptionKey(int algorithm, int key)
    {
        mAlgorithm = algorithm;
        mKey = key;
    }

    public abstract boolean isEncrypted();

    public int getAlgorithm()
    {
        return mAlgorithm;
    }

    public int getKey()
    {
        return mKey;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ALGORITHM:").append(getAlgorithm());
        sb.append(" KEY:").append(getKey());
        return sb.toString();
    }

    @Override
    public int compareTo(EncryptionKey other)
    {
        if(getAlgorithm() == other.getKey())
        {
            return Integer.compare(getKey(), other.getKey());
        }
        else
        {
            return Integer.compare(getAlgorithm(), other.getAlgorithm());
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        EncryptionKey that = (EncryptionKey)o;
        return getAlgorithm() == that.getAlgorithm() &&
            getKey() == that.getKey();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getAlgorithm(), getKey());
    }
}
