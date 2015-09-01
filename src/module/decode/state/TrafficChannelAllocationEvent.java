package module.decode.state;

import module.decode.event.CallEvent;

/**
 * Traffic channel allocation event wraps a call event indicating the channel
 * and frequency for a traffic channel allocation event, so that the traffic
 * channel manager can respond and allocate a decoder channel and source if 
 * available, or change the wrapped call event to a call detect event and log it.
 */
public class TrafficChannelAllocationEvent extends DecoderStateEvent
{
	private CallEvent mCallEvent;
	
	public TrafficChannelAllocationEvent( Object source, CallEvent callEvent )
	{
		super( source, Event.TRAFFIC_CHANNEL_ALLOCATION, State.CALL );
		mCallEvent = callEvent;
	}
	
	public CallEvent getCallEvent()
	{
		return mCallEvent;
	}
}
