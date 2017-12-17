package io.github.dsheirer.instrument.tap;

import java.util.ArrayList;
import java.util.List;

public class TapGroup
{
	private String mName;
	private List<Tap> mTaps = new ArrayList<>();
	
	public TapGroup( String name )
	{
		mName = name;
	}
	
	public String getName()
	{
		return mName;
	}
	
	public String toString()
	{
		return getName();
	}

	public List<Tap> getTaps()
	{
		return mTaps;
	}

	public void add( Tap tap )
	{
		mTaps.add( tap );
	}
}
