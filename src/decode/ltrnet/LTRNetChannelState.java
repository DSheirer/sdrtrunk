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
package decode.ltrnet;

import java.awt.EventQueue;
import java.util.HashMap;

import message.Message;
import alias.Alias;
import alias.AliasList;
import controller.activity.CallEvent;
import controller.activity.CallEvent.CallEventType;
import controller.activity.CallEventModel;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import controller.state.ChannelState.State;

public class LTRNetChannelState extends ChannelState
{
	private String mTalkgroup;
	private Alias mTalkgroupAlias;
	private String mDescription;
	private int mChannelNumber;
    private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();
	private LTRNetActivitySummary mActivitySummary;
	
	public LTRNetChannelState( ProcessingChain channel, AliasList aliasList )
	{
		super( channel, aliasList );
		
		mActivitySummary = new LTRNetActivitySummary( aliasList );
	}

	/**
	 * Intercept the fade event so that we can generate a call end event
	 */
	@Override
	public void fade( final CallEventType type )
	{
		/*
		 * We can receive multiple call tear-down messages -- only respond to
		 * the message that can change the state to fade
		 */
		if( getState().canChangeTo( State.FADE ) )
		{
			LTRCallEvent current = getCurrentLTRCallEvent();

			if( current != null )
			{
            	/* Close out the call event.  If we only received 1 call 
            	 * message, then flag it as noise and remove the call event */
            	if( current.isValid() )
            	{
            		mCallEventModel.setEnd( current );
            	}
            	else
            	{
            		mCallEventModel.remove( current );
            	}
			}
		}
		
		setCurrentCallEvent( null );
		
		mActiveCalls.clear();

		super.fade( type );
	}
	
	public LTRCallEvent getCurrentLTRCallEvent()
	{
		CallEvent current = getCurrentCallEvent();
		
		if( current != null )
		{
			return (LTRCallEvent)current;
		}
		
		return null;
	}
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( mActivitySummary.getSummary() );
		
		for( AuxChannelState state: mAuxChannelStates )
		{
			sb.append( state.getActivitySummary() );
			sb.append( "\n\n" );
		}
		
		return sb.toString();
    }
	
	@Override
    public void receive( Message message )
    {
		super.receive( message );
		
		if( message instanceof LTRNetOSWMessage )
		{
			mActivitySummary.receive( message );
			
			LTRNetOSWMessage ltr = (LTRNetOSWMessage)message;
			
			Alias talkgroupAlias = ltr.getTalkgroupIDAlias();
			
			switch( ltr.getMessageType() )
			{
				case CA_STRT:
					if( mChannelNumber == 0 )
					{
						mChannelNumber = ltr.getChannel();
					}
					
					if( ltr.getChannel() == mChannelNumber )
					{
						processCallMessage( ltr );

						if( ltr.getGroup() == 253 )
						{
							mDescription = "REGISTER";
							broadcastChange( ChangedAttribute.DESCRIPTION );
						}
						else
						{
							mTalkgroup = ltr.getTalkgroupID();
							broadcastChange( ChangedAttribute.TO_TALKGROUP );
							
							if( talkgroupAlias != null )
							{
								mTalkgroupAlias = talkgroupAlias;
								broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
							}
						}
					}
					else
					{
						//Call Detect
						if( !mActiveCalls.containsKey( ltr.getChannel() ) ||
							!mActiveCalls.get( ltr.getChannel() )
								.contentEquals( ltr.getTalkgroupID() ) )
						{
							mActiveCalls.put( ltr.getChannel(), 
											  ltr.getTalkgroupID() );
							
							mCallEventModel.add(  
								new LTRCallEvent.Builder( CallEventType.CALL_DETECT )
								.aliasList( mAliasList )
								.channel( ltr.getChannel() )
								.to( ltr.getTalkgroupID() )
								.build() );
						}
					}
					break;
				case CA_ENDD:
					if( ltr.getGroup() != 253 )
					{
						mTalkgroup = ltr.getTalkgroupID();
						broadcastChange( ChangedAttribute.TO_TALKGROUP );

						if( talkgroupAlias != null )
						{
							mTalkgroupAlias = talkgroupAlias;
							broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
						}
					}
					
					fade( CallEventType.CALL_END );
					break;
				case ID_UNIQ:
					mDescription = "REGISTER UID";
					broadcastChange( ChangedAttribute.DESCRIPTION );

					mTalkgroup = String.valueOf( ltr.getRadioUniqueID() );
					broadcastChange( ChangedAttribute.TO_TALKGROUP );
					
					mTalkgroupAlias = ltr.getRadioUniqueIDAlias();
					broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );

					final LTRCallEvent currentEvent = getCurrentLTRCallEvent();
					
					if( currentEvent != null && 
						currentEvent.getCallEventType() == CallEventType.REGISTER )
					{
						final String uid = String.valueOf( ltr.getRadioUniqueID() );
						
						if( uid != null )
						{
							mCallEventModel.setFromID( currentEvent, uid );
						}
					}
					else
					{
						mCallEventModel.add(  
							new LTRCallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "Radio Unique ID" )
								.frequency( mProcessingChain.getChannel()
										.getTunerChannel().getFrequency() )
								.from( String.valueOf( ltr.getRadioUniqueID() ) )
								.build() );
					}

					setState( State.CALL );
					break;
				case SY_IDLE:
					mChannelNumber = ltr.getChannel();
					broadcastChange( ChangedAttribute.CHANNEL_NUMBER );
					break;
				default:
					break;
			}
			
		}
		else if( message instanceof LTRNetISWMessage )
		{
			mActivitySummary.receive( message );
			
			LTRNetISWMessage ltr = (LTRNetISWMessage)message;
			
			Alias talkgroupAlias = ltr.getTalkgroupIDAlias();
			
			switch( ltr.getMessageType() )
			{
				case CA_STRT:
					processCallMessage( ltr );

					if( ltr.getGroup() == 253 )
					{
						mDescription = "REGISTER";
						broadcastChange( ChangedAttribute.DESCRIPTION );
					}
					else
					{
						mTalkgroup = ltr.getTalkgroupID();
						broadcastChange( ChangedAttribute.TO_TALKGROUP );
						
						if( talkgroupAlias != null )
						{
							mTalkgroupAlias = talkgroupAlias;
							broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
						}
					}
					break;
				case CA_ENDD:
					mTalkgroup = ltr.getTalkgroupID();
					broadcastChange( ChangedAttribute.TO_TALKGROUP );

					if( talkgroupAlias != null )
					{
						mTalkgroupAlias = talkgroupAlias;
						broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
					}
					fade( CallEventType.CALL_END );
					break;
				case ID_UNIQ:
					mDescription = "REGISTER UID";
					broadcastChange( ChangedAttribute.DESCRIPTION );

					mTalkgroup = String.valueOf( ltr.getRadioUniqueID() );
					broadcastChange( ChangedAttribute.TO_TALKGROUP );

					mTalkgroupAlias = ltr.getRadioUniqueIDAlias();
					broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
					
					if( getState() != State.DATA )
					{
						LTRCallEvent event = 
							new LTRCallEvent.Builder( CallEventType.REGISTER )
						.aliasList( mAliasList )
						.channel( ltr.getChannel() )
						.frequency( mProcessingChain.getChannel()
								.getTunerChannel().getFrequency() )
						.to( ltr.getTalkgroupID() )
						.build();
						
						mCallEventModel.add( event );
						
						setCurrentCallEvent( event );

						setState( State.DATA );
					}
					
					if( mCurrentCallEvent != null )
					{
						mCurrentCallEvent.setFromID( 
								String.valueOf( ltr.getRadioUniqueID() ) );
						mCurrentCallEvent.setDetails( "Unique ID" );
					}
					break;
				case ID_ESNH:
				case ID_ESNL:
					mDescription = "ESN";
					broadcastChange( ChangedAttribute.DESCRIPTION );
					
					mTalkgroup = ltr.getESN();
					broadcastChange( ChangedAttribute.TO_TALKGROUP );

					mTalkgroupAlias = ltr.getESNAlias();
					broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );

					setState( State.DATA );

					mCallEventModel.add( 
							new LTRCallEvent.Builder( CallEventType.REGISTER_ESN )
							.aliasList( mAliasList )
							.details( "ESN:" + ltr.getESN() )
							.frequency( mProcessingChain.getChannel()
									.getTunerChannel().getFrequency() )
							.from( ltr.getESN() )
							.build() );
					break;
				default:
					break;
			}
			
		}
    }
	
	private void processCallMessage( LTRNetMessage message )
	{
		LTRCallEvent current = getCurrentLTRCallEvent();

		if( current == null || !current.addMessage( message ) )
		{
			if( current != null )
			{
				mCallEventModel.remove( current );
			}
			
			LTRCallEvent event;
			
			if( message.getGroup() == 253 )
			{
				event = new LTRCallEvent.Builder( CallEventType.REGISTER )
					.aliasList( mAliasList )
					.channel( message.getChannel() )
					.frequency( mProcessingChain.getChannel()
							.getTunerChannel().getFrequency() )
					.to( message.getTalkgroupID() )
					.build();
				
				setState( State.DATA );
			}
			else
			{
				event = new LTRCallEvent.Builder( CallEventType.CALL )
					.aliasList( mAliasList )
					.channel( message.getChannel() )
					.frequency( mProcessingChain.getChannel()
							.getTunerChannel().getFrequency() )
					.to( message.getTalkgroupID() )
					.build();

				setState( State.CALL );
			}
			
			mCallEventModel.add( event );

			setCurrentCallEvent( event );
		}
		else
		{
			if( getState() == State.CALL )
			{
				setState( State.CALL );
			}
			else if( getState() == State.DATA )
			{
				setState( State.DATA );
			}
		}
	}
	
	public void reset()
	{
		mTalkgroup = null;
		broadcastChange( ChangedAttribute.TO_TALKGROUP );

		mTalkgroupAlias = null;
		broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );

		mDescription = null;
		broadcastChange( ChangedAttribute.DESCRIPTION );

		super.reset();
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
}
