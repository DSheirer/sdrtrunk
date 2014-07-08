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

import message.Message;
import alias.Alias;
import alias.AliasList;
import controller.activity.CallEvent.CallEventType;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;

public class LTRNetChannelState extends ChannelState
{
	private String mToTalkgroup;
	private Alias mToTalkgroupAlias;
	private String mFromTalkgroup;
	private Alias mFromTalkgroupAlias;
	private String mFromTalkgroupType;
	private int mChannelNumber;
	private LTRCallEvent mCurrentCallEvent;
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
		 * We can receive multiple call tear-down messages -- only post a call
		 * end event for the one that can change the state to fade
		 */
		if( getState().canChangeTo( State.FADE ) )
		{
			mCallEventModel.add( 
					new LTRCallEvent.Builder( type )
						.aliasList( getAliasList() )
						.channel( mChannelNumber )
						.frequency( mProcessingChain.getChannel()
								.getTunerChannel().getFrequency() )
						.to( mToTalkgroup )
						.build() );
			
		}
		
		mCurrentCallEvent = null;

		super.fade( type );
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
					mToTalkgroup = ltr.getTalkgroupID();
					broadcastChange( ChangedAttribute.TO_TALKGROUP );

					if( talkgroupAlias != null )
					{
						mToTalkgroupAlias = talkgroupAlias;
						broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
					}
					
					mChannelNumber = ltr.getChannel();
					broadcastChange( ChangedAttribute.CHANNEL_NUMBER );

					if( getState() != ChannelState.State.CALL )
					{
						mCallEventModel.add( 
								new LTRCallEvent.Builder( CallEventType.CALL_START )
								.aliasList( mAliasList )
								.channel( ltr.getChannel() )
								.frequency( mProcessingChain.getChannel()
										.getTunerChannel().getFrequency() )
								.to( mToTalkgroup )
								.build() );
					}

					setState( State.CALL );
					break;
				case CA_ENDD:
					mToTalkgroup = ltr.getTalkgroupID();
					broadcastChange( ChangedAttribute.TO_TALKGROUP );

					if( talkgroupAlias != null )
					{
						mToTalkgroupAlias = talkgroupAlias;
						broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
					}
					
					fade( CallEventType.CALL_END );
					break;
				case ID_UNIQ:
					mFromTalkgroupType = "UID";
					broadcastChange( ChangedAttribute.FROM_TALKGROUP_TYPE );

					mFromTalkgroup = String.valueOf( ltr.getRadioUniqueID() );
					broadcastChange( ChangedAttribute.FROM_TALKGROUP );
					
					mFromTalkgroupAlias = ltr.getRadioUniqueIDAlias();
					broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );

					mCallEventModel.add( 
							new LTRCallEvent.Builder( CallEventType.REGISTER )
							.aliasList( mAliasList )
							.details( "Radio Unique ID" )
							.frequency( mProcessingChain.getChannel()
									.getTunerChannel().getFrequency() )
							.from( String.valueOf( ltr.getRadioUniqueID() ) )
							.build() );

					setState( State.CALL );
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
					mToTalkgroup = ltr.getTalkgroupID();
					broadcastChange( ChangedAttribute.TO_TALKGROUP );
					
					if( talkgroupAlias != null )
					{
						mToTalkgroupAlias = talkgroupAlias;
						broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
					}

					if( getState() != State.CALL )
					{
						mCurrentCallEvent = 
							new LTRCallEvent.Builder( CallEventType.CALL_START )
								.aliasList( mAliasList )
								.channel( ltr.getChannel() )
								.frequency( mProcessingChain.getChannel()
										.getTunerChannel().getFrequency() )
								.to( mToTalkgroup )
								.build();
						
						mCallEventModel.add( mCurrentCallEvent );
					}
					setState( State.CALL );
					break;
				case CA_ENDD:
					mToTalkgroup = ltr.getTalkgroupID();
					broadcastChange( ChangedAttribute.TO_TALKGROUP );

					if( talkgroupAlias != null )
					{
						mToTalkgroupAlias = talkgroupAlias;
						broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
					}
					fade( CallEventType.CALL_END );
					break;
				case ID_UNIQ:
					mFromTalkgroupType = "UID";
					broadcastChange( ChangedAttribute.FROM_TALKGROUP_TYPE );

					mFromTalkgroup = String.valueOf( ltr.getRadioUniqueID() );
					broadcastChange( ChangedAttribute.FROM_TALKGROUP );

					mFromTalkgroupAlias = ltr.getRadioUniqueIDAlias();
					broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );
					
					if( getState() != State.CALL )
					{
						mCallEventModel.add( new LTRCallEvent.Builder( 
								CallEventType.ID_UNIQUE )
								.aliasList( mAliasList )
								.details( "Unique ID" )
								.frequency( mProcessingChain.getChannel()
										.getTunerChannel().getFrequency() )
								.from( String.valueOf( ltr.getRadioUniqueID() ) )
								.build() );
					}
					else
					{
						if( mCurrentCallEvent != null )
						{
							mCurrentCallEvent.setFromID( 
									String.valueOf( ltr.getRadioUniqueID() ) );
							mCurrentCallEvent.setDetails( "Unique ID" );
						}
					}
					setState( State.CALL );
					break;
				case ID_ESNH:
				case ID_ESNL:
					mFromTalkgroupType = "ESN";
					broadcastChange( ChangedAttribute.FROM_TALKGROUP_TYPE );
					
					mFromTalkgroup = ltr.getESN();
					broadcastChange( ChangedAttribute.FROM_TALKGROUP );

					mFromTalkgroupAlias = ltr.getESNAlias();
					broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );

					setState( State.CALL );

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
	
	public void reset()
	{
		mToTalkgroup = null;
		broadcastChange( ChangedAttribute.TO_TALKGROUP );

		mToTalkgroupAlias = null;
		broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );

		mFromTalkgroup = null;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP );

		mFromTalkgroupAlias = null;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );

		mFromTalkgroupType = " ";
		broadcastChange( ChangedAttribute.FROM_TALKGROUP_TYPE );

		super.reset();
	}
	
	public String getToTalkgroup()
	{
		return mToTalkgroup;
	}
	
	public Alias getToTalkgroupAlias()
	{
		return mToTalkgroupAlias;
	}
	
	public String getFromTalkgroup()
	{
		return mFromTalkgroup;
	}
	
	public Alias getFromTalkgroupAlias()
	{
		return mFromTalkgroupAlias;
	}

	public String getFromTalkgroupType()
	{
		return mFromTalkgroupType;
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
