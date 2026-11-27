/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Pager capcode (address) identifier.  The capcode identifies the pager receiving the message (TO).
 */
public class PagerCapcodeIdentifier extends RadioIdentifier implements Comparable<PagerCapcodeIdentifier>
{
    private final Protocol mProtocol;

    /**
     * Constructs an instance
     * @param capcode value
     * @param protocol POCSAG or FLEX
     */
    public PagerCapcodeIdentifier(int capcode, Protocol protocol)
    {
        super(capcode, Role.TO);
        mProtocol = protocol;
    }

    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    @Override
    public boolean isValid()
    {
        return getValue() >= 0;
    }

    @Override
    public int compareTo(PagerCapcodeIdentifier other)
    {
        int comparison = getProtocol().compareTo(other.getProtocol());
        return comparison != 0 ? comparison : Integer.compare(getValue(), other.getValue());
    }

    @Override
    public int hashCode()
    {
        return 31 * getValue() + mProtocol.hashCode();
    }
}
