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
package io.github.dsheirer.alias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.record.Record;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@JacksonXmlRootElement(localName = "alias")
public class Alias
{
    private String mList;
    private String mGroup;
    private String mName;
    private int mColor;
    private String mIconName;
    private List<AliasID> mAliasIDs = new ArrayList<AliasID>();
    private List<AliasAction> mAliasActions = new ArrayList<AliasAction>();

    public Alias()
    {
    }

    public Alias(String name)
    {
        mName = name;
    }

    public String toString()
    {
        return getName();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        mName = name;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "list")
    public String getList()
    {
        return mList;
    }

    public void setList(String list)
    {
        mList = list;
    }

    public boolean hasList()
    {
        return mList != null;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "group")
    public String getGroup()
    {
        return mGroup;
    }

    public void setGroup(String group)
    {
        mGroup = group;
    }

    public boolean hasGroup()
    {
        return mGroup != null;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "color")
    public int getColor()
    {
        return mColor;
    }

    public void setColor(int color)
    {
        mColor = color;
    }

    @JsonIgnore
    public Color getDisplayColor()
    {
        return new Color(getColor());
    }

    @JacksonXmlProperty(isAttribute = true, localName = "iconName")
    public String getIconName()
    {
        return mIconName;
    }

    public void setIconName(String iconName)
    {
        mIconName = iconName;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "id")
    public List<AliasID> getId()
    {
        return mAliasIDs;
    }

    public void setId(ArrayList<AliasID> id)
    {
        mAliasIDs = id;
    }

    public void addAliasID(AliasID id)
    {
        mAliasIDs.add(id);
    }

    public void removeAliasID(AliasID id)
    {
        mAliasIDs.remove(id);
    }

    @JacksonXmlProperty(isAttribute = false, localName = "action")
    public List<AliasAction> getAction()
    {
        return mAliasActions;
    }

    public void setAction(List<AliasAction> actions)
    {
        mAliasActions = actions;
    }

    public void addAliasAction(AliasAction action)
    {
        mAliasActions.add(action);
    }

    public void removeAliasAction(AliasAction action)
    {
        mAliasActions.remove(action);
    }

    public boolean hasActions()
    {
        return !mAliasActions.isEmpty();
    }

    /**
     * Perform any validation/cleanup actions on this alias.
     */
    public void validate()
    {
    }

    /**
     * Returns the priority level of this alias, if defined, or the default priority
     */
    @JsonIgnore
    public int getPlaybackPriority()
    {
        for(AliasID id : mAliasIDs)
        {
            if(id.getType() == AliasIDType.PRIORITY)
            {
                return ((Priority)id).getPriority();
            }
        }

        return Priority.DEFAULT_PRIORITY;
    }

    public boolean hasCallPriority()
    {
        for(AliasID id : mAliasIDs)
        {
            if(id.getType() == AliasIDType.PRIORITY)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets or updates the call priority
     */
    public void setCallPriority(int priority)
    {
        if(priority == Priority.DO_NOT_MONITOR ||
            (Priority.MIN_PRIORITY <= priority && priority <= Priority.MAX_PRIORITY))
        {
            for(AliasID id : mAliasIDs)
            {
                if(id.getType() == AliasIDType.PRIORITY)
                {
                    ((Priority)id).setPriority(priority);
                    return;
                }
            }

            //If we don't find a priority id, create one
            Priority p = new Priority();
            p.setPriority(priority);
            addAliasID(p);
        }
    }

    /**
     * Inspects the alias for a non-recordable alias id.  Default is true;
     */
    @JsonIgnore
    public boolean isRecordable()
    {
        for(AliasID id : getId())
        {
            if(id.getType() == AliasIDType.RECORD)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets or removes the record alias ID for this alias.
     */
    public void setRecordable(boolean recordable)
    {
        if(recordable)
        {
            for(AliasID id : getId())
            {
                if(id.getType() == AliasIDType.RECORD)
                {
                    return;
                }
            }

            addAliasID(new Record());
        }
        else
        {
            AliasID toRemove = null;

            for(AliasID id : getId())
            {
                if(id.getType() == AliasIDType.RECORD)
                {
                    toRemove = id;
                    break;
                }
            }

            if(toRemove != null)
            {
                removeAliasID(toRemove);
            }
        }
    }

    /**
     * Inspects the alias for broadcast channel/streamable alias ids.  Default is false;
     */
    @JsonIgnore
    public boolean isStreamable()
    {
        for(AliasID id : getId())
        {
            if(id.getType() == AliasIDType.BROADCAST_CHANNEL)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * List of broadcast channels specified for this alias.
     */
    @JsonIgnore
    public Set<BroadcastChannel> getBroadcastChannels()
    {
        Set<BroadcastChannel> broadcastChannels = new TreeSet<>();

        for(AliasID id : getId())
        {
            if(id.getType() == AliasIDType.BROADCAST_CHANNEL)
            {
                broadcastChannels.add((BroadcastChannel)id);
            }
        }

        return broadcastChannels;
    }

    /**
     * Indicates if this alias contains a broadcast channel alias id with the channel name.
     */
    public boolean hasBroadcastChannel(String channel)
    {
        if(channel == null || channel.isEmpty())
        {
            return false;
        }

        for(AliasID id : getId())
        {
            if(id.getType() == AliasIDType.BROADCAST_CHANNEL &&
                ((BroadcastChannel)id).getChannelName().contentEquals(channel))
            {
                return true;
            }
        }

        return false;
    }
}
