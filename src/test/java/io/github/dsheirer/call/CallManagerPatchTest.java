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
package io.github.dsheirer.call;

import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The call manager captures the talkgroups patched into a patch group call.
 */
public class CallManagerPatchTest
{
    @Test
    public void capturesPatchedTalkgroups()
    {
        PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(500));
        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(1001));
        patchGroup.addPatchedTalkgroup(APCO25FullyQualifiedTalkgroupIdentifier.createTo(1002, 781824, 1, 1002));

        List<PatchedTalkgroup> patched = CallManager.getPatchedTalkgroups(APCO25PatchGroup.create(patchGroup));

        assertEquals(List.of(new PatchedTalkgroup(1001, null), new PatchedTalkgroup(1002, "781824.1.1002")), patched);
        assertEquals(List.of(), CallManager.getPatchedTalkgroups(APCO25Talkgroup.create(1001)), "not a patch group");
    }
}
