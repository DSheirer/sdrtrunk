package io.github.dsheirer.source.tuner.hydrasdr;

public class HydraSdrSampleRate
{
	private int mIndex;
	private int mValue;
	private String mLabel;

	public HydraSdrSampleRate( int index, int value, String label )
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
