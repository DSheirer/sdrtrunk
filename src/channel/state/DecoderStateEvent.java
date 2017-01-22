package channel.state;

public class DecoderStateEvent
{
	private Object mSource;
	private Event mEvent;
	private State mState;
	private long mFrequency;
	
	public DecoderStateEvent( Object source, Event event, State state )
	{
		mSource = source;
		mEvent = event;
		mState = state;
	}
	
	public DecoderStateEvent( Object source, Event event, State state, long frequency )
	{
		this( source, event, state );
		
		mFrequency = frequency;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Decoder State Event - source[" + mSource.getClass() + 
				"] event[" + mEvent.toString() + 
				"] state[" + mState.toString() +
				"] frequency [" + mFrequency + "]" );
		
		return sb.toString();
	}
	
	public Object getSource()
	{
		return mSource;
	}
	
	public Event getEvent()
	{
		return mEvent;
	}
	
	public State getState()
	{
		return mState;
	}
	
	public long getFrequency()
	{
		return mFrequency;
	}
	
	public enum Event
	{
		ALWAYS_UNSQUELCH,
		CHANGE_CALL_TIMEOUT,
		CONTINUATION,
		DECODE,
		END,
		RESET,
		SOURCE_FREQUENCY,
		START,
		TRAFFIC_CHANNEL_ALLOCATION;
	}
}