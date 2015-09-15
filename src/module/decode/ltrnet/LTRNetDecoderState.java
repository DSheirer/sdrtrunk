/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode.ltrnet;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import message.Message;
import module.decode.event.CallEvent.CallEventType;
import module.decode.mpt1327.MPT1327DecoderState;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;
import module.decode.state.DecoderStateEvent.Event;
import module.decode.state.State;
import alias.Alias;
import alias.AliasList;

public class LTRNetDecoderState extends DecoderState
{
	private final static Logger mLog = LoggerFactory.getLogger( LTRNetDecoderState.class );

	private DecimalFormat mDecimalFormatter = new DecimalFormat( "0.00000" );

	private TreeSet<String> mTalkgroups = new TreeSet<String>();
	private TreeSet<String> mTalkgroupsFirstHeard = new TreeSet<String>();
	private TreeSet<String> mESNs = new TreeSet<String>();
	private TreeSet<Integer> mUniqueIDs = new TreeSet<Integer>();
	private TreeSet<String> mNeighborIDs = new TreeSet<String>();
	private TreeSet<String> mSiteIDs = new TreeSet<String>();
	private HashMap<Integer,Long> mReceiveFrequencies = 
			new HashMap<Integer,Long>();
	private HashMap<Integer,Long> mTransmitFrequencies = 
			new HashMap<Integer,Long>();
    private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();

	private String mTalkgroup;
	private Alias mTalkgroupAlias;
	private String mDescription;
	private int mChannelNumber;
	private long mFrequency = 0;

	public LTRNetDecoderState( AliasList aliasList )
	{
		super( aliasList );
	}

	@Override
    public void receive( Message message )
    {
		if( message.isValid() )
		{
			if( message instanceof LTRNetOSWMessage )
			{
				LTRNetOSWMessage ltr = (LTRNetOSWMessage)message;
				
				switch( ltr.getMessageType() )
				{
					case CA_STRT:
					case CA_ENDD:
						if( mChannelNumber == 0 )
						{
							mChannelNumber = ltr.getChannel();
							broadcast( ChangedAttribute.CHANNEL_NUMBER );
						}
						
						if( ltr.getChannel() == 31 )
						{
							processCallEndMessage( ltr );
						}
						else if( ltr.getChannel() == mChannelNumber )
						{
							processCallMessage( ltr );
						}
						else
						{
							processCallDetectMessage( ltr );
						}
						break;
					case SY_IDLE:
						if( mChannelNumber != ltr.getChannel() )
						{
							mChannelNumber = ltr.getChannel();
							broadcast( ChangedAttribute.CHANNEL_NUMBER );
						}
						break;
					case MA_CHNH:
						break;
					case MA_CHNL:
						break;
					case FQ_RXHI:
					case FQ_RXLO:
						if( ltr.getFrequency() > 0 )
						{
							mReceiveFrequencies.put( ltr.getHomeRepeater(), 
									ltr.getFrequency() );
						}
						break;
					case FQ_TXHI:
					case FQ_TXLO:
						if( ltr.getFrequency() > 0 )
						{
							mTransmitFrequencies.put( ltr.getHomeRepeater(), 
									ltr.getFrequency() );
						}
						break;
					case ID_NBOR:
						String neighborID = ltr.getNeighborID();
						
						if( neighborID != null )
						{
							mNeighborIDs.add( neighborID );
						}
						break;
					case ID_UNIQ:
						int uniqueID = ltr.getRadioUniqueID();
						
						if( uniqueID != LTRNetOSWMessage.INT_NULL_VALUE )
						{
							mUniqueIDs.add( uniqueID );
						}
						
						if( getCurrentLTRCallEvent() == null )
						{
							mCurrentCallEvent = new LTRCallEvent.Builder( CallEventType.REGISTER )
								.aliasList( getAliasList() )
								.channel( String.valueOf( mChannelNumber ) )
								.frequency( mFrequency )
								.from( String.valueOf( uniqueID ) )
								.build();
						}
						else
						{
							mCurrentCallEvent.setFromID( 
									String.valueOf( ltr.getRadioUniqueID() ) );
							mCurrentCallEvent.setDetails( "Unique ID" );
							
							broadcast( mCurrentCallEvent );
						}

						mTalkgroup = String.valueOf( uniqueID );
						broadcast( ChangedAttribute.TO_TALKGROUP );
						
						mTalkgroupAlias = ltr.getRadioUniqueIDAlias();
						broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );
						break;
					case ID_SITE:
						String siteID = ltr.getSiteID();
						
						if( siteID != null )
						{
							mSiteIDs.add( siteID );
						}
						break;
					default:
						break;
				}
			}
			else if( message instanceof LTRNetISWMessage )
			{
				LTRNetISWMessage ltr = ((LTRNetISWMessage)message);
				
				switch( ltr.getMessageType() )
				{
					case CA_STRT:
						processCallMessage( ltr );
						break;
					case CA_ENDD:
						processCallEndMessage( ltr );
						break;
					case ID_ESNH:
					case ID_ESNL:
						String esn = ltr.getESN();
						
						if( !esn.contains( "xxxx" ) )
						{
							mESNs.add( ltr.getESN() );
						}
						
						mDescription = "ESN";
						broadcast( ChangedAttribute.DESCRIPTION );
						
						mTalkgroup = ltr.getESN();
						broadcast( ChangedAttribute.TO_TALKGROUP );

						mTalkgroupAlias = ltr.getESNAlias();
						broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );

						broadcast( new DecoderStateEvent( this, Event.DECODE, State.DATA ) );

						if( mCurrentCallEvent == null )
						{
							mCurrentCallEvent = new LTRCallEvent.Builder( CallEventType.REGISTER_ESN )
							.aliasList( getAliasList() )
							.details( "ESN:" + ltr.getESN() )
							.frequency( mFrequency )
							.from( ltr.getESN() )
							.build();
							
							broadcast( mCurrentCallEvent );
						}
						
 						break;
					case ID_UNIQ:
						int uniqueid = ltr.getRadioUniqueID();
						
						if( uniqueid != LTRNetISWMessage.INT_NULL_VALUE )
						{
							mUniqueIDs.add( uniqueid );
							
							mDescription = "REGISTER UID";
							broadcast( ChangedAttribute.DESCRIPTION );

							mTalkgroup = String.valueOf( uniqueid );
							broadcast( ChangedAttribute.TO_TALKGROUP );

							mTalkgroupAlias = ltr.getRadioUniqueIDAlias();
							broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );
							
							if( getCurrentLTRCallEvent() == null )
							{
								mCurrentCallEvent = new LTRCallEvent.Builder( CallEventType.REGISTER )
									.aliasList( getAliasList() )
									.channel( String.valueOf( mChannelNumber ) )
									.frequency( mFrequency )
									.from( String.valueOf( uniqueid ) )
									.build();
							}
							else
							{
								mCurrentCallEvent.setFromID( 
										String.valueOf( ltr.getRadioUniqueID() ) );
								mCurrentCallEvent.setDetails( "Unique ID" );
								
								broadcast( mCurrentCallEvent );
							}
						}
						break;
					default:
						break;
				}
			}
		}
    }
	
	public LTRCallEvent getCurrentLTRCallEvent()
	{
		if( mCurrentCallEvent != null )
		{
			return (LTRCallEvent)mCurrentCallEvent;
		}
		
		return null;
	}
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "Activity Summary\n" );
		sb.append( "Decoder:\tLTR-Net\n\n" );

		if( mSiteIDs.isEmpty() )
		{
			sb.append( "Site:\tUnknown\n" );
		}
		else
		{
			Iterator<String> it = mSiteIDs.iterator();
			
			while( it.hasNext() )
			{
				sb.append( "Site:\t" );
				
				String siteID = it.next();

				sb.append( siteID );
				
				if( hasAliasList() )
				{
					Alias siteAlias = getAliasList().getSiteID( String.valueOf( siteID ) );

					if( siteAlias != null )
					{
						sb.append( " " );
						sb.append( siteAlias.getName() );
					}
				}

				sb.append( "\n" );
			}
		}
		
		sb.append( "\nLCNs (transmit | receive)\n" );
		
		if( mReceiveFrequencies.isEmpty() && mTransmitFrequencies.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			for( int x = 1; x < 21; x++ )
			{
				long rcv = 0;
				
				if( mReceiveFrequencies.containsKey( x ) )
				{
					rcv = mReceiveFrequencies.get( x );
				}
				
				long xmt = 0;
				
				if( mTransmitFrequencies.containsKey( x ) )
				{
					xmt = mTransmitFrequencies.get( x );
				}

				if( rcv > 0 || xmt > 0 )
				{
					if( x < 10 )
					{
						sb.append( " " );
					}

					sb.append( x );
					sb.append( ": " );

					if( xmt == 0 )
					{
						sb.append( "---.-----" );
					}
					else
					{
						sb.append( mDecimalFormatter.format( (double)xmt/1E6d ) );
					}
					sb.append( " | " );
					
					if( rcv == 0 )
					{
						sb.append( "---.-----" );
					}
					else
					{
						sb.append( mDecimalFormatter.format( (double)rcv/1E6d ) );
					}
					
					if( x == mChannelNumber )
					{
						sb.append( " **" );
					}
					
					sb.append( "\n" );
				}
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
		
		sb.append( "\nRadio Unique IDs\n" );
		
		if( mUniqueIDs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<Integer> it = mUniqueIDs.iterator();
			
			while( it.hasNext() )
			{
				int uid = it.next();
				
				sb.append( "  " );
				sb.append( uid );
				sb.append( " " );
				
				if( hasAliasList() )
				{
					Alias alias = getAliasList().getUniqueID( uid );
					
					if( alias != null )
					{
						sb.append( alias.getName() );
					}
				}
				
				sb.append( "\n" );
			}
		}

		sb.append( "\nESNs\n" );
		
		if( mESNs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mESNs.iterator();
			
			while( it.hasNext() )
			{
				String esn = it.next();
				
				sb.append( "  " );
				sb.append( esn );
				sb.append( " " );
				
				if( hasAliasList() )
				{
					Alias alias = getAliasList().getESNAlias( esn );
					
					if( alias != null )
					{
						sb.append( alias.getName() );
					}
				}
				
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nNeighbor Sites\n" );
		
		if( mNeighborIDs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mNeighborIDs.iterator();
			
			while( it.hasNext() )
			{
				String neighbor = it.next();
				
				sb.append( "  " );
				sb.append( neighbor );
				sb.append( " " );
				
				if( hasAliasList() )
				{
					Alias alias = getAliasList().getSiteID( String.valueOf( neighbor ) );
					
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

	/**
	 * Call Detect - current channel messages indicate a call on another channel
	 */
	private void processCallDetectMessage( LTRNetMessage message )
	{
		if( !mActiveCalls.containsKey( message.getChannel() ) ||
			!mActiveCalls.get( message.getChannel() ).contentEquals( message.getTalkgroupID() ) )
		{
			mActiveCalls.put( message.getChannel(), message.getTalkgroupID() );
			
			int channel = message.getChannel();

			long frequency = 0;
			
			if( mTransmitFrequencies.containsKey( channel ) )
			{
				frequency = mTransmitFrequencies.get( channel );
			}

			broadcast( new LTRCallEvent.Builder( CallEventType.CALL_DETECT )
				.aliasList( getAliasList() )
				.channel( String.valueOf( message.getChannel() ) )
				.frequency( frequency )
				.to( message.getTalkgroupID() )
				.build() );
		}
	}

	private void processCallMessage( LTRNetMessage message )
	{
		mLog.debug( "Processing Call Message [" + message.getMessage() + "]" );
		LTRCallEvent current = getCurrentLTRCallEvent();
		
		int group = message.getGroup();

		/* If this is a new call or the talkgroup is different from the current
		 * call, create a new call event */
		if( current == null || !current.isMatchingTalkgroup( message ) )
		{
			/* Invalidate the current call */
			if( current != null )
			{
				current.setValid( false );
				broadcast( current );
			}
			
			mTalkgroup = message.getTalkgroupID();
			broadcast( ChangedAttribute.TO_TALKGROUP );

			/* A talkgroup must be seen at least once before it will be added
			 * to the mTalkgroups list that is used in the activity summary,
			 * so that we don't pollute the summary with one-off error talkgroups */
			if( mTalkgroupsFirstHeard.contains( mTalkgroup ) )
			{
				mTalkgroups.add( mTalkgroup );
			}
			else
			{
				mTalkgroupsFirstHeard.add( mTalkgroup );
			}
			
			mTalkgroupAlias = message.getTalkgroupIDAlias();
			broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );

			CallEventType type = CallEventType.CALL;

			if( group == 253 )
			{
				type = CallEventType.REGISTER;
				
				mDescription = "REGISTER";
				broadcast( ChangedAttribute.DESCRIPTION );
			}
			else if( group == 254 )
			{
				type = CallEventType.STATION_ID;

				mDescription = "STATION ID";
				broadcast( ChangedAttribute.DESCRIPTION );
			}
			
			mCurrentCallEvent = new LTRCallEvent.Builder( type )
				.aliasList( getAliasList() )
				.channel( String.valueOf( message.getChannel() ) )
				.frequency( mFrequency )
				.to( message.getTalkgroupID() )
				.build();
		
			broadcast( mCurrentCallEvent );
			
			mLog.debug( "Decoder State: Call Start" );
			
			broadcast( new DecoderStateEvent( this, Event.START, 
					( type == CallEventType.CALL ) ? State.CALL : State.DATA ) );
		}
		else
		{
			mLog.debug( "Decoder State: Call Continuation Group:" + group );
			
			broadcast( new DecoderStateEvent( this, Event.CONTINUATION, 
				( group == 253 || group == 254 ) ? State.DATA : State.CALL ) );
		}
	}
	
	private void processCallEndMessage( LTRNetMessage message )
	{
		if( mCurrentCallEvent != null && 
			mCurrentCallEvent.getCallEventType() == CallEventType.CALL )
		{
			mCurrentCallEvent.setEnd( System.currentTimeMillis() );
			broadcast( mCurrentCallEvent );
			
		}

		mCurrentCallEvent = null;
		
		mLog.debug( "Decoder State: Call End" );
		broadcast( new DecoderStateEvent( this, Event.END, State.FADE ) );
	}

	/**
	 * Resets the decoder state for re-use on another channel
	 */
	public void reset()
	{
		mChannelNumber = 0;
		
		mActiveCalls.clear();
		mESNs.clear();
		mNeighborIDs.clear();
		mReceiveFrequencies.clear();
		mSiteIDs.clear();
		mTalkgroups.clear();
		mTransmitFrequencies.clear();
		mUniqueIDs.clear();
		
		resetState();
	}

	/**
	 * Resets the decoder state after a call event
	 */
	private void resetState()
	{
		if( mCurrentCallEvent != null && mCurrentCallEvent
				.getCallEventType() == CallEventType.CALL )
		{
			mCurrentCallEvent.setEnd( System.currentTimeMillis() );
			broadcast( mCurrentCallEvent );
		}
			
		mCurrentCallEvent = null;
		
		mTalkgroup = null;
		broadcast( ChangedAttribute.TO_TALKGROUP );

		mTalkgroupAlias = null;
		broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );

		mDescription = null;
		broadcast( ChangedAttribute.DESCRIPTION );
	}

	public String getToTalkgroup()
	{
		return mTalkgroup;
	}
	
	public Alias getToTalkgroupAlias()
	{
		return mTalkgroupAlias;
	}
	
	public String getDescription()
	{
		return mDescription;
	}
	
	public boolean hasChannelNumber()
	{
	    return mChannelNumber != 0;
	}
	
	public int getChannelNumber()
	{
	    return mChannelNumber;
	}

	@Override
	public void init()
	{
		/* No initialization steps required */
	}
	
	@Override
	public void receiveDecoderStateEvent( DecoderStateEvent event )
	{
		switch( event.getEvent() )
		{
			case RESET:
				resetState();
				break;
			case SOURCE_FREQUENCY:
				mFrequency = event.getFrequency();
				break;
			default:
				break;
		}
	}
}
