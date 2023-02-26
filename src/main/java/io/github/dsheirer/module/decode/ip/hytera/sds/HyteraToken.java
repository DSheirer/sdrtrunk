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

package io.github.dsheirer.module.decode.ip.hytera.sds;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Base abstract class for a field parsed from a Hytera Data Service message.
 */
public abstract class HyteraToken
{
    protected CorrectedBinaryMessage mMessage;

    /**
     * Constructs an instance
     * @param message containing both the token, run-length, and content
     */
    public HyteraToken(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    /**
     * Identifies the token or field type for this field.
     */
    abstract HyteraTokenType getTokenType();
}
