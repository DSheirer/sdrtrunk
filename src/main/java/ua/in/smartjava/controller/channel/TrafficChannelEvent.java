package ua.in.smartjava.controller.channel;

import ua.in.smartjava.module.decode.event.CallEvent;
import ua.in.smartjava.channel.traffic.TrafficChannelManager;

public class TrafficChannelEvent extends ChannelEvent
{
	private TrafficChannelManager mTrafficChannelManager;
	private CallEvent mCallEvent;
	
	/**
	 * Call event with traffic ua.in.smartjava.channel manager for call back, and original creation event.
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
