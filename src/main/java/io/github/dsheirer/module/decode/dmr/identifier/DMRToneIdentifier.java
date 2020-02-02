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

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.tone.ToneIdentifier;
import io.github.dsheirer.identifier.tone.ToneSequence;
import io.github.dsheirer.protocol.Protocol;

/**
 * DMR AMBE audio codec tone metadata
 */
public class DMRToneIdentifier extends ToneIdentifier
{
    public DMRToneIdentifier(ToneSequence value)
    {
        super(value);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    public static DMRToneIdentifier create(ToneSequence value)
    {
        return new DMRToneIdentifier(value);
    }
}
