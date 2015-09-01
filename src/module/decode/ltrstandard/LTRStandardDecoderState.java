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
package module.decode.ltrstandard;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import module.decode.event.CallEvent.CallEventType;
import module.decode.ltrnet.LTRCallEvent;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;
import module.decode.state.DecoderStateEvent.Event;
import module.decode.state.State;
import alias.Alias;
import alias.AliasList;

public class LTRStandardDecoderState extends DecoderState
{
	private static final int sINT_NULL_VALUE = -1;
	private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");

    private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();
	private HashSet<String> mTalkgroupsFirstHeard = new HashSet<String>();
	private TreeSet<String> mTalkgroups = new TreeSet<String>();
	private TreeSet<Integer> mActiveLCNs = new TreeSet<Integer>();
	private int mMonitoredLCN;
	
	private String mTalkgroup;
	private Alias mTalkgroupAlias;
    private int mChannelNumber;
    
    private long mFrequency;
	
	public LTRStandardDecoderState( AliasList aliasList )
	{
		super( aliasList );
	}
	
	private LTRCallEvent getCurrentCallEvent()
	{
		return (LTRCallEvent)mCurrentCallEvent;
	}

	@Override
    public void receive( Message message )
    {
		if( message.isValid() )
		{
			if( message instanceof LTRStandardOSWMessage )
			{
				LTRStandardOSWMessage ltr = ((LTRStandardOSWMessage)message);
				
				switch( ltr.getMessageType() )
				{
					case CA_STRT:
					    if( 0 < ltr.getChannel() && ltr.getChannel() <=20 )
					    {
					    	/* If we haven't captured the LCN yet ... */
					    	if( mChannelNumber == 0 )
					    	{
	                            mChannelNumber = ltr.getChannel();
	                            broadcast( ChangedAttribute.CHANNEL_NUMBER );
					    	}

	                        /* If this call event is for this channel, then update
	                         * talkgroup and alias */
	                        if( ltr.getChannel() == mChannelNumber )
	                        {
	                            mTalkgroup = ltr.getTalkgroupID();
	                            broadcast( ChangedAttribute.TO_TALKGROUP );
	                            
	                            mTalkgroupAlias = ltr.getTalkgroupIDAlias();
	                            broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );
	                            
	                        	LTRCallEvent current = getCurrentCallEvent();
	                        	
	                        	/* If we have an ongoing call, then add this message
	                        	 * to the current event - if addMessage returns 
	                        	 * false, then we have a different talkgroup and a
	                        	 * new call event, so close out the old one. */
	                        	if( current != null && !current.addMessage( ltr ) )
	                        	{
	                        		mCurrentCallEvent.setEnd( System.currentTimeMillis() );
	                        		broadcast( mCurrentCallEvent );
	                        		mCurrentCallEvent = null;
	                        	}
	                        	
	                    		if( current == null )
	                    		{
	                    			mCurrentCallEvent = 
    	                        		new LTRCallEvent.Builder( CallEventType.CALL )
        	                            .to( ltr.getTalkgroupID() )
        	                            .aliasList( getAliasList() )
        	                            .channel( String.valueOf( ltr.getChannel() ) )
        	                            .frequency( mFrequency )
        	                            .build();

	                    			broadcast( mCurrentCallEvent );
	                    			
		                    		broadcast( new DecoderStateEvent( this, Event.START, State.CALL ) );
	                    		}
	                    		else
	                    		{
		                    		broadcast( new DecoderStateEvent( this, Event.CONTINUATION, State.CALL ) );
	                    		}
	                        }
					    }
						break;
					case CA_ENDD:
	                    mTalkgroup = ltr.getTalkgroupID();
	                    broadcast( ChangedAttribute.FROM_TALKGROUP );

	                    mTalkgroupAlias = ltr.getTalkgroupIDAlias();
	                    broadcast( ChangedAttribute.FROM_TALKGROUP_ALIAS );

	                    if( mCurrentCallEvent != null )
	                    {
	                    	mCurrentCallEvent.setEnd( System.currentTimeMillis() );
	                    	broadcast( mCurrentCallEvent );
                    		broadcast( new DecoderStateEvent( this, Event.END, State.FADE ) );
                    		mCurrentCallEvent = null;
	                    }

						int home = ltr.getHomeRepeater();
					    
					    if( 0 < home && home <= 20 )
					    {
	                        String talkgroup = ltr.getTalkgroupID( false );
	                        
	                        if( mTalkgroupsFirstHeard.contains( talkgroup ) )
	                        {
	                            mTalkgroups.add( talkgroup );
	                        }
	                        else
	                        {
	                            mTalkgroupsFirstHeard.add( talkgroup );
	                        }
					    }

					    int channel = ltr.getChannel();
					    
					    if( 0 < channel && channel <= 20 )
					    {
	                        if( channel == home )
	                        {
	                        	if( mMonitoredLCN != channel )
	                        	{
		                            mMonitoredLCN = channel;
		                            broadcast( ChangedAttribute.CHANNEL_NUMBER );
	                        	}
	                        	
	                            mActiveLCNs.add( channel );
	                        }
	                        
	                        int free = ltr.getFree();
	                        
	                        if( 0 < free && free <= 20 )
	                        {
	                            mActiveLCNs.add( free );
	                        }
					    }

						break;
					case SY_IDLE:
					    int idleChannel = ltr.getChannel();
					    
					    if( idleChannel == ltr.getHomeRepeater() &&
					        idleChannel == ltr.getFree() )
					    {
                        	if( mMonitoredLCN != idleChannel )
                        	{
	                            mMonitoredLCN = idleChannel;
	                            broadcast( ChangedAttribute.CHANNEL_NUMBER );
                        	}
                        	
	                        mActiveLCNs.add( idleChannel );
					    }
					    broadcast( new DecoderStateEvent( this, Event.CONTINUATION, State.IDLE ) );
						break;
					case UN_KNWN:
					default:
						break;
				}
			}
		}
    }
	
//    /**
//     * Intercept the fade event so that we can generate a call end event
//     */
//    @Override
//    public void fade( final CallEventType type )
//    {
//        /* We can receive multiple call tear-down messages -- only respond to
//         * the message that can change the state to fade */
//        if( getState().canChangeTo( State.FADE ) )
//        {
//            mActiveCalls.clear();
//            
//            LTRCallEvent current = getCurrentLTRCallEvent();
//            
//            if( current != null )
//            {
//            	/* Close out the call event.  If we only received 1 call 
//            	 * message, then flag it as noise and remove the call event */
//            	if( current.isValid() )
//            	{
//            		mCallEventModel.setEnd( current );
//            	}
//            	else
//            	{
//            		mCallEventModel.remove( current );
//            	}
//            }
//            
//    		setCurrentCallEvent( null );
//        }
//        
//        super.fade( type );
//    }
    
	public void reset()
	{
		resetState();
		
		if( mCurrentCallEvent != null )
		{
			mCurrentCallEvent.setEnd( System.currentTimeMillis() );
			broadcast( mCurrentCallEvent );
			mCurrentCallEvent = null;
		}
		
	}
	
	private void resetState()
	{
		mTalkgroup = null;
		broadcast( ChangedAttribute.FROM_TALKGROUP );

		mTalkgroupAlias = null;
        broadcast( ChangedAttribute.FROM_TALKGROUP_ALIAS );
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

	@Override
	public void init()
	{
		
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
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "Activity Summary\n\n" );
		
		sb.append( "Decoder:\tLTR-Standard\n" );
		
		sb.append( "Monitored LCN: " );
		
		if( mMonitoredLCN > 0 )
		{
	        sb.append( mMonitoredLCN );
		}
		else
		{
		    sb.append( "unknown" );
		}
		
		sb.append( "\n" );
		
		Integer[] lcns = mActiveLCNs.toArray( new Integer[0] );
		
		sb.append( "Active LCNs:\t" );
		
		if( lcns.length > 0 )
		{
	        sb.append(  Arrays.toString( lcns ) );
		}
		else
		{
		    sb.append( "none" );
		}
		sb.append( "\n\n" );

		sb.append( "Talkgroups\n" );
		
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
				sb.append( formatTalkgroup( tgid ) );
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
		
	    return sb.toString();
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
	
}
