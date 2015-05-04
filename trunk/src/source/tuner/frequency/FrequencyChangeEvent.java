package source.tuner.frequency;

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
	private Number mValue;
	
	public FrequencyChangeEvent( Attribute attribute, Number value )
	{
		mAttribute = attribute;
		mValue = value;
	}
	
	public Attribute getAttribute()
	{
		return mAttribute;
	}
	
	public Number getValue()
	{
		return mValue;
	}
}
