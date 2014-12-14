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
package decode.ltrstandard;

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
import decode.ltrnet.LTRCallEvent;

public class LTRChannelState extends ChannelState
{
	private String mTalkgroup;
	private Alias mTalkgroupAlias;
    private int mChannelNumber;
    private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();
    private LTRStandardActivitySummary mActivitySummary;
	
	public LTRChannelState( ProcessingChain channel, AliasList aliasList )
	{
		super( channel, aliasList );
		
		mActivitySummary = new LTRStandardActivitySummary( aliasList );
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
		
		if( message instanceof LTRStandardOSWMessage )
		{
			mActivitySummary.receive( message );
			
			LTRStandardOSWMessage ltr = (LTRStandardOSWMessage)message;
			
			switch( ltr.getMessageType() )
			{
				/**
				 * Process Call Message
				 * 
				 * States:
				 * 1) Call message with invalid LCN
				 * 2) State=IDLE/FADE, new CA_STRT
				 * 3) State=CALL, new CA_STRT, same talkgroup
				 * 4) State=CALL, new CA_STRT, different talkgroup
				 */
				case CA_STRT:
				    if( 0 < ltr.getChannel() && ltr.getChannel() <=20 )
				    {
				    	/* If we haven't captured the LCN yet ... */
				    	if( mChannelNumber == 0 )
				    	{
                            mChannelNumber = ltr.getChannel();
                            broadcastChange( ChangedAttribute.CHANNEL_NUMBER );
				    	}

                        /* If this call event is for this channel, then update
                         * talkgroup and alias */
                        if( ltr.getChannel() == mChannelNumber )
                        {
                            mTalkgroup = ltr.getTalkgroupID();
                            broadcastChange( ChangedAttribute.FROM_TALKGROUP );
                            
                            mTalkgroupAlias = ltr.getTalkgroupIDAlias();
                            broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );
                            
                        	LTRCallEvent current = getCurrentLTRCallEvent();
                        	
                        	/* If we have an ongoing call, then add this message
                        	 * to the current event - if addMessage returns 
                        	 * false, then we have a different talkgroup and a
                        	 * new call event, so close out the old one. */
                        	if( current != null && !current.addMessage( ltr ) )
                        	{
                    			setState( State.FADE );
                        	}
                        	
                    		if( current == null )
                    		{
    	                        LTRCallEvent event = 
    	                        		new LTRCallEvent.Builder( CallEventType.CALL )
        	                            .to( ltr.getTalkgroupID() )
        	                            .aliasList( getAliasList() )
        	                            .channel( String.valueOf( ltr.getChannel() ) )
        	                            .frequency( ( ltr.getChannel() == mChannelNumber ) ? 
        	                                    mProcessingChain.getChannel()
        	                                        .getTunerChannel().getFrequency() : 0 )
        	                            .build();
        	                        
        	                        mCallEventModel.add( event );

        	                        setCurrentCallEvent( event );
                    		}
                        
                        	setState( State.CALL );
                        }
				    }
					break;
				case CA_ENDD:
                    mTalkgroup = ltr.getTalkgroupID();
                    broadcastChange( ChangedAttribute.FROM_TALKGROUP );

                    mTalkgroupAlias = ltr.getTalkgroupIDAlias();
                    broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );

					fade( CallEventType.CALL_END );
					break;
				case SY_IDLE:
				    int channel = ltr.getChannel();
				    
				    if( 0 < channel && channel <= 20 && 
				        channel == ltr.getHomeRepeater() &&
				        channel == ltr.getFree() )
				    {
	                    mChannelNumber = channel;
                        broadcastChange( ChangedAttribute.CHANNEL_NUMBER );
				    }
				    break;
				default:
					break;
			}
		}
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
	
    /**
     * Intercept the fade event so that we can generate a call end event
     */
    @Override
    public void fade( final CallEventType type )
    {
        /* We can receive multiple call tear-down messages -- only respond to
         * the message that can change the state to fade */
        if( getState().canChangeTo( State.FADE ) )
        {
            mActiveCalls.clear();
            
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
            
    		setCurrentCallEvent( null );
        }
        
        super.fade( type );
    }
    
	public void reset()
	{
		mTalkgroup = null;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP );

		mTalkgroupAlias = null;
        broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );

		super.reset();
	}
	
	public String getTalkgroup()
	{
		return mTalkgroup;
	}
	
	public Alias getTalkgroupAlias()
	{
		return mTalkgroupAlias;
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
