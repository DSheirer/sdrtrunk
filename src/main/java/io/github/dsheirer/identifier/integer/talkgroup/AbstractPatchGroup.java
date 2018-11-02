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
 * Patch Group - a single talkgroup used to temporarily combine disparate talkgroups
 */
public abstract class AbstractPatchGroup extends AbstractTalkgroup
{
    private List<IIdentifier> mPatchedGroups = new ArrayList<>();

    /**
     * Abstract integer patch group identifier class.
     *
     * @param patchGroup identifier for the patch group
     */
    public AbstractPatchGroup(int patchGroup)
    {
        super(patchGroup);
    }

    /**
     * List of talkgroups that are patched into this patch group
     */
    public List<IIdentifier> getPatchedGroups()
    {
        return mPatchedGroups;
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

    @Override
    public boolean isPatchGroup()
    {
        return true;
    }

    @Override
    boolean isGroup()
    {
        return true;
    }

    @Override
    public Role getRole()
    {
        return Role.TO;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getValue());
        sb.append(" PATCHED FROM ").append(getPatchedGroups());
        return sb.toString();
    }
}
