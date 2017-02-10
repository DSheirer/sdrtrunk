/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package module.decode.p25;

import alias.Alias;
import alias.AliasList;
import alias.PatchGroupAlias;
import alias.id.talkgroup.TalkgroupID;
import module.decode.event.CallEvent;
import sample.Broadcaster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PatchGroupManager
{
    private AliasList mAliasList;
    private List<PatchGroup> mPatchGroups = new ArrayList<>();
    private Broadcaster<CallEvent> mCallEventBroadcaster;

    /**
     * Manages active P25 patch groups and auto-expires patch groups if not updated every 5 seconds.
     *
     * @param aliasList to keep updated with patch group changes
     * @param broadcaster for rebroadcasting patch group related call events
     */
    public PatchGroupManager(AliasList aliasList, Broadcaster<CallEvent> broadcaster)
    {
        mAliasList = aliasList;
        mCallEventBroadcaster = broadcaster;
    }

    /**
     * Removes the patch group.
     *
     * @param patchGroupID of the patch group to remove
     */
    public void removePatchGroup(String patchGroupID)
    {
        removePatchGroup(patchGroupID, false);
    }

    /**
     * Removes the patch group.
     *
     * @param patchGroupID of the patch group to remove
     * @param expired true if the patch group is being removed because it expired, false if we receive a
     * message commanding commanding a patch group delete.
     */
    private void removePatchGroup(String patchGroupID, boolean expired)
    {
        PatchGroup patchGroup = getPatchGroup(patchGroupID);

        if(patchGroup != null)
        {
            //Cleanup the alias list
            if(mAliasList != null)
            {
                Alias alias = mAliasList.getTalkgroupAlias(patchGroupID);

                if(alias instanceof PatchGroupAlias)
                {
                    PatchGroupAlias patchGroupAlias = (PatchGroupAlias)alias;

                    mAliasList.removeAlias(patchGroupAlias);

                    //Replace our temporary patch group alias with the original alias for the patch group ID
                    if(patchGroupAlias.hasPatchGroupAlias())
                    {
                        mAliasList.addAlias(patchGroupAlias.getPatchGroupAlias());
                    }
                }
            }

            mPatchGroups.remove(patchGroup);

            if(mCallEventBroadcaster != null)
            {
                StringBuilder sb = new StringBuilder();

                sb.append((expired ? "EXPIRED " : "DELETED "));
                sb.append("PATCH GROUP:").append(patchGroup.getPatchGroupID());
                sb.append(" ").append(patchGroup.getPatchedGroups());


                mCallEventBroadcaster.broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PATCH_GROUP_DELETE)
                    .to(patchGroup.getPatchGroupID())
                    .details(sb.toString())
                    .build());
            }
        }
    }

    /**
     * Adds/updates a patch group alias to the alias list containing aliases for each of the patched talkgroups.  If the
     * patch group alias already exists, any patched talkgroups will be removed from the existing patch group alias and
     * replaced with the aliases corresponding to the patched talkgroup aliases.
     *
     * @param patchGroupID for the patch group
     * @param patchedTalkgroups containing the talkgroup IDs for each of the patched talkgroups
     */
    public void updatePatchGroup(String patchGroupID, List<String> patchedTalkgroups)
    {
        boolean updated = false;

        PatchGroup patchGroup = getPatchGroup(patchGroupID);

        if(patchGroup == null)
        {
            //Remove any expired patch groups before we add a new one
            cleanupPatchGroups();
            patchGroup = new PatchGroup(patchGroupID);
            mPatchGroups.add(patchGroup);
            updated = true;
        }
        else
        {
            patchGroup.updateTimestamp();
        }

        for(String patchedGroup : patchedTalkgroups)
        {
            if(patchGroup.addPatchedGroup(patchedGroup))
            {
                updated = true;
            }
        }

        //If we make any changes to the patch group, update the patch group alias
        if(updated)
        {
            PatchGroupAlias patchGroupAlias = null;

            //Check for an existing alias for the patch talkgroup - do not include wildcard aliases
            if(mAliasList != null)
            {
                Alias existingAlias = mAliasList.getTalkgroupAlias(patchGroupID, false);

                if(existingAlias instanceof PatchGroupAlias)
                {
                    patchGroupAlias = (PatchGroupAlias)existingAlias;
                }
                else
                {
                    patchGroupAlias = new PatchGroupAlias();

                    patchGroupAlias.addAliasID(new TalkgroupID(patchGroupID));

                    if(existingAlias != null)
                    {
                        mAliasList.removeAlias(existingAlias);

                        patchGroupAlias.setPatchGroupAlias(existingAlias);
                    }

                    mAliasList.addAlias(patchGroupAlias);
                }

                if(patchGroupAlias != null)
                {
                    patchGroupAlias.setPatchedTalkgroupIDs(patchGroup.getPatchedGroups());

                    patchGroupAlias.clearPatchedAliases();

                    for(String patchedTalkgroup : patchGroup.getPatchedGroups())
                    {
                        Alias patchedAlias = mAliasList.getTalkgroupAlias(patchedTalkgroup);

                        if(patchedAlias != null)
                        {
                            patchGroupAlias.addPatchedAlias(patchedAlias);
                        }
                    }
                }
            }

            if(mCallEventBroadcaster != null)
            {
                StringBuilder sb = new StringBuilder();

                sb.append("PATCH GROUP:").append(patchGroup.getPatchGroupID());
                sb.append(" ").append(patchGroup.getPatchedGroups());

                mCallEventBroadcaster.broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PATCH_GROUP_ADD)
                    .aliasList(mAliasList)
                    .details(sb.toString())
                    .build());
            }
        }
    }

    /**
     * Returns the patch group matching the patch group ID or null
     */
    private PatchGroup getPatchGroup(String patchGroupID)
    {
        for(PatchGroup patchGroup : mPatchGroups)
        {
            if(patchGroup.getPatchGroupID().equals(patchGroupID))
            {
                return patchGroup;
            }
        }

        return null;
    }

    /**
     * Checks the last updated timestamp on all patch groups and removes any patch groups that have not been
     * updated in the past 5 seconds
     */
    public void cleanupPatchGroups()
    {
        Iterator<PatchGroup> it = mPatchGroups.iterator();

        while(it.hasNext())
        {
            PatchGroup patchGroup = it.next();

            if(patchGroup.isExpired())
            {
                it.remove();

                removePatchGroup(patchGroup.getPatchGroupID(), true);
            }
        }
    }
}
