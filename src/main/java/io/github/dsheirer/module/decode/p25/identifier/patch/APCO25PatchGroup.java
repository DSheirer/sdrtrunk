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
package io.github.dsheirer.module.decode.p25.identifier.patch;

import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO25 Patch Group
 */
public class APCO25PatchGroup extends PatchGroupIdentifier
{
    public APCO25PatchGroup(PatchGroup value)
    {
        super(value);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    public static APCO25PatchGroup create(PatchGroup patchGroup)
    {
        return new APCO25PatchGroup(patchGroup);
    }

    public static APCO25PatchGroup create(int supergroup)
    {
        return new APCO25PatchGroup(new PatchGroup(APCO25Talkgroup.create(supergroup)));
    }
}
