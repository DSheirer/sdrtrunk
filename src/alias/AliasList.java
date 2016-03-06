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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;

import module.decode.lj1200.LJ1200Message.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.id.AliasID;
import alias.id.esn.Esn;
import alias.id.fleetsync.FleetsyncID;
import alias.id.lojack.LoJackFunctionAndID;
import alias.id.mdc.MDC1200ID;
import alias.id.mobileID.Min;
import alias.id.mpt1327.MPT1327ID;
import alias.id.siteID.SiteID;
import alias.id.status.StatusID;
import alias.id.talkgroup.TalkgroupID;
import alias.id.uniqueID.UniqueID;
import audio.metadata.MetadataType;

public class AliasList implements Listener<AliasEvent>
{
	private final static Logger mLog = LoggerFactory.getLogger( AliasList.class );

	private List<Alias> mAliases = new ArrayList<>();
	private Map<String,Alias> mESN = new HashMap<>();
	private Map<String,Alias> mFleetsync = new HashMap<>();
	private Map<LoJackFunctionAndID,Alias> mLoJack = new HashMap<>();
	private Map<String,Alias> mMDC1200 = new HashMap<>();
	private Map<String,Alias> mMobileID = new HashMap<>();
	private Map<String,Alias> mMPT1327 = new HashMap<>();
	private Map<String, Alias> mSiteID = new HashMap<>();
	private Map<Integer, Alias> mStatus = new HashMap<>();
	private Map<String,Alias> mTalkgroup = new HashMap<>();
	private Map<Integer,Alias> mUniqueID = new HashMap<>();
	
	private boolean mESNWildcard = false;
	private boolean mMobileIDWildcard = false;
	private boolean mFleetsyncWildcard = false;
	private boolean mMDC1200Wildcard = false;
	private boolean mMPT1327Wildcard = false;
	private boolean mSiteWildcard = false;
	private boolean mTalkgroupWildcard = false;
	
	private String mName;
	
	/**
	 * List of aliases where all aliases share the same list name.  Contains
	 * several methods for alias lookup from identifier values, like talkgroups.
	 * 
	 * Responds to alias change events to keep the internal alias list updated.
	 */
	public AliasList( String name )
	{
		mName = name;
	}

	/**
	 * Adds the alias to this list
	 */
	public void addAlias( Alias alias )
	{
		if( alias != null )
		{
			mAliases.add( alias );
			
			for( AliasID aliasID: alias.getId() )
			{
				addAliasID( aliasID, alias );
			}
		}
	}

	/**
	 * Adds the alias and alias identifier to the internal type mapping.
	 */
	private void addAliasID( AliasID id, Alias alias )
	{
		switch( id.getType() )
		{
			case ESN:
				Esn esn = (Esn)id;
				if( esn.getEsn().contains( "*" ) )
				{
					mESNWildcard = true;

					mESN.put( fixWildcard( esn.getEsn() ), alias );
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
					
					mFleetsync.put( fixWildcard( fs.getIdent() ), alias );
				}
				else
				{
					mFleetsync.put( fs.getIdent(), alias );
				}
				break;
			case LoJack:
				mLoJack.put( (LoJackFunctionAndID)id, alias );
				break;
			case MDC1200:
				MDC1200ID mdc = (MDC1200ID)id;
				
				if( mdc.getIdent().contains( "*" ) )
				{
					mMDC1200Wildcard = true;
					
					//Replace (*) wildcard with regex wildcard (.)
					mMDC1200.put( fixWildcard( mdc.getIdent() ), alias );
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
						mMPT1327.put( fixWildcard( ident ), alias );
					}
					else
					{
						mMPT1327.put( ident, alias );
					}
				}
				break;
			case MIN:
				Min min = (Min)id;
				
				if( min.getMin().contains( "*" ) )
				{
					mMobileIDWildcard = true;
					mMobileID.put( fixWildcard( min.getMin() ), alias );
				}
				else
				{
					mMobileID.put( min.getMin(), alias );
				}
				break;
			case LTRNetUID:
				UniqueID uid = (UniqueID)id;
				
				mUniqueID.put( uid.getUid(), alias );
				break;
			case Site:
				SiteID siteID = (SiteID)id;

				if( siteID.getSite().contains( "*" ) )
				{
					mSiteWildcard = true;
					mSiteID.put( fixWildcard( siteID.getSite() ), alias );
				}
				else
				{
					mSiteID.put( siteID.getSite(), alias );
				}
				
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
					mTalkgroup.put( fixWildcard( tgid.getTalkgroup() ), alias );
				}
				else
				{
					mTalkgroup.put( tgid.getTalkgroup(), alias );
				}
				break;
			case NonRecordable:
			case Priority:
				//We don't maintain lookups for these items
				break;
			default:
				mLog.warn( "Unrecognized Alias ID Type:" + id.getType().name() );
				break;
		}
	}

	/**
	 * Removes the alias from this list
	 */
	public void removeAlias( Alias alias )
	{
		if( alias != null )
		{
			for( AliasID aliasID: alias.getId() )
			{
				removeAliasID( aliasID, alias );
			}
			
			mAliases.remove( alias );
		}
	}

	/**
	 * Removes the alias and alias identifier from internal mappings.
	 */
	private void removeAliasID( AliasID id, Alias alias )
	{
		switch( id.getType() )
		{
			case ESN:
				Esn esn = (Esn)id;
				if( mESN.containsKey( esn.getEsn() ) )
				{
					Alias esnAlias = mESN.get( esn.getEsn() );
					
					if( esnAlias != null && esnAlias == alias )
					{
						mESN.remove( esn.getEsn() );
					}
				}
				break;
			case Fleetsync:
				FleetsyncID fs = (FleetsyncID)id;

				if( mFleetsync.containsKey( fs.getIdent() ) )
				{
					Alias fsAlias = mFleetsync.get( fs.getIdent() );
					
					if( fsAlias != null && fsAlias == alias )
					{
						mFleetsync.remove( fs.getIdent() );
					}
				}
				break;
			case LoJack:
				LoJackFunctionAndID lj = (LoJackFunctionAndID)id;
				
				if( mLoJack.containsKey( lj ) )
				{
					Alias ljAlias = mLoJack.get( lj );
					
					if( ljAlias != null && ljAlias == alias )
					{
						mLoJack.remove( lj );
					}
				}
				break;
			case MDC1200:
				MDC1200ID mdc = (MDC1200ID)id;

				if( mMDC1200.containsKey( mdc.getIdent() ) )
				{
					Alias mdcAlias = mMDC1200.get( mdc.getIdent() );
					
					if( mdcAlias != null && mdcAlias == alias )
					{
						mMDC1200.remove( mdc.getIdent() );
					}
				}
				break;
			case MPT1327:
				MPT1327ID mpt = (MPT1327ID)id;
				
				if( mMPT1327.containsKey( mpt.getIdent() ) )
				{
					Alias mptAlias = mMDC1200.get( mpt.getIdent() );
					
					if( mptAlias != null && mptAlias == alias )
					{
						mMPT1327.remove( mpt.getIdent() );
					}
				}
				break;
			case MIN:
				Min min = (Min)id;
				
				if( mMobileID.containsKey( min.getMin() ) )
				{
					Alias minAlias = mMobileID.get( min.getMin() );
					
					if( minAlias != null && minAlias == alias )
					{
						mMobileID.remove( min.getMin() );
					}
				}
				break;
			case LTRNetUID:
				UniqueID uid = (UniqueID)id;
				
				if( mUniqueID.containsKey( uid.getUid() ) )
				{
					Alias uidAlias = mUniqueID.get( uid.getUid() );
					
					if( uidAlias != null && uidAlias == alias )
					{
						mUniqueID.remove( uid.getUid() );
					}
				}
				break;
			case Site:
				SiteID sid = (SiteID)id;

				if( mSiteID.containsKey( sid.getSite() ) )
				{
					Alias sidAlias = mSiteID.get( sid.getSite() );
					
					if( sidAlias != null && sidAlias == alias )
					{
						mSiteID.remove( sid.getSite() );
					}
				}
				break;
			case Status:
				StatusID stat = (StatusID)id;
				
				if( mStatus.containsKey( stat.getStatus() ) )
				{
					Alias statAlias = mStatus.get( stat.getStatus() );
					
					if( statAlias != null && statAlias == alias )
					{
						mStatus.remove( stat.getStatus() );
					}
				}
				break;
			case Talkgroup:
				TalkgroupID tg = (TalkgroupID)id;
				
				if( mTalkgroup.containsKey( tg.getTalkgroup() ) )
				{
					Alias tgAlias = mTalkgroup.get( tg.getTalkgroup() );
					
					if( tgAlias != null && tgAlias == alias )
					{
						mTalkgroup.remove( tg.getTalkgroup() );
					}
				}
				break;
			case NonRecordable:
			case Priority:
				//We don't maintain lookups for these items
				break;
			default:
				mLog.warn( "Unrecognized Alias ID Type:" + id.getType().name() );
				break;
		}
	}

	/**
	 * Converts user wildcard character (*) to regex single character wildcard (.)
	 * but ignores a regex multi-character wildcard (.*)
	 */
	private String fixWildcard( String value )
	{
		if( value.contains( "*" ) && !value.contains( ".*" ) )
		{
			return value.replace( "*", "." );
		}

		return value;
	}

	/**
	 * Lookup alias by site ID
	 */
	public Alias getSiteID( String siteID )
	{
		if( siteID != null )
		{
			if( mSiteWildcard )
			{
				for( String regex: mSiteID.keySet() )
				{
					if( siteID.matches( regex ) )
					{
						return mSiteID.get( regex );
					}
				}
			}
			else
			{
				return mSiteID.get( siteID );
			}
		}
		
		return null;
	}

	/**
	 * Lookup alias by status ID
	 */
	public Alias getStatus( int status )
	{
		return mStatus.get( status );
	}
	
	/**
	 * Lookup alias by Unique ID (UID)
	 */
	public Alias getUniqueID( int uniqueID )
	{
		return mUniqueID.get( uniqueID );
	}
	
	/**
	 * Lookup alias by ESN
	 */
	public Alias getESNAlias( String esn )
	{
		if( esn != null )
		{
			if( mESNWildcard )
			{
				for( String regex: mESN.keySet() )
				{
					if( esn.matches( regex ) )
					{
						return mESN.get( regex );
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
	
	/**
	 * Lookup alias by fleetsync ID
	 */
	public Alias getFleetsyncAlias( String ident )
	{
		if( ident != null )
		{
			if( mFleetsyncWildcard )
			{
				for( String regex: mFleetsync.keySet() )
				{
					if( ident.matches( regex ) )
					{
						return mFleetsync.get( regex );
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
	
	/**
	 * Lookup alias by lojack function and ID
	 */
	public Alias getLoJackAlias( Function function, String id )
	{
		if( id != null )
		{
			for( LoJackFunctionAndID lojack: mLoJack.keySet() )
			{
				if( lojack.matches( function, id ) )
				{
					return mLoJack.get( lojack );
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Lookup alias by MDC1200 ID
	 */
	public Alias getMDC1200Alias( String ident )
	{
		if( ident != null )
		{
			if( mMDC1200Wildcard )
			{
				for( String regex: mMDC1200.keySet() )
				{
					if( ident.matches( regex ) )
					{
						return mMDC1200.get( regex );
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
	
	/**
	 * Lookup alias by MPT1327 Ident
	 */
	public Alias getMPT1327Alias( String ident )
	{
		if( ident != null )
		{
			if( mMPT1327Wildcard )
			{
				for( String regex: mMPT1327.keySet() )
				{
					if( ident.matches( regex ) )
					{
						return mMPT1327.get( regex );
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
	
	/**
	 * Lookup alias by Mobile ID Number (MIN)
	 */
	public Alias getMobileIDNumberAlias( String ident )
	{
		if( ident != null )
		{
			if( mMobileIDWildcard )
			{
				for( String regex: mMobileID.keySet() )
				{
					if( ident.matches( regex ) )
					{
						return mMobileID.get( regex );
					}
				}
			}
			else
			{
				return mMobileID.get( ident );
			}
		}
		
		return null;
	}
	
	/**
	 * Lookup alias by talkgroup
	 */
	public Alias getTalkgroupAlias( String tgid )
	{
		if( tgid != null )
		{
			if( mTalkgroupWildcard )
			{
				for( String regex: mTalkgroup.keySet() )
				{
					if( tgid.matches( regex ) )
					{
						return mTalkgroup.get( regex );
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

	/**
	 * Alias list name
	 */
	public String toString()
	{
		return mName;
	}

	/**
	 * Alias list name
	 */
	@XmlAttribute
	public String getName()
	{
		return mName;
	}

	/**
	 * Lookup an alias by metadata type and string value
	 */
	public Alias getAlias( String value, MetadataType type )
	{
		switch( type )
		{
			case ESN:
				return getESNAlias( value );
			case FLEETSYNC:
				return getFleetsyncAlias( value );
			case MDC1200:
				return getMDC1200Alias( value );
			case MOBILE_ID:
				return getMobileIDNumberAlias( value );
			case MPT1327:
				return getMPT1327Alias( value );
			case SITE_ID:
				return getSiteID( value );
			case STATUS:
				if( value != null )
				{
					try
					{
						return getStatus( Integer.valueOf( value ) );
					}
					catch( Exception e )
					{
						//do nothing, we couldn't parse the int value
					}
				}
				break;
			case FROM:
			case TO:
				return getTalkgroupAlias( value );
			case UNIQUE_ID:
				if( value != null )
				{
					try
					{
						return getUniqueID( Integer.valueOf( value ) );
					}
					catch( Exception e )
					{
						//do nothing, we couldn't parse the int value
					}
				}
				break;
			default:
				break;
		}
		
		return null;
	}

	/**
	 * Receive alias change event notifications and modify this list accordingly
	 */
	@Override
	public void receive( AliasEvent event )
	{
		Alias alias = event.getAlias();
		
		switch( event.getEvent() )
		{
			case ADD:
				if( alias.getList() != null && 
					getName().equalsIgnoreCase( alias.getList() ) )
				{
					addAlias( alias );
				}
				break;
			case CHANGE:
				if( alias.getList() != null &&
					getName().equalsIgnoreCase( alias.getList() ) )
				{
					if( !mAliases.contains( alias ) )
					{
						//Alias was moved to this list - add it 
						addAlias( alias );
					}
				}
				else if( mAliases.contains( alias ) )
				{
					//An alias in this list was moved to another list
					removeAlias( alias );
				}
				break;
			case DELETE:
				if( mAliases.contains( alias ) )
				{
					removeAlias( alias );
				}
				break;
			default:
				break;
		}
	}
}
