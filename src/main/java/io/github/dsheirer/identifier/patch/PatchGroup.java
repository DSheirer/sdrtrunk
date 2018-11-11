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

package io.github.dsheirer.identifier.patch;

import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatchGroup
{
    private TalkgroupIdentifier mPatchGroupIdentifier;
    private List<TalkgroupIdentifier> mPatchedGroupIdentifiers = new ArrayList<>();

    public PatchGroup(TalkgroupIdentifier patchGroupIdentifier)
    {
        mPatchGroupIdentifier = patchGroupIdentifier;
    }

    public TalkgroupIdentifier getPatchGroup()
    {
        return mPatchGroupIdentifier;
    }

    public void addPatchedGroup(TalkgroupIdentifier patchedGroupIdentifier)
    {
        mPatchedGroupIdentifiers.add(patchedGroupIdentifier);
    }

    public void addPatchedGroups(List<TalkgroupIdentifier> patchedGroupIdentifiers)
    {
        mPatchedGroupIdentifiers.addAll(patchedGroupIdentifiers);
    }

    public List<TalkgroupIdentifier> getPatchedGroupIdentifiers()
    {
        return mPatchedGroupIdentifiers;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getPatchGroup());

        if(!getPatchedGroupIdentifiers().isEmpty())
        {
            sb.append(" PATCHED FROM ").append(getPatchedGroupIdentifiers());
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        PatchGroup that = (PatchGroup)o;
        return Objects.equals(mPatchGroupIdentifier, that.mPatchGroupIdentifier);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPatchGroupIdentifier);
    }
}
