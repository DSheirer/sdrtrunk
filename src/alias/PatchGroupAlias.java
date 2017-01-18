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
package alias;

import alias.action.AliasAction;
import alias.id.AliasID;
import alias.id.AliasIDType;
import alias.id.broadcast.BroadcastChannel;
import alias.id.priority.Priority;
import alias.id.talkgroup.TalkgroupID;

import java.util.ArrayList;
import java.util.List;

public class PatchGroupAlias extends Alias
{
    private Alias mPatchGroupAlias;
    private List<Alias> mPatchedAliases = new ArrayList<>();
    private List<String> mPatchedTalkgroupIDs = new ArrayList<>();

    /**
     * Patch group alias is a single alias for a group of aliases that are temporarily patched together.  This will
     * normally be used for patched talkgroups, but could be used with any type of alias identifiers that can be
     * temporarily joined to form a patch group.
     *
     * Alias properties such as recordable, streamable and call priority are an aggregation from the set of patched
     * aliases contained in this patch group alias and the patch group ID alias and where applicable, return the highest
     * value/priority from the group of patched aliases.
     */
    public PatchGroupAlias()
    {
    }

    /**
     * Sets an alias for this patch group ID
     */
    public void setPatchGroupAlias(Alias alias)
    {
        mPatchGroupAlias = alias;
    }

    /**
     * Alias for this patch group
     */
    public Alias getPatchGroupAlias()
    {
        return mPatchGroupAlias;
    }

    /**
     * Indicates if this patch group has an alias
     */
    public boolean hasPatchGroupAlias()
    {
        return mPatchGroupAlias != null;
    }

    /**
     * List of patched aliases contained in this patch group
     */
    public List<Alias> getPatchedAliases()
    {
        return mPatchedAliases;
    }

    /**
     * Removes/clears all patched aliases from this patch alias group
     */
    public void clearPatchedAliases()
    {
        mPatchedAliases.clear();
    }

    /**
     * Adds the patched alias to this patch alias group
     */
    public void addPatchedAlias(Alias alias)
    {
        if(alias == PatchGroupAlias.this)
        {
            throw new IllegalArgumentException("Can't add patch group alias to itself - would create an infinite loop");
        }

        mPatchedAliases.add(alias);
    }

    /**
     * Removes the patched alias from this patch alias group
     */
    public void removePatchedAlias(Alias alias)
    {
        mPatchedAliases.remove(alias);
    }

    /**
     * Sets the list of patched talkgroup IDs for this patch group
     */
    public void setPatchedTalkgroupIDs(List<String> talkgroupIDs)
    {
        mPatchedTalkgroupIDs = talkgroupIDs;
    }

    /**
     * List of patched talkgroup IDs for this patch group
     */
    public List<String> getPatchedTalkgroupIDs()
    {
        return mPatchedTalkgroupIDs;
    }

    @Override
    public String getName()
    {
        if(hasPatchGroupAlias())
        {
            return getPatchGroupAlias().getName();
        }
        else
        {
            for(AliasID id: getId())
            {
                if(id.getType() == AliasIDType.TALKGROUP)
                {
                    return "PATCH:" + ((TalkgroupID)id).getTalkgroup();
                }
            }
        }

        return "PATCH:****";
    }

    @Override
    public List<AliasAction> getAction()
    {
        List<AliasAction> aliasActions = new ArrayList<>();

        if(hasPatchGroupAlias())
        {
            aliasActions.addAll(getPatchGroupAlias().getAction());
        }

        for(Alias alias: mPatchedAliases)
        {
            for(AliasAction action: alias.getAction())
            {
                if(!aliasActions.contains(action))
                {
                    aliasActions.add(action);
                }
            }
        }

        return aliasActions;
    }

    @Override
    public boolean hasActions()
    {
        for(Alias alias: mPatchedAliases)
        {
            if(alias.hasActions())
            {
                return true;
            }
        }

        return hasPatchGroupAlias() && getPatchGroupAlias().hasActions();
    }

    @Override
    public int getCallPriority()
    {
        int highestPriority = (hasPatchGroupAlias() ? getPatchGroupAlias().getCallPriority() : Priority.DEFAULT_PRIORITY);

        for(Alias alias: mPatchedAliases)
        {
            if(alias.hasCallPriority() && alias.getCallPriority() < highestPriority)
            {
                highestPriority = alias.getCallPriority();
            }
        }

        return highestPriority;
    }

    @Override
    public boolean hasCallPriority()
    {
        if(hasPatchGroupAlias() && getPatchGroupAlias().hasCallPriority())
        {
            return true;
        }

        for(Alias alias: mPatchedAliases)
        {
            if(alias.hasCallPriority())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isRecordable()
    {
        if(hasPatchGroupAlias() && getPatchGroupAlias().isRecordable())
        {
            return true;
        }

        for(Alias alias: mPatchedAliases)
        {
            if(alias.isRecordable())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isStreamable()
    {
        if(hasPatchGroupAlias() && getPatchGroupAlias().isStreamable())
        {
            return true;
        }

        for(Alias alias: mPatchedAliases)
        {
            if(alias.isStreamable())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<BroadcastChannel> getBroadcastChannels()
    {
        List<BroadcastChannel> broadcastChannels = new ArrayList<>();

        if(hasPatchGroupAlias())
        {
            broadcastChannels.addAll(getPatchGroupAlias().getBroadcastChannels());
        }

        for(Alias alias: mPatchedAliases)
        {
            for(BroadcastChannel broadcastChannel: alias.getBroadcastChannels())
            {
                if(!broadcastChannels.contains(broadcastChannel))
                {
                    broadcastChannels.add(broadcastChannel);
                }
            }
        }

        return broadcastChannels;
    }

    @Override
    public boolean hasBroadcastChannel(String channel)
    {
        if(hasPatchGroupAlias() && getPatchGroupAlias().hasBroadcastChannel(channel))
        {
            return true;
        }

        for(Alias alias: mPatchedAliases)
        {
            if(alias.hasBroadcastChannel(channel))
            {
                return true;
            }
        }

        return false;
    }
}
