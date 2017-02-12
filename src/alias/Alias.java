/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package alias;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import alias.action.AliasAction;
import alias.id.AliasID;
import alias.id.AliasIDType;
import alias.id.broadcast.BroadcastChannel;
import alias.id.nonrecordable.NonRecordable;
import alias.id.priority.Priority;

@XmlSeeAlso( { AliasID.class, AliasAction.class } )
@XmlRootElement( name = "alias" )
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

	public Alias( String name )
	{
		mName = name;
	}
	
	public String toString()
	{
		return getName();
	}

	@XmlAttribute( name="name" )
	public String getName()
	{
		return mName;
	}
	
	public void setName( String name )
	{
		mName = name;
	}

	@XmlAttribute( name="list" )
	public String getList()
	{
		return mList;
	}
	
	public void setList( String list )
	{
		mList = list;
	}
	
	public boolean hasList()
	{
		return mList != null;
	}

	@XmlAttribute( name="group" )
	public String getGroup()
	{
		return mGroup;
	}
	
	public void setGroup( String group )
	{
		mGroup = group;
	}
	
	public boolean hasGroup()
	{
		return mGroup != null;
	}

	@XmlAttribute
	public int getColor()
	{
		return mColor;
	}

	public void setColor( int color )
	{
		mColor = color;
	}

	public Color getDisplayColor()
	{
		return new Color( getColor() );
	}
	
	@XmlAttribute
	public String getIconName()
	{
		return mIconName;
	}
	
	public void setIconName( String iconName )
	{
		mIconName = iconName;
	}
	
	@XmlElement( name="id" )
	public List<AliasID> getId()
	{
		return mAliasIDs;
	}
	
	public void setId( ArrayList<AliasID> id )
	{
		mAliasIDs = id;
	}
	
	public void addAliasID( AliasID id )
	{
		mAliasIDs.add( id );
	}
	
	public void removeAliasID( AliasID id )
	{
		mAliasIDs.remove( id );
	}
	
	@XmlElement( name="action" )
	public List<AliasAction> getAction()
	{
		return mAliasActions;
	}
	
	public void setAction( List<AliasAction> actions )
	{
		mAliasActions = actions;
	}
	
	public void addAliasAction( AliasAction action )
	{
		mAliasActions.add( action );
	}
	
	public void removeAliasAction( AliasAction action )
	{
		mAliasActions.remove( action );
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
	@XmlTransient
	public int getCallPriority()
	{
		for( AliasID id: mAliasIDs )
		{
			if( id.getType() == AliasIDType.PRIORITY)
			{
				return ((Priority)id).getPriority();
			}
		}
		
		return Priority.DEFAULT_PRIORITY;
	}
	
	public boolean hasCallPriority()
	{
		for( AliasID id: mAliasIDs )
		{
			if( id.getType() == AliasIDType.PRIORITY)
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Sets or updates the call priority
	 */
	public void setCallPriority( int priority )
	{
		if( priority == Priority.DO_NOT_MONITOR ||
			( Priority.MIN_PRIORITY <= priority && priority <= Priority.MAX_PRIORITY ) )
		{
			for( AliasID id: mAliasIDs )
			{
				if( id.getType() == AliasIDType.PRIORITY)
				{
					((Priority)id).setPriority( priority );
					return;
				}
			}
			
			//If we don't find a priority id, create one
			Priority p = new Priority();
			p.setPriority( priority );
			addAliasID( p );
		}
	}

	/**
	 * Inspects the alias for a non-recordable alias id.  Default is true;
	 */
	@XmlTransient
	public boolean isRecordable()
	{
		for( AliasID id: getId() )
		{
			if( id.getType() == AliasIDType.NON_RECORDABLE)
			{
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Sets or removes the non-recordable alias ID for this alias.
	 */
	public void setRecordable( boolean recordable )
	{
		if( recordable )
		{
			AliasID toRemove = null;
			
			for( AliasID id: getId() )
			{
				if( id.getType() == AliasIDType.NON_RECORDABLE)
				{
					toRemove = id;
					break;
				}
			}
			
			if( toRemove != null )
			{
				removeAliasID( toRemove );
			}
		}
		else
		{
			for( AliasID id: getId() )
			{
				if( id.getType() == AliasIDType.NON_RECORDABLE)
				{
					return;
				}
			}
			
			addAliasID( new NonRecordable() );
		}
	}

    /**
     * Inspects the alias for broadcast channel/streamable alias ids.  Default is false;
     */
    @XmlTransient
    public boolean isStreamable()
    {
        for( AliasID id: getId() )
        {
            if( id.getType() == AliasIDType.BROADCAST_CHANNEL)
            {
                return true;
            }
        }

        return false;
    }

    /**
	 * List of broadcast channels specified for this alias.
	 */
    @XmlTransient
	public Set<BroadcastChannel> getBroadcastChannels()
	{
		Set<BroadcastChannel> broadcastChannels = new TreeSet<>();

		for(AliasID id: getId())
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

		for(AliasID id: getId())
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
