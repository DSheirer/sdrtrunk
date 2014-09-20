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

import gui.SDRTrunk;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import message.Message;
import alias.Alias;
import alias.AliasList;
import audio.AudioType;
import audio.SquelchListener;
import controller.activity.CallEvent;
import controller.activity.CallEvent.CallEventType;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;

public class PassportChannelState extends ChannelState
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( PassportChannelState.class );

	private String mTalkgroup;
	private Alias mTalkgroupAlias;
	private String mMobileID;
	private Alias mMobileIDAlias;
	private int mChannelNumber;
	private int mSiteNumber;
	private PassportBand mSiteBand;
	private PassportActivitySummary mActivitySummary;
	private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();
	
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
	                    if( mChannelNumber == 0 )
	                    {
	                    	mChannelNumber = passport.getLCN();
	                    }
	                	
	                    if( passport.getLCN() == mChannelNumber )
	                    {
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
		                    		mLog.error( "Error setting audio for "
		                    				+ "passport call", e );
		                    	}
		                    }
		                    
		                    PassportCallEvent current = 
		                    		getCurrentPassportCallEvent();

		                    /* If we're already in a call event, add the message
		                     * to the current call event ... if false, then we
		                     * have a different call ... cleanup the old one. */
		                    if( current != null && !current.addMessage( passport ) )
		                    {
		                    	if( current.isValid() )
		                    	{
		                    		final PassportCallEvent endEvent = 
		                    				getCurrentPassportCallEvent();
		                    		
		                    		mCallEventModel.setEnd( endEvent );
		                    	}
		                    	else
		                    	{
		                    		mCallEventModel.remove( current );
		                    	}
		                    	
		                    	current = null;
		                    	setCurrentCallEvent( null );
		                    }

		                    if( current == null )
		                    {
		                    	mCurrentCallEvent = new PassportCallEvent
		                    			.Builder( CallEventType.CALL )
		                    			.aliasList( mAliasList )
		                    			.channel( mChannelNumber )
		                    			.frequency( passport.getLCNFrequency() )
		                    			.to( String.valueOf( passport.getTalkgroupID() ) )
		                    			.build();
		                    	
			                    mCallEventModel.add( mCurrentCallEvent );
		                    }
		                    
		                    State state = getState();
		                    
		                    if( state == State.IDLE || state == State.CALL )
		                    {
			                    setState( State.CALL );
		                    }
		                    else if( state == State.DATA )
		                    {
		                    	setState( State.DATA );
		                    }
	                    }
	                    else
	                    {
	                    	//Call Detection
	                    	int lcn = passport.getLCN();
	                    	String tg = String.valueOf( passport.getTalkgroupID() );
	                    	
	                    	if( !mActiveCalls.containsKey( lcn ) ||
	                    		!mActiveCalls.get( lcn ).contentEquals( tg ) )
	                    	{
	                    		mActiveCalls.put( lcn, tg );

	                    		PassportCallEvent event = new PassportCallEvent
		                    			.Builder( CallEventType.CALL_DETECT )
		                    			.aliasList( mAliasList )
		                    			.channel( lcn )
		                    			.details( "Site: " + passport.getSite() )
		                    			.frequency( passport.getLCNFrequency() )
		                    			.to( tg )
		                    			.build();
	                    		
	                    		mCallEventModel.add( event );
	                    	}
	                    }
	                    break;
	                case DA_STRT:
//TODO: add channel number checking and log a call event if on another site
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
	                	
	                	final String mid = passport.getMobileID();
	                	
	                	final PassportCallEvent event = 
	                			getCurrentPassportCallEvent();
	                	
	                	if( event != null && mid != null )
	                	{
	                		mCallEventModel.setFromID( event, mid );
	                	}

	                    setState( State.CALL );
	                	break;
	                case RA_REGI:
	                	mCallEventModel.add( new PassportCallEvent
            			.Builder( CallEventType.REGISTER )
            			.aliasList( mAliasList )
            			.channel( passport.getLCN() )
            			.frequency( passport.getLCNFrequency() )
            			.to( passport.getToID() )
            			.build() );
//TODO: add logic to recognize the register event and suppress the subsequent
//call messages, but capture the group assignments. 
	                	setState( State.DATA );
	                	break;
	                case SY_IDLE:
	                	PassportBand band = passport.getSiteBand();
	                	
	                	if( mSiteBand == null || mSiteBand != band )
	                	{
	                		mSiteBand = band;
	                		
	                		mSiteNumber = passport.getSite();
	                		
	                		long frequency = mProcessingChain.getChannel()
	                				.getTunerChannel().getFrequency();
	                		
	                		mChannelNumber = mSiteBand.getChannel( frequency );
	                		
	                		broadcastChange( ChangedAttribute.CHANNEL_NUMBER );
	                	}
                	default:
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
			final PassportCallEvent current = getCurrentPassportCallEvent();
			
			if( current != null )
			{
				mCallEventModel.setEnd( current );
			}
		}
		
		setCurrentCallEvent( null );
		
		mActiveCalls.clear();
		
		super.fade( type );
	}
	
	public PassportCallEvent getCurrentPassportCallEvent()
	{
		CallEvent current = getCurrentCallEvent();
		
		if( current != null )
		{
			return (PassportCallEvent)current;
		}
		
		return null;
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
