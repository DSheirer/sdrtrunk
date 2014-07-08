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
package decode.passport;

import message.Message;
import alias.Alias;
import alias.AliasList;
import audio.AudioType;
import audio.SquelchListener;
import controller.activity.CallEvent.CallEventType;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;

public class PassportChannelState extends ChannelState
{
	private String mTalkgroup;
	private Alias mTalkgroupAlias;
	private String mMobileID;
	private Alias mMobileIDAlias;
	private int mChannelNumber;
	private long mFrequency;
	private PassportCallEvent mCurrentCallEvent;
	private PassportActivitySummary mActivitySummary;
	
	public PassportChannelState( ProcessingChain channel, AliasList aliasList )
	{
		super( channel, aliasList );
		
		mActivitySummary = new PassportActivitySummary( aliasList );
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
	@SuppressWarnings( "incomplete-switch" )
    public void receive( Message message )
    {
		super.receive( message );
		
		if( message instanceof PassportMessage )
		{
			mActivitySummary.receive( message );
			
			PassportMessage passport = (PassportMessage)message;
			
			if( passport.isValid() )
			{
				switch( passport.getMessageType() )
				{
	                case CA_STRT:
	                	setTalkgroup( String.valueOf( passport.getTalkgroupID() ) );
	                	setTalkgroupAlias( passport.getTalkgroupIDAlias() );
	                    
	                    if( mTalkgroupAlias != null )
	                    {
	                    	try
	                    	{
		                    	if( mTalkgroupAlias.hasAudioType() )
		                    	{
	                				mAudioTypeListener.setAudioType( 
	                							mTalkgroupAlias.getAudioType() );
		                    	}
	                    	}
	                    	catch( Exception e )
	                    	{
	                    		e.printStackTrace();
	                    	}
	                    }
	                    
	                    mChannelNumber = passport.getLCN();
	                    mFrequency = passport.getLCNFrequency();
	                    
	                    if( getState() != State.CALL )
	                    {
	                    	mCurrentCallEvent = new PassportCallEvent
	                    			.Builder( CallEventType.CALL_START )
	                    			.aliasList( mAliasList )
	                    			.channel( mChannelNumber )
	                    			.frequency( mFrequency )
	                    			.to( String.valueOf( passport.getTalkgroupID() ) )
	                    			.build();
	                    	
		                    mCallEventModel.add( mCurrentCallEvent );
	                    }
	                    
	                    setState( State.CALL );
	                    break;
	                case DA_STRT:
	                	setTalkgroup( String.valueOf( passport.getTalkgroupID() ) );
	                	setTalkgroupAlias( passport.getTalkgroupIDAlias() );
	                    setState( State.DATA );
	                    break;
	                case CA_ENDD:
	                	setTalkgroup( String.valueOf( passport.getTalkgroupID() ) );
	                	setTalkgroupAlias( passport.getTalkgroupIDAlias() );
	                    fade( CallEventType.CALL_END );
	                    break;
	                case ID_RDIO:
	                	setMobileID( passport.getMobileID() );
	                	setMobileIDAlias( passport.getMobileIDAlias() );
	                	
	                	if( mCurrentCallEvent != null )
	                	{
	                		mCurrentCallEvent.setFromID( mMobileID );
	                	}
	                    setState( State.CALL );
	                	break;
				}
			}
		}
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
					new PassportCallEvent.Builder( type )
						.aliasList( getAliasList() )
						.channel( mChannelNumber )
						.frequency( mProcessingChain.getChannel()
								.getTunerChannel().getFrequency() )
						.to( mTalkgroup )
						.build() );
		}
		
		super.fade( type );
	}
	
    
	
    /**
     * Make the ConventionalChannelState always unsquelched
     */
    public void addListener( SquelchListener listener )
    {
        super.addListener( listener );
    }
	
	public void reset()
	{
		mAudioTypeListener.setAudioType( AudioType.NORMAL ); 
		
		mCurrentCallEvent = null;

		setTalkgroup( null );
		setTalkgroupAlias( null );

		setMobileID( null );
		setMobileIDAlias( null );

		super.reset();
	}
	
	public String getTalkgroup()
	{
		return mTalkgroup;
	}
	
	public void setTalkgroup( String talkgroup )
	{
		mTalkgroup = talkgroup;
		broadcastChange( ChangedAttribute.TO_TALKGROUP );
	}
	
	public Alias getTalkgroupAlias()
	{
		return mTalkgroupAlias;
	}
	
	public void setTalkgroupAlias( Alias alias )
	{
		mTalkgroupAlias = alias;
		broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
	}

	public String getMobileID()
	{
		return mMobileID;
	}
	
	public void setMobileID( String id )
	{
		mMobileID = id;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP );
	}
	
	public Alias getMobileIDAlias()
	{
		return mMobileIDAlias;
	}
	
	public void setMobileIDAlias( Alias alias )
	{
		mMobileIDAlias = alias;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );
	}
}
