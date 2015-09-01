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
package module.decode.passport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.Alias;
import alias.AliasList;

public class PassportDecoderState extends DecoderState
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( PassportDecoderState.class );

	private static final int sINT_NULL_VALUE = -1;
	private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");

	private HashSet<String> mTalkgroupsFirstHeard = new HashSet<String>();
	private TreeSet<String> mTalkgroups = new TreeSet<String>();
	private TreeSet<String> mMobileIDs = new TreeSet<String>();
	private HashMap<Integer,String> mSiteLCNs = new HashMap<Integer,String>(); 
	private HashMap<Integer,String> mNeighborLCNs = new HashMap<Integer,String>(); 
	
	private String mTalkgroup;
	private Alias mTalkgroupAlias;
	private String mMobileID;
	private Alias mMobileIDAlias;
	private int mChannelNumber;
	private int mSiteNumber;
	private PassportBand mSiteBand;
	private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();
	
	public PassportDecoderState( AliasList aliasList )
	{
		super( aliasList );
	}
	
    @Override
	@SuppressWarnings( "incomplete-switch" )
    public void receive( Message message )
    {
//		if( message instanceof PassportMessage )
//		{
//			PassportMessage passport = ((PassportMessage)message);
//			
//			if( passport.isValid() )
//			{
//				switch( passport.getMessageType() )
//				{
//					case CA_STRT:
//						mSiteLCNs.put( passport.getLCN(), 
//									   passport.getLCNFrequencyFormatted() );
//					case CA_ENDD:
//						String talkgroup = 
//							String.valueOf( passport.getTalkgroupID() );
//						
//						if( mTalkgroupsFirstHeard.contains( talkgroup ) )
//						{
//							mTalkgroups.add( talkgroup );
//						}
//						else
//						{
//							mTalkgroupsFirstHeard.add( talkgroup );
//						}
//						break;
//					case ID_RDIO:
//						String min = passport.getMobileID();
//						
//						if( min != null )
//						{
//							mMobileIDs.add( min );
//						}
//						break;
//					case SY_IDLE:
//						if( passport.getFree() != 0 )
//						{
//							mNeighborLCNs.put( passport.getFree(), 
//									   passport.getFreeFrequencyFormatted() );
//						}
//						break;
//					default:
//						break;
//				}
//			}
//		}
//
//		if( message instanceof PassportMessage )
//		{
//			mActivitySummary.receive( message );
//			
//			PassportMessage passport = (PassportMessage)message;
//			
//			if( passport.isValid() )
//			{
//				switch( passport.getMessageType() )
//				{
//	                case CA_STRT:
//	                    if( mChannelNumber == 0 )
//	                    {
//	                    	mChannelNumber = passport.getLCN();
//	                    }
//	                	
//	                    if( passport.getLCN() == mChannelNumber )
//	                    {
//		                	setTalkgroup( String.valueOf( passport.getTalkgroupID() ) );
//		                	setTalkgroupAlias( passport.getTalkgroupIDAlias() );
//		                    
//		                    if( mTalkgroupAlias != null )
//		                    {
//		                    	try
//		                    	{
//			                    	if( mTalkgroupAlias.hasAudioType() )
//			                    	{
//		                				mAudioTypeListener.setAudioType( 
//		                							mTalkgroupAlias.getAudioType() );
//			                    	}
//		                    	}
//		                    	catch( Exception e )
//		                    	{
//		                    		mLog.error( "Error setting audio for "
//		                    				+ "passport call", e );
//		                    	}
//		                    }
//		                    
//		                    PassportCallEvent current = 
//		                    		getCurrentPassportCallEvent();
//
//		                    /* If we're already in a call event, add the message
//		                     * to the current call event ... if false, then we
//		                     * have a different call ... cleanup the old one. */
//		                    if( current != null && !current.addMessage( passport ) )
//		                    {
//		                    	if( current.isValid() )
//		                    	{
//		                    		final PassportCallEvent endEvent = 
//		                    				getCurrentPassportCallEvent();
//		                    		
//		                    		mCallEventModel.setEnd( endEvent );
//		                    	}
//		                    	else
//		                    	{
//		                    		mCallEventModel.remove( current );
//		                    	}
//		                    	
//		                    	current = null;
//		                    	setCurrentCallEvent( null );
//		                    }
//
//		                    if( current == null )
//		                    {
//		                    	mCurrentCallEvent = new PassportCallEvent
//		                    			.Builder( CallEventType.CALL )
//		                    			.aliasList( mAliasList )
//		                    			.channel( String.valueOf( mChannelNumber ) )
//		                    			.frequency( passport.getLCNFrequency() )
//		                    			.to( String.valueOf( passport.getTalkgroupID() ) )
//		                    			.build();
//		                    	
//			                    mCallEventModel.add( mCurrentCallEvent );
//		                    }
//		                    
//		                    State state = getState();
//		                    
//		                    if( state == State.IDLE || state == State.CALL )
//		                    {
//			                    setState( State.CALL );
//		                    }
//		                    else if( state == State.DATA )
//		                    {
//		                    	setState( State.DATA );
//		                    }
//	                    }
//	                    else
//	                    {
//	                    	//Call Detection
//	                    	int lcn = passport.getLCN();
//	                    	String tg = String.valueOf( passport.getTalkgroupID() );
//	                    	
//	                    	if( !mActiveCalls.containsKey( lcn ) ||
//	                    		!mActiveCalls.get( lcn ).contentEquals( tg ) )
//	                    	{
//	                    		mActiveCalls.put( lcn, tg );
//
//	                    		PassportCallEvent event = new PassportCallEvent
//		                    			.Builder( CallEventType.CALL_DETECT )
//		                    			.aliasList( mAliasList )
//		                    			.channel( String.valueOf( lcn ) )
//		                    			.details( "Site: " + passport.getSite() )
//		                    			.frequency( passport.getLCNFrequency() )
//		                    			.to( tg )
//		                    			.build();
//	                    		
//	                    		mCallEventModel.add( event );
//	                    	}
//	                    }
//	                    break;
//	                case DA_STRT:
////TODO: add channel number checking and log a call event if on another site
//	                	setTalkgroup( String.valueOf( passport.getTalkgroupID() ) );
//	                	setTalkgroupAlias( passport.getTalkgroupIDAlias() );
//	                    setState( State.DATA );
//	                    break;
//	                case CA_ENDD:
//	                	setTalkgroup( String.valueOf( passport.getTalkgroupID() ) );
//	                	setTalkgroupAlias( passport.getTalkgroupIDAlias() );
//	                    fade( CallEventType.CALL_END );
//	                    break;
//	                case ID_RDIO:
//	                	setMobileID( passport.getMobileID() );
//	                	setMobileIDAlias( passport.getMobileIDAlias() );
//	                	
//	                	final String mid = passport.getMobileID();
//	                	
//	                	final PassportCallEvent event = 
//	                			getCurrentPassportCallEvent();
//	                	
//	                	if( event != null && mid != null )
//	                	{
//	                		mCallEventModel.setFromID( event, mid );
//	                	}
//
//	                    setState( State.CALL );
//	                	break;
//	                case RA_REGI:
//	                	mCallEventModel.add( new PassportCallEvent
//            			.Builder( CallEventType.REGISTER )
//            			.aliasList( mAliasList )
//            			.channel( String.valueOf( passport.getLCN() ) )
//            			.frequency( passport.getLCNFrequency() )
//            			.to( passport.getToID() )
//            			.build() );
////TODO: add logic to recognize the register event and suppress the subsequent
////call messages, but capture the group assignments. 
//	                	setState( State.DATA );
//	                	break;
//	                case SY_IDLE:
//	                	PassportBand band = passport.getSiteBand();
//	                	
//	                	if( mSiteBand == null || mSiteBand != band )
//	                	{
//	                		mSiteBand = band;
//	                		
//	                		mSiteNumber = passport.getSite();
//	                		
//	                		long frequency = mProcessingChain.getChannel()
//	                				.getTunerChannel().getFrequency();
//	                		
//	                		mChannelNumber = mSiteBand.getChannel( frequency );
//	                		
//	                		broadcast( ChangedAttribute.CHANNEL_NUMBER );
//	                	}
//                	default:
//                		break;
//				}
//			}
//		}
    }
    
	public static String formatTalkgroup( String talkgroup )
	{
		StringBuilder sb = new StringBuilder();
		
		if( talkgroup.length() == 6 )
		{
			sb.append( talkgroup.substring( 0, 1 ) );
			sb.append( "-" );
			sb.append( talkgroup.substring( 1, 3 ) );
			sb.append( "-" );
			sb.append( talkgroup.substring( 3, 6 ) );

			return sb.toString();
		}
		else
		{
			return talkgroup;
		}
	}

	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "Activity Summary\n" );
		sb.append( "Decoder:\tPassport\n\n" );
		sb.append( "Site Channels\n" );

		if( mSiteLCNs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			ArrayList<Integer> channels = new ArrayList( mSiteLCNs.keySet() );
			Collections.sort( channels );
			
			for( Integer channel: channels )
			{
				sb.append( "  " + channel );
				sb.append( "\t" + mSiteLCNs.get( channel ) );
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nNeighbor Channels\n" );

		if( mNeighborLCNs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			ArrayList<Integer> channels = new ArrayList( mNeighborLCNs.keySet() );
			Collections.sort( channels );
			
			for( Integer channel: channels )
			{
				sb.append( "  " + channel );
				sb.append( "\t" + mNeighborLCNs.get( channel ) );
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nTalkgroups\n" );
		
		if( mTalkgroups.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mTalkgroups.iterator();
			
			while( it.hasNext() )
			{
				String tgid = it.next();
				
				sb.append( "  " );
				sb.append( tgid );
				sb.append( " " );
				
				if( hasAliasList() )
				{
					Alias alias = getAliasList().getTalkgroupAlias( tgid );
					
					if( alias != null )
					{
						sb.append( alias.getName() );
					}
				}
				
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nMobile ID Numbers\n" );
		
		if( mMobileIDs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mMobileIDs.iterator();
			
			while( it.hasNext() )
			{
				String min = it.next();
				
				sb.append( "  " );
				sb.append( min );
				sb.append( " " );
				
				if( hasAliasList() )
				{
					Alias alias = getAliasList().getMobileIDNumberAlias( min );
					
					if( alias != null )
					{
						sb.append( alias.getName() );
					}
				}
				
				sb.append( "\n" );
			}
		}
		
	    return sb.toString();
    }

//	/**
//	 * Intercept the fade event so that we can generate a call end event
//	 */
//	@Override
//	public void fade( final CallEventType type )
//	{
//		/*
//		 * We can receive multiple call tear-down messages -- only post a call
//		 * end event for the one that can change the state to fade
//		 */
//		if( getState().canChangeTo( State.FADE ) )
//		{
//			final PassportCallEvent current = getCurrentPassportCallEvent();
//			
//			if( current != null )
//			{
//				mCallEventModel.setEnd( current );
//			}
//		}
//		
//		setCurrentCallEvent( null );
//		
//		mActiveCalls.clear();
//		
//		super.fade( type );
//	}
	
	public void reset()
	{
		//TODO: finish this
		resetState();
	}
	
	private void resetState()
	{
		setTalkgroup( null );
		setTalkgroupAlias( null );

		setMobileID( null );
		setMobileIDAlias( null );
	}
	
	public int getChannelNumber()
	{
		return mChannelNumber;
	}
	
	public int getSiteNumber()
	{
		return mSiteNumber;
	}
	
	public String getTalkgroup()
	{
		return mTalkgroup;
	}
	
	public void setTalkgroup( String talkgroup )
	{
		mTalkgroup = talkgroup;
		broadcast( ChangedAttribute.TO_TALKGROUP );
	}
	
	public Alias getTalkgroupAlias()
	{
		return mTalkgroupAlias;
	}
	
	public void setTalkgroupAlias( Alias alias )
	{
		mTalkgroupAlias = alias;
		broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );
	}

	public String getMobileID()
	{
		return mMobileID;
	}
	
	public void setMobileID( String id )
	{
		mMobileID = id;
		broadcast( ChangedAttribute.FROM_TALKGROUP );
	}
	
	public Alias getMobileIDAlias()
	{
		return mMobileIDAlias;
	}
	
	public void setMobileIDAlias( Alias alias )
	{
		mMobileIDAlias = alias;
		broadcast( ChangedAttribute.FROM_TALKGROUP_ALIAS );
	}

	@Override
	public void init()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receiveDecoderStateEvent( DecoderStateEvent event )
	{
		// TODO Auto-generated method stub
		
	}
}
