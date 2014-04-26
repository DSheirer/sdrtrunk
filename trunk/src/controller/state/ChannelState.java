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
package controller.state;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JTable;

import log.Log;
import message.Message;
import properties.SystemProperties;
import sample.Broadcaster;
import sample.Listener;
import alias.AliasList;
import audio.AudioType;
import audio.AudioTypeListener;
import audio.SquelchListener;
import audio.SquelchListener.SquelchState;
import controller.activity.CallEvent;
import controller.activity.CallEvent.CallEventType;
import controller.activity.CallEventAliasCellRenderer;
import controller.activity.CallEventModel;
import controller.activity.MessageActivityModel;
import controller.channel.Channel.ChannelType;
import controller.channel.ProcessingChain;

/**
 * ChannelState provides a state machine for tracking a voice or data call
 * through it's life cycle of CALL/DATA, FADE, and then END, and then back to
 * an IDLE state.  This class also provides squelch control for unsquelching
 * sound to the speakers, and/or sound to an audio recorder
 * 
 * State Descriptions:
 *  IDLE		No voice or data call activity
 * 	CALL/DATA	A voice or data call has started, or is continuing
 *  CONTROL     Control channel
 *  FADE		The phase after a voice or data call when either an explicit
 *  			call end has been received, or when no new signalling updates
 *  			have been received, and the fade timer has expired.  This phase
 *  			allows for gui updates to signal to the user that the call is
 *  			ended, while continuing to display the call details for the user
 *  END			The phase after the voice or data call FADE, when the call is
 *  			now over, and the gui should be reset to IDLE.
 * 
 * When a call is started, invoke mTimerService.startFadeTimer() to start the 
 * call fade monitoring timer process that will automatically signal a call end
 * once no more call signalling has been detected after the fade timeout has
 * occurred, by invoking the fade() method.
 * 
 * When a call ends, invoke the mTimerService.stopFadeTimer() and then invoke
 * the mTimerService.startResetTimer().  Once the reset timeout occurs, 
 * the reset() method will be called, allowing the channel state to perform
 * any reset actions, like clearing status and id labels on any view panels.
 * 
 * AuxChannelState class allows for auxiliary decoders like Fleetsync to both
 * have a state and leverage the state machine of the parent ChannelState.  
 * This allows a non-squelching channel state like for conventional fm to have
 * an aux decoder like fleetsync, and allow the fleetsync auxchannelstate to 
 * leverage the call/end states of the parent conventional channelstate.
 *
 */
public abstract class ChannelState implements Listener<Message>
{
	private State mState = State.IDLE;
	private Object mCallFadeLock = new Object();
	private Object mCallResetLock = new Object();
	private long mFadeTimeout = -1;
	private long mResetTimeout = -1;
	
	protected long mCallFadeTimeout;
	protected long mCallResetTimeout;	
	protected TimerService mTimerService = new TimerService();
	protected ArrayList<SquelchListener> mSquelchListeners =
				new ArrayList<SquelchListener>();
	protected ArrayList<AuxChannelState> mAuxChannelStates =
			new ArrayList<AuxChannelState>();
	protected Broadcaster<ChangedAttribute> mChangeBroadcaster =
			new Broadcaster<ChangedAttribute>();
	protected AudioTypeListener mAudioTypeListener;
	protected AudioType mAudioType = AudioType.NORMAL;
	
	
	protected CallEventModel mCallEventModel = new CallEventModel();

	protected JTable mCallEventTable;
	
	protected MessageActivityModel mMessageActivityModel = 
					new MessageActivityModel();
	protected JTable mMessageActivityTable;
	
	protected ProcessingChain mProcessingChain;

	protected AliasList mAliasList;
	
	public ChannelState( ProcessingChain processingChain, AliasList aliasList )
	{
		mProcessingChain = processingChain;
		mAliasList = aliasList;

		
		/* Register to receive decoded messages from the processing chain */
		mProcessingChain.addListener( this );
		
		SystemProperties props = SystemProperties.getInstance();
		mCallFadeTimeout = props.get( "call.fade.timeout", 2000 );
		mCallResetTimeout = props.get( "call.reset.timeout", 4000 );
	}
	
	/**
	 * Allows setting the call event model to use for traffic channels, so that
	 * all traffic channels can share the parent control channel call event
	 * model.
	 * @param model
	 */
	public void setCallEventModel( CallEventModel model )
	{
		mCallEventModel = model;
	}
	
	public CallEventModel getCallEventModel()
	{
		return mCallEventModel;
	}
	
	public void receiveCallEvent( CallEvent event )
	{
		mCallEventModel.add( event );
	}
	
	public void dispose()
	{
		mTimerService.dispose();
		
		mAuxChannelStates.clear();
		mSquelchListeners.clear();
		mChangeBroadcaster.dispose();
		mChangeBroadcaster = null;

		if( mProcessingChain.getChannel().getChannelType() == ChannelType.STANDARD )
		{
			mCallEventModel.dispose();
		}
		/* If we're a traffic channel, set the call event model to null so that
		 * we don't dispose the parent control channel's call event model */
		else
		{
			mCallEventModel = null;
		}

		mMessageActivityModel.dispose();
		mMessageActivityModel = null;
		
		mAliasList = null;
		mAudioTypeListener = null;
		mCallEventModel = null;
		mProcessingChain = null;
	}

	/**
	 * Channel Activity Summary - implemented by the sub-class.
	 */
	public abstract String getActivitySummary();

	public AliasList getAliasList()
	{
		return mAliasList;
	}
	
	public boolean hasAliasList()
	{
		return mAliasList != null;
	}
	
	public void broadcastChange( ChangedAttribute attribute )
	{
		if( mChangeBroadcaster != null )
		{
			mChangeBroadcaster.broadcast( attribute );
		}
	}
	
	public void setCallFadeTimeout( long milliseconds )
	{
		mCallFadeTimeout = milliseconds;
	}
	
	public JTable getCallEventTable()
	{
		if( mCallEventTable == null )
		{
			mCallEventTable = new JTable( mCallEventModel );
			mCallEventTable.setAutoCreateRowSorter( true );
			mCallEventTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );

			CallEventAliasCellRenderer renderer = 
					new CallEventAliasCellRenderer( mProcessingChain
							.getResourceManager().getSettingsManager() );
			
			mCallEventTable.getColumnModel().getColumn( 3 )
							.setCellRenderer( renderer );

			mCallEventTable.getColumnModel().getColumn( 5 )
							.setCellRenderer( renderer );
		}
		
		return mCallEventTable;
	}
	
	/**
	 * Returns the message activity model wrapped in a JTable.  Although this
	 * may seem to violate separation between model and view, it simplifies
	 * retaining the JTable state for each channel by keeping a local reference
	 * to reuse
	 */
	public JTable getMessageActivityTable()
	{
		if( mMessageActivityTable == null )
		{
			mMessageActivityTable = new JTable( mMessageActivityModel );
			mMessageActivityTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
		}

		return mMessageActivityTable;
	}
	
	public void addAuxChannelState( AuxChannelState state )
	{
		mAuxChannelStates.add( state );
	}
	
	public ArrayList<AuxChannelState> getAuxChannelStates()
	{
		return mAuxChannelStates;
	}
	
	public void receive( Message message )
	{
		if( mMessageActivityModel != null )
		{
			mMessageActivityModel.receive( message );
		}

		for( AuxChannelState state: mAuxChannelStates )
		{
			if( state != null )
			{
				state.receive( message );
			}
		}
	}
	
	public ProcessingChain getProcessingChain()
	{
		return mProcessingChain;
	}
	
	public State getState()
	{
		return mState;
	}
	
	public void setState( State state )
	{
		/* Let all CALL state changes go through in order to update the fade
		 * timer, otherwise, don't update the state if it hasn't changed */
		if( mState != state || state == State.CALL )
		{
			if( mState.canChangeTo( state ) )
			{
				mState = state;

				switch( state )
				{
					case CALL:
						setSquelchState( SquelchState.UNSQUELCH );
					case DATA:
						mTimerService.stopResetTimer();
						mTimerService.startFadeTimer();
						break;
					case NO_TUNER:
						mTimerService.stopResetTimer();
						if( getProcessingChain().getChannel()
								.getChannelType() == ChannelType.TRAFFIC )
						{
							mTimerService.startFadeTimer();
						}
						break;
					case FADE:
						mTimerService.stopFadeTimer();
						mTimerService.startResetTimer();
						setSquelchState( SquelchState.SQUELCH );
						if( mAudioTypeListener != null )
						{
							mAudioTypeListener.setAudioType( mAudioType );
						}
						break;
					case END:
						mTimerService.stopResetTimer();
						mTimerService.stopFadeTimer();
						setSquelchState( SquelchState.SQUELCH );
						if( mAudioTypeListener != null )
						{
							mAudioTypeListener.setAudioType( mAudioType );
						}
						
						/* If this is a traffic channel, stop it */
						if( mProcessingChain.getChannel()
								.getChannelType() == ChannelType.TRAFFIC )
						{
							mProcessingChain.getChannel().setEnabled( false );
						}
						break;
					case IDLE:
						break;
					default:
						break;
				}

				broadcastChange( ChangedAttribute.CHANNEL_STATE );
			}
			else
			{
				Log.error( "Channel State - can't change from [" + 
					mState.toString() + "] to [" + state.toString() + "]" );
			}
		}
	}

	/**
	 * End method to reset the channel state once a call has completely
	 * ended and now you want to return the channel state to idle
	 */
	protected void reset()
	{
		setState( State.END );
		
		for( AuxChannelState state: mAuxChannelStates )
		{
			state.reset();
		}
		
		//Reset the state to idle
		setState( State.IDLE );
	}
	
	/**
	 * Method to perform any call fade actions.  On fade, you should change the
	 * channel state to fading, and construct a call-reset timer to execute a few
	 * seconds later.  This allows the view to signal to the user that the call
	 * has faded/ended, and display faded information to the user, until the 
	 * cleanup() task is run.
	 */
	protected void fade( CallEventType type )
	{
		for( AuxChannelState state: mAuxChannelStates )
		{
			state.fade();
		}
		
		setState( State.FADE );
	}
	
	public void setListener( AudioTypeListener listener )
	{
		mAudioTypeListener = listener;
	}
	
	public void addListener( SquelchListener listener )
	{
		mSquelchListeners.add( listener );
	}
	
	public void removeListener( SquelchListener listener )
	{
		mSquelchListeners.remove( listener );
	}
	
	protected void setSquelchState( SquelchState state )
	{
		Iterator<SquelchListener> it = mSquelchListeners.iterator();
		
		while( it.hasNext() )
		{
			it.next().setSquelch( state );
		}
	}

	/**
	 * Timer service
	 */
	public class TimerService extends ScheduledThreadPoolExecutor
	{
		private long mTimerMonitorRevisitRate = 100; //100 milliseconds
		
		private ScheduledFuture<?> mTimerMonitor;
		
		public TimerService()
        {
	        super( 1 );
        }
		
		public void dispose()
		{
			if( mTimerMonitor != null )
			{
				mTimerMonitor.cancel( true );
			}
			
			this.shutdownNow();
		}
		
		private void checkMonitor()
		{
			boolean createTimer = false;
			
			synchronized( mCallFadeLock )
			{
				if( mFadeTimeout != -1 )
				{
					createTimer = true;
				}
			}
			
			synchronized( mCallResetLock )
			{
				if( mResetTimeout != -1 )
				{
					createTimer = true;
				}
			}
			
			if( createTimer )
			{
				//Check to see if the timer is already running, if not, start it
				
				if( mTimerMonitor == null || mTimerMonitor.isDone() )
				{
					scheduleAtFixedRate( new StateTimeoutMonitor(),
										 mTimerMonitorRevisitRate,
										 mTimerMonitorRevisitRate,
										 TimeUnit.MILLISECONDS );
				}
			}
			else
			{
				if( mTimerMonitor != null )
				{
					mTimerMonitor.cancel( true );
				}
			}
		}
		
		public void startFadeTimer()
		{
			synchronized( mCallFadeLock )
			{
				mFadeTimeout = System.currentTimeMillis() + mCallFadeTimeout;
			}
			
			checkMonitor();
		}
		
		public void resetFadeTimer()
		{
			startFadeTimer();
		}
		
		public void stopFadeTimer()
		{
			synchronized( mCallFadeLock )
			{
				mFadeTimeout = -1;
			}
			
			checkMonitor();
		}
		
		public void startResetTimer()
		{
			synchronized( mCallResetLock )
			{
				mResetTimeout = System.currentTimeMillis() + mCallResetTimeout;
			}
			
			checkMonitor();
		}
		
		public void stopResetTimer()
		{
			synchronized( mCallResetLock )
			{
				mResetTimeout = -1;
			}
			
			checkMonitor();
		}
	}
	
	public class StateTimeoutMonitor implements Runnable
	{
		@Override
        public void run()
        {
			long now = System.currentTimeMillis();
			
			synchronized( mCallFadeLock )
			{
				if( mFadeTimeout != -1 && now >= mFadeTimeout )
				{
					mFadeTimeout = -1;
					
					fade( CallEventType.CALL_TIMEOUT );
				}
			}
			
			synchronized( mCallResetLock )
			{
				if( mResetTimeout != -1 && now >= mResetTimeout )
				{
					mResetTimeout = -1;
					
					reset();
				}
			}
        }
	}

	
	public enum State
	{ 
		IDLE( "IDLE" ) {
	        @Override
	        public boolean canChangeTo( State state )
	        {
		        return state == IDLE ||
		        	   state == CALL ||
		        	   state == CONTROL ||
		        	   state == DATA ||
		        	   state == FADE ||
		        	   state == NO_TUNER;
	        }
        }, 
		CALL( "CALL" ) {
	        @Override
	        public boolean canChangeTo( State state )
	        {
		        return state == CALL ||
		        	   state == CONTROL ||
		        	   state == DATA ||
		        	   state == FADE;
	        }
        }, 
		DATA( "DATA" ) {
	        @Override
	        public boolean canChangeTo( State state )
	        {
		        return state == CALL ||
		        	   state == CONTROL ||
			           state == DATA ||
			           state == FADE;
	        }
        },
        CONTROL( "CONTROL" )
        {
			@Override
            public boolean canChangeTo( State state )
            {
	            return true;
            }
        },
		FADE( "FADE" ) {
	        @Override
	        public boolean canChangeTo( State state )
	        {
		        return state != FADE; //All states except fade allowed
	        }
        },
		END( "END" ) {
	        @Override
	        public boolean canChangeTo( State state )
	        {
		        return state != FADE;  //Fade is only disallowed state
	        }
        },
        NO_TUNER( "NO TUNER" ) {
	        @Override
	        public boolean canChangeTo( State state )
	        {
		        return state == FADE || state == END;
	        }
        };
		
		private String mDisplayValue;
		
		private State( String displayValue )
		{
			mDisplayValue = displayValue;
		}
		
		public abstract boolean canChangeTo( State state );
		
		public String getDisplayValue()
		{
			return mDisplayValue;
		}
	}
	
	/**
	 * Registers a listener to receive channel state attribute changes
	 */
	public void addListener( Listener<ChangedAttribute> listener )
	{
	    mChangeBroadcaster.addListener( listener );
	}
	
	/**
	 * Removes a listener from receiving channel state attribute changes
	 */
	public void removeListener( Listener<ChangedAttribute> listener )
	{
	    mChangeBroadcaster.removeListener( listener );
	}
	
    public enum ChangedAttribute
    {
    	AUDIO_TYPE,
        CHANNEL_NUMBER,
        CHANNEL_NAME,
        CHANNEL_SITE_NUMBER,
        CHANNEL_SITE_NUMBER_ALIAS,
        CHANNEL_STATE,
        FROM_TALKGROUP,
        FROM_TALKGROUP_ALIAS,
        FROM_TALKGROUP_TYPE,
        MESSAGE,
        MESSAGE_TYPE,
        SITE_NAME,
        SOURCE,
        SYSTEM_NAME,
        SQUELCH,
        TO_TALKGROUP,
        TO_TALKGROUP_ALIAS,
        TO_TALKGROUP_TYPE;
    }
}
