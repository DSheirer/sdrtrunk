package controller.channel;

import module.decode.event.CallEvent;
import channel.traffic.TrafficChannelManager;

public class TrafficChannelEvent extends ChannelEvent
{
	private TrafficChannelManager mTrafficChannelManager;
	private CallEvent mCallEvent;
	
	/**
	 * Call event with traffic channel manager for call back, and original creation event.
	 */
	public TrafficChannelEvent( TrafficChannelManager trafficChannelManager,
								Channel channel, 
								Event event, 
								CallEvent callEvent )
	{
		super( channel, event );
		
		mTrafficChannelManager = trafficChannelManager;
		mCallEvent = callEvent;
	}
	
	public TrafficChannelManager getTrafficChannelManager()
	{
		return mTrafficChannelManager;
	}
	
	public CallEvent getCallEvent()
	{
		return mCallEvent;
	}
}
