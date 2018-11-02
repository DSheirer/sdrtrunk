/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.identifier.integer.talkgroup;

import io.github.dsheirer.protocol.Protocol;

/**
 * APCO25 Patch Group
 */
public class APCO25PatchGroup extends AbstractPatchGroup
{
    /**
     * Constructs an APCO25 Patch Group with an implicit TO role
     * @param patchGroup of the talkgroup
     */
    public APCO25PatchGroup(int patchGroup)
    {
        super(patchGroup);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Creates a TO APCO-25 patch group talkgroup identifier
     */
    public static APCO25PatchGroup create(int patchGroup)
    {
        return new APCO25PatchGroup(patchGroup);
    }
}
