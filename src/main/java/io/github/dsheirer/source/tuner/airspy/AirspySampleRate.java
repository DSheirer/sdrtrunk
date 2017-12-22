package io.github.dsheirer.source.tuner.airspy;

public class AirspySampleRate
{
	private int mIndex;
	private int mValue;
	private String mLabel;
	
	public AirspySampleRate( int index, int value, String label )
	{
		mIndex = index;
		mValue = value;
		mLabel = label;
	}
	
	public int getIndex()
	{
		return mIndex;
	}
	
	public int getRate()
	{
		return mValue;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public String toString()
	{
		return getLabel();
	}
}
