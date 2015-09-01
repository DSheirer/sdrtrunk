package module.decode.state;

public class TrafficChannelStatusListener
{
	private TrafficChannelManager mTrafficChannelManager;
	private String mChannelNumber;
	
	public TrafficChannelStatusListener( TrafficChannelManager manager, String channelNumber )
	{
		mTrafficChannelManager = manager;
		mChannelNumber = channelNumber;
	}
	
	public void callEnd()
	{
		if( mTrafficChannelManager != null )
		{
			mTrafficChannelManager.callEnd( mChannelNumber );
		}
	}
}
