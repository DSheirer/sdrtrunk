/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.voice.embedded;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Short burst payload that doesn't pass the BPTC 16 fec check.
 */
public class NonStandardShortBurst extends ShortBurst
{
    /**
     * Constructor
     *
     * @param message containing the delinterleaved and error-corrected short burst payload.
     */
    public NonStandardShortBurst(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        return "NON-STANDARD SHORT BURST:" + getMessage().toHexString();
    }
}
