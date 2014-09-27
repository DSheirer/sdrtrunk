package source.tuner;

public class FrequencyChangeEvent
{
	public enum Attribute
	{
		SAMPLE_RATE_ERROR,
		BANDWIDTH,
		FREQUENCY,
		FREQUENCY_ERROR,
		SAMPLE_RATE;
	}

	private Attribute mAttribute;
	private long mValue;
	
	public FrequencyChangeEvent( Attribute attribute, long value )
	{
		mAttribute = attribute;
		mValue = value;
	}
	
	public Attribute getAttribute()
	{
		return mAttribute;
	}
	
	public long getValue()
	{
		return mValue;
	}
	
	
}
