/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import alias.esn.Esn;
import alias.fleetsync.FleetsyncID;
import alias.fleetsync.StatusID;
import alias.mdc.MDC1200ID;
import alias.mobileID.Min;
import alias.mpt1327.MPT1327ID;
import alias.siteID.SiteID;
import alias.talkgroup.TalkgroupID;
import alias.uniqueID.UniqueID;

@XmlSeeAlso( { Alias.class,
			   AliasID.class,
			   FleetsyncID.class,
			   Esn.class,
			   Group.class,
			   MDC1200ID.class,
			   Min.class,
			   MPT1327ID.class,
			   UniqueID.class,
			   SiteID.class,
			   StatusID.class,
			   TalkgroupID.class } )
@XmlRootElement( name = "alias_list" )
public class AliasList implements Comparable<AliasList>
{
	private String mName;
	private ArrayList<Group> mGroups = new ArrayList<Group>();
	
	private HashMap<String,Alias> mESN = new HashMap<String,Alias>();
	private HashMap<String,Alias> mFleetsync = new HashMap<String,Alias>();
	private HashMap<String,Alias> mMDC1200 = new HashMap<String,Alias>();
	private HashMap<String,Alias> mMobileID = new HashMap<String,Alias>();
	private HashMap<String,Alias> mMPT1327 = new HashMap<String,Alias>();
	private HashMap<Integer, Alias> mSiteID = new HashMap<Integer,Alias>();
	private HashMap<Integer, Alias> mStatus = new HashMap<Integer,Alias>();
	private HashMap<String,Alias> mTalkgroup = new HashMap<String,Alias>();
	private HashMap<Integer,Alias> mUniqueID = new HashMap<Integer,Alias>();
	
	private boolean mESNWildcard = false;
	private boolean mFleetsyncWildcard = false;
	private boolean mMDC1200Wildcard = false;
	private boolean mMPT1327Wildcard = false;
	private boolean mTalkgroupWildcard = false;
	
	public AliasList()
	{
		this( null );
	}
	
	public AliasList( String name )
	{
		mName = name;
		update();
	}
	
	/**
	 * Load/Reload all lookup hashmaps
	 */
	public void update()
	{
		//Clear hashmaps
		mESN.clear();
		mFleetsync.clear();
		mMDC1200.clear();
		mMobileID.clear();
		mMPT1327.clear();
		mSiteID.clear();
		mStatus.clear();
		mTalkgroup.clear();
		mUniqueID.clear();
		
		for( Group group: mGroups )
		{
			for( Alias alias: group.getAlias() )
			{
				for( AliasID id: alias.getId() )
				{
					switch( id.getType() )
					{
						case ESN:
							Esn esn = (Esn)id;
							if( esn.getEsn().contains( "*" ) )
							{
								mESNWildcard = true;

								//Replace (*) wildcard with regex wildcard (.)
								mESN.put( esn.getEsn().replace( "*", "." ), alias );
							}
							else
							{
								mESN.put( esn.getEsn(), alias );
							}
							break;
						case Fleetsync:
							FleetsyncID fs = (FleetsyncID)id;
							
							if( fs.getIdent().contains( "*" ) )
							{
								mFleetsyncWildcard = true;
								
								//Replace (*) wildcard with regex wildcard (.)
								mFleetsync.put( fs.getIdent().replace( "*", "." ), alias );
							}
							else
							{
								mFleetsync.put( fs.getIdent(), alias );
							}
							break;
						case MDC1200:
							MDC1200ID mdc = (MDC1200ID)id;
							
							if( mdc.getIdent().contains( "*" ) )
							{
								mMDC1200Wildcard = true;
								
								//Replace (*) wildcard with regex wildcard (.)
								mMDC1200.put( mdc.getIdent().replace( "*", "." ), alias );
							}
							else
							{
								mMDC1200.put( mdc.getIdent(), alias );
							}
							break;
						case MPT1327:
							MPT1327ID mpt = (MPT1327ID)id;
							
							String ident = mpt.getIdent();
							
							if( ident != null )
							{
								if( ident.contains( "*" ) )
								{
									mMPT1327Wildcard = true;
									
									//Replace (*) wildcard with regex wildcard (.)
									mMPT1327.put( ident.replace( "*", "." ), alias );
								}
								else
								{
									mMPT1327.put( ident, alias );
								}
							}
							break;
						case MIN:
							Min min = (Min)id;
							mMobileID.put( min.getMin(), alias );
							break;
						case LTRNetUID:
							UniqueID uid = (UniqueID)id;
							mUniqueID.put( uid.getUid(), alias );
							break;
						case Site:
							mSiteID.put( ((SiteID)id).getSite(), alias );
							break;
						case Status:
							mStatus.put( ((StatusID)id).getStatus(), alias );
							break;
						case Talkgroup:
							TalkgroupID tgid = (TalkgroupID)id;
							
							if( tgid.getTalkgroup().contains( "*" ) )
							{
								mTalkgroupWildcard = true;
								
								//Replace (*) wildcard with regex wildcard (.)
								mTalkgroup.put( tgid.getTalkgroup()
										.replace( "*", "." ), alias );
							}
							else
							{
								mTalkgroup.put( tgid.getTalkgroup(), alias );
							}
							break;
					}
				}
			}
		}
	}
	
	public Alias getSiteID( int siteID )
	{
		return mSiteID.get( siteID );
	}
	
	public Alias getStatus( int status )
	{
		return mStatus.get( status );
	}
	
	public Alias getUniqueID( int uniqueID )
	{
		return mUniqueID.get( uniqueID );
	}
	
	public Alias getESNAlias( String esn )
	{
		if( esn != null )
		{
			Alias retVal = null;
			
			if( mESNWildcard )
			{
				for( String key: mESN.keySet() )
				{
					if( esn.matches( key ) )
					{
						return mESN.get( key );
					}
				}
			}
			else
			{
				return mESN.get( esn );
			}
		}
		
		return null;
	}
	

	public Alias getFleetsyncAlias( String ident )
	{
		if( ident != null )
		{
			Alias retVal = null;
			
			if( mFleetsyncWildcard )
			{
				for( String key: mFleetsync.keySet() )
				{
					if( ident.matches( key ) )
					{
						return mFleetsync.get( key );
					}
				}
			}
			else
			{
				return mFleetsync.get( ident );
			}
		}
		
		return null;
	}
	
	public Alias getMDC1200Alias( String ident )
	{
		if( ident != null )
		{
			Alias retVal = null;
			
			if( mMDC1200Wildcard )
			{
				for( String key: mMDC1200.keySet() )
				{
					if( ident.matches( key ) )
					{
						return mMDC1200.get( key );
					}
				}
			}
			else
			{
				return mMDC1200.get( ident );
			}
		}
		
		return null;
	}
	
	public Alias getMPT1327Alias( String ident )
	{
		if( ident != null )
		{
			Alias retVal = null;
			
			if( mMPT1327Wildcard )
			{
				for( String key: mMPT1327.keySet() )
				{
					if( ident.matches( key ) )
					{
						return mMPT1327.get( key );
					}
				}
			}
			else
			{
				return mMPT1327.get( ident );
			}
		}
		
		return null;
	}
	
	public Group getGroup( Alias alias )
	{
		if( alias != null )
		{
			for( Group group: mGroups )
			{
				if( group.contians( alias ) )
				{
					return group;
				}
			}
		}
		
		return null;
	}
	
	public Alias getMobileIDNumberAlias( String ident )
	{
		return mMobileID.get( ident );
	}
	
	public Alias getTalkgroupAlias( String tgid )
	{
		if( tgid != null )
		{
			Alias retVal = null;
			
			if( mTalkgroupWildcard )
			{
				for( String key: mTalkgroup.keySet() )
				{
					if( tgid.matches( key ) )
					{
						return mTalkgroup.get( key );
					}
				}
			}
			else
			{
				return mTalkgroup.get( tgid );
			}
		}
		
		return null;
	}
	
	public String toString()
	{
		return mName;
	}

	@XmlAttribute
	public String getName()
	{
		return mName;
	}
	
	public void setName( String name )
	{
		mName = name;
	}
	
	public ArrayList<Group> getGroup()
	{
		return mGroups;
	}
	
	public void setGroup( ArrayList<Group> groups )
	{
		mGroups = groups;
	}
	
	public void addGroup( Group group )
	{
		mGroups.add( group );
	}

	public void removeGroup( Group group )
	{
		mGroups.remove( group );
	}

	@Override
    public int compareTo( AliasList otherAliasList )
    {
	    return getName().compareTo( otherAliasList.getName() );
    }
}
