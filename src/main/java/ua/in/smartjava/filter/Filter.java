package ua.in.smartjava.filter;

import java.util.List;

public abstract class Filter<T> implements IFilter<T>
{
	protected String mName;
	protected boolean mEnabled = true;
	
	public Filter( String name )
	{
		mName = name;
	}

	/**
	 * Indicates if this ua.in.smartjava.filter is enabled or disabled
	 */
	public boolean isEnabled()
	{
		return mEnabled;
	}

	/**
	 * Enables (true) or disables (false) this ua.in.smartjava.filter.  An enabled ua.in.smartjava.filter will
	 * evaluate messages and a disabled ua.in.smartjava.filter will return false on all ua.in.smartjava.message
	 * test methods.
	 */
	public void setEnabled( boolean enabled )
	{
		mEnabled = enabled;
	}
	
	/**
	 * List of ua.in.smartjava.filter elements managed by this ua.in.smartjava.filter
	 */
	public abstract List<FilterElement<?>> getFilterElements();

	/**
	 * Name of this ua.in.smartjava.filter
	 */
	public String getName()
	{
		return mName;
	}
	
	public void setName( String name )
	{
		mName = name;
	}
	
	public String toString()
	{
		return mName;
	}
}
