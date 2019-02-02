/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for (temporary) patch groups and related identifiers.
 *
 * Patch groups are primarily used on Motorola P25 systems.  This manager monitors patch group additions and deletions
 * and maintains a map of the current patch groups.
 *
 * Traffic channel calls will sometimes reference a patch group by simply using the patch group identifier.  This
 * manager will replace that reference with the full patch group so that the call event has the full patch group
 * including all of the patched groups, which may not have been included in the patch group reference on the
 * traffic channel.
 */
public class PatchGroupManager
{
    //    private final static Logger mLog = LoggerFactory.getLogger(PatchGroupManager.class);
    private Map<Integer,Long> mPatchGroupLastUpdatedMap = new HashMap<>();
    private Map<Integer,PatchGroupIdentifier> mPatchGroupMap = new HashMap<>();

    public PatchGroupManager()
    {
    }

    /**
     * Adds the patch group identifier to the set of managed patch groups.  If the patch group is already being
     * managed, the new identifier is checked for additional patched talkgroups and they are added to the existing
     * managed patch group.
     *
     * @param patchGroupIdentifier to add or update
     */
    public synchronized void addPatchGroup(PatchGroupIdentifier patchGroupIdentifier)
    {
        PatchGroup update = patchGroupIdentifier.getValue();

        if(update.getPatchGroup().getValue() > 0)
        {
            if(mPatchGroupMap.containsKey(update.getPatchGroup().getValue()))
            {
                PatchGroup current = mPatchGroupMap.get(update.getPatchGroup().getValue()).getValue();
                current.addPatchedGroups(update.getPatchedGroupIdentifiers());
            }
            else
            {
                mPatchGroupMap.put(update.getPatchGroup().getValue(), patchGroupIdentifier);
            }
        }
    }

    /**
     * Adds any patch group identifiers contained in the list.
     */
    public synchronized void addPatchGroups(List<Identifier> identifiers)
    {
        for(Identifier identifier : identifiers)
        {
            if(identifier instanceof PatchGroupIdentifier)
            {
                addPatchGroup((PatchGroupIdentifier)identifier);
            }
        }
    }

    /**
     * Removes the patch group from this manager if it is currently being managed.
     *
     * @param patchGroupIdentifier to remove
     */
    public synchronized void removePatchGroup(PatchGroupIdentifier patchGroupIdentifier)
    {
        int id = patchGroupIdentifier.getValue().getPatchGroup().getValue();
        mPatchGroupMap.remove(id);
        mPatchGroupLastUpdatedMap.remove(id);
    }

    /**
     * Removes any patch group identifiers contained in the list.
     */
    public synchronized void removePatchGroups(List<Identifier> identifiers)
    {
        for(Identifier identifier : identifiers)
        {
            if(identifier instanceof PatchGroupIdentifier)
            {
                removePatchGroup((PatchGroupIdentifier)identifier);
            }
        }
    }

    /**
     * Updates the list of identifiers by replacing any talkgroups or patch groups with the current
     * version of the patch group and a complete listing of the patched talkgroups.
     * @param identifiers to update
     * @return list of identifiers
     */
    public List<Identifier> update(List<Identifier> identifiers)
    {
        List<Identifier> updated = new ArrayList<>();

        for(Identifier identifier: identifiers)
        {
            updated.add(update(identifier));
        }

        return updated;
    }

    /**
     * Checks the PATCH GROUP identifier and replaces the identifier with the current patch group
     * if the identifier matches a currently managed patch group.
     *
     * @param identifier for a talkgroup or a patch group.
     * @return current patch group or the original identifier
     */
    public Identifier update(Identifier identifier)
    {
        if(identifier == null)
        {
            return identifier;
        }

        if(identifier.getIdentifierClass() == IdentifierClass.USER && identifier.getRole() == Role.TO)
        {
            switch(identifier.getForm())
            {
                case PATCH_GROUP:
                    if(identifier instanceof PatchGroupIdentifier)
                    {
                        PatchGroupIdentifier patchGroupIdentifier = (PatchGroupIdentifier)identifier;
                        int patchGroupId = patchGroupIdentifier.getValue().getPatchGroup().getValue();

                        if(mPatchGroupMap.containsKey(patchGroupId))
                        {
                            return mPatchGroupMap.get(patchGroupId);
                        }
                        else
                        {
                            addPatchGroup(patchGroupIdentifier);
                        }
                    }
                    break;
            }
        }

        return identifier;
    }
}
