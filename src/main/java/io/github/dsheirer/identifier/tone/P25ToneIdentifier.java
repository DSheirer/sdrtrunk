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

package io.github.dsheirer.identifier.tone;

import io.github.dsheirer.protocol.Protocol;

/**
 * P25 Phase II AMBE audio codec tone metadata
 */
public class P25ToneIdentifier extends ToneIdentifier
{
    public P25ToneIdentifier(ToneSequence value)
    {
        super(value);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    public static P25ToneIdentifier create(ToneSequence value)
    {
        return new P25ToneIdentifier(value);
    }
}
