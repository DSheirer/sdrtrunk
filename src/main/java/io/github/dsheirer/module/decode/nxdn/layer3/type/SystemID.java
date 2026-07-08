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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;

/**
 * Type-D System ID.
 */
public class SystemID extends LocationID
{
    private static final IntField INTEGRATOR_CODE = IntField.length4(0);
    private static final IntField SYSTEM_CODE = IntField.length15(4);

    /**
     * Constructs an instance
     * @param message containing the System ID field
     * @param offset to the start of the field
     */
    public SystemID(CorrectedBinaryMessage message, int offset)
    {
        super(message.getInt(INTEGRATOR_CODE, offset), message.getInt(SYSTEM_CODE, offset));
    }
}
