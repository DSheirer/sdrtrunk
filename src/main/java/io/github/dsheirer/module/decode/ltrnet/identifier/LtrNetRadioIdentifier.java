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

package io.github.dsheirer.module.decode.ltrnet.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Unique ID or radio ID for LTR-Net radios is a 16-bit logical network radio identifier value.
 */
public class LtrNetRadioIdentifier extends TalkgroupIdentifier implements Comparable<LtrNetRadioIdentifier>
{
    public LtrNetRadioIdentifier(int value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LTR_NET;
    }

    /**
     * Creates a unique identifier for the FROM radio role
     */
    public static LtrNetRadioIdentifier createFrom(int value)
    {
        return new LtrNetRadioIdentifier(value, Role.FROM);
    }

    /**
     * Creates a unique identifier for the TO radio role
     */
    public static LtrNetRadioIdentifier createTo(int value)
    {
        return new LtrNetRadioIdentifier(value, Role.TO);
    }

    @Override
    public int compareTo(LtrNetRadioIdentifier o)
    {
        return getValue().compareTo(o.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LtrNetRadioIdentifier)) return false;
        return compareTo((LtrNetRadioIdentifier) o) == 0;
    }
}
