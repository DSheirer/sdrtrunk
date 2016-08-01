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
import module.decode.lj1200.LJ1200Message.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AliasList implements Listener<AliasEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger( AliasList.class );
    public static final String WILDCARD = "*";

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

	private List<String> mESNWildcards = new ArrayList<>();
	private List<String> mMobileIDWildcards = new ArrayList<>();
	private List<String> mFleetsyncWildcards = new ArrayList<>();
	private List<String> mMDC1200Wildcards = new ArrayList<>();
	private List<String> mMPT1327Wildcards = new ArrayList<>();
	private List<String> mSiteWildcards = new ArrayList<>();
	private List<String> mTalkgroupWildcards = new ArrayList<>();

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
        if(id.isValid())
        {
            switch( id.getType() )
            {
                case ESN:
                    Esn esn = (Esn)id;
                    if( esn.getEsn().contains(WILDCARD) )
                    {
                        String regexESN = convertToRegex(esn.getEsn());

                        mESNWildcards.add(regexESN);
                        mESN.put( regexESN, alias );
                    }
                    else
                    {
                        mESN.put( esn.getEsn(), alias );
                    }
                    break;
                case Fleetsync:
                    FleetsyncID fs = (FleetsyncID)id;

                    if( fs.getIdent().contains(WILDCARD) )
                    {
                        String regexFleetsync = convertToRegex( fs.getIdent() );

                        mFleetsyncWildcards.add(regexFleetsync);
                        mFleetsync.put( regexFleetsync, alias );
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

                    if( mdc.getIdent().contains(WILDCARD) )
                    {
                        String regexMDC = convertToRegex(mdc.getIdent());

                        mMDC1200Wildcards.add(regexMDC);
                        mMDC1200.put(regexMDC, alias);
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
                        if( ident.contains(WILDCARD) )
                        {
                            String regexMPT1327 = convertToRegex(ident);

                            mMPT1327Wildcards.add(regexMPT1327);
                            mMPT1327.put(regexMPT1327, alias);
                        }
                        else
                        {
                            mMPT1327.put( ident, alias );
                        }
                    }
                    break;
                case MIN:
                    Min min = (Min)id;

                    if( min.getMin().contains(WILDCARD) )
                    {
                        String regexMIN = convertToRegex(min.getMin());

                        mMobileIDWildcards.add(regexMIN);
                        mMobileID.put( regexMIN, alias );
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

                    if( siteID.getSite().contains(WILDCARD) )
                    {
                        String regexSiteID = convertToRegex(siteID.getSite());

                        mSiteWildcards.add(regexSiteID);
                        mSiteID.put( regexSiteID, alias );
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

                    if( tgid.getTalkgroup().contains(WILDCARD) )
                    {
                        String regexTalkgroup = convertToRegex(tgid.getTalkgroup());

                        mTalkgroupWildcards.add(regexTalkgroup);
                        mTalkgroup.put( regexTalkgroup, alias );
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
		}
	}

	/**
	 * Removes the alias and alias identifier from internal mappings.
	 */
	private void removeAliasID( AliasID id, Alias alias )
	{
        if(id.isValid())
        {
            switch( id.getType() )
            {
                case ESN:
                    Esn esn = (Esn)id;

                    if( esn.getEsn().contains(WILDCARD))
                    {
                        String regexESN = convertToRegex(esn.getEsn());
                        mESN.remove(regexESN);
                        mESNWildcards.remove(regexESN);
                    }
                    else if( mESN.containsKey( esn.getEsn() ) )
                    {
                        mESN.remove(esn.getEsn());
                    }
                    break;
                case Fleetsync:
                    FleetsyncID fs = (FleetsyncID)id;

                    if(fs.getIdent().contains(WILDCARD))
                    {
                        String regexFleetsync = convertToRegex(fs.getIdent());
                        mFleetsync.remove(regexFleetsync);
                        mFleetsyncWildcards.remove(regexFleetsync);
                    }
                    else
                    {
                        mFleetsync.remove( fs.getIdent() );
                    }
                    break;
                case LoJack:
                    mLoJack.remove((LoJackFunctionAndID)id);
                    break;
                case MDC1200:
                    MDC1200ID mdc = (MDC1200ID)id;

                    if(mdc.getIdent().contains(WILDCARD))
                    {
                        String regexMDC1200 = convertToRegex(mdc.getIdent());
                        mMDC1200.remove(regexMDC1200);
                        mMDC1200Wildcards.remove(regexMDC1200);
                    }
                    else
                    {
                        mMDC1200.remove( mdc.getIdent() );
                    }
                    break;
                case MPT1327:
                    MPT1327ID mpt = (MPT1327ID)id;

                    if(mpt.getIdent().contains(WILDCARD))
                    {
                        String regexMPT1327 = convertToRegex(mpt.getIdent());
                        mMPT1327.remove(regexMPT1327);
                        mMPT1327Wildcards.remove(regexMPT1327);
                    }
                    else
                    {
                        mMPT1327.remove( mpt.getIdent() );
                    }
                    break;
                case MIN:
                    Min min = (Min)id;

                    if(min.getMin().contains(WILDCARD))
                    {
                        String regexMIN = convertToRegex(min.getMin());
                        mMobileID.remove(regexMIN);
                        mMobileIDWildcards.remove(regexMIN);
                    }
                    else
                    {
                        mMobileID.remove( min.getMin() );
                    }
                    break;
                case LTRNetUID:
                    mUniqueID.remove( ((UniqueID)id).getUid() );
                    break;
                case Site:
                    mSiteID.remove( ((SiteID)id).getSite() );
                    break;
                case Status:
                    mStatus.remove( ((StatusID)id).getStatus() );
                    break;
                case Talkgroup:
                    TalkgroupID tg = (TalkgroupID)id;

                    if(tg.getTalkgroup().contains(WILDCARD))
                    {
                        String regexTalkgroup = convertToRegex(tg.getTalkgroup());
                        mTalkgroup.remove(regexTalkgroup);
                        mTalkgroupWildcards.remove(regexTalkgroup);
                    }
                    else
                    {
                        mTalkgroup.remove( tg.getTalkgroup() );
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
	}

	/**
	 * Converts user wildcard character (*) to regex single character wildcard (.)
	 * but ignores a regex multi-character wildcard (.*)
	 */
	private String convertToRegex(String value )
	{
		if( value.contains(WILDCARD) && !value.contains( ".*" ) )
		{
			return value.replace(WILDCARD, "." );
		}

		return value;
	}

    /**
     * Returns the first matching regex wildcard from the list of wildcards that matches the
     * identifier.
     *
     * @param id to match
     * @param wildcards to match against
     * @return matching wildcard ID or null
     */
    private String getWildcardMatch( String id, List<String> wildcards )
    {
        if(id != null)
        {
            for(String wildcard: wildcards)
            {
                if(id.matches(wildcard))
                {
                    return wildcard;
                }
            }
        }

        return null;
    }

	/**
	 * Lookup alias by site ID
	 */
	public Alias getSiteID( String siteID )
	{
        Alias alias = null;

        if(siteID != null)
        {
            alias = mSiteID.get(siteID);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(siteID, mSiteWildcards);

                if(wildcard != null)
                {
                    alias = mSiteID.get(wildcard);
                }
            }
        }

		return alias;
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
        Alias alias = null;

        if(esn != null)
        {
            alias = mESN.get(esn);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(esn, mESNWildcards);

                if(wildcard != null)
                {
                    alias = mESN.get(wildcard);
                }
            }
        }

        return alias;
	}
	
	/**
	 * Lookup alias by fleetsync ID
	 */
	public Alias getFleetsyncAlias( String ident )
	{
        Alias alias = null;

		if( ident != null )
		{
            alias = mFleetsync.get(ident);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(ident, mFleetsyncWildcards);

                if(wildcard != null)
                {
                    alias = mFleetsync.get(wildcard);
                }
            }
		}
		
		return alias;
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
        Alias alias = null;

		if( ident != null )
		{
            alias = mMDC1200.get(ident);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(ident, mMDC1200Wildcards);

                if(wildcard != null)
                {
                    alias = mMDC1200.get(wildcard);
                }
            }
		}
		
		return alias;
	}
	
	/**
	 * Lookup alias by MPT1327 Ident
	 */
	public Alias getMPT1327Alias( String ident )
	{
        Alias alias = null;

		if( ident != null )
		{
            alias = mMPT1327.get(ident);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(ident, mMPT1327Wildcards);

                if(wildcard != null)
                {
                    alias = mMPT1327.get(wildcard);
                }
            }
		}
		
		return alias;
	}
	
	/**
	 * Lookup alias by Mobile ID Number (MIN)
	 */
	public Alias getMobileIDNumberAlias( String ident )
	{
        Alias alias = null;

		if( ident != null )
		{
            alias = mMobileID.get(ident);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(ident, mMobileIDWildcards);

                if(wildcard != null)
                {
                    alias = mMobileID.get(wildcard);
                }
            }
		}
		
		return alias;
	}
	
	/**
	 * Lookup alias by talkgroup
	 */
	public Alias getTalkgroupAlias( String tgid )
	{
        Alias alias = null;

		if( tgid != null )
		{
            alias = mTalkgroup.get(tgid);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(tgid, mTalkgroupWildcards);

                if(wildcard != null)
                {
                    alias = mTalkgroup.get(wildcard);
                }
            }
		}
		
		return alias;
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
				if( alias.getList() != null && getName().equalsIgnoreCase( alias.getList() ) )
				{
					addAlias( alias );
				}
				break;
			case CHANGE:
				if( alias.getList() != null && getName().equalsIgnoreCase( alias.getList() ) )
				{
                    addAlias( alias );
				}
				break;
			case DELETE:
                if( alias.getList() != null && getName().equalsIgnoreCase( alias.getList() ) )
				{
					removeAlias( alias );
				}
				break;
			default:
				break;
		}
	}
}
