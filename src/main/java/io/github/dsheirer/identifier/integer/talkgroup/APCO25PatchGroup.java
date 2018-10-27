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

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * APCO25 Talkgroup Identifier with a FROM role.
 */
public class APCO25PatchGroup extends APCO25Talkgroup
{
    private List<IIdentifier> mPatchedGroups = new ArrayList<>();

    /**
     * Constructs an APCO25 Talkgroup Identifier with a FROM role.
     *
     * @param value of the talkgroup
     */
    public APCO25PatchGroup(int value)
    {
        super(value);
    }

    @Override
    public Role getRole()
    {
        return Role.TO;
    }

    @Override
    boolean isGroup()
    {
        return true;
    }

    @Override
    public boolean isPatchGroup()
    {
        return true;
    }

    @Override
    public List<IIdentifier> getPatchedGroups()
    {
        return mPatchedGroups;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getValue());
        sb.append(" PATCHED FROM ").append(getPatchedGroups());
        return sb.toString();
    }


    /**
     * Adds the patched group to this patch group
     * @param patchedGroup to add
     */
    public void addPatchedGroup(IIdentifier patchedGroup)
    {
        mPatchedGroups.add(patchedGroup);
    }

    /**
     * Adds the patched groups to this patch group
     * @param patchedGroups to add
     */
    public void addPatchedGroups(List<IIdentifier> patchedGroups)
    {
        mPatchedGroups.addAll(patchedGroups);
    }

    /**
     * Creates a TO APCO-25 patch group talkgroup identifier
     */
    public static APCO25PatchGroup create(int patchGroup)
    {
        return new APCO25PatchGroup(patchGroup);
    }
}
