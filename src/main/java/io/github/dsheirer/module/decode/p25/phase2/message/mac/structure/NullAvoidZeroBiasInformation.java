/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import java.util.Collections;
import java.util.List;

/**
 * Null avoid zero bias information message
 */
public class NullAvoidZeroBiasInformation extends MacStructureVariableLength
{
    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param timestamp of the final bit of the message
     */
    public NullAvoidZeroBiasInformation(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode().toString());
        sb.append(" LENGTH:").append(getLength());
        sb.append(" MSG:").append(getMessage().getSubMessage(getOffset(), getOffset() + (getLength() * 8)).toHexString());
        return sb.toString();
    }

    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
