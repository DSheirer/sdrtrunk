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
package io.github.dsheirer.module.decode.ctcss;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

/**
 * Identifier for CTCSS (PL) tones.
 */
public class CTCSSIdentifier extends Identifier<CTCSSCode>
{
    /**
     * Constructor
     * @param code the detected CTCSS code
     */
    public CTCSSIdentifier(CTCSSCode code)
    {
        super(code, IdentifierClass.USER, Form.TONE, Role.FROM);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.CTCSS;
    }

    /**
     * @return the CTCSS code
     */
    public CTCSSCode getCode()
    {
        return getValue();
    }

    /**
     * @return the tone frequency in Hz
     */
    public float getFrequency()
    {
        return getValue().getFrequency();
    }

    @Override
    public String toString()
    {
        return getValue().getDisplayString();
    }
}